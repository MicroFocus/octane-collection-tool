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
package com.microfocus.mqm.clt.xml;

import com.microfocus.mqm.clt.Settings;
import com.microfocus.mqm.clt.tests.TestResult;
import com.microfocus.mqm.clt.tests.TestResultStatus;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

public class TestResultXmlWriter {

    private File targetPath;
    private XMLStreamWriter writer;
    private OutputStream outputStream;

    public TestResultXmlWriter(File targetPath) {
        this.targetPath = targetPath;
    }

    public void add(List<TestResult> testResults, Settings settings) throws InterruptedException, XMLStreamException, IOException {
        Iterator<TestResult> items = testResults.iterator();
        initialize(settings);

        while (items.hasNext()) {
            TestResult item = items.next();
            writer.writeStartElement("test_run");
            writer.writeAttribute("package", item.getPackageName());
            writer.writeAttribute("class", item.getClassName());
            writer.writeAttribute("name", item.getTestName());
            writer.writeAttribute("status", item.getResult().toPrettyName());
            writer.writeAttribute("duration", String.valueOf(item.getDuration()));
            writer.writeAttribute("started", String.valueOf(item.getStarted()));

            if (TestResultStatus.FAILED.equals(item.getResult())) {
                writer.writeStartElement("error");
                writer.writeAttribute("type", item.getErrorType());
                writer.writeAttribute("message", item.getErrorMsg());
                writer.writeCharacters(item.getStackTraceStr());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    public void close() throws XMLStreamException {
        if (outputStream != null) {
            writer.writeEndElement(); // test_runs
            writer.writeEndElement(); // test_result
            writer.writeEndDocument();
            writer.close();
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void initialize(Settings settings) throws IOException, InterruptedException, XMLStreamException {
        if (outputStream == null) {
            outputStream = new FileOutputStream(targetPath);
            writer = possiblyCreateIndentingWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream));
            writer.writeStartDocument();

            writer.writeStartElement("test_result");
            writeFields(settings);
            writer.writeStartElement("test_runs");
        }
    }

    private void writeFields(Settings settings) throws XMLStreamException {
        if (settings == null) {
            return;
        }
        if (settings.getRelease() != null) {
            writer.writeStartElement("release_ref");
            writer.writeAttribute("id", String.valueOf(settings.getRelease()));
            writer.writeEndElement(); // release_ref
        }
        if (settings.getBacklogItems() != null) {
            writeRefFields("backlog_items", "backlog_item_ref", settings.getBacklogItems());
        }
        if (settings.getProductAreas() != null) {
            writeRefFields("product_areas", "product_area_ref", settings.getProductAreas());
        }
        if (settings.getFields() != null) {
            writeTypeValueArray("test_fields", "test_field", settings.getFields());
        }
        if (settings.getTags() != null) {
            writeTypeValueArray("environment", "taxonomy", settings.getTags());
        }
    }

    private void writeTypeValueArray(String elementName, String fieldName, List<String> typeValueArray) throws XMLStreamException {
        writer.writeStartElement(elementName);
        for (String typeValueField : typeValueArray) {
            // Array values were validated - are in TYPE:VALUE format
            String [] typeAndValue = typeValueField.split(":");
            writeTypeValueField(fieldName, typeAndValue[0], typeAndValue[1]);
        }
        writer.writeEndElement();
    }

    private void writeTypeValueField(String fieldName, String type, String value) throws XMLStreamException {
        writer.writeStartElement(fieldName);
        writer.writeAttribute("type", type);
        writer.writeAttribute("value", value);
        writer.writeEndElement();
    }

    private void writeRefFields(String elementName, String refName, List<Integer> values) throws XMLStreamException {
        writer.writeStartElement(elementName);
        for (Integer value : values) {
            writer.writeStartElement(refName);
            writer.writeAttribute("id", String.valueOf(value));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private XMLStreamWriter possiblyCreateIndentingWriter(XMLStreamWriter writer) {
        try {
            Class<?> clazz = Class.forName("com.sun.xml.txw2.output.IndentingXMLStreamWriter");
            XMLStreamWriter xmlStreamWriter = (XMLStreamWriter) clazz.getConstructor(XMLStreamWriter.class).newInstance(writer);
            clazz.getMethod("setIndentStep", String.class).invoke(xmlStreamWriter, " ");
            return xmlStreamWriter;
        } catch (Exception e) {
            // do without indentation
            return writer;
        }
    }
}
