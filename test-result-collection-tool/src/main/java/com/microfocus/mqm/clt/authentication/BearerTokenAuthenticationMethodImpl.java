/*
 *     Copyright 2015-2023 Open Text
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
package com.microfocus.mqm.clt.authentication;

import com.microfocus.mqm.clt.Settings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;

import java.nio.charset.StandardCharsets;

/**
 * Bearer Token authentication method.
 *
 * This authentication method uses a pre-obtained bearer token that is added
 * directly to the Authorization header of each request. No login/authentication
 * flow is required.
 */
public class BearerTokenAuthenticationMethodImpl implements AuthenticationMethod {

    @Override
    public HttpPost getLoginRequest(Settings settings) {
        throw new UnsupportedOperationException(
                "Bearer token authentication does not require a login request");
    }

    @Override
    public Cookie handleCookies(HttpRequest request, HttpResponse response) {
        return null;
    }

    @Override
    public boolean isPreAuthenticated() {
        return true;
    }

    @Override
    public void addAuthorizationHeader(HttpUriRequest request, Settings settings) {
        byte[] token = settings.getBearerToken().orElseThrow(
                () -> new IllegalStateException("Bearer token is not set"));
        String tokenString = new String(token, StandardCharsets.UTF_8);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenString);
    }
}

