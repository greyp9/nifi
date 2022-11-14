package org.apache.nifi.processors.standard.coral;

import org.apache.nifi.xml.processing.parsers.StandardDocumentProvider;
import org.apache.nifi.xml.processing.transform.StandardTransformProvider;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class CoralXhtmlTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testCreateWriteDocumentStandard() throws IOException {
        final StandardDocumentProvider provider = new StandardDocumentProvider();
        final Document document = provider.newDocument();
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
        transformProvider.transform(source, result);
        logger.info(new String(bos.toByteArray(), StandardCharsets.UTF_8));
    }
}
