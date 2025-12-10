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

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class CliParser {

    private static final String CMD_LINE_SYNTAX = "java -jar test-result-collection-tool.jar [OPTIONS]... FILE [FILE]...\n";
    private static final String HEADER = "Open Text ALM Octane Test Result Collection Tool";
    private static final String FOOTER = "";
    private static final String VERSION = "1.0.15";

    private Options options = new Options();
    private LinkedList<String> argsWithSingleOccurrence = new LinkedList<String>();
    private LinkedList<String> argsRestrictedForInternal = new LinkedList<>();
    private LinkedList<String> argsForBuildContext = new LinkedList<>();

    public CliParser() {
        options.addOption("h", "help", false, "show this help");
        options.addOption("v", "version", false, "show version of this tool");

        options.addOption("i", "internal", false, "supplied XML files are in the API internal XML format");
        options.addOption("e", "skip-errors", false, "skip errors on the server side");
        options.addOption(Option.builder("o").longOpt("output-file").desc("write output in the API internal XML format to file instead of pushing it to the server").hasArg().argName("FILE").build());
        options.addOption(Option.builder("c").longOpt("config-file").desc("configuration file location").hasArg().argName("FILE").build());

        options.addOption(Option.builder("s").longOpt("server").desc("server URL with protocol and port").hasArg().argName("URL:PORT").build());
        options.addOption(Option.builder("d").longOpt("shared-space").desc("server shared space to push to").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder("w").longOpt("workspace").desc("server workspace to push to").hasArg().argName("ID").type(Number.class).build());

        options.addOption(Option.builder("u").longOpt("user").desc("server username").hasArg().argName("USERNAME").build());
        OptionGroup passGroup = new OptionGroup();
        passGroup.addOption(Option.builder("p").longOpt("password").desc("server password").hasArg().argName("PASSWORD").optionalArg(true).build());
        passGroup.addOption(Option.builder().longOpt("password-file").desc("location of file with server password").hasArg().argName("FILE").build());
        options.addOptionGroup(passGroup);

        options.addOption(Option.builder().longOpt("proxy-host").desc("proxy host").hasArg().argName("HOSTNAME").build());
        options.addOption(Option.builder().longOpt("proxy-port").desc("proxy port").hasArg().argName("PORT").type(Number.class).build());
        options.addOption(Option.builder().longOpt("proxy-user").desc("proxy username").hasArg().argName("USERNAME").build());

        OptionGroup proxyPassGroup = new OptionGroup();
        proxyPassGroup.addOption(Option.builder().longOpt("proxy-password").desc("proxy password").hasArg().argName("PASSWORD").optionalArg(true).build());
        proxyPassGroup.addOption(Option.builder().longOpt("proxy-password-file").desc("location of file with proxy password").hasArg().argName("FILE").build());
        options.addOptionGroup(proxyPassGroup);

        options.addOption(Option.builder().longOpt("check-result").desc("check test result status after push").build());
        options.addOption(Option.builder().longOpt("check-result-timeout").desc("timeout for test result push status retrieval").hasArg().argName("SEC").type(Number.class).build());

        options.addOption(Option.builder("t").longOpt("tag").desc("assign environment tag to test runs").hasArg().argName("TYPE:VALUE").build());
        options.addOption(Option.builder().longOpt("access-token").desc("IDP access token for authentication").hasArg().argName("PASSWORD").build());
        options.addOption(Option.builder("f").longOpt("field").desc("assign field tag to test result, relevant for the following fields : Testing_Tool_Type, Framework, Test_Level, Testing_Tool_Type").hasArg().argName("TYPE:VALUE").build());

        options.addOption(Option.builder("r").longOpt("release").desc("assign release to test result").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder().longOpt("release-default").desc("assign default release to test result (relevant for ALM Octane 15.1.8 and above)").build());
        options.addOption(Option.builder().longOpt("program").desc("assign program to test result").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder("m").longOpt("milestone").desc("assign milestone to test result").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder("a").longOpt("product-area").desc("assign the test result to product area").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder("b").longOpt("backlog-item").desc("assign the test result to backlog item").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder().longOpt("started").desc("start time in milliseconds").hasArg().argName("TIMESTAMP").type(Number.class).build());

        options.addOption(Option.builder().longOpt("suite").desc("assign suite to test result (relevant for ALM Octane 15.1.8 and above)").hasArg().argName("ID").type(Number.class).build());
        options.addOption(Option.builder().longOpt("suite-external-run-id").desc("assign name to suite run aggregating test results").hasArg().build());

        options.addOption(Option.builder().longOpt("build-context-server-id").desc("Server instance id for defining build context.").hasArg().build());
        options.addOption(Option.builder().longOpt("build-context-build-id").desc("Build id for defining build context.").hasArg().build());
        options.addOption(Option.builder().longOpt("build-context-job-id").desc("Job id for defining build context.").hasArg().build());



        argsWithSingleOccurrence.addAll(Arrays.asList("o", "c", "s", "d", "w", "u", "p", "password-file", "r", "release-default", "m", "started", "check-status", "program",
                "check-status-timeout", "proxy-host", "proxy-port", "proxy-user", "proxy-password", "proxy-password-file", "suite", "suite-external-run-id",
                "build-context-server-id","build-context-build-id","build-context-job-id"));
        argsRestrictedForInternal.addAll(Arrays.asList("o", "t", "f", "r", "m", "a", "b", "started", "suite", "suite-external-run-id", "program", "release-default",
                "build-context-server-id","build-context-build-id","build-context-job-id"));
        argsForBuildContext.addAll(Arrays.asList("build-context-server-id","build-context-build-id","build-context-job-id"));
    }

    public Settings parse(String[] args) {
        Settings settings = new Settings();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp();
                System.exit(ReturnCode.SUCCESS.getReturnCode());
            }

            if (cmd.hasOption("v")) {
                printVersion();
                System.exit(ReturnCode.SUCCESS.getReturnCode());
            }

            if (!areCmdArgsValid(cmd)) {
                printHelp();
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }

            if (!addInputFilesToSettings(cmd, settings)) {
                printHelp();
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }

            // load config
            String filename = null;
            if (cmd.hasOption("c")) {
                filename = cmd.getOptionValue("c");
            }
            try {
                settings.load(filename);
            } catch (NumberFormatException e) {
                System.out.println("Can not convert string from properties file to integer: " + e.getMessage());
                System.exit(ReturnCode.FAILURE.getReturnCode());
            } catch (IllegalArgumentException e) {
                // Inform user that loading was not successful
                // Configuration must be specified in arguments in this case
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Can not read from properties file: " + filename);
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }

            if (cmd.hasOption("i")) {
                settings.setInternal(true);
            }

            if (cmd.hasOption("e")) {
                settings.setSkipErrors(true);
            }

            if (cmd.hasOption("o")) {
                settings.setOutputFile(cmd.getOptionValue("o"));
            }

            if (cmd.hasOption("s")) {
                settings.setServer(cmd.getOptionValue("s"));
            }

            if (cmd.hasOption("d")) {
                settings.setSharedspace(((Long) cmd.getParsedOptionValue("d")).intValue());
            }

            if (cmd.hasOption("w")) {
                settings.setWorkspace(((Long) cmd.getParsedOptionValue("w")).intValue());
            }

            if (cmd.hasOption("u")) {
                settings.setUser(cmd.getOptionValue("u"));
            }

            if (settings.getOutputFile() == null) {
                if (cmd.hasOption("p")) {
                    settings.setPassword(cmd.getOptionValue("p").getBytes(StandardCharsets.UTF_8));
                } else if (cmd.hasOption("password-file")) {
                    try {
                        settings.setPassword(FileUtils.readFileToString(new File(cmd.getOptionValue("password-file"))).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        System.out.println("Can not read the password file: " + cmd.getOptionValue("password-file"));
                        System.exit(ReturnCode.FAILURE.getReturnCode());
                    }
                } else if (settings.getPassword() == null) {//password was not  added in configuration file
                    System.out.println("Please enter your password if it's required and hit enter: ");
                    settings.setPassword(new String(System.console().readPassword()).getBytes(StandardCharsets.UTF_8));
                }
            }

            if (cmd.hasOption("proxy-host")) {
                settings.setProxyHost(cmd.getOptionValue("proxy-host"));
            }

            if (cmd.hasOption("proxy-port")) {
                settings.setProxyPort(((Long) cmd.getParsedOptionValue("proxy-port")).intValue());
            }

            if (cmd.hasOption("proxy-user")) {
                settings.setProxyUser(cmd.getOptionValue("proxy-user"));
            }

            if (settings.getOutputFile() == null && StringUtils.isNotEmpty(settings.getProxyUser())) {
                if (cmd.hasOption("proxy-password")) {
                    settings.setProxyPassword(cmd.getOptionValue("proxy-password"));
                } else if (cmd.hasOption("proxy-password-file")) {
                    try {
                        settings.setProxyPassword(FileUtils.readFileToString(new File(cmd.getOptionValue("proxy-password-file"))));
                    } catch (IOException e) {
                        System.out.println("Can not read the password file: " + cmd.getOptionValue("proxy-password-file"));
                        System.exit(ReturnCode.FAILURE.getReturnCode());
                    }
                } else {
                    System.out.println("Please enter your proxy password if it's required and hit enter: ");
                    settings.setProxyPassword(new String(System.console().readPassword()));
                }
            }

            if (cmd.hasOption("check-result")) {
                settings.setCheckResult(true);
            }

            if (cmd.hasOption("check-result-timeout")) {
                settings.setCheckResultTimeout(((Long) cmd.getParsedOptionValue("check-result-timeout")).intValue());
            }

            if (cmd.hasOption("t")) {
                settings.setTags(Arrays.asList(cmd.getOptionValues("t")));
            }

            if (cmd.hasOption("f")) {
                settings.setFields(Arrays.asList(cmd.getOptionValues("f")));
            }

            if (cmd.hasOption("r")) {
                settings.setRelease(((Long) cmd.getParsedOptionValue("r")).intValue());
            }

            if (cmd.hasOption("release-default")) {
                settings.setDefaultRelease();
            }

            if (cmd.hasOption("program")) {
                settings.setProgram(((Long) cmd.getParsedOptionValue("program")).intValue());
            }

            if (cmd.hasOption("m")) {
                settings.setMilestone(((Long) cmd.getParsedOptionValue("m")).intValue());
            }

            if (cmd.hasOption("suite")) {
                settings.setSuite(((Long) cmd.getParsedOptionValue("suite")).intValue());
            }

            if (cmd.hasOption("suite-external-run-id")) {
                settings.setSuiteExternalRunId((cmd.getOptionValue("suite-external-run-id")));
            }

            if (cmd.hasOption("started")) {
                settings.setStarted((Long) cmd.getParsedOptionValue("started"));
            }

            if (cmd.hasOption("a")) {
                settings.setProductAreas(cmd.getOptionValues("a"));
            }

            if (cmd.hasOption("b")) {
                settings.setBacklogItems(cmd.getOptionValues("b"));
            }

            if (cmd.hasOption("build-context-server-id")) {
                settings.setBuildContextServerId(cmd.getOptionValue("build-context-server-id"));
            }

            if (cmd.hasOption("build-context-build-id")) {
                settings.setBuildContextBuildId(cmd.getOptionValue("build-context-build-id"));
            }

            if (cmd.hasOption("build-context-job-id")) {
                settings.setBuildContextJobId(cmd.getOptionValue("build-context-job-id"));
            }

            if (!areSettingsValid(settings)) {
                System.exit(ReturnCode.FAILURE.getReturnCode());
            }

            if(cmd.hasOption(Settings.PROP_ACCESS_TOKEN)){
                settings.setAccessToken(cmd.getOptionValue(Settings.PROP_ACCESS_TOKEN).getBytes(StandardCharsets.UTF_8));
            }

        } catch (ParseException e) {
            System.out.println("Failed to parse : " + e.getMessage());
            System.exit(ReturnCode.FAILURE.getReturnCode());
        }
        return settings;
    }

    private boolean addInputFilesToSettings(CommandLine cmd, Settings settings) {
        List<String> argList = cmd.getArgList();
        List<String> inputFiles = new LinkedList<String>();
        for (String inputFile : argList) {
            if (!new File(inputFile).isFile()) {
                System.out.println("Path '" + inputFile + "' does not lead to a file");
                continue;
            }
            if (!new File(inputFile).canRead()) {
                System.out.println("File '" + inputFile + "' is not readable");
                continue;
            }
            inputFiles.add(inputFile);
        }

        if (inputFiles.isEmpty()) {
            System.out.println("No readable files with tests to push");
            return false;
        }

        settings.setInputXmlFileNames(inputFiles);
        return true;
    }

    private boolean areCmdArgsValid(CommandLine cmd) {
        List<String> argList = cmd.getArgList();
        if (argList.isEmpty()) {
            System.out.println("At least one XML file must be specified as input for push");
            return false;
        }

        for (String arg : argsWithSingleOccurrence) {
            if (cmd.getOptionProperties(arg).size() > 1) {
                System.out.println("Only single occurrence is allowed for argument: '" + arg + "'");
                return false;
            }
        }

        if (cmd.hasOption("i")) {
            for (String arg : argsRestrictedForInternal) {
                if (cmd.hasOption(arg)) {
                    System.out.println("Invalid argument for internal mode: '" + arg + "'");
                    return false;
                }
            }
        }

        //build context must have all properties filled
        boolean buildContextDefined = false;
        for (String arg : argsForBuildContext) {
            if (cmd.hasOption(arg)) {
                buildContextDefined = true;
                break;
            }
        }
        if (buildContextDefined) {
            List<String> missingArgs = new ArrayList<>();
            for (String arg : argsForBuildContext) {
                if (!cmd.hasOption(arg)) {
                    missingArgs.add(arg);
                }
            }
            if (!missingArgs.isEmpty()) {
                System.out.println("For defining build context, need to define additional parameters : " + missingArgs);
                return false;
            }
        }

        if (!isTagFormatValid(cmd, "t") || !isTagFormatValid(cmd, "f")) {
            return false;
        }

        String configurationFile = cmd.getOptionValue("c");
        if (configurationFile != null && !(new File(configurationFile).canRead())) {
            System.out.println("Can not read the configuration file: " + configurationFile);
            return false;
        }

        String passwordFile = cmd.getOptionValue("password-file");
        if (passwordFile != null && !(new File(passwordFile).canRead())) {
            System.out.println("Can not read the password file: " + passwordFile);
            return false;
        }

        String proxyPasswordFile = cmd.getOptionValue("proxy-password-file");
        if (proxyPasswordFile != null && !new File(proxyPasswordFile).canRead()) {
            System.out.println("Can not read the proxy password file: " + passwordFile);
            return false;
        }

        String outputFilePath = cmd.getOptionValue("o");
        if (outputFilePath != null) {
            if (argList.size() != 1) {
                System.out.println("Only single JUnit input file is allowed for output mode");
                return false;
            }
            File outputFile = new File(outputFilePath);
            if (!outputFile.exists()) {
                try {
                    if (!outputFile.createNewFile()) {
                        System.out.println("Can not create the output file: " + outputFile.getAbsolutePath());
                        return false;
                    }
                } catch (IOException e) {
                    System.out.println("Can not create the output file: " + outputFile.getAbsolutePath());
                    return false;
                }
            }
            if (!outputFile.canWrite()) {
                System.out.println("Can not write to the output file: " + outputFile.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private boolean isTagFormatValid(CommandLine cmd, String option) {
        String[] tags = cmd.getOptionValues(option);
        if (tags == null) {
            return true;
        }
        // CODE REVIEW, Johnny, 19Oct2015 - consult with Mirek with regards to localization, this is very probably
        // good for this release, but I can imagine it will have to be relaxed once we for example start to support
        // languages like French (and all their funny characters)
        String partialPatternStr = "\\w([\\s\\w._]+)?\\w";
        String fullPatternStr = String.format("^%s:%s$", partialPatternStr, partialPatternStr);
        Pattern pattern = Pattern.compile(fullPatternStr);
        for (String tag : tags) {
            if (!pattern.matcher(tag).matches()) {
                System.out.println("Tag and field tag arguments must be in TYPE:VALUE format: " + tag);
                return false;
            }
        }
        return true;
    }

    private boolean areSettingsValid(Settings settings) {
        if (settings.getOutputFile() == null) {
            // Server access is required
            if (!isSettingPresent(settings.getServer(), "server")) {
                return false;
            }

            if (!isSettingPresent(settings.getSharedspace(), "sharedspace")) {
                return false;
            }

            if (!isSettingPresent(settings.getWorkspace(), "workspace")) {
                return false;
            }

            if (settings.getProxyHost() != null && settings.getProxyPort() == null) {
                System.out.println("Proxy port was not specified for proxy host: " + settings.getProxyHost());
                return false;
            }

            if (settings.getProxyPassword() != null && settings.getProxyUser() == null) {
                System.out.println("Proxy user name was not specified for proxy password");
                return false;
            }

            if (settings.getCheckResultTimeout() != null && settings.getCheckResultTimeout() < 1) {
                System.out.println("Timeout has to be positive integer");
                return false;
            }

            if (settings.getRelease() != null && settings.isDefaultRelease()) {
                System.out.println("Default release cannot be assigned along with release ID assignment");
                return false;
            }
        }
        return true;
    }

    boolean isSettingPresent(Object setting, String settingName) {
        if (setting == null) {
            System.out.println("Mandatory setting '" + settingName + "' was not specified in the CLI arguments or configuration file");
            return false;
        }
        return true;
    }


    private void printVersion() {
        System.out.println(HEADER);
        System.out.println("Version: " + VERSION);
        System.out.println(FOOTER);
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(CMD_LINE_SYNTAX, HEADER, options, FOOTER);
    }
}
