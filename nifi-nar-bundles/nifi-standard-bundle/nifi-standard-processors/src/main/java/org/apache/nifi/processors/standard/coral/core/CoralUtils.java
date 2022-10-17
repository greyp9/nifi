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
package org.apache.nifi.processors.standard.coral.core;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.xml.processing.parsers.StandardDocumentProvider;
import org.apache.nifi.xml.processing.transform.StandardTransformProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

@SuppressWarnings("unused")
public final class CoralUtils {

    private CoralUtils() {
    }

    private static final String XSD_UTC_MILLI = "yyyy-MM-dd HH:mm:ss.SSS'Z'";
    private static final TimeZone TZ_GMT = TimeZone.getTimeZone("GMT");

    public static <T> T as(final Object o, final Class<T> clazz) {
        return Optional.of(o).filter(clazz::isInstance).map(clazz::cast).orElse(null);
    }

    public static String toStringZ(final Date date) {
        final DateFormat dateFormat = new SimpleDateFormat(XSD_UTC_MILLI, Locale.getDefault());
        dateFormat.setTimeZone(TZ_GMT);
        return dateFormat.format(date);
    }

    public static void toResponse(final HttpServletResponse response, final int status) throws IOException {
        final byte[] payload = toBytesUTF8(String.format("%d", status));
        toResponse(response, status, "text/plain", payload.length, payload);
    }

    public static void toResponse(final HttpServletResponse response, final int status, final String contentType,
                                final int contentLength, final byte[] payload) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setContentLength(contentLength);
        response.getOutputStream().write(payload);
    }

    public static String fromBytesUTF8(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] toBytesUTF8(final String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(final InputStream is) throws IOException {
        return IOUtils.toByteArray(is);
    }

    public static byte[] toBytes(final File file) throws IOException {
        return IOUtils.toByteArray(Files.newInputStream(Objects.requireNonNull(file).toPath()));
    }

    public static byte[] toBytes(final Class<?> c, final String resource) throws IOException {
        final URL url = Objects.requireNonNull(c.getClassLoader().getResource(resource));
        return IOUtils.toByteArray(Objects.requireNonNull(url.openStream()));
    }

    public static Document create(final String rootElementName, final Attribute... attrs) {
        final StandardDocumentProvider documentProvider = new StandardDocumentProvider();
        final Document document = documentProvider.newDocument();
        final Element element = document.createElement(rootElementName);
        for (Attribute attr : attrs) {
            element.setAttribute(attr.getName(), attr.getValue());
        }
        document.appendChild(element);
        return document;
    }

    public static Document toDocument(final byte[] xhtml) {
        final StandardDocumentProvider documentProvider = new StandardDocumentProvider();
        return documentProvider.parse(new ByteArrayInputStream(xhtml));
    }

    public static byte[] toXml(final Document document) {
        final DOMSource source = new DOMSource(document);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Result result = new StreamResult(new OutputStreamWriter(bos, StandardCharsets.UTF_8));
        final StandardTransformProvider transformProvider = new StandardTransformProvider();
        transformProvider.setOmitXmlDeclaration(false);
        transformProvider.setMethod("xml");
        transformProvider.setIndent(true);
        //transformProvider.setDocTypePublic(DOCTYPE_PUBLIC_XHTML11);
        //transformProvider.setDocTypeSystem(DOCTYPE_SYSTEM_XHTML11);
        transformProvider.setDocTypeSystem(DOCTYPE_SYSTEM_COMPAT);
        transformProvider.setEncoding(StandardCharsets.UTF_8.name());
        transformProvider.transform(source, result);
        return bos.toByteArray();
    }

    public static String sha256(final byte[] input) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return Hex.encodeHexString(messageDigest.digest(input));
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Element getChild(final Element element, final String name) {
        final NodeList elements = element.getElementsByTagName(name);
        return (Element) ((elements.getLength() == 0) ? null : elements.item(0));
    }

    public static Element addChild(final Element element, final String name, final Attribute... attrs) {
        return addChild(element, name, null, attrs);
    }

    public static Element addChild(final Element element, final String name, final String text, final Attribute... attrs) {
        final Document document = element.getOwnerDocument();
        final Element child = document.createElement(name);
        if ((text != null) && (!text.isEmpty())) {
            child.setTextContent(text);
        }
        for (Attribute attr : attrs) {
            child.setAttribute(attr.getName(), attr.getValue());
        }
        return (Element) element.appendChild(child);
    }

    private static final String DOCTYPE_PUBLIC_XHTML11 = "-//W3C//DTD XHTML 1.1//EN";
    private static final String DOCTYPE_SYSTEM_XHTML11 = "https://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";
    private static final String DOCTYPE_SYSTEM_COMPAT = "about:legacy-compat";
}
