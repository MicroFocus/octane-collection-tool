package com.microfocus.mqm.clt.authentication;

import com.microfocus.mqm.clt.Settings;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;

public interface AuthenticationMethod {
    HttpPost getLoginRequest(Settings settings);

    Cookie handleCookies(HttpRequest request, HttpResponse response);

    default boolean isPreAuthenticated() {
        return false;
    }

    default void addAuthorizationHeader(HttpUriRequest request, Settings settings) {
        // No-op by default - cookie-based authentication methods don't need this
    }
}
