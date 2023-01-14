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

import org.apache.nifi.processors.standard.coral.core.Attribute;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFile;
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
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CoralMetadataServlet extends HttpServlet {
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
        final Matcher matcher = PATTERN.matcher(request.getRequestURI());
        if (matcher.matches()) {
            final Optional<CoralFlowFile> flowFile = coralState.getFlowFile(matcher.group(1));
            if (flowFile.isPresent()) {
                final byte[] payload = doGetHtml(flowFile.get());
                CoralUtils.toResponse(response, HttpServletResponse.SC_OK, "text/html", payload.length, payload);
            } else {
                CoralUtils.toResponse(response, HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            CoralUtils.toResponse(response, HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("/flowfile/metadata/(.+)");

    private byte[] doGetHtml(final CoralFlowFile flowFile) throws IOException {
        final Document document = CoralUtils.create("html");
        final Element head = CoralUtils.addChild(document.getDocumentElement(), "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        CoralUtils.addChild(head, "link", new Attribute("href", "/coral.css"),
                new Attribute("rel", "stylesheet"), new Attribute("type", "text/css"));
        final Element body = CoralUtils.addChild(document.getDocumentElement(), "body");
        final Element divHeader = CoralUtils.addChild(body, "div", new Attribute("class", "header"));
        CoralUtils.addChild(divHeader, "h1", "Coral Processor - FlowFile Metadata - NiFi");
        CoralUtils.addChild(divHeader, "p", "Non-content details associated with this FlowFile.");

        final Element divContent = CoralUtils.addChild(body, "div", new Attribute("class", "content"));
        final Element divMetadata = CoralUtils.addChild(divContent, "div", new Attribute("id", "metadata"));
        CoralUtils.addChild(divMetadata, "h2", "FlowFile Metadata");
        CoralUtils.addChild(divMetadata, "p", "Metadata associated with the FlowFile.");
        addTableMetadata(divMetadata, flowFile);
        final Element divAttributes = CoralUtils.addChild(divContent, "div", new Attribute("id", "attributes"));
        CoralUtils.addChild(divAttributes, "h2", "FlowFile Attributes");
        CoralUtils.addChild(divAttributes, "p", "Attributes associated with the FlowFile.");
        addTableAttributes(divAttributes, flowFile);

        XhtmlUtils.createFooter(body, true);

        return CoralUtils.toXml(document);
    }

    private void addTableMetadata(final Element div, final CoralFlowFile flowFile) {
        final Element table = CoralUtils.addChild(div, "table", new Attribute("class", "table"));
        final Element thead = CoralUtils.addChild(table, "thead", new Attribute("class", "table"));
        final Element trHead = CoralUtils.addChild(thead, "tr");
        CoralUtils.addChild(trHead, "th", "Name");
        CoralUtils.addChild(trHead, "th", "Value");

        final Element tbody = CoralUtils.addChild(table, "tbody", new Attribute("class", "table"));
        addRow(tbody, "ID", flowFile.getId());
        addRow(tbody, "Entry Date", CoralUtils.toStringZ(new Date(flowFile.getEntryDate())));
        addRow(tbody, "Lineage Start Date", CoralUtils.toStringZ(new Date(flowFile.getEntryDate())));
        addRow(tbody, "Lineage Start Index", flowFile.getLineageStartIndex());
        addRow(tbody, "Last Queue Index", flowFile.getQueueDateIndex());
        addRow(tbody, "Size", flowFile.getSize());
    }

    private void addTableAttributes(final Element div, final CoralFlowFile flowFile) {
        final Element table = CoralUtils.addChild(div, "table", new Attribute("class", "table"));
        final Element thead = CoralUtils.addChild(table, "thead", new Attribute("class", "table"));
        final Element trHead = CoralUtils.addChild(thead, "tr");
        CoralUtils.addChild(trHead, "th", "Name");
        CoralUtils.addChild(trHead, "th", "Value");

        final Element tfoot = CoralUtils.addChild(table, "tfoot", new Attribute("class", "table"));
        final Element trFoot = CoralUtils.addChild(tfoot, "tr");
        final String footer = String.format("%d attributes(s)", flowFile.getAttributes().size());
        CoralUtils.addChild(trFoot, "th", footer, new Attribute("colspan", "2"));

        final Element tbody = CoralUtils.addChild(table, "tbody", new Attribute("class", "table"));
        final List<Map.Entry<String, String>> entries = flowFile.getAttributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
        for (Map.Entry<String, String> entry : entries) {
            addRow(tbody, entry.getKey(), entry.getValue());
        }
    }

    private void addRow(final Element tbody, final Object... columns) {
        final Element tr = CoralUtils.addChild(tbody, "tr");
        for (final Object column : columns) {
            CoralUtils.addChild(tr, "td", column.toString());
        }
    }
}
