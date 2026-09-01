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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;

public class CliParserTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    Options options;

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        CliParser cliParser = new CliParser();
        Field optionsFiled = cliParser.getClass().getDeclaredField("options");
        optionsFiled.setAccessible(true);
        options = (Options) optionsFiled.get(cliParser);
    }

    @Test
    public void testHelp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CliParser parser = new CliParser();
        Method help = parser.getClass().getDeclaredMethod("printHelp");
        help.setAccessible(true);
        help.invoke(parser);
    }

    @Test
    public void testVersion() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CliParser parser = new CliParser();
        Method help = parser.getClass().getDeclaredMethod("printVersion");
        help.setAccessible(true);
        help.invoke(parser);
    }

    @Test
    public void testArgs_missingArgument() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            parser.parse(options, new String[]{"-i", "-d"});
            Assert.fail();
        } catch (MissingArgumentException e) {
            Assert.assertEquals("Missing argument for option: d", e.getMessage());
        }
    }

    @Test
    public void testArgs_invalidOption() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            parser.parse(options, new String[]{"-i", "-xyz"});
            Assert.fail();
        } catch (UnrecognizedOptionException e) {
            Assert.assertEquals("Unrecognized option: -xyz", e.getMessage());
        }
    }

    @Test
    public void testArgs_invalidIntegerValue() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, new String[]{"-i", "-r", "invalidIntegerValue"});
        Assert.assertTrue(commandLine.hasOption("r"));
        try {
            commandLine.getParsedOptionValue("r");
            Assert.fail();
        } catch (ParseException e){
            Assert.assertEquals("For input string: \"invalidIntegerValue\"", e.getMessage());
        }
    }

    @Test
    public void testArgs_invalidInternalCombination() throws NoSuchMethodException, ParseException, InvocationTargetException, IllegalAccessException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // -i + -w + positional file: valid
        CommandLine cmdArgs = parser.parse(options, new String[]{ "-i", "-w", "1002", "publicApi.xml"});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue(result);

        // -i + -b: invalid (backlog-item is restricted for internal mode)
        cmdArgs = parser.parse(options, new String[]{"-i", "-b", "1002", "publicApi.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);
    }

    @Test
         public void testArgs_duplicates() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException, IOException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // Single release + positional file: valid
        CommandLine cmdArgs = parser.parse(options, new String[]{ "-r", "1", "test.xml"});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue(result);

        // Duplicate -r: invalid
        cmdArgs = parser.parse(options, new String[]{"-r", "1", "-r", "2", "test.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);

        // No input at all: invalid
        cmdArgs = parser.parse(options, new String[]{});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);

        // --output-file with positional file: valid
        File outputFolder = temporaryFolder.newFolder();
        cmdArgs = parser.parse(options, new String[]{"--output-file",
                outputFolder.getPath() + File.separator + "testResults.xml",
                "JUnit.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue(result);
    }

    @Test
    public void testArgs_inputFiles() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException, URISyntaxException, IOException {
        CliParser cliParser = new CliParser();
        Method inputFilesValidation = cliParser.getClass().getDeclaredMethod("addTestResultsFilesToSettings", CommandLine.class, Settings.class);
        inputFilesValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // Non-existent positional file: failure
        CommandLine cmdArgs = parser.parse(options, new String[]{"nonexistent_file_that_will_never_exist.xml"});
        Settings settings = new Settings();
        Boolean result = (Boolean) inputFilesValidation.invoke(cliParser, cmdArgs, settings);
        Assert.assertFalse(result);
        Assert.assertNull(settings.getTestResultsFileNames());

        // Two real positional files: success
        cmdArgs = parser.parse(options, new String[]{
                getClass().getResource("JUnit-minimalAccepted.xml").toURI().getPath(),
                getClass().getResource("JUnit-missingTestName.xml").toURI().getPath()});
        result = (Boolean) inputFilesValidation.invoke(cliParser, cmdArgs, settings);
        Assert.assertTrue(result);
        List<String> fileNames = settings.getTestResultsFileNames();
        Assert.assertNotNull(fileNames);
        Assert.assertEquals(2, fileNames.size());
        Assert.assertTrue(fileNames.get(0).contains("JUnit-minimalAccepted.xml"));
        Assert.assertTrue(fileNames.get(1).contains("JUnit-missingTestName.xml"));
    }

    @Test
    public void testArgs_positionalFiles() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException, URISyntaxException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        Method inputFilesValidation = cliParser.getClass().getDeclaredMethod("addTestResultsFilesToSettings", CommandLine.class, Settings.class);
        inputFilesValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // Positional args should pass areCmdArgsValid
        String realFile = getClass().getResource("JUnit-minimalAccepted.xml").toURI().getPath();
        CommandLine cmdArgs = parser.parse(options, new String[]{realFile});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue("Positional args should be accepted", result);

        // Positional args should populate test results in settings
        Settings settings = new Settings();
        result = (Boolean) inputFilesValidation.invoke(cliParser, cmdArgs, settings);
        Assert.assertTrue(result);
        Assert.assertNotNull(settings.getTestResultsFileNames());
        Assert.assertEquals(1, settings.getTestResultsFileNames().size());
        Assert.assertTrue(settings.getTestResultsFileNames().get(0).contains("JUnit-minimalAccepted.xml"));

        // Non-existent positional file should be skipped
        settings = new Settings();
        cmdArgs = parser.parse(options, new String[]{"nonexistent_file.xml"});
        result = (Boolean) inputFilesValidation.invoke(cliParser, cmdArgs, settings);
        Assert.assertFalse("Non-existent positional file should fail", result);
        Assert.assertNull(settings.getTestResultsFileNames());
    }

    @Test
    public void testArgs_tagFormat() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        CommandLine cmdArgs = parser.parse(options, new String[]{"-t", "OS:Linux", "test.xml"});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue(result);

        cmdArgs = parser.parse(options, new String[]{"-t", "OS:", "test.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);

        cmdArgs = parser.parse(options, new String[]{"-f", "OS::Linux", "test.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);

        cmdArgs = parser.parse(options, new String[]{"-f", ":", "test.xml"});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);
    }


    @Test
    public void testArgs_passwordFile() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException, URISyntaxException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        CommandLine cmdArgs = parser.parse(options, new String[]{"--password-file",
                getClass().getResource("testPasswordFile").toURI().getPath(),
                getClass().getResource("JUnit-minimalAccepted.xml").toURI().getPath()});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertTrue(result);

        cmdArgs = parser.parse(options, new String[]{"--password-file",
                "invalidPasswordFile",
                getClass().getResource("JUnit-minimalAccepted.xml").toURI().getPath()});
        result = (Boolean) argsValidation.invoke(cliParser, cmdArgs);
        Assert.assertFalse(result);
    }

    @Test
    public void testArgs_settingsValidation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method settingsValidation = cliParser.getClass().getDeclaredMethod("areSettingsValid", Settings.class);
        settingsValidation.setAccessible(true);
        Settings settings = new Settings();

        Boolean result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertFalse(result);

        settings.setServer("http://test.hp.com:8080");
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertFalse(result);

        settings.setSharedspace(1001);
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertFalse(result);

        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertFalse(result); // no auth configured

        // bearer token alone is sufficient, workspace is optional
        settings.setBearerToken("mytoken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result);

        settings.setCheckResult(true);
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result);

        settings.setWorkspace(1002);
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result); // workspace without build-context params is valid

        settings.setSuite(42);
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result); // suite with workspace is valid

        settings.setBuildContextJobId("job1");
        settings.setBuildContextBuildId("build1");
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertFalse(result); // workspace + build-context requires build-context-server-id

        settings.setBuildContextServerId("server1");
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result);

        // build-context-server-id without workspace is also invalid
        Settings settingsNoWorkspace = new Settings();
        settingsNoWorkspace.setServer("http://test.hp.com:8080");
        settingsNoWorkspace.setSharedspace(1001);
        settingsNoWorkspace.setBearerToken("mytoken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        settingsNoWorkspace.setBuildContextServerId("server1");
        result = (Boolean) settingsValidation.invoke(cliParser, settingsNoWorkspace);
        Assert.assertFalse(result); // build-context-server-id without workspace is invalid

        Settings settingsSuiteNoWorkspace = new Settings();
        settingsSuiteNoWorkspace.setServer("http://test.hp.com:8080");
        settingsSuiteNoWorkspace.setSharedspace(1001);
        settingsSuiteNoWorkspace.setBearerToken("mytoken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        settingsSuiteNoWorkspace.setSuite(42);
        result = (Boolean) settingsValidation.invoke(cliParser, settingsSuiteNoWorkspace);
        Assert.assertFalse(result); // suite without workspace is invalid

        Settings settingsInternalNoWorkspace = new Settings();
        settingsInternalNoWorkspace.setServer("http://test.hp.com:8080");
        settingsInternalNoWorkspace.setSharedspace(1001);
        settingsInternalNoWorkspace.setBearerToken("mytoken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        settingsInternalNoWorkspace.setInternal(true);
        result = (Boolean) settingsValidation.invoke(cliParser, settingsInternalNoWorkspace);
        Assert.assertFalse(result); // internal mode without workspace is invalid

        settingsInternalNoWorkspace.setWorkspace(1002);
        result = (Boolean) settingsValidation.invoke(cliParser, settingsInternalNoWorkspace);
        Assert.assertTrue(result); // internal mode with workspace is valid

        // user + password is also sufficient
        settings.setBearerToken(null);
        settings.setSuite(null);
        settings.setCheckResult(false);
        settings.setUser("admin");
        settings.setPassword("password".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        result = (Boolean) settingsValidation.invoke(cliParser, settings);
        Assert.assertTrue(result);
    }

    @Test
    public void testParser_outputFile() throws IOException, URISyntaxException {
        CliParser cliParser = new CliParser();
        File outputFile = new File(temporaryFolder.newFolder(), "testResults.xml");
        Settings settings = cliParser.parse(new String[]{"--started", "123456", "-a", "2",
                "--output-file", outputFile.getPath(),
                "-c", getClass().getResource("test.properties").toURI().getPath(),
                getClass().getResource("JUnit-minimalAccepted.xml").toURI().getPath()});
        Assert.assertEquals(Long.valueOf(123456), settings.getStarted());
        Assert.assertTrue(outputFile.canWrite());
        Assert.assertEquals("http://localhost:8080/qcbin", settings.getServer());
        Assert.assertEquals(Integer.valueOf(1001), settings.getSharedspace());
        Assert.assertEquals(Integer.valueOf(1002), settings.getWorkspace());
        Assert.assertEquals("admin", settings.getUser());
    }

    // -------------------------------------------------------------------------
    // Coverage report option tests
    // -------------------------------------------------------------------------

    @Test
    public void testArgs_coverageReportType_validValues() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        for (String validType : CliParser.COVERAGE_REPORT_TYPES) {
            CommandLine cmd = parser.parse(options, new String[]{
                    "--coverage-reports", "dummy.xml",
                    "--coverage-report-type", validType,
                    "--build-context-server-id", "srv1",
                    "--build-context-job-id", "job1",
                    "--build-context-build-id", "build1"
            });
            Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
            Assert.assertTrue("Expected valid for coverage-report-type: " + validType, result);
        }
    }

    @Test
    public void testArgs_coverageReportType_invalidValue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-reports", "dummy.xml",
                "--coverage-report-type", "invalidType",
                "--build-context-server-id", "srv1",
                "--build-context-job-id", "job1",
                "--build-context-build-id", "build1"
        });
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertFalse("Expected invalid for unknown coverage-report-type", result);
    }

    @Test
    public void testArgs_coverageReports_requiresType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // --coverage-reports without --coverage-report-type should fail
        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-reports", "dummy.xml",
                "--build-context-server-id", "srv1",
                "--build-context-job-id", "job1",
                "--build-context-build-id", "build1"
        });
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertFalse("Expected failure: --coverage-reports without --coverage-report-type", result);
    }

    @Test
    public void testArgs_coverageReports_requiresBuildContext() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // --coverage-reports without build-context params should fail
        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-reports", "dummy.xml",
                "--coverage-report-type", "JACOCOXML"
        });
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertFalse("Expected failure: --coverage-reports without build-context params", result);
    }

    @Test
    public void testArgs_coverageReportType_requiresCoverageReports() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // --coverage-report-type without --coverage-reports should fail
        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-report-type", "JACOCOXML",
                "test.xml"
        });
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertFalse("Expected failure: --coverage-report-type without --coverage-reports", result);
    }

    @Test
    public void testArgs_noInput_fails() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // No positional args, no --coverage-reports -> should fail
        CommandLine cmd = parser.parse(options, new String[]{});
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertFalse("Expected failure when no input is specified", result);
    }

    @Test
    public void testArgs_coverageOnlyMode_valid() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method argsValidation = cliParser.getClass().getDeclaredMethod("areCmdArgsValid", CommandLine.class);
        argsValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // --coverage-reports alone (no positional args) should be valid in areCmdArgsValid
        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-reports", "target/site/jacoco/*.xml",
                "--coverage-report-type", "JACOCOXML",
                "--build-context-server-id", "srv1",
                "--build-context-job-id", "job1",
                "--build-context-build-id", "build1"
        });
        Boolean result = (Boolean) argsValidation.invoke(cliParser, cmd);
        Assert.assertTrue("Expected valid for coverage-only mode", result);
    }

    @Test
    public void testArgs_coverageOnlyMode_noTestResultsInSettings() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ParseException {
        CliParser cliParser = new CliParser();
        Method inputFilesValidation = cliParser.getClass().getDeclaredMethod("addTestResultsFilesToSettings", CommandLine.class, Settings.class);
        inputFilesValidation.setAccessible(true);
        CommandLineParser parser = new DefaultParser();

        // Coverage-only: no positional args -> inputXmlFileNames should remain null
        CommandLine cmd = parser.parse(options, new String[]{
                "--coverage-reports", "target/site/jacoco/*.xml",
                "--coverage-report-type", "JACOCOXML",
                "--build-context-server-id", "srv1",
                "--build-context-job-id", "job1",
                "--build-context-build-id", "build1"
        });
        Settings settings = new Settings();
        Boolean result = (Boolean) inputFilesValidation.invoke(cliParser, cmd, settings);
        Assert.assertTrue("Expected success for coverage-only mode (no test result files required)", result);
        Assert.assertNull("inputXmlFileNames should be null in coverage-only mode", settings.getTestResultsFileNames());
    }
}
