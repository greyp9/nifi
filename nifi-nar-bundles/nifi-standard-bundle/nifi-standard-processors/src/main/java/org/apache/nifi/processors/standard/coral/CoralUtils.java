package org.apache.nifi.processors.standard.coral;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.xml.processing.parsers.StandardDocumentProvider;
import org.apache.nifi.xml.processing.transform.StandardTransformProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class CoralUtils {

    private CoralUtils() {
    }

    public static byte[] toBytes(final Class<?> c, final String resource) throws IOException {
        final URL url = Objects.requireNonNull(c.getClassLoader().getResource(resource));
        return IOUtils.toByteArray(Objects.requireNonNull(url.openStream()));
    }

    public static Document create(final String rootElementName) {
        final StandardDocumentProvider documentProvider = new StandardDocumentProvider();
        final Document document = documentProvider.newDocument();
        final Element element = document.createElement(rootElementName);
        document.appendChild(element);
        return document;
    }

    public static Document toDocument(final byte[] xhtml) {
        final StandardDocumentProvider documentProvider = new StandardDocumentProvider();
        //documentProvider.setDisallowDoctypeDecl(false);
        return documentProvider.parse(new ByteArrayInputStream(xhtml));
    }

    public static byte[] toXml(final Document document) {
        final Source source = new DOMSource(document);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Result result = new StreamResult(bos);
        final StandardTransformProvider transformProvider = new StandardTransformProvider();
        transformProvider.setOmitXmlDeclaration(true);
        transformProvider.setMethod("xml");
        transformProvider.setIndent(true);
        transformProvider.transform(source, result);
        return bos.toByteArray();
    }

    public static Element getChild(final Element element, final String name) {
        final NodeList elements = element.getElementsByTagName(name);
        return (Element) ((elements.getLength() == 0) ? null : elements.item(0));
    }

    public static Element addChild(final Element element, final String name) {
        return addChild(element, name, null);
    }

    public static Element addChild(final Element element, final String name, final String text) {
        final Document document = element.getOwnerDocument();
        final Element child = document.createElement(name);
        if (text != null) {
            child.setTextContent(text);
        }
        return (Element) element.appendChild(child);
    }
}
