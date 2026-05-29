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

import com.microfocus.mqm.clt.xml.XmlFormatDetector;
import com.microfocus.mqm.clt.xml.XmlFormatDetector.XmlTestResultFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

public class XmlFormatDetectorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testDetect_junit_testsuite() throws URISyntaxException, IOException, XMLStreamException {
        File file = new File(getClass().getResource("JUnit-minimalAccepted.xml").toURI());
        Assert.assertEquals(XmlTestResultFormat.JUNIT, XmlFormatDetector.detect(file));
    }

    @Test
    public void testDetect_junit_testsuites() throws IOException, XMLStreamException {
        File file = temporaryFolder.newFile("testsuites.xml");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><testsuites><testsuite/></testsuites>");
        }
        Assert.assertEquals(XmlTestResultFormat.JUNIT, XmlFormatDetector.detect(file));
    }

    @Test
    public void testDetect_nunit3() throws URISyntaxException, IOException, XMLStreamException {
        File file = new File(getClass().getResource("NUnit3-allStatuses.xml").toURI());
        Assert.assertEquals(XmlTestResultFormat.NUNIT, XmlFormatDetector.detect(file));
    }

    @Test
    public void testDetect_unknown() throws IOException, XMLStreamException {
        File file = temporaryFolder.newFile("unknown.xml");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><some-unknown-root/>");
        }
        Assert.assertEquals(XmlTestResultFormat.UNKNOWN, XmlFormatDetector.detect(file));
    }
}

