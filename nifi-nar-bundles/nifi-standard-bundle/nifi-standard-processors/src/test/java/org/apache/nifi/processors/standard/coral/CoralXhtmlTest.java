/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.standard.coral;

import org.apache.nifi.processors.standard.coral.core.CoralUtils;
import org.apache.nifi.processors.standard.xml.DocumentTypeAllowedDocumentProvider;
import org.apache.nifi.xml.processing.parsers.StandardDocumentProvider;
import org.apache.nifi.xml.processing.transform.StandardTransformProvider;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoralXhtmlTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testCreateWriteDocumentXhtml11() {
        final StandardDocumentProvider documentProvider = new DocumentTypeAllowedDocumentProvider();
        final Document document = documentProvider.newDocument();
        assertNull(document.getInputEncoding());
        assertNull(document.getDoctype());

        final Element html = document.createElement("html");
        document.appendChild(html);
        final Element head = CoralUtils.addChild(html, "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        final Element body = CoralUtils.addChild(html, "body");
        CoralUtils.addChild(body, "div", "Coral Body");

        final Source source = new DOMSource(document);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Result result = new StreamResult(bos);
        final StandardTransformProvider transformProvider = new StandardTransformProvider();
        transformProvider.setOmitXmlDeclaration(true);
        transformProvider.setMethod("xml");
        transformProvider.setIndent(true);
        transformProvider.setDocTypePublic(DOCTYPE_PUBLIC_XHTML11);
        transformProvider.setDocTypeSystem(DOCTYPE_SYSTEM_XHTML11);
        transformProvider.setEncoding(StandardCharsets.UTF_8.name());

        transformProvider.transform(source, result);
        final Document documentTransform = documentProvider.parse(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(StandardCharsets.UTF_8.name(), documentTransform.getInputEncoding());
        final DocumentType doctypeTransform = documentTransform.getDoctype();
        assertEquals(DOCTYPE_PUBLIC_XHTML11, doctypeTransform.getPublicId());
        assertEquals(DOCTYPE_SYSTEM_XHTML11, doctypeTransform.getSystemId());
        logger.trace(new String(bos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void testCreateWriteDocumentStandard() {
        final StandardDocumentProvider documentProvider = new DocumentTypeAllowedDocumentProvider();
        final Document document = documentProvider.newDocument();
        assertNull(document.getInputEncoding());
        assertNull(document.getDoctype());

        final Element html = document.createElement("html");
        document.appendChild(html);
        final Element head = CoralUtils.addChild(html, "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        final Element body = CoralUtils.addChild(html, "body");
        CoralUtils.addChild(body, "div", "Coral Body");

        final Source source = new DOMSource(document);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Result result = new StreamResult(bos);
        final StandardTransformProvider transformProvider = new StandardTransformProvider();
        transformProvider.setOmitXmlDeclaration(true);
        transformProvider.setMethod("xml");
        transformProvider.setIndent(true);
        transformProvider.setDocTypeSystem(DOCTYPE_SYSTEM_COMPAT);
        transformProvider.setEncoding(StandardCharsets.UTF_8.name());

        transformProvider.transform(source, result);
        final Document documentTransform = documentProvider.parse(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(StandardCharsets.UTF_8.name(), documentTransform.getInputEncoding());
        final DocumentType doctypeTransform = documentTransform.getDoctype();
        assertNull(doctypeTransform.getPublicId());
        assertEquals(DOCTYPE_SYSTEM_COMPAT, doctypeTransform.getSystemId());
        logger.trace(new String(bos.toByteArray(), StandardCharsets.UTF_8));
    }

    private static final String DOCTYPE_PUBLIC_XHTML11 = "-//W3C//DTD XHTML 1.1//EN";
    private static final String DOCTYPE_SYSTEM_XHTML11 = "https://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";
    private static final String DOCTYPE_SYSTEM_COMPAT = "about:legacy-compat";
}
