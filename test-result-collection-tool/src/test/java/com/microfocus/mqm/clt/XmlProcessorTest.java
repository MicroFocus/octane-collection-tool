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
import com.microfocus.mqm.clt.tests.TestResultStatus;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class XmlProcessorTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testXmlProcessor_minimalAcceptedJUnitFormat() throws URISyntaxException {
        // Public API requires at least testName, duration, started and status fields to be filled for every test
        XmlProcessor xmlProcessor = new XmlProcessor();
        List<TestResult> testResults = xmlProcessor.processJunitTestReport(new File(getClass().getResource("JUnit-minimalAccepted.xml").toURI()), 1444291726L);
        Assert.assertNotNull(testResults);
        Assert.assertEquals(4, testResults.size());
        assertTestResult(testResults.get(0), "", "", "testName", TestResultStatus.PASSED, 0, 1444291726L);
        assertTestResult(testResults.get(1), "", "", "testNameSkipped", TestResultStatus.SKIPPED, 2, 1444291726L);
        assertTestResult(testResults.get(2), "", "", "testNameFailed", TestResultStatus.FAILED, 3, 1444291726L);
        assertTestResult(testResults.get(3), "", "", "testNameWithError", TestResultStatus.FAILED, 4, 1444291726L);
    }

    @Test
    public void testXmlProcessor_testMissingTestName() throws URISyntaxException, IOException, XMLStreamException, InterruptedException {
        XmlProcessor xmlProcessor = new XmlProcessor();
        List<TestResult> testResults = xmlProcessor.processJunitTestReport(new File(getClass().getResource("JUnit-missingTestName.xml").toURI()), 1445937556462L);
        Assert.assertNotNull(testResults);
        Assert.assertEquals(3, testResults.size());
        assertTestResult(testResults.get(0), "com.examples.example", "SampleClass", "testOne", TestResultStatus.PASSED, 2, 1445937556462L);
        assertTestResult(testResults.get(1), "com.examples.example", "SampleClass", "testTwo", TestResultStatus.SKIPPED, 5, 1445937556462L);
        assertTestResult(testResults.get(2), "com.examples.example", "SampleClass", "testThree", TestResultStatus.SKIPPED, 5, 1445937556462L);
    }

    @Test
    public void testXmlProcessor_unclosedElement() throws URISyntaxException {
        systemOutRule.enableLog();
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(new Assertion() {
            @Override
            public void checkAssertion() throws Exception {
                Assert.assertTrue(systemOutRule.getLog().contains("Unable to process JUnit XML file"));
            }
        });
        XmlProcessor xmlProcessor = new XmlProcessor();
        xmlProcessor.processJunitTestReport(new File(getClass().getResource("JUnit-unclosedElement.xmlx").toURI()), null);
    }

    @Test
    public void testXmlProcessor_junitFileDoesNotExist() throws URISyntaxException {
        systemOutRule.enableLog();
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(new Assertion() {
            @Override
            public void checkAssertion() throws Exception {
                Assert.assertTrue(systemOutRule.getLog().contains("Can not read the JUnit XML file:"));
            }
        });
        XmlProcessor xmlProcessor = new XmlProcessor();
        xmlProcessor.processJunitTestReport(new File("fileDoesNotExist.xml"), null);
    }

    @Test
    public void testXmlProcessor_writeXml() throws URISyntaxException, IOException, XMLStreamException, ParseException {
        File targetFile = temporaryFolder.newFile();
        long currentTime = System.currentTimeMillis();
        XmlProcessor xmlProcessor = new XmlProcessor();
        List<TestResult> testResults = new LinkedList<TestResult>();
        testResults.add(new TestResult("com.examples.example", "SampleClass", "testOne", TestResultStatus.PASSED, 2, currentTime));
        testResults.add(new TestResult("com.examples.example", "SampleClass", "testTwo", TestResultStatus.SKIPPED, 5, currentTime));
        testResults.add(new TestResult("com.examples.example", "SampleClass", "testThree", TestResultStatus.SKIPPED, 5, currentTime));
        List<String> tags = new LinkedList<String>();
        tags.add("OS:Linux");
        tags.add("DB:Oracle");
        List<String> fields = new LinkedList<String>();
        fields.add("Framework:TestNG");
        fields.add("Test_Level:Unit Test");
        Settings settings = new Settings();
        settings.setTags(tags);
        settings.setFields(fields);
        settings.setProductAreas(new String[]{"1001", "1002"});
        settings.setRelease(1010);
        settings.setBacklogItems(new String[]{"1020", "1021"});

        xmlProcessor.writeTestResults(testResults, settings, targetFile);

        Set<XmlElement> xmlElements = new HashSet<XmlElement>();
        xmlElements.add(new XmlElement("taxonomy", "OS", "Linux"));
        xmlElements.add(new XmlElement("taxonomy", "DB", "Oracle"));
        xmlElements.add(new XmlElement("test_field", "Framework", "TestNG"));
        xmlElements.add(new XmlElement("test_field", "Test_Level", "Unit Test"));
        xmlElements.add(new XmlElement("product_area_ref", "1001"));
        xmlElements.add(new XmlElement("product_area_ref", "1002"));
        xmlElements.add(new XmlElement("backlog_item_ref", "1020"));
        xmlElements.add(new XmlElement("backlog_item_ref", "1021"));
        xmlElements.add(new XmlElement("release_ref", "1010"));
        assertXml(new LinkedList<TestResult>(testResults), xmlElements, targetFile);
    }

    private void assertTestResult(TestResult testResult, String packageName, String className, String testName,
                                  TestResultStatus result, long duration, long started) {
        Assert.assertEquals(packageName, testResult.getPackageName());
        Assert.assertEquals(className, testResult.getClassName());
        Assert.assertEquals(testName, testResult.getTestName());
        Assert.assertEquals(result, testResult.getResult());
        Assert.assertEquals(duration, testResult.getDuration());
        Assert.assertEquals(started, testResult.getStarted());
    }

    private void assertXml(List<TestResult> expectedTestResults, Set<XmlElement> expectedElements, File xmlFile) throws FileNotFoundException, XMLStreamException {
        FileInputStream fis = new FileInputStream(xmlFile);
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", true);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(fis);

        boolean isFirstEvent = true;
        while(xmlStreamReader.hasNext()){
            if (!isFirstEvent) {
                xmlStreamReader.next();
            } else {
                isFirstEvent = false;
            }

            if (xmlStreamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                String localName = xmlStreamReader.getLocalName();
                if ("taxonomy".equals(localName)) {
                    assertElement(localName, false, xmlStreamReader, expectedElements);
                } else if ("test_field".equals(localName)) {
                    assertElement(localName, false, xmlStreamReader, expectedElements);
                } else if ("product_area_ref".equals(localName)) {
                    assertElement(localName, true, xmlStreamReader, expectedElements);
                } else if ("backlog_item_ref".equals(localName)) {
                    assertElement(localName, true, xmlStreamReader, expectedElements);
                } else if ("release_ref".equals(localName)) {
                    assertElement(localName, true, xmlStreamReader, expectedElements);
                } else if ("test_run".equals(localName)) {
                    assertXmlTest(xmlStreamReader, expectedTestResults);
                }
            }
        }
        xmlStreamReader.close();
        IOUtils.closeQuietly(fis);
        Assert.assertTrue(expectedElements.isEmpty());
        Assert.assertTrue(expectedTestResults.isEmpty());
    }

    private void assertXmlTest(XMLStreamReader xmlStreamReader, List<TestResult> testResults) {
        String testName = xmlStreamReader.getAttributeValue(null, "name");
        String statusName = xmlStreamReader.getAttributeValue(null, "status");
        String duration = xmlStreamReader.getAttributeValue(null, "duration");
        String started = xmlStreamReader.getAttributeValue(null, "started");
        Assert.assertNotNull(testName);
        Assert.assertNotNull(statusName);
        Assert.assertNotNull(duration);
        Assert.assertNotNull(started);

        TestResult testToFind = new TestResult(
                xmlStreamReader.getAttributeValue(null, "package"),
                xmlStreamReader.getAttributeValue(null, "class"),
                testName, TestResultStatus.fromPrettyName(statusName),
                Long.valueOf(duration), Long.valueOf(started));

        for (TestResult testResult : testResults) {
            if (areTestResultsEqual(testResult, testToFind)) {
                testResults.remove(testResult);
                return;
            }
        }
        Assert.fail("Can not find the expected test result");
    }

    private boolean areTestResultsEqual(TestResult first, TestResult second) {
        return StringUtils.equals(first.getPackageName(), second.getPackageName()) &&
                StringUtils.equals(first.getClassName(), second.getClassName()) &&
                StringUtils.equals(first.getTestName(), second.getTestName()) &&
                first.getResult() == second.getResult() &&
                first.getDuration() == second.getDuration() &&
                first.getStarted() == second.getStarted();
    }

    private void assertElement(String elemName, boolean isReference, XMLStreamReader xmlStreamReader, Set<XmlElement> expectedElements) {
        String type = null;
        String value;
        if (isReference) {
            value = xmlStreamReader.getAttributeValue(null, "id");
            Assert.assertNotNull(value);
        } else {
            type = xmlStreamReader.getAttributeValue(null, "type");
            value = xmlStreamReader.getAttributeValue(null, "value");
            Assert.assertNotNull(type);
            Assert.assertNotNull(value);
        }
        XmlElement element = new XmlElement(elemName, type, value);
        Assert.assertTrue(expectedElements.contains(element));
        expectedElements.remove(element);
    }

    private class XmlElement {

        private String elemName;
        private String type;
        private String value;

        private XmlElement(String elemName, String type, String value) {
            this.elemName = elemName;
            this.type = type;
            this.value = value;
        }

        private XmlElement(String elemName, String value) {
            this(elemName, null, value);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof XmlElement))
                return false;
            if (obj == this)
                return true;
            return (StringUtils.equals (this.elemName, ((XmlElement) obj).elemName) &&
                    StringUtils.equals (this.type, ((XmlElement) obj).type) &&
                    StringUtils.equals (this.value, ((XmlElement) obj).value));
        }

        public int hashCode(){
            int prime = 31;
            int result = (elemName != null) ? elemName.hashCode() : prime;
            result = prime * result + ((type != null) ? type.hashCode() : prime);
            result = prime * result + ((value != null) ? value.hashCode() : prime);
            return result;
        }
    }
}
