/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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
		return getStringValue("mqm.user", "admin");
	}

	public static String getPassword() {
		return getStringValue("mqm.password", "changeit");
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
