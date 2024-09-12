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

import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class SettingsTest {

    @Test
    public void testSettings() throws IOException, URISyntaxException {
        Settings settings = new Settings();
        settings.load(getClass().getResource("test.properties").getPath());
        Assert.assertEquals("http://localhost:8080/qcbin", settings.getServer());
        Assert.assertEquals(Integer.valueOf(1001), settings.getSharedspace());
        Assert.assertEquals(Integer.valueOf(1002), settings.getWorkspace());
        Assert.assertEquals("admin", settings.getUser());
        Assert.assertEquals("test.proxy.hpe.com", settings.getProxyHost());
        Assert.assertEquals(Integer.valueOf(8282), settings.getProxyPort());
        Assert.assertEquals("proxyadmin", settings.getProxyUser());
    }

    @Test
    public void testSettings_empty() throws IOException, URISyntaxException {
        Settings settings = new Settings();
        try {
            settings.load(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Can not read the default configuration file: config.properties", e.getMessage());
        }
        Assert.assertNull(settings.getServer());
        Assert.assertNull(settings.getSharedspace());
        Assert.assertNull(settings.getWorkspace());
        Assert.assertNull(settings.getUser());
        Assert.assertNull(settings.getProxyHost());
        Assert.assertNull(settings.getProxyPort());
        Assert.assertNull(settings.getProxyUser());

        settings.setDefaultConfigFilenameProvider(new TestDefaultConfigFilenameProvider());
        settings.load(null);
        Assert.assertEquals("http://localhost:8282/qcbin", settings.getServer());
        Assert.assertEquals(Integer.valueOf(1011), settings.getSharedspace());
        Assert.assertEquals(Integer.valueOf(1012), settings.getWorkspace());
        Assert.assertEquals("user", settings.getUser());
        Assert.assertEquals("some.proxy.hpe.com", settings.getProxyHost());
        Assert.assertNull(settings.getProxyPort());
        Assert.assertEquals("proxyuser", settings.getProxyUser());
    }

    @Test
    public void testSettings_invalidIntegerValue() throws IOException, URISyntaxException {
        Settings settings = new Settings();
        try {
            settings.load(getClass().getResource("test.propertiesx").getPath());
            Assert.fail();
        } catch (NumberFormatException e) {
            Assert.assertEquals("For input string: \"invalid-integer\"", e.getMessage());
        }
    }

    @Test
    public void testSettings_productAreas() throws ParseException {
        Settings settings = new Settings();
        Assert.assertNull(settings.getProductAreas());
        settings.setProductAreas(new String[]{"1001"});
        Assert.assertNotNull(settings.getProductAreas());
        Assert.assertEquals(1, settings.getProductAreas().size());
        Assert.assertEquals(Integer.valueOf(1001), settings.getProductAreas().get(0));
        settings.setProductAreas(null);
        Assert.assertNull(settings.getProductAreas());
        try {
            settings.setProductAreas(new String[]{"invalid-integer"});
            Assert.fail();
        } catch (ParseException e) {
            Assert.assertEquals("Unable to parse string to product area ID: invalid-integer", e.getMessage());
        }
    }

    @Test
    public void testSettings_backlogItems() throws ParseException {
        Settings settings = new Settings();
        Assert.assertNull(settings.getBacklogItems());
        settings.setBacklogItems(new String[]{"1001"});
        Assert.assertNotNull(settings.getBacklogItems());
        Assert.assertEquals(1, settings.getBacklogItems().size());
        Assert.assertEquals(Integer.valueOf(1001), settings.getBacklogItems().get(0));
        settings.setBacklogItems(null);
        Assert.assertNull(settings.getBacklogItems());
        try {
            settings.setBacklogItems(new String[]{"invalid-integer"});
            Assert.fail();
        } catch (ParseException e) {
            Assert.assertEquals("Unable to parse string to backlog item ID: invalid-integer", e.getMessage());
        }
    }

    private class TestDefaultConfigFilenameProvider implements Settings.DefaultConfigFilenameProvider {

        @Override
        public String getDefaultConfigFilename() {
            return getClass().getResource("testDefault.properties").getPath();
        }
    }
}
