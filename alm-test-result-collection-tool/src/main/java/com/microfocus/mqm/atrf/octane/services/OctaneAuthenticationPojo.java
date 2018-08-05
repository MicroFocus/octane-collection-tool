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

/**
 * Created by berkovir on 05/12/2016.
 */
public class OctaneAuthenticationPojo {

    private String user;
    private String password;
    private boolean enable_csrf;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null) {
            this.password = "";
        } else {
            this.password = password;
        }
    }

    public boolean getEnable_csrf() {
        return enable_csrf;
    }

    public void setEnable_csrf(boolean enable_csrf) {
        this.enable_csrf = enable_csrf;
    }
}
