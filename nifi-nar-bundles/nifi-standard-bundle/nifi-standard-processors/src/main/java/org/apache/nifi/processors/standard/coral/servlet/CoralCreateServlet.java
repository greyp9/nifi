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
package org.apache.nifi.processors.standard.coral.servlet;

import org.apache.commons.codec.binary.Hex;
import org.apache.nifi.processors.standard.coral.core.Attribute;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFileCursor;
import org.apache.nifi.processors.standard.coral.core.CoralState;
import org.apache.nifi.processors.standard.coral.core.CoralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CoralCreateServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CoralState coralState;

    @Override
    public void init() throws ServletException {
        super.init();
        coralState = (CoralState) getServletContext().getAttribute(CoralState.class.getName());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.trace("GET {}", request.getRequestURI());
        final boolean textUI = !request.getRequestURI().contains("/file");
        final boolean fileUI = !request.getRequestURI().contains("/text");
        doGetHtml(textUI, fileUI, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        logger.trace("POST {}", request.getRequestURI());
        final String contentType = request.getHeader("Content-Type");
        if (contentType == null) {
            logger.trace("no Content-Type header");
        } else if (contentType.equals("application/x-www-form-urlencoded")) {
            final Map<String, String[]> parameters = request.getParameterMap();
            final CoralFlowFileCursor flowFileCursor = coralState.getFlowFileCursor();

            if (parameters.containsKey("addAttribute")) {
                final String name = request.getParameter("name");
                final String value = request.getParameter("value");
                if (!name.isEmpty() && !value.isEmpty()) {
                    flowFileCursor.setAttribute(name, value);
                }
            } else if (parameters.containsKey("deleteAttribute")) {
                final String name = request.getParameter("name");
                flowFileCursor.removeAttribute(name);
            } else if (parameters.containsKey("updateText")) {
                // https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1
                final String text = request.getParameter("text").replaceAll("\r\n", "\n");
                logger.trace("TEXT: size=[{}], sha256=[{}]",
                        text.length(), CoralUtils.sha256(text.getBytes(StandardCharsets.UTF_8)));
                //logger.trace(Hex.encodeHexString(CoralUtils.toBytesUTF8(text)));
                flowFileCursor.setContent(CoralUtils.toBytesUTF8(text));
            } else if (parameters.containsKey("create")) {
                final Map<String, String> attributes = flowFileCursor.getAttributes();
                final byte[] content = flowFileCursor.getContent().toByteArray();
                coralState.createFlowFile(coralState.create(System.currentTimeMillis(), attributes, content));
            } else if (parameters.containsKey("reset")) {
                flowFileCursor.reset();
            }
        } else if (contentType.startsWith("multipart/form-data")) {
            final Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getName().equals("uploadFile")) {
                    final byte[] bytes = CoralUtils.toBytes(part.getInputStream());
                    logger.trace("UPLOAD: file=[{}], size=[{}], sha256=[{}]",
                            part.getName(), bytes.length, CoralUtils.sha256(bytes));
                    coralState.getFlowFileCursor().setContent(bytes);
                }
            }
        }
        response.setHeader("Location", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FOUND);
    }

    private void doGetHtml(final boolean textUI, final boolean fileUI, final HttpServletResponse response) throws IOException {
        final Document document = CoralUtils.create("html");
        final Element head = CoralUtils.addChild(document.getDocumentElement(), "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        CoralUtils.addChild(head, "link", new Attribute("href", "/coral.css"),
                new Attribute("rel", "stylesheet"), new Attribute("type", "text/css"));
        final Element body = CoralUtils.addChild(document.getDocumentElement(), "body");

        final Element divHeader = CoralUtils.addChild(body, "div", new Attribute("class", "header"));
        CoralUtils.addChild(divHeader, "h1", "Coral Processor - Create FlowFile - NiFi");
        CoralUtils.addChild(divHeader, "p", "Create a FlowFile from user inputs.");

        final Element divContent = CoralUtils.addChild(body, "div", new Attribute("class", "content"));

        final Element divMetadata = CoralUtils.addChild(divContent, "div", new Attribute("id", "metadata"));
        addTableMetadata(CoralUtils.addChild(divMetadata, "div"), coralState.getFlowFileCursor());
        final Element divAttributes = CoralUtils.addChild(divContent, "div", new Attribute("id", "attributes"));
        addTableAttributes(divAttributes, coralState.getFlowFileCursor().getAttributes());

        final Element divAttribute = CoralUtils.addChild(divContent, "div", new Attribute("id", "attribute"));
        final Element divFormAddAttribute = CoralUtils.addChild(divAttribute, "div", new Attribute("class", "form"));
        CoralUtils.addChild(divFormAddAttribute, "h2", "Attribute");
        CoralUtils.addChild(divFormAddAttribute, "p", "Add / Delete a FlowFile attribute here.");
        final Element formAttribute = CoralUtils.addChild(divFormAddAttribute, "form",
                new Attribute("action", ""), new Attribute("method", "post"));
        CoralUtils.addChild(formAttribute, "span", "Name");
        CoralUtils.addChild(formAttribute, "input", new Attribute("name", "name"), new Attribute("type", "text"), new Attribute("value", ""));
        CoralUtils.addChild(formAttribute, "span", "Value");
        CoralUtils.addChild(formAttribute, "input", new Attribute("name", "value"), new Attribute("type", "text"), new Attribute("value", ""));
        CoralUtils.addChild(formAttribute, "input", new Attribute("name", "addAttribute"), new Attribute("type", "submit"), new Attribute("value", "Add"));
        CoralUtils.addChild(formAttribute, "input", new Attribute("name", "deleteAttribute"), new Attribute("type", "submit"), new Attribute("value", "Delete"));

        if (fileUI) {
            final Element divFile = CoralUtils.addChild(divContent, "div", new Attribute("id", "file"));
            CoralUtils.addChild(divFile, "h2", "Content (Upload File)");
            CoralUtils.addChild(divFile, "p", "Upload FlowFile content from your filesystem here.  (Any "
                    + "existing content for this FlowFile will be replaced.)");
            final Element divFormUpload = CoralUtils.addChild(divFile, "div", new Attribute("class", "form"));
            final Element formUpload = CoralUtils.addChild(divFormUpload, "form",
                    new Attribute("action", ""), new Attribute("method", "post"), new Attribute("enctype", "multipart/form-data"));
            CoralUtils.addChild(formUpload, "input", new Attribute("name", "uploadFile"), new Attribute("type", "file"));
            CoralUtils.addChild(formUpload, "input", new Attribute("name", "submitUploadFile"), new Attribute("type", "submit"), new Attribute("value", "Upload Content"));
        }

        if (textUI) {
            final Element divText = CoralUtils.addChild(divContent, "div", new Attribute("id", "text"));
            CoralUtils.addChild(divText, "h2", "Content (Edit)");
            CoralUtils.addChild(divText, "p", "Enter text to be used as the FlowFile content here.  (Any "
                    + "existing content for this FlowFile will be replaced.)");
            final Element divFormEdit = CoralUtils.addChild(divText, "div", new Attribute("class", "form"));
            final Element formEdit = CoralUtils.addChild(divFormEdit, "form",
                    new Attribute("action", ""), new Attribute("method", "post"));
            final Element divFormEdit1 = CoralUtils.addChild(formEdit, "div");
            final String contentFlowFile = CoralUtils.fromBytesUTF8(coralState.getFlowFileCursor().getContent().toByteArray());
            final String content = contentFlowFile.isEmpty() ? "\n" : contentFlowFile;
            CoralUtils.addChild(divFormEdit1, "textarea", content, new Attribute("placeholder", "enter text"), new Attribute("rows", "12"), new Attribute("cols", "132"), new Attribute("name", "text"));
            final Element divFormEdit2 = CoralUtils.addChild(formEdit, "div");
            CoralUtils.addChild(divFormEdit2, "input", new Attribute("name", "updateText"), new Attribute("type", "submit"), new Attribute("value", "Update Content"));
        }

        final Element divSubmit = CoralUtils.addChild(divContent, "div", new Attribute("id", "metadata"));
        CoralUtils.addChild(divSubmit, "h2", "FlowFile");
        final Element ul = CoralUtils.addChild(divSubmit, "ul");
        CoralUtils.addChild(ul, "li", "To add the currently specified FlowFile, click the [Create FlowFile] button.");
        CoralUtils.addChild(ul, "li", "To clear the currently specified FlowFile, click the [Reset FlowFile] button.");

        final Element divFormCreate = CoralUtils.addChild(divSubmit, "div", new Attribute("class", "form"));
        final Element form = CoralUtils.addChild(divFormCreate, "form",
                new Attribute("action", ""), new Attribute("method", "post"));
        CoralUtils.addChild(form, "button", "Create FlowFile", new Attribute("accesskey", "F"),
                new Attribute("type", "submit"), new Attribute("name", "create"), new Attribute("value", "flowfile"));
        CoralUtils.addChild(form, "button", "Reset FlowFile", new Attribute("accesskey", "R"),
                new Attribute("type", "submit"), new Attribute("name", "reset"), new Attribute("value", "flowfile"));

        XhtmlUtils.createFooter(body, true);

        final byte[] xhtml = CoralUtils.toXml(document);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset='UTF-8'");
        response.setContentLength(xhtml.length);
        response.getOutputStream().write(xhtml);
    }

    private void addTableMetadata(final Element div, final CoralFlowFileCursor flowFileCursor) {
        CoralUtils.addChild(div, "h2", "Metadata");
        CoralUtils.addChild(div, "p", "This table lists the metadata associated with the FlowFile currently being "
                + "edited.");
        final Element table = CoralUtils.addChild(div, "table", new Attribute("class", "table"));
        final Element thead = CoralUtils.addChild(table, "thead", new Attribute("class", "table"));
        final Element trHead = CoralUtils.addChild(thead, "tr");
        CoralUtils.addChild(trHead, "th", "Name");
        CoralUtils.addChild(trHead, "th", "Value");

        final Element tbody = CoralUtils.addChild(table, "tbody", new Attribute("class", "table"));
        addRow(tbody, "Size (Bytes)", flowFileCursor.getContent().toByteArray().length);
    }

    private void addTableAttributes(final Element div, final Map<String, String> attributes) {
        CoralUtils.addChild(div, "h2", "Attributes");
        CoralUtils.addChild(div, "p", "This table lists the FlowFile attributes associated with the FlowFile "
                + "currently being edited.");
        final Element table = CoralUtils.addChild(div, "table", new Attribute("class", "table"));
        final Element thead = CoralUtils.addChild(table, "thead", new Attribute("class", "table"));
        final Element trHead = CoralUtils.addChild(thead, "tr");
        CoralUtils.addChild(trHead, "th", "Name");
        CoralUtils.addChild(trHead, "th", "Value");

        final Element tfoot = CoralUtils.addChild(table, "tfoot", new Attribute("class", "table"));
        final Element trFoot = CoralUtils.addChild(tfoot, "tr");
        final String footer = String.format("%d attributes(s)", attributes.size());
        CoralUtils.addChild(trFoot, "th", footer, new Attribute("colspan", "2"));

        final Element tbody = CoralUtils.addChild(table, "tbody", new Attribute("class", "table"));
        final List<Map.Entry<String, String>> entries = attributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
        for (Map.Entry<String, String> entry : entries) {
            addRow(tbody, entry.getKey(), entry.getValue());
        }
        if (entries.isEmpty()) {
            final Element tr = CoralUtils.addChild(tbody, "tr");
            CoralUtils.addChild(tr, "td", new Attribute("colspan", "2"));
        }
    }

    private void addRow(final Element tbody, final Object... columns) {
        final Element tr = CoralUtils.addChild(tbody, "tr");
        for (final Object column : columns) {
            CoralUtils.addChild(tr, "td", column.toString());
        }
    }
}
