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


package com.microfocus.mqm.atrf.core.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by berkovir on 20/12/2016.
 */
public class ConfigurationUtilities {

    private static boolean writeToLastSentRun = true;

    private static String lastSentFilePath;
    private static FetchConfiguration fetchConfiguration;
    static final Logger logger = LogManager.getLogger();


    public static void setConfiguration(FetchConfiguration conf) {
        fetchConfiguration = conf;
    }


    private static String getLastSentFilePath() {

        if (lastSentFilePath == null) {

            String almHost = "ALM", octaneHost = "OCTANE";
            try {
                URI uri = new URI(fetchConfiguration.getAlmServerUrl());
                almHost = uri.getHost();

                uri = new URI(fetchConfiguration.getOctaneServerUrl());
                octaneHost = uri.getHost();
            } catch (URISyntaxException e) {

            }

            String splitter = "_";
            StringBuilder sb = new StringBuilder("logs/lastSent").append(splitter);
            sb.append(almHost).append(splitter);
            sb.append(fetchConfiguration.getAlmDomain()).append(splitter);
            sb.append(fetchConfiguration.getAlmProject()).append(splitter);
            sb.append(octaneHost).append(splitter);
            sb.append(fetchConfiguration.getOctaneSharedSpaceId()).append(splitter);
            sb.append(fetchConfiguration.getOctaneWorkspaceId());
            sb.append(".txt");
            lastSentFilePath = sb.toString();
        }
        return lastSentFilePath;
    }

    public static String readLastSentRunId() {
        String value = null;
        Path path = Paths.get(getLastSentFilePath());
        if (Files.exists(path)) {
            try {
                value = new String(Files.readAllBytes(path)).trim();
                int intValue = Integer.parseInt(value);

            } catch (IOException e) {
                logger.error(String.format("Failed to read LastSentRunId from %s : %s", path.toFile().getAbsolutePath()), e.getMessage());
            } catch (NumberFormatException e) {
                logger.error(String.format("Failed to parse content of LastSentRunId from %s : %s", path.toFile().getAbsolutePath()), e.getMessage());
            }
        }

        return value;
    }

    public static void saveLastSentRunId(String lastSentId) {

        if (writeToLastSentRun) {
            Path path = Paths.get(getLastSentFilePath());
            try {
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                Files.write(path, lastSentId.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

            } catch (IOException e) {
                logger.error(String.format("Failed to write LAST_SENT_ID to %s", path.toFile().getAbsolutePath()));
                writeToLastSentRun = false;
            } catch (Exception e) {
                logger.error(String.format("Exception occurred during writing LAST_SENT_ID to %s : %s", path.toFile().getAbsolutePath()), e.getMessage());
                writeToLastSentRun = false;
            }
        }
    }
}
