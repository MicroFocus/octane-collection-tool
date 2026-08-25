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

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects and pushes code coverage reports to the Octane
 * {@code /analytics/ci/coverage} endpoint.
 *
 * <p>Coverage reports are pushed one file at a time using an HTTP PUT request.
 * Supported formats (passed via {@code --coverage-report-type}):
 * <ul>
 *   <li>{@code JACOCOXML} - JaCoCo XML report</li>
 *   <li>{@code SONAR}     - SonarQube generic coverage XML</li>
 *   <li>{@code LCOV}      - LCOV / gcov trace file</li>
 * </ul>
 *
 * <p>Build-context parameters ({@code --build-context-server-id},
 * {@code --build-context-job-id}, {@code --build-context-build-id}) are passed
 * as query parameters so that Octane can associate the coverage data with the
 * correct pipeline run.
 */
public class CoverageCollectionTool {

    private final Settings settings;
    private RestClient client;

    public CoverageCollectionTool(Settings settings) {
        this.settings = settings;
    }

    /**
     * Iterates over all resolved coverage-report files held in {@link Settings}
     * and pushes each one to the Octane coverage endpoint.
     */
    public void collectAndPushCoverageReports() {
        if (settings.getCoverageReportFileNames() == null || settings.getCoverageReportFileNames().isEmpty()) {
            System.out.println("No coverage report files to push");
            return;
        }

        client = new RestClient(settings);
        try {
            for (String fileName : settings.getCoverageReportFileNames()) {
                File coverageFile = new File(fileName);
                try {
                    client.putCoverageReport(coverageFile, settings.getCoverageReportType());
                    System.out.println("Coverage report from file '" + fileName + "' ("
                            + settings.getCoverageReportType() + ") was pushed to the server");
                } catch (Exception e) {
                    releaseClient();
                    System.out.println("Unable to push coverage report '" + fileName + "': " + e.getMessage());
                    System.exit(ReturnCode.FAILURE.getReturnCode());
                }
            }
        } finally {
            releaseClient();
        }
    }

    private void releaseClient() {
        changeLogLevel(Level.SEVERE);
        if (client != null) {
            try {
                client.release();
            } catch (IOException e) {
                System.out.println("Unable to release client session: " + e.getMessage());
            }
        }
    }

    /**
     * Sets the output level of all root logger handlers to the given {@code level}.
     *
     * <p>Called to suppress the noisy-but-harmless Apache HttpClient shutdown messages (e.g. "Connection discarded",
     * "SSL session invalidated") that would otherwise appear on the console during connection-pool
     * teardown. As this is a short-lived CLI tool that exits immediately after, the level is not
     * restored.</p>
     *
     * @param level the log level to apply to all root logger handlers
     */
    private void changeLogLevel(Level level) {
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            handler.setLevel(level);
        }
    }
}