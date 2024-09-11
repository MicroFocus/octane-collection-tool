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

import com.microfocus.mqm.clt.tests.TestResult;
import com.microfocus.mqm.clt.xml.JunitXmlIterator;
import com.microfocus.mqm.clt.xml.TestResultXmlWriter;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class XmlProcessor {

    public List<TestResult> processJunitTestReport(File junitTestReport, Long started) {
        if (junitTestReport == null || !junitTestReport.canRead()) {
            String filePathInfo = (junitTestReport == null) ? "" : ": " + junitTestReport.getAbsolutePath();
            System.out.println("Can not read the JUnit XML file" + filePathInfo);
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }

        List<TestResult> testResults = new LinkedList<TestResult>();
        try {
            JunitXmlIterator iterator = new JunitXmlIterator(junitTestReport, started);
            while (iterator.hasNext()) {
                testResults.add(iterator.next());
            }
        } catch (IOException e) {
            System.out.println("Unable to process JUnit XML file '" + junitTestReport.getAbsolutePath() + "': " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (XMLStreamException e) {
            System.out.println("Unable to process JUnit XML file '" + junitTestReport.getAbsolutePath() + "', XML stream exception has occurred: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (InterruptedException e) {
            System.out.println("Unable to process JUnit XML file '" + junitTestReport.getAbsolutePath() + "', thread was interrupted: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (RuntimeException e) {
            System.out.println("Unable to process JUnit XML file '" + junitTestReport.getAbsolutePath() + "', XSD validation was not successful: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }

        if (testResults.isEmpty()) {
            System.out.println("No valid test results to push in JUnit XML file '" + junitTestReport.getAbsolutePath() + "'");
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }
        return testResults;
    }

    public void writeTestResults(List<TestResult> testResults, Settings settings, File targetPath) {
        if (targetPath == null || !targetPath.canWrite()) {
            String filePathInfo = (targetPath == null) ? "" : ": " + targetPath.getAbsolutePath();
            System.out.println("Can not write test results to file" + filePathInfo);
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }
        TestResultXmlWriter testResultXmlWriter = new TestResultXmlWriter(targetPath);
        try {
            testResultXmlWriter.add(testResults, settings);
        } catch (InterruptedException e) {
            System.out.println("Unable to process test results to file '" + targetPath.getAbsolutePath() + "', thread was interrupted: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (XMLStreamException e) {
            System.out.println("Unable to process test results to file '" + targetPath.getAbsolutePath() + "', XML stream exception has occurred: " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } catch (IOException e) {
            System.out.println("Unable to process test results to file '" + targetPath.getAbsolutePath() + "': " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        } finally {
            try {
                testResultXmlWriter.close();
            } catch (XMLStreamException e) {
                System.out.println("Can not close the XML file'" + targetPath.getAbsolutePath() + "'" + e.getMessage());
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }
        }
    }
}
