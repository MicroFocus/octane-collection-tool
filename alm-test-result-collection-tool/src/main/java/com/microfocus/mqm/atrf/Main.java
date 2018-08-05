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


package com.microfocus.mqm.atrf;

import com.microfocus.mqm.atrf.core.configuration.CliParser;
import com.microfocus.mqm.atrf.core.configuration.ConfigurationUtilities;
import com.microfocus.mqm.atrf.core.configuration.FetchConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class Main {

    static final Logger logger = LogManager.getLogger();


    public static void main(String[] args) {

        long start = System.currentTimeMillis();
        setUncaughtExceptionHandler();


        CliParser cliParser = new CliParser();
        cliParser.handleHelpAndVersionOptions(args);

        configureLog4J();
        logger.info(System.lineSeparator() + System.lineSeparator());
        logger.info("************************************************************************************");
        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
        DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.getDefault());
        logger.info((String.format("Starting Micro Focus ALM Test Result Collection Tool %s %s", dateFormatter.format(new Date()), timeFormatter.format(new Date()))));
        logger.info("************************************************************************************");


        FetchConfiguration configuration = cliParser.parse(args);
        ConfigurationUtilities.setConfiguration(configuration);

        App app = new App(configuration);
        app.start();

        long end = System.currentTimeMillis();
        logger.info(String.format("Finished creating tests and test results on ALM Octane in %s seconds", (end - start) / 1000));
        logger.info(System.lineSeparator());
    }

    private static void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    private static void configureLog4J() {

        //set process Id on local
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        String pid = rt.getName();
        ThreadContext.put("PID", pid);


        String log4jConfiguration = System.getProperty("log4j.configuration");
        if (StringUtils.isEmpty(log4jConfiguration)) {
            //try to take from file
            File f = new File("log4j2.xml");
            URI uri = null;
            if (f.exists() && !f.isDirectory() && f.canRead()) {
                uri = f.toURI();
            } else {
                //take it from resources
                try {
                    uri = Main.class.getClassLoader().getResource("log4j2.xml").toURI();
                } catch (URISyntaxException e) {
                    logger.info("Failed to load Log4j configuration from resource file");
                }
            }

            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(uri);
            //logger.info("Log4j configuration loaded from " + uri.toString());
        } else {
            logger.info("Log4j configuration is loading from log4j.configuration=" + log4jConfiguration);
        }
    }
}
