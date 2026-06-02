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

package com.microfocus.mqm.clt.xml;

import com.microfocus.mqm.clt.tests.TestResult;
import com.microfocus.mqm.clt.tests.TestResultStatus;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Parses NUnit 3 XML test result files (root element: {@code <test-run>}) into
 * {@link TestResult} objects.
 *
 * <p>NUnit 3 XML structure (simplified):
 * <pre>
 * &lt;test-run&gt;
 *   &lt;test-suite ...&gt;
 *     &lt;test-case name="..." classname="..." result="Passed|Failed|Skipped|Inconclusive" duration="0.001"&gt;
 *       &lt;failure&gt;
 *         &lt;message&gt;...&lt;/message&gt;
 *         &lt;stack-trace&gt;...&lt;/stack-trace&gt;
 *       &lt;/failure&gt;
 *     &lt;/test-case&gt;
 *   &lt;/test-suite&gt;
 * &lt;/test-run&gt;
 * </pre>
 *
 * <p>NUnit {@code Inconclusive} is mapped to {@link TestResultStatus#SKIPPED}.
 */
public class NunitXmlIterator extends AbstractXmlIterator<TestResult> {

    /** NUnit 3 result attribute values. */
    private static final String RESULT_FAILED       = "Failed";
    private static final String RESULT_SKIPPED      = "Skipped";
    private static final String RESULT_INCONCLUSIVE = "Inconclusive";

    private static final String TEST_CASE_ELEMENT = "test-case";
    private static final String NAME_ELEMENT = "name";
    private static final String CLASSNAME_ELEMENT = "classname";
    private static final String RESULT_ELEMENT = "result";
    private static final String DURATION_ELEMENT = "duration";
    private static final String MESSAGE_ELEMENT = "message";
    private static final String STACK_TRACE_ELEMENT = "stack-trace";
    private static final String EMPTY_STRING = "";
    private static final String PACKAGE_SEPARATOR = ".";

    private String packageName;
    private String className;
    private String testName;
    private TestResultStatus status;
    private long duration;
    private final long started;

    private String errorMsg;
    private String stackTraceStr;

    // State tracking for character aggregation
    private boolean insideFailureMessage;
    private boolean insideFailureStackTrace;

    public NunitXmlIterator(File nunitXmlFile, Long started) throws XMLStreamException, IOException {
        super(nunitXmlFile);
        this.started = (started == null) ? System.currentTimeMillis() : started;
    }

    @Override
    protected void onEvent(XMLEvent event) {
        if (event instanceof StartElement) {
            StartElement element = (StartElement) event;
            String localName = element.getName().getLocalPart();

            if (TEST_CASE_ELEMENT.equals(localName)) {
                // Reset state for this test case
                packageName = EMPTY_STRING;
                className = EMPTY_STRING;
                testName = EMPTY_STRING;
                status = TestResultStatus.PASSED;
                duration = 0;
                errorMsg = EMPTY_STRING;
                stackTraceStr = EMPTY_STRING;
                insideFailureMessage = false;
                insideFailureStackTrace = false;

                Iterator<Attribute> attrIterator = element.getAttributes();
                while (attrIterator.hasNext()) {
                    Attribute attribute = attrIterator.next();
                    String attrName = attribute.getName().getLocalPart();
                    if (NAME_ELEMENT.equals(attrName)) {
                        testName = restrictSizeTo255(attribute.getValue());
                    } else if (CLASSNAME_ELEMENT.equals(attrName)) {
                        parseClassname(attribute.getValue());
                    } else if (RESULT_ELEMENT.equals(attrName)) {
                        status = parseStatus(attribute.getValue());
                    } else if (DURATION_ELEMENT.equals(attrName)) {
                        duration = parseDuration(attribute.getValue());
                    }
                }

            } else if (MESSAGE_ELEMENT.equals(localName) && status == TestResultStatus.FAILED) {
                insideFailureMessage = true;

            } else if (STACK_TRACE_ELEMENT.equals(localName) && status == TestResultStatus.FAILED) {
                insideFailureStackTrace = true;
            }

        } else if (event instanceof EndElement) {
            EndElement element = (EndElement) event;
            String localName = element.getName().getLocalPart();

            if (TEST_CASE_ELEMENT.equals(localName) && StringUtils.isNotEmpty(testName)) {
                TestResult testResult = new TestResult(packageName, className, testName, status, duration, started);
                if (TestResultStatus.FAILED.equals(status)) {
                    // NUnit 3 does not expose a distinct exception type attribute;
                    // pass null for errorType - error message and stack trace are used.
                    testResult.setFailedInfo(null, errorMsg, stackTraceStr);
                }
                addItem(testResult);
            } else if (MESSAGE_ELEMENT.equals(localName)) {
                insideFailureMessage = false;
            } else if (STACK_TRACE_ELEMENT.equals(localName)) {
                insideFailureStackTrace = false;
            }

        } else if (event instanceof Characters) {
            String data = ((Characters) event).getData();
            if (insideFailureMessage) {
                errorMsg += data;
            } else if (insideFailureStackTrace) {
                stackTraceStr += data;
            }
        }
    }

    /**
     * Maps NUnit 3 result strings to {@link TestResultStatus}.
     * {@code Inconclusive} is treated as {@code Skipped}.
     */
    private TestResultStatus parseStatus(String nunitResult) {
        if (RESULT_FAILED.equals(nunitResult)) {
            return TestResultStatus.FAILED;
        } else if (RESULT_SKIPPED.equals(nunitResult) || RESULT_INCONCLUSIVE.equals(nunitResult)) {
            return TestResultStatus.SKIPPED;
        } else {
            return TestResultStatus.PASSED;
        }
    }

    /** Splits a fully qualified NUnit classname into a package and a simple class name. */
    private void parseClassname(String fullyQualifiedName) {
        if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
            packageName = EMPTY_STRING;
            className = EMPTY_STRING;
            return;
        }
        int lastDotIndex = fullyQualifiedName.lastIndexOf(PACKAGE_SEPARATOR);
        if (lastDotIndex > 0) {
            packageName = fullyQualifiedName.substring(0, lastDotIndex);
            className = fullyQualifiedName.substring(lastDotIndex + 1);
        } else {
            packageName = EMPTY_STRING;
            className = fullyQualifiedName;
        }
    }

    /** Converts an NUnit duration (fractional seconds) to milliseconds. */
    private long parseDuration(String duration) {
        try {
            double seconds = Double.parseDouble(duration);
            return (long) (seconds * 1000);
        } catch (NumberFormatException e) {
            System.out.println("Unable to parse NUnit test duration: " + duration);
        }
        return 0;
    }

    private String restrictSizeTo255(String value) {
        if (value != null && value.length() > 255) {
            return value.substring(0, 255);
        }
        return value;
    }
}