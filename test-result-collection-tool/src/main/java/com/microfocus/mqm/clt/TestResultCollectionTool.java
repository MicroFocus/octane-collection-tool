/*
 *     Copyright 2015-2023 Open Text
 *
 *     The only warranties for products and services of Open Text and
 *     its affiliates and licensors ("Open Text") are as may be set forth
 *     in the express warranty statements accompanying such products and services.
 *     Nothing herein should be construed as constituting an additional warranty.
 *     Open Text shall not be liable for technical or editorial errors or
 *     omissions contained herein. The information contained herein is subject
 *     to change without notice.
 *
 *     Except as specifically indicated otherwise, this document contains
 *     confidential information and a valid license is required for possession,
 *     use or copying. If this work is provided to the U.S. Government,
 *     consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *     Computer Software Documentation, and Technical Data for Commercial Items are
 *     licensed to the U.S. Government under vendor's standard commercial license.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.mqm.clt;

import com.microfocus.mqm.clt.Exception.ValidationException;
import com.microfocus.mqm.clt.tests.TestResult;
import com.microfocus.mqm.clt.tests.TestResultPushStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestResultCollectionTool {

    private Settings settings;
    private RestClient client;

    public TestResultCollectionTool(Settings settings) {
        this.settings = settings;
    }

    public void collectAndPushTestResults() {
        Map<File, String> publicApiXMLs = new LinkedHashMap<File, String>();
        if (settings.isInternal()) {
            for (String fileName : settings.getInputXmlFileNames()) {
                publicApiXMLs.put(new File(fileName), fileName);
            }
        } else if (settings.getOutputFile() != null) {
            processJunitReport(new File(settings.getInputXmlFileNames().get(0)), new File(settings.getOutputFile()));
            System.out.println("JUnit report was saved to the output file");
            System.exit(ReturnCode.SUCCESS.getReturnCode());
        } else {
            for (String fileName : settings.getInputXmlFileNames()) {
                File publicApiTempXML = null;
                try {
                    publicApiTempXML = File.createTempFile("testResult.xml", null);
                    publicApiTempXML.deleteOnExit();
                } catch (IOException e) {
                    System.out.println("Can not create temp file for test result");
                    System.exit(ReturnCode.FAILURE.getReturnCode());
                }
                processJunitReport(new File(fileName), publicApiTempXML);
                publicApiXMLs.put(publicApiTempXML, fileName);
            }
        }

        client = new RestClient(settings);
        try {
            for (Map.Entry<File, String> publicApiXML : publicApiXMLs.entrySet()) {
                long testResultId;
                try {
                    testResultId = client.postTestResult(new FileEntity(publicApiXML.getKey(), ContentType.APPLICATION_XML));
                } catch (ValidationException e) {
                    // One invalid public API XML should not stop the whole process when supplied externally
                    System.out.println("Test result from file '" + publicApiXML.getValue() + "' was not pushed");
                    System.out.println(e.getMessage());
                    continue;
                }
                if (settings.isCheckResult()) {
                    validatePublishResult(testResultId, publicApiXML.getValue());
                } else {
                    System.out.println("Test result from file '" + publicApiXML.getValue() + "' was pushed to the server with ID " + testResultId);
                }
            }
        } catch (IOException e) {
            releaseClient();
            System.out.println("Unable to push test result: " + e.getMessage());
            if(e.getMessage().contains("access_denied")){
                System.out.println("Validate proxy configuration.");
            }
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (RuntimeException e) {
            releaseClient();
            System.out.println("Unable to push test result: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } finally {
            releaseClient();
        }
    }

    private void releaseClient() {

        changeLogLevel(Level.SEVERE);
        //why we do it, in production, logout return cookie with path \msg that is rejected by java and WARNING log is printed.
        //Because its logout and anyway application is going to be closed, we don't want to show such kind of log messages
        //WARNING: Cookie rejected [JSESSIONID="1E3A86F611BDB687CC4C5BF04B73676E", version:0, domain:almoctane-eur.saas.hpe.com, path:/msg, expiry:null] Illegal 'path' attribute "/msg". Path of origin: "/authentication/sign_out"

        if (client != null) {
            try {
                client.release();
                settings.cleanSetting();
            } catch (IOException e) {
                System.out.println("Unable to release client session: " + e.getMessage());
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }
        }
     }

     private void changeLogLevel(Level level){
         Handler[] handlers = Logger.getLogger( "" ).getHandlers();
         for ( int index = 0; index < handlers.length; index++ ) {
             handlers[index].setLevel( level);

         }
     }

    private void validatePublishResult(long testResultId, String fileName) {
        TestResultPushStatus publishResult = null;
        try {
            publishResult = getPublishResult(testResultId);
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }
        if (publishResult == null) {
            System.out.println("Unable to verify publish result of the last push from file '" + fileName + "' with ID: " + testResultId);
        }
        System.out.println("Test result from file '" + fileName + "' was pushed to the server with ID " + testResultId + ", injection status is '" + publishResult.getStatus() + "'"
                + (StringUtils.isNotEmpty(publishResult.getErrorMessage()) ? ", error message is '" + publishResult.getErrorMessage() + "'" : ""));
    }

    private void processJunitReport(File junitReport, File outputFile) {
        XmlProcessor xmlProcessor = new XmlProcessor();
        List<TestResult> testResults = new LinkedList<TestResult>();
        testResults.addAll(xmlProcessor.processJunitTestReport(junitReport, settings.getStarted()));
        xmlProcessor.writeTestResults(testResults, settings, outputFile);
    }

    private TestResultPushStatus getPublishResult(long id) throws InterruptedException {
        TestResultPushStatus testResultPushStatus = null;
        int timeout = (settings.getCheckResultTimeout() != null) ? settings.getCheckResultTimeout() : 10;
        for (int i = 0; i < timeout * 10; i++) {
            testResultPushStatus = client.getTestResultStatus(id);
            String status = testResultPushStatus.getStatus();
            if (!"running".equals(status) && !"queued".equals(status)) {
                break;
            }
            Thread.sleep(100);
        }
        return testResultPushStatus;
    }
}
