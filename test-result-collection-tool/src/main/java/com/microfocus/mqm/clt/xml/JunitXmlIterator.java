/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

package com.microfocus.mqm.clt.xml;

import com.microfocus.mqm.clt.tests.TestResult;
import com.microfocus.mqm.clt.tests.TestResultStatus;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.ValidationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class JunitXmlIterator extends AbstractXmlIterator<TestResult> {

    private String packageName;
    private String className;
    private String testName;
    private TestResultStatus status;
    private long duration;
    private long started;

    private String stackTraceStr;
    private String errorType;
    private String errorMsg;
    private boolean allowStackTraceAggregation;


    public JunitXmlIterator(File junitXmlFile, Long started) throws XMLStreamException, ValidationException, IOException {
        super(junitXmlFile);
        this.started = (started == null) ? System.currentTimeMillis() : started;
    }

    @Override
    protected void onEvent(XMLEvent event) throws IOException {
        Iterator attrIterator;
        if (event instanceof StartElement) {
            StartElement element = (StartElement) event;
            String localName = element.getName().getLocalPart();
            if ("testcase".equals(localName)) { // NON-NLS
                packageName = "";
                className = "";
                testName = "";
                status = TestResultStatus.PASSED;
                duration = 0;

                attrIterator = element.getAttributes();
                while (attrIterator.hasNext()) {
                    Attribute attribute = (Attribute) attrIterator.next();
                    if ("classname".equals(attribute.getName().toString())) {
                        parseClassname(attribute.getValue());
                    } else if ("name".equals(attribute.getName().toString())) {
                        testName = restrictSizeTo255(attribute.getValue());
                    } else if ("time".equals(attribute.getName().toString())) {
                        duration = parseDuration(attribute.getValue());
                    }
                }
            } else if ("skipped".equals(localName)) { // NON-NLS
                status = TestResultStatus.SKIPPED;
            } else if ("failure".equals(localName) || "error".equals(localName)) { // NON-NLS
                allowStackTraceAggregation = true;
                status = TestResultStatus.FAILED;
                stackTraceStr = "";
                attrIterator = element.getAttributes();
                while (attrIterator.hasNext()) {
                    Attribute attribute = (Attribute) attrIterator.next();
                    if ("message".equals(attribute.getName().toString())) {
                        errorMsg = attribute.getValue();
                    } else if ("type".equals(attribute.getName().toString())) {
                        errorType = attribute.getValue();
                    }
                }
            }
        } else if (event instanceof EndElement) {
            EndElement element = (EndElement) event;
            String localName = element.getName().getLocalPart();
            allowStackTraceAggregation = false;
            if ("testcase".equals(localName) && StringUtils.isNotEmpty(testName)) { // NON-NLS
                TestResult tr = new TestResult(packageName, className, testName, status, duration, started);
                if(TestResultStatus.FAILED.equals(status)){
                    tr.setFailedInfo(errorType, errorMsg, stackTraceStr);
                }
                addItem(tr);
            }
        } else if (event instanceof Characters) {
            if (allowStackTraceAggregation) {
                stackTraceStr += ((Characters) event).getData();
            }
        }
    }

    private String restrictSizeTo255(String value) {
        int RESTRICT_SIZE = 255;
        String result = value;
        if (value != null && value.length() > RESTRICT_SIZE) {
            result = value.substring(0, RESTRICT_SIZE);
        }
        return result;
    }

    private long parseDuration(String timeString) {
        try {
            float seconds = Float.parseFloat(timeString);
            return (long) (seconds * 1000);
        } catch (NumberFormatException e) {
            System.out.println("Unable to parse test duration: " + timeString);
        }
        return 0;
    }

    private void parseClassname(String fqn) {
        int p = fqn.lastIndexOf(".");
        className = fqn.substring(p + 1);
        if (p > 0) {
            packageName = fqn.substring(0, p);
        } else {
            packageName = "";
        }
    }
}
