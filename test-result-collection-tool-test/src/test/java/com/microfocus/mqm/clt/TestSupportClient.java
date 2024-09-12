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

import com.microfocus.mqm.clt.model.PagedList;
import com.microfocus.mqm.clt.model.Release;
import com.microfocus.mqm.clt.model.Taxonomy;
import com.microfocus.mqm.clt.model.TestRun;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestSupportClient extends RestClient {

    private static final String URI_RELEASES = "releases?fields=name";
    private static final String URI_TEST_RUN = "runs";
    private static final String URI_TAXONOMY_NODES = "taxonomy_nodes?fields=name,category";

    private static final String FILTERING_FRAGMENT = "query={query}";
    private static final String PAGING_FRAGMENT = "offset={offset}&limit={limit}";
    private static final String ORDER_BY_FRAGMENT = "order_by={order}";

    protected TestSupportClient(Settings settings) {
        super(settings);
    }

    public Release createRelease(String name) throws IOException {
        JSONObject releaseObject = ResourceUtils.readJson("release.json");
        releaseObject.put("name", name);

        JSONObject resultObject = postEntity(URI_RELEASES, releaseObject);
        return new Release(resultObject.getLong("id"), resultObject.getString("name"));
    }

    public Taxonomy createTaxonomyCategory(String name) throws IOException {
        JSONObject taxonomyTypeObject = ResourceUtils.readJson("taxonomyType.json");
        taxonomyTypeObject.put("name", name);

        JSONObject resultObject = postEntity(URI_TAXONOMY_NODES, taxonomyTypeObject);
        return new Taxonomy(resultObject.getLong("id"), resultObject.getString("name"), null);
    }

    public Taxonomy createTaxonomyItem(Long typeId, String name) throws IOException {
        JSONObject taxonomyObject = ResourceUtils.readJson("taxonomy.json");
        taxonomyObject.getJSONObject("category").put("id", typeId);
        taxonomyObject.put("name", name);

        JSONObject resultObject = postEntity(URI_TAXONOMY_NODES, taxonomyObject);
        return new Taxonomy(resultObject.getLong("id"), resultObject.getString("name"),
                new Taxonomy(resultObject.getJSONObject("category").getLong("id"), resultObject.getJSONObject("category").getString("name"), null));
    }

    public PagedList<TestRun> queryTestRuns(String name, int offset, int limit) {
        List<String> conditions = new LinkedList<String>();
        if (!StringUtils.isEmpty(name)) {
            conditions.add(condition("name", "*" + name + "*"));
        }
        return getEntities(getEntityURI(URI_TEST_RUN, conditions, offset, limit, null), offset, new TestRunEntityFactory());
    }

    private JSONObject postEntity(String uri, JSONObject entityObject) throws IOException {
        URI requestURI = createWorkspaceApiUri(uri);
        HttpPost request = new HttpPost(requestURI);
        JSONArray data = new JSONArray();
        data.add(entityObject);
        JSONObject body = new JSONObject();
        body.put("data", data);
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = null;
        try {
            response = execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                String payload = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                throw new IOException("Posting failed with status code " + response.getStatusLine().getStatusCode() + ", reason " + response.getStatusLine().getReasonPhrase() + " and payload: " + payload);
            }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            response.getEntity().writeTo(result);
            JSONObject jsonObject = JSONObject.fromObject(new String(result.toByteArray(), "UTF-8"));
            return jsonObject.getJSONArray("data").getJSONObject(0);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    protected <E> PagedList<E> getEntities(URI uri, int offset, EntityFactory<E> factory) {
        HttpGet request = new HttpGet(uri);
        CloseableHttpResponse response = null;
        try {
            response = execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Entity retrieval failed");
            }
            String entitiesJson = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            JSONObject entities =  JSONObject.fromObject(entitiesJson);

            LinkedList<E> items = new LinkedList<E>();
            for (JSONObject entityObject : getJSONObjectCollection(entities, "data")) {
                items.add(factory.create(entityObject.toString()));
            }
            return new PagedList<E>(items, offset, entities.getInt("total_count"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot retrieve entities from MQM.", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    protected URI getEntityURI(String collection, List<String> conditions, int offset, int limit, String orderBy) {
        Map<String, Object> params = pagingParams(offset, limit);
        StringBuilder template = new StringBuilder(collection + "?" + PAGING_FRAGMENT);

        if (!conditions.isEmpty()) {
            StringBuilder expr = new StringBuilder();
            for (String condition : conditions) {
                if (expr.length() > 0) {
                    expr.append(";");
                }
                expr.append(condition);
            }
            params.put("query", "\"" + expr.toString() + "\"");
            template.append("&" + FILTERING_FRAGMENT);
        }

        if (!StringUtils.isEmpty(orderBy)) {
            params.put("order", orderBy);
            template.append("&" + ORDER_BY_FRAGMENT);
        }

        return createWorkspaceApiUriMap(template.toString(), params);

    }

    private static class TestRunEntityFactory implements EntityFactory<TestRun> {

        @Override
        public TestRun create(String json) {
            JSONObject entityObject = JSONObject.fromObject(json);
            return new TestRun(
                    entityObject.getInt("id"),
                    entityObject.getString("name"),
                    getRelease(entityObject),
                    getTaxonomies(entityObject));
        }

        private Release getRelease(JSONObject entityObject) {
            JSONObject release = entityObject.getJSONObject("release");
            if (release != null && !release.isEmpty()) {
                ReleaseEntityFactory factory = new ReleaseEntityFactory();
                return factory.create(release.toString());
            }
            return null;
        }

        private List<Taxonomy> getTaxonomies(JSONObject entityObject) {
            JSONObject taxonomies = entityObject.getJSONObject("taxonomies");
            if (taxonomies != null && !taxonomies.isEmpty()) {
                TaxonomyEntityFactory factory = new TaxonomyEntityFactory();
                List<Taxonomy> items = new LinkedList<Taxonomy>();
                for (JSONObject taxonomy : getJSONObjectCollection(taxonomies, "data")) {
                    items.add(factory.create(taxonomy.toString()));
                }
                return (items.isEmpty()) ? null : items;
            }
            return null;
        }
    }

    private static class TaxonomyEntityFactory implements EntityFactory<Taxonomy> {

        @Override
        public Taxonomy create(String json) {
            JSONObject entityObject = JSONObject.fromObject(json);
            JSONObject taxonomy_root = entityObject.optJSONObject("category");
            if (taxonomy_root != null) {
                return new Taxonomy(entityObject.getLong("id"), entityObject.getString("name"), create(taxonomy_root.toString()));
            } else {
                return new Taxonomy(entityObject.getLong("id"), entityObject.getString("name"), null);
            }
        }
    }

    private static class ReleaseEntityFactory implements EntityFactory<Release> {

        @Override
        public Release create(String json) {
            JSONObject entityObject = JSONObject.fromObject(json);
            return new Release(entityObject.getLong("id"), entityObject.getString("name"));
        }
    }

    private String condition(String name, String value) {
        return name + "='" + escapeQueryValue(value) + "'";
    }

    private static String escapeQueryValue(String value) {
        return value.replaceAll("(\\\\)", "$1$1").replaceAll("([\"'])", "\\\\$1");
    }
    private Map<String, Object> pagingParams(int offset, int limit) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("offset", offset);
        params.put("limit", limit);
        return params;
    }

    static Collection<JSONObject> getJSONObjectCollection(JSONObject object, String key) {
        JSONArray array = object.getJSONArray(key);
        return (Collection<JSONObject>) array.subList(0, array.size());
    }

    interface EntityFactory<E> {

        E create(String json);

    }
}