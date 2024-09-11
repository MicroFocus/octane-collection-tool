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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class TestResultCollectionToolTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static final String LOCATION = ConnectionProperties.getLocation();
	private static final int SHARED_SPACE = ConnectionProperties.getSharedSpaceId();
    private static final int WORKSPACE = ConnectionProperties.getWorkspaceId();
	private static final String USERNAME = ConnectionProperties.getUsername();
	private static final String PASSWORD = ConnectionProperties.getPassword();
    private static final String PROXY_HOST = ConnectionProperties.getProxyHost();
    private static final Integer PROXY_PORT = ConnectionProperties.getProxyPort();

	private Settings testClientSettings;
	private TestSupportClient testSupportClient;

	@Before
	public void init() {
		testSupportClient = new TestSupportClient(getDefaultSettings());
        testClientSettings = getDefaultSettings();
    }

    @After
    public void cleanup() throws IOException {
        testSupportClient.release();
    }

    @Test
    public void testCollectAndPushTestResults_junit() throws IOException, URISyntaxException, InterruptedException {
        long timestamp = System.currentTimeMillis();
        String typeName = "TaxonomyType" + timestamp;
        Taxonomy taxonomyType = testSupportClient.createTaxonomyCategory(typeName);
        Taxonomy taxonomy = testSupportClient.createTaxonomyItem(taxonomyType.getId(), "Taxonomy" + timestamp);
        String releaseName = "Release" + timestamp;
        Release release = testSupportClient.createRelease(releaseName);
        List<String> tags = new LinkedList<String>();
        tags.add("TaxonomyType" + timestamp + ":" + "Taxonomy" + timestamp);
        String junitXml1 = ResourceUtils.readContent("JUnit1.xml")
                .replaceAll("%%%TIMESTAMP%%%", String.valueOf(timestamp));
        final File junit1 = temporaryFolder.newFile();
        FileUtils.write(junit1, junitXml1);
        String junitXml2 = ResourceUtils.readContent("JUnit2.xml")
                .replaceAll("%%%TIMESTAMP%%%", String.valueOf(timestamp));
        final File junit2 = temporaryFolder.newFile();
        FileUtils.write(junit2, junitXml2);

        List<String> fileNames = new LinkedList<String>();
        fileNames.add(junit1.getPath());
        fileNames.add(junit2.getPath());
        testClientSettings.setInputXmlFileNames(fileNames);
        testClientSettings.setRelease(release.getId().intValue());
        testClientSettings.setTags(tags);
        testClientSettings.setCheckResult(true);
        TestResultCollectionTool testResultCollectionTool = new TestResultCollectionTool(testClientSettings);
        testResultCollectionTool.collectAndPushTestResults();

        PagedList<TestRun> pagedList = testSupportClient.queryTestRuns("sampleTest1" + timestamp, 0, 50);
        Assert.assertEquals(1, pagedList.getItems().size());
        TestRun testRun = pagedList.getItems().get(0);
        Assert.assertEquals("sampleTest1" + timestamp, testRun.getName());
        Assert.assertEquals(release.getId(), testRun.getRelease().getId());
        Assert.assertEquals(1, testRun.getTaxonomies().size());
        Assert.assertEquals(taxonomy.getId(), testRun.getTaxonomies().get(0).getId());

        pagedList = testSupportClient.queryTestRuns("sampleTest2" + timestamp, 0, 50);
        Assert.assertEquals(1, pagedList.getItems().size());
        testRun = pagedList.getItems().get(0);
        Assert.assertEquals("sampleTest2" + timestamp, testRun.getName());
        Assert.assertEquals(release.getId(), testRun.getRelease().getId());
        Assert.assertEquals(1, testRun.getTaxonomies().size());
        Assert.assertEquals(taxonomy.getId(), testRun.getTaxonomies().get(0).getId());
    }

    @Test
    public void testCollectAndPushTestResults_internal() throws IOException, URISyntaxException, InterruptedException {
        long timestamp = System.currentTimeMillis();
        String junitXml1 = ResourceUtils.readContent("publicApi.xmlx")
                .replaceAll("%%%TIMESTAMP%%%", String.valueOf(timestamp));
        final File junit1 = temporaryFolder.newFile();
        FileUtils.write(junit1, junitXml1);
        String junitXml2 = ResourceUtils.readContent("publicApi.xml")
                .replaceAll("%%%TIMESTAMP%%%", String.valueOf(timestamp));
        final File junit2 = temporaryFolder.newFile();
        FileUtils.write(junit2, junitXml2);

        List<String> fileNames = new LinkedList<String>();
        fileNames.add(junit1.getPath());
        fileNames.add(junit2.getPath());
        testClientSettings.setInputXmlFileNames(fileNames);
        testClientSettings.setInternal(true);
        TestResultCollectionTool testResultCollectionTool = new TestResultCollectionTool(testClientSettings);
        testResultCollectionTool.collectAndPushTestResults();

        PagedList<TestRun> pagedList = testSupportClient.queryTestRuns("testOne" + timestamp, 0, 50);
        Assert.assertEquals(0, pagedList.getItems().size());

        pagedList = testSupportClient.queryTestRuns("testTwo" + timestamp, 0, 50);
        Assert.assertEquals(1, pagedList.getItems().size());
        TestRun testRun = pagedList.getItems().get(0);
        Assert.assertEquals("testTwo" + timestamp, testRun.getName());
    }


    private Settings getDefaultSettings() {
        Settings settings = new Settings();
        settings.setServer(LOCATION);
        settings.setSharedspace(SHARED_SPACE);
        settings.setWorkspace(WORKSPACE);
        settings.setUser(USERNAME);
        settings.setPassword(PASSWORD);
        settings.setProxyHost(PROXY_HOST);
        settings.setProxyPort(PROXY_PORT);
        if (ConnectionProperties.getProxyUsername() != null) {
            settings.setProxyUser(ConnectionProperties.getProxyUsername());
            settings.setProxyPassword(ConnectionProperties.getProxyPassword());
        }
        return settings;
    }
}
