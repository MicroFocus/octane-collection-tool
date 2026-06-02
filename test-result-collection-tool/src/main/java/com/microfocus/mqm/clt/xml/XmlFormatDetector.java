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

package com.microfocus.mqm.clt.xml;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Detects the format of an XML test result file by inspecting its root element.
 *
 * <ul>
 *   <li>{@code <testsuite>} or {@code <testsuites>} → {@link XmlTestResultFormat#JUNIT}</li>
 *   <li>{@code <test-run>} → {@link XmlTestResultFormat#NUNIT}</li>
 *   <li>Anything else → {@link XmlTestResultFormat#UNKNOWN}</li>
 * </ul>
 */
public class XmlFormatDetector {

    public enum XmlTestResultFormat {
        JUNIT,
        NUNIT,
        UNKNOWN
    }

    private static final String JUNIT_ROOT_ELEMENT = "testsuite";
    private static final String JUNIT_ALTERNATIVE_ROOT_ELEMENT = "testsuites";
    private static final String NUNIT_ROOT_ELEMENT = "test-run";

    private XmlFormatDetector() {}

    /**
     * Reads only the root element of {@code file} and returns the detected format.
     *
     * @param file the XML file to inspect
     * @return the detected {@link XmlTestResultFormat}
     * @throws IOException        if the file cannot be opened
     * @throws XMLStreamException if the XML is malformed before or at the root element
     */
    public static XmlTestResultFormat detect(File file) throws IOException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Disables DTDs (prevents DOCTYPE declaration)
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // Disables external entity resolution
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        // Enables namespace awareness
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        // Prevents automatic expansion of entities
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        // Prevents combining adjacent text and CDATA
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        // Disables validation, don't validate against external schema
        factory.setProperty(XMLInputFactory.IS_VALIDATING, false);

        try (FileInputStream fis = new FileInputStream(file)) {
            XMLStreamReader reader = factory.createXMLStreamReader(fis);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String localName = reader.getLocalName();
                        if (JUNIT_ROOT_ELEMENT.equals(localName) || JUNIT_ALTERNATIVE_ROOT_ELEMENT.equals(localName)) {
                            return XmlTestResultFormat.JUNIT;
                        } else if (NUNIT_ROOT_ELEMENT.equals(localName)) {
                            return XmlTestResultFormat.NUNIT;
                        } else {
                            return XmlTestResultFormat.UNKNOWN;
                        }
                    }
                }
            } finally {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    System.out.println("Warning: failed to close XML reader for file '" + file.getAbsolutePath() + "': " + e.getMessage());
                }
            }
        }
        return XmlTestResultFormat.UNKNOWN;
    }
}

