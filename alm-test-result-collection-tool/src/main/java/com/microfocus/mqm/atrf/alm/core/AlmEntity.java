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

package com.microfocus.mqm.atrf.alm.core;

import com.microfocus.mqm.atrf.core.entities.MapBasedObject;

/**
 * Created by berkovir on 21/11/2016.
 */
public class AlmEntity extends MapBasedObject {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static String FIELD_PARENT_ID = "parent-id";

    private String type;

    public AlmEntity(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    public String getId() {
        return getString(FIELD_ID);
    }

    public String getName() {
        return getString(FIELD_NAME);
    }


    @Override
    public String toString() {
        return getType() + " #" + getId() + " " + getName();
    }
}

