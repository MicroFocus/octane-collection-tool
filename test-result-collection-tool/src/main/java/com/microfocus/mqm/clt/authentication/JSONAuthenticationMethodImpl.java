package com.microfocus.mqm.clt.authentication;

import com.microfocus.mqm.clt.RestClient;
import com.microfocus.mqm.clt.Settings;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

import java.nio.charset.StandardCharsets;

public class JSONAuthenticationMethodImpl implements AuthenticationMethod {

    private final String URI_AUTHENTICATION = "authentication/sign_in";

    private final String LWSSO_COOKIE_NAME = "LWSSO_COOKIE_KEY";

    private final CookieStore cookieStore;
    public JSONAuthenticationMethodImpl(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    @Override
    public HttpPost getLoginRequest(Settings settings) {
        return authenticate(settings);
    }

    private HttpPost authenticate(Settings settings) {
        HttpPost post = new HttpPost(RestClient.createBaseUri(URI_AUTHENTICATION,settings.getServer()));
        post.setHeader("HPECLIENTTYPE", "HPE_CI_CLIENT");


        byte[] password = settings.getPassword() != null ? settings.getPassword() : new byte[0];
        String userPart = String.format("{\"user\":\"%s\",\"password\":\"",settings.getUser() != null ? settings.getUser() : "");
        String stringPartEnd = "\"}";
        byte[] authorization = AuthenticationUtils.mergeArrays(userPart.getBytes(StandardCharsets.UTF_8), password,stringPartEnd.getBytes(StandardCharsets.UTF_8));

        ByteArrayEntity httpEntity = new ByteArrayEntity(authorization, ContentType.APPLICATION_JSON);
        post.setEntity(httpEntity);

        return post;
    }

    public Cookie handleCookies(HttpRequest request, HttpResponse response){
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookie.getName().equals(LWSSO_COOKIE_NAME)) {
                return cookie;
            }
        }
        return null;
    }
}
