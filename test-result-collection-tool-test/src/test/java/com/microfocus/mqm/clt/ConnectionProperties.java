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

public class ConnectionProperties {

	public static String getLocation() {
		return getStringValue("mqm.location", "http://localhost:8080");
	}

	public static int getSharedSpaceId() {
		return getIntValue("mqm.sharedSpace", 1001);
	}

	public static int getWorkspaceId() {
		return getIntValue("mqm.workspace", 1002);
	}

	public static String getUsername() {
		return getStringValue("mqm.user", "sa@nga");
	}

	public static String getPassword() {
		return getStringValue("mqm.password", "Welcome1");
	}

    public static String getProxyHost() {
        return getStringValue("mqm.proxyHost", null);
    }

    public static Integer getProxyPort() {
        return getIntValue("mqm.proxyPort", null);
    }

    public static String getProxyUsername() {
        return getStringValue("mqm.proxyUsername", null);
    }

    public static String getProxyPassword() {
        return getStringValue("mqm.proxyPassword", null);
    }

	private static Integer getIntValue(String propName, Integer defaultValue) {
		String value = System.getProperty(propName);
		return value != null ? Integer.valueOf(value) : defaultValue;
	}

	private static String getStringValue(String propName, String defaultValue) {
		String value = System.getProperty(propName);
		return value != null ? value : defaultValue;
	}
}
