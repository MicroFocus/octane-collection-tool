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


package com.microfocus.mqm.atrf.octane.services;

import com.microfocus.mqm.atrf.core.rest.RestConnector;
import com.microfocus.mqm.atrf.octane.core.OctaneEntityCollection;
import com.microfocus.mqm.atrf.octane.core.OctaneTestResultOutput;
import com.microfocus.mqm.atrf.octane.entities.Test;
import com.microfocus.mqm.atrf.octane.entities.Workspace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by berkovir on 05/12/2016.
 */
public class OctaneWrapperService {

    static final Logger logger = LogManager.getLogger();
    RestConnector restConnector;
    OctaneEntityService octaneEntityService;


    public OctaneWrapperService(String baseUrl, long sharedSpaceId, long workspaceId) {

        restConnector = new RestConnector();
        restConnector.setBaseUrl(baseUrl);
        restConnector.setCSRF("HPSSO-HEADER-CSRF", "HPSSO_COOKIE_CSRF");

        octaneEntityService = new OctaneEntityService(restConnector);
        octaneEntityService.setSharedSpaceId(sharedSpaceId);
        octaneEntityService.setWorkspaceId(workspaceId);
    }

    public boolean login(String user, String password) {
        return octaneEntityService.login(user, password);
    }

    public boolean validateConnectionToWorkspace() {
        try {
            OctaneQueryBuilder qb = OctaneQueryBuilder.create();
            qb.addQueryCondition("id", "0");
            octaneEntityService.getEntities(Test.TYPE, qb);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateConnectionToSharedspace() {
        try {
            OctaneQueryBuilder qb = OctaneQueryBuilder.create();
            qb.addPageSize(1);
            // qb.addQueryCondition("id","1002");
            OctaneEntityCollection col = octaneEntityService.getEntities(Workspace.TYPE, qb);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public OctaneTestResultOutput postTestResults(String xml) {
        return octaneEntityService.postTestResults(xml);
    }

    public OctaneTestResultOutput getTestResultStatus(OctaneTestResultOutput output) {
        return octaneEntityService.getTestResultStatus(output);
    }
}
