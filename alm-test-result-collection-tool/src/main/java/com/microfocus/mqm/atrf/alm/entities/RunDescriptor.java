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


package com.microfocus.mqm.atrf.alm.entities;

import com.microfocus.mqm.atrf.alm.core.AlmEntity;
import com.microfocus.mqm.atrf.alm.core.AlmEntityDescriptor;

/**
 * Created by berkovir on 22/11/2016.
 */
public class RunDescriptor extends AlmEntityDescriptor {

    @Override
    public Class<? extends AlmEntity> getEntityClass() {
        return Run.class;
    }

    @Override
    public String getEntityTypeName() {
        return Run.TYPE;
    }

    @Override
    public String getCollectionName() {
        return Run.COLLECTION_NAME;
    }

    @Override
    public String getAlmRefUrlFormat() {
        //td://p1.radi.myd-vm02033.hpeswlab.net:8080/qcbin/TestRunsModule-00000000090859589?EntityType=IRun&EntityID=6;
        return "%s://%s.%s.%s/TestRunsModule-00000000090859589?EntityType=IRun&EntityID=%s";
    }
}
