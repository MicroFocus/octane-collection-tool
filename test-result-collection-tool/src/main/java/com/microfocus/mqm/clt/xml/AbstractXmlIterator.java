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

import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public abstract class AbstractXmlIterator<E> {

    private InputStream is;
    private XMLEventReader reader;
    private LinkedList<E> queue;
    private boolean closed;

    public AbstractXmlIterator(File xmlFile) throws XMLStreamException, FileNotFoundException {
        this.is = new FileInputStream(xmlFile);
        reader = createXmlInputFactory().createXMLEventReader(is, "UTF-8");
        queue = new LinkedList<E>();
    }

    public boolean hasNext() throws XMLStreamException, IOException, InterruptedException {
        while (queue.isEmpty() && !closed) {
            if (reader.hasNext()) {
                onEvent(reader.nextEvent());
            } else {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    // close quietly
                }
                IOUtils.closeQuietly(is);
                closed = true;
            }
        }
        return !queue.isEmpty();
    }

    public E next() throws XMLStreamException, IOException, InterruptedException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            return queue.removeFirst();
        }
    }

    protected abstract void onEvent(XMLEvent event) throws IOException, InterruptedException;

    protected void addItem(E item) {
        queue.add(item);
    }

    private static XMLInputFactory createXmlInputFactory() {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return xmlFactory;
    }
}
