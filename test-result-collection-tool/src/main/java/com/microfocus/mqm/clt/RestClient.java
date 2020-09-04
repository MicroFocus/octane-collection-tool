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

package com.microfocus.mqm.clt;

import com.microfocus.mqm.clt.tests.TestResultPushStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.*;
import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class RestClient {

    private static final String URI_AUTHENTICATION = "authentication/sign_in";
    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";
    private static final String HEADER_VALUE_BASIC_AUTH = "Basic ";
    //private static final String HEADER_CLIENT_TYPE = "HPECLIENTTYPE";
    private static final String HPSSO_HEADER_NAME = "HPSSO-HEADER-CSRF";
    private static final String LWSSO_COOKIE_NAME = "LWSSO_COOKIE_KEY";

    static final String URI_LOGOUT = "authentication/sign_out";

    private static final String SHARED_SPACE_API_URI = "api/shared_spaces/{0}";
    private static final String WORKSPACE_API_URI = SHARED_SPACE_API_URI + "/workspaces/{1}";

    private static final String URI_TEST_RESULT_PUSH = "test-results?skip-errors={0}";
    private static final String URI_TEST_RESULT_STATUS = "test-results/{0}";

    private static final String URI_PARAM_ENCODING = "UTF-8";

    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final int DEFAULT_CONNECTION_TIMEOUT = 20000; // in milliseconds
    public static final int DEFAULT_SO_TIMEOUT = 40000; // in milliseconds
    private CookieStore cookieStore;
    private Cookie LWSSO_TOKEN;
    //private String CSRF_TOKEN;
    //private String LWSSO_TOKEN;


    private CloseableHttpClient httpClient;
    private Settings settings;

    private volatile boolean isLoggedIn = false;

    public RestClient(Settings settings) {
        this.settings = settings;

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if(settings.getServer().trim().toLowerCase().startsWith("https")){
            configureSSLContext(httpClientBuilder);
        }

        cookieStore = new BasicCookieStore();
        httpClientBuilder.setDefaultCookieStore(cookieStore);
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setSocketTimeout(DEFAULT_SO_TIMEOUT)
                .setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);



        // proxy setting
        if (StringUtils.isNotEmpty(settings.getProxyHost())) {
            HttpHost proxy = new HttpHost(settings.getProxyHost(), settings.getProxyPort());
            requestConfigBuilder.setProxy(proxy);
            if (settings.getProxyUser() != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(settings.getProxyHost(), settings.getProxyPort()),
                        new UsernamePasswordCredentials(settings.getProxyUser(), settings.getProxyPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }
        httpClient = httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build()).build();

    }

    private void configureSSLContext(HttpClientBuilder httpClientBuilder) {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManagers(), new java.security.SecureRandom());
        } catch (Exception e) {
            sslContext = SSLContexts.createSystemDefault();
        }
        HostnameVerifier hostnameVerifier = new CustomHostnameVerifier();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        //set new connection manager
        httpClientBuilder.setConnectionManager(connectionManager);
    }

    private TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        TrustManager[] tmArr = tmf.getTrustManagers();
        if (tmArr.length == 1 && tmArr[0] instanceof X509TrustManager) {
            final X509TrustManager defaultTm = (X509TrustManager) tmArr[0];
            TrustManager myTM = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.getAcceptedIssuers();
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(certs, authType);
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    try {
                        defaultTm.checkServerTrusted(certs, authType);
                    } catch (CertificateException e) {
                        for (X509Certificate cer : certs) {
                            if (cer.getIssuerDN().getName() != null && cer.getIssuerDN().getName().toLowerCase().contains("microfocus")) {
                                return;
                            }
                        }
                        throw e;
                    }
                }
            };

            return new TrustManager[]{myTM};
        } else {
            //add log
            return tmArr;
        }
    }

    public static final class CustomHostnameVerifier implements HostnameVerifier {
        private final HostnameVerifier defaultVerifier = new DefaultHostnameVerifier();

        public boolean verify(String host, SSLSession sslSession) {
            boolean result = defaultVerifier.verify(host, sslSession);
            if (!result) {
                try {
                    Certificate[] ex = sslSession.getPeerCertificates();
                    X509Certificate x509 = (X509Certificate) ex[0];
                    Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
                    for (List<?> namePair : altNames) {
                        if (namePair != null &&
                                namePair.size() > 1 &&
                                namePair.get(1) instanceof String &&
                                "*.saas.microfocus.com".equals(namePair.get(1))) {
                            result = true;
                            break;
                        }
                    }
                } catch (CertificateParsingException | SSLException cpe) {
                    //add log
                }
            }
            return result;
        }
    }

    public long postTestResult(HttpEntity entity) throws IOException, ValidationException {
        HttpPost request = new HttpPost(createWorkspaceApiUri(URI_TEST_RESULT_PUSH, settings.isSkipErrors()));
        request.setEntity(entity);
        CloseableHttpResponse response = null;
        JSONObject jsonObject;
        try {
            response = execute(request);
            String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                //failed to parse json
                throw new RuntimeException("Test result post ended with status code (" +  response.getStatusLine().getStatusCode()
                        + ").Validate that the URL of the ALM Octane server is defined with its fully qualified domain name. Original message : " + json);
            }

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
                String description = "";
                if (jsonObject.has("description")) {
                    description = ": " + jsonObject.getString("description");
                }
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST && settings.isInternal()) {
                    // Error was probably caused by XSD validation failure
                    throw new ValidationException("Test result XML was refused by server" + description);
                }
                throw new RuntimeException("Test result post failed with status code (" +
                        response.getStatusLine().getStatusCode() + ")" + description);
            } else {
                return jsonObject.getLong("id");
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public TestResultPushStatus getTestResultStatus(long id) {
        HttpGet request = new HttpGet(createWorkspaceApiUri(URI_TEST_RESULT_STATUS, id));
        CloseableHttpResponse response = null;
        try {
            response = execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Result status retrieval failed");
            }
            String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            Date until = null;
            if (jsonObject.has("until")) {
                try {
                    until = parseDatetime(jsonObject.getString("until"));
                } catch (ParseException e) {
                    throw new RuntimeException("Cannot obtain status", e);
                }
            }
            return new TestResultPushStatus(jsonObject.getString("status"), until, jsonObject.optString("errorDetails"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot obtain status.", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

//    private void doFirstLogin() throws IOException{
//        if (!isLoggedIn) {
//            login();
//        }
//    }
    protected CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        //doFirstLogin();
        if (LWSSO_TOKEN == null) {
            login();
        }
        HttpContext localContext = new BasicHttpContext();
        CookieStore localCookies = new BasicCookieStore();
        localCookies.addCookie(LWSSO_TOKEN);
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, localCookies);
        addClientTypeHeader(request);
        CloseableHttpResponse response = httpClient.execute(request, localContext);
        if (isLoginNecessary(response)) { // if request fails with 401 do login and execute request again
            HttpClientUtils.closeQuietly(response);
            login();
            localCookies.clear();
            localCookies.addCookie(LWSSO_TOKEN);
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, localCookies);
            response = httpClient.execute(request, localContext);
        }
        return response;
    }

    private boolean isLoginNecessary(HttpResponse response) {
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    protected synchronized void login() throws IOException {
        authenticate();
        isLoggedIn = true;
    }

    private void authenticate() throws IOException {
        HttpPost post = new HttpPost(createBaseUri(URI_AUTHENTICATION));
        addClientTypeHeader(post, true);

        String username = settings.getUser() != null ? settings.getUser() : "";
        String password = settings.getPassword() != null ? settings.getPassword() : "";
        String authorization = "{\"user\":\"" + username + "\",\"password\":\"" + password + "\"}";
        StringEntity httpEntity = new StringEntity(authorization, ContentType.APPLICATION_JSON);
        post.setEntity(httpEntity);

        HttpResponse response = null;
        try {
            cookieStore.clear();
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Authentication failed: code=" + response.getStatusLine().getStatusCode() + "; reason=" + response.getStatusLine().getReasonPhrase());
            } else {
                handleCookies();
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public void release() throws IOException {
        logout();
    }

    protected synchronized void logout() throws IOException {
        if (isLoggedIn) {
            HttpPost post = new HttpPost(createBaseUri(URI_LOGOUT));
            addClientTypeHeader(post);
            HttpResponse response = null;
            try {
                response = httpClient.execute(post);
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK
                        && response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) { // required until defect #2919 is fixed
                    throw new RuntimeException("Logout failed: code=" + response.getStatusLine().getStatusCode() + "; reason=" + response.getStatusLine().getReasonPhrase());
                }
                isLoggedIn = false;
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    protected URI createWorkspaceApiUri(String template, Object... params) {
        return URI.create(createBaseUri(WORKSPACE_API_URI, settings.getSharedspace(), settings.getWorkspace()).toString() + "/" + resolveTemplate(template, asMap(params)));
    }

    protected URI createWorkspaceApiUriMap(String template, Map<String, ?> params) {
        return URI.create(createBaseUri(WORKSPACE_API_URI, settings.getSharedspace(), settings.getWorkspace()).toString() + "/" + resolveTemplate(template, params));
    }

    private URI createBaseUri(String template, Object... params) {
        String result = settings.getServer() + "/" + resolveTemplate(template, asMap(params));
        return URI.create(result);
    }

    private String resolveTemplate(String template, Map<String, ?> params) {
        String result = template;
        for (String param : params.keySet()) {
            Object value = params.get(param);
            result = result.replaceAll(Pattern.quote("{" + param + "}"), encodeParam(value == null ? "" : value.toString()));
        }
        return result;
    }

    private String encodeParam(String param) {
        try {
            return URLEncoder.encode(param, URI_PARAM_ENCODING).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding used for URI parameter encoding.", e);
        }
    }

    private void addClientTypeHeader(HttpUriRequest request) {
        addClientTypeHeader(request, false);
    }
//
    private void addClientTypeHeader(HttpUriRequest request, boolean isLoginRequest) {
        request.setHeader("HPECLIENTTYPE", "HPE_CI_CLIENT");
//        if (!isLoginRequest) {
//            if (request.getFirstHeader(HPSSO_HEADER_NAME) == null) {
//                request.setHeader(HPSSO_HEADER_NAME, CSRF_TOKEN.getValue());
//            }
//        }
    }

    private Date parseDatetime(String datetime) throws ParseException {
        return new SimpleDateFormat(DATETIME_FORMAT).parse(datetime);
    }

    private Map<String, Object> asMap(Object... params) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < params.length; i++) {
            map.put(String.valueOf(i), params[i]);
        }
        return map;
    }
    private void handleCookies() {
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookie.getName().equals(LWSSO_COOKIE_NAME)) {
                LWSSO_TOKEN = cookie;
            }
        }
//        boolean isCSRF = false;
//        //boolean isLWSSO = false;
//        Header[] headers = response.getHeaders("Set-Cookie");
//        for (Header h : headers) {
//            HeaderElement[] he = h.getElements();
//            for (HeaderElement e : he) {
//                if (e.getName().equals(HPSSO_COOKIE_NAME)) {
//                    CSRF_TOKEN = e.getValue();
//                    isCSRF = true;
//                    break;
//                }else if(e.getName().equals(LWSSO_COOKIE_NAME)){
//                        LWSSO_TOKEN = e.getValue();
//                    //isLWSSO =
//                }
//            }
////            if (isCSRF && ) {
////                break;
////            }
//        }
    }
}
