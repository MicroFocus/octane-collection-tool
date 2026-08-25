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
import com.microfocus.mqm.clt.xml.NunitXmlIterator;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NunitXmlIteratorTest {

    private static final long FIXED_STARTED = 1444291726000L;

    @Test
    public void testNunit3_allStatuses() throws URISyntaxException, XMLStreamException, IOException, InterruptedException {
        File xmlFile = new File(Objects.requireNonNull(getClass().getResource("NUnit3-allStatuses.xml")).toURI());
        List<TestResult> results = parseAll(xmlFile);

        Assert.assertEquals(5, results.size());

        // PassingTest - result="Passed", duration=0.001 s -> 1 ms
        assertTestResult(Objects.requireNonNull(findByName(results, "PassingTest")),
                "com.example.tests", "SampleFixture", "PassingTest",
                TestResultStatus.PASSED, 1L);

        // FailingTest - result="Failed", duration=0.002 s -> 2 ms
        TestResult failingTest = findByName(results, "FailingTest");
        Assert.assertNotNull(failingTest);
        assertTestResult(failingTest,
                "com.example.tests", "SampleFixture", "FailingTest",
                TestResultStatus.FAILED, 2L);
        Assert.assertNull(failingTest.getErrorType());                         // NUnit 3 has no separate type attribute
        Assert.assertEquals("Expected 1 but was 2", failingTest.getErrorMsg().trim());
        Assert.assertTrue(failingTest.getStackTraceStr().contains("SampleFixture.cs:line 25"));

        // SkippedTest - result="Skipped", duration=0 ms
        assertTestResult(Objects.requireNonNull(findByName(results, "SkippedTest")),
                "com.example.tests", "SampleFixture", "SkippedTest",
                TestResultStatus.SKIPPED, 0L);

        // InconclusiveTest - result="Inconclusive" -> SKIPPED, duration=0.003 s -> 3 ms
        assertTestResult(Objects.requireNonNull(findByName(results, "InconclusiveTest")),
                "com.example.tests", "SampleFixture", "InconclusiveTest",
                TestResultStatus.SKIPPED, 3L);

        // AnotherPassingTest - result="Passed", duration=0.004 s -> 4 ms
        assertTestResult(Objects.requireNonNull(findByName(results, "AnotherPassingTest")),
                "com.example.tests", "SampleFixture", "AnotherPassingTest",
                TestResultStatus.PASSED, 4L);
    }

    @Test
    public void testNunit3_nestedSuites() throws URISyntaxException, XMLStreamException, IOException, InterruptedException {
        File xmlFile = new File(Objects.requireNonNull(getClass().getResource("NUnit3-nestedSuites.xml")).toURI());
        List<TestResult> results = parseAll(xmlFile);

        // Both test-cases live inside deeply nested test-suite elements
        Assert.assertEquals(2, results.size());

        assertTestResult(Objects.requireNonNull(findByName(results, "FirstTest")),
                "com.example", "DeepFixture", "FirstTest",
                TestResultStatus.PASSED, 5L);

        assertTestResult(Objects.requireNonNull(findByName(results, "SecondTest")),
                "com.example", "DeepFixture", "SecondTest",
                TestResultStatus.PASSED, 5L);
    }

    @Test
    public void testNunit3_generatedFile() throws URISyntaxException, XMLStreamException, IOException, InterruptedException {
        File xmlFile = new File(Objects.requireNonNull(getClass().getResource("NUnit3-generated-file.xml")).toURI());
        List<TestResult> results = parseAll(xmlFile);

        Assert.assertEquals(3, results.size());

        TestResult failingTest = findByName(results, "FailingTest");
        Assert.assertNotNull(failingTest);
        assertTestResult(failingTest,
                "TestApplication.Tests", "CalculatorTests", "FailingTest",
                TestResultStatus.FAILED, 48L);
        Assert.assertNull(failingTest.getErrorType());
        Assert.assertEquals("Assert.That(calc.Add(2, 2), Is.EqualTo(5))\n                            Expected: 5\n                            But was:  4", failingTest.getErrorMsg().trim());
        Assert.assertTrue(failingTest.getStackTraceStr().contains("CalculatorTests.cs:line 22"));

        assertTestResult(Objects.requireNonNull(findByName(results, "PassingTest")),
                "TestApplication.Tests", "CalculatorTests", "PassingTest",
                TestResultStatus.PASSED, 0L);

        assertTestResult(Objects.requireNonNull(findByName(results, "SkippedTest")),
                "TestApplication.Tests", "CalculatorTests", "SkippedTest",
                TestResultStatus.SKIPPED, 0L);
    }


    @Test
    public void testNunit3_startedTimestamp() throws URISyntaxException, XMLStreamException, IOException, InterruptedException {
        File xmlFile = new File(Objects.requireNonNull(getClass().getResource("NUnit3-nestedSuites.xml")).toURI());
        NunitXmlIterator iterator = new NunitXmlIterator(xmlFile, FIXED_STARTED);
        while (iterator.hasNext()) {
            TestResult result = iterator.next();
            Assert.assertEquals(FIXED_STARTED, result.getStarted());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<TestResult> parseAll(File xmlFile) throws XMLStreamException, IOException, InterruptedException {
        NunitXmlIterator iterator = new NunitXmlIterator(xmlFile, FIXED_STARTED);
        List<TestResult> results = new ArrayList<>();
        while (iterator.hasNext()) {
            results.add(iterator.next());
        }
        return results;
    }

    private TestResult findByName(List<TestResult> results, String testName) {
        for (TestResult r : results) {
            if (testName.equals(r.getTestName())) {
                return r;
            }
        }
        Assert.fail("Test result with name '" + testName + "' not found");
        return null; // unreachable
    }

    private void assertTestResult(TestResult result,
                                  String expectedPackage,
                                  String expectedClass,
                                  String expectedName,
                                  TestResultStatus expectedStatus,
                                  long expectedDurationMs) {
        Assert.assertEquals("package", expectedPackage, result.getPackageName());
        Assert.assertEquals("class",   expectedClass,   result.getClassName());
        Assert.assertEquals("name",    expectedName,    result.getTestName());
        Assert.assertEquals("status",  expectedStatus,  result.getResult());
        Assert.assertEquals("duration (ms)", expectedDurationMs, result.getDuration());
    }
}