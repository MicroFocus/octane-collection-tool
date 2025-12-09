package com.microfocus.mqm.clt.authentication;

import com.microfocus.mqm.clt.RestClient;
import com.microfocus.mqm.clt.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenExchangeAuthenticationMethodImpl implements AuthenticationMethod {


    private final String EXCHANGE_TOKEN_URL = "osp/a/au/auth/oauth2/token";
    private final String ACCESS_TOKEN_COOKIE_KEY = "access_token";

    private final String TOKEN_EXCHANGE_GRANT_TYPE_KEY = "grant_type";
    private final String TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE_KEY = "subject_token_type";
    private final String TOKEN_EXCHANGE_SUBJECT_TOKEN_KEY = "subject_token";
    private final String TOKEN_EXCHANGE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private final String TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

    @Override
    public HttpPost getLoginRequest(Settings settings) {
        return authenticate(settings);
    }

    private HttpPost authenticate(Settings settings){

        HttpPost post = new HttpPost(RestClient.createBaseUri(EXCHANGE_TOKEN_URL,settings.getServer()));
        post.setHeader("HPECLIENTTYPE", "HPE_CI_CLIENT");

        String username = (settings.getUser() != null ? settings.getUser() : "") + ":";
        byte[] password = settings.getPassword() != null ? settings.getPassword() : new byte[0];
        byte[] basicAuth = AuthenticationUtils.mergeArrays(username.getBytes(StandardCharsets.UTF_8), password);

        byte[] basicAuthByte = Base64.getEncoder()
                .encode(basicAuth);
        post.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(basicAuthByte,StandardCharsets.UTF_8));

        byte[] body = (TOKEN_EXCHANGE_GRANT_TYPE_KEY + "=" + URLEncoder.encode(TOKEN_EXCHANGE_GRANT_TYPE, StandardCharsets.UTF_8) +
                "&" + TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE_KEY + "=" + URLEncoder.encode(TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE, StandardCharsets.UTF_8) +
                "&" + TOKEN_EXCHANGE_SUBJECT_TOKEN_KEY + "=")
                .getBytes(StandardCharsets.UTF_8);

        byte[] byteContent = AuthenticationUtils.mergeArrays(body, settings.getAccessToken().get());

        post.setEntity(new ByteArrayEntity(byteContent,ContentType.APPLICATION_FORM_URLENCODED));

        post.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        return post;

    }

    public Cookie handleCookies(HttpRequest request, HttpResponse response) {
        try {
            JSONObject responseJson = new JSONObject(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            String accessToken = responseJson.optString(ACCESS_TOKEN_COOKIE_KEY);
            BasicClientCookie authToken = new BasicClientCookie(ACCESS_TOKEN_COOKIE_KEY, accessToken);

            authToken.setDomain(URI.create(request.getRequestLine().getUri()).getHost());
            return authToken;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse access token from response", e);
        }
    }



}
