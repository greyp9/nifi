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

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processors.standard.coral.core.Attribute;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFile;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFileRoute;
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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CoralFlowfilesServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CoralState coralState;

    @Override
    public void init() throws ServletException {
        super.init();
        coralState = (CoralState) getServletContext().getAttribute(CoralState.class.getName());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.warn("GET {}", request.getRequestURI());
        doGetHtml(response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        logger.warn("POST {}", request.getRequestURI());
        final Map<String, String[]> parameters = new LinkedHashMap<>();
        if ("application/x-www-form-urlencoded".equals(request.getHeader("Content-Type"))) {
            parameters.putAll(request.getParameterMap());
        }
        for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
            final String key = entry.getKey();
            for (final String value : entry.getValue()) {
                if ("accept".equals(key) && ("flowfile".equals(value))) {
                    coralState.incrementToConsume(1);
                } else if ("route".equals(key)) {
                    final Matcher matcher = PATTERN.matcher(value);
                    if (matcher.matches()) {
                        final String id = matcher.group(1);
                        final String relationship = matcher.group(2);
                        coralState.routeFlowFile(id, relationship);
                    }
                } else if ("action".equals(key)) {
                    final Matcher matcher = PATTERN.matcher(value);
                    if (matcher.matches()) {
                        final String id = matcher.group(1);
                        final String action = matcher.group(2);
                        coralState.actionFlowFile(id, action);
                    }
                //} else {
                //    log(String.format("doPost()::%s=%s", key, value));
                }
            }
        }
        response.setHeader("Location", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FOUND);
    }

    private void doGetHtml(final HttpServletResponse response) throws IOException {
        final Document document = CoralUtils.create("html");
        final Element head = CoralUtils.addChild(document.getDocumentElement(), "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        CoralUtils.addChild(head, "link", new Attribute("href", "/coral.css"),
                new Attribute("rel", "stylesheet"), new Attribute("type", "text/css"));
        final Element body = CoralUtils.addChild(document.getDocumentElement(), "body");
        final Element divHeader = CoralUtils.addChild(body, "div", new Attribute("class", "header"));
        CoralUtils.addChild(divHeader, "h1", "Coral Content - NiFi");

        final Element divContent = CoralUtils.addChild(body, "div", new Attribute("class", "content"));
        final List<String> actions = Arrays.asList("CLONE", "DROP");
        final Set<String> relationships = coralState.getRelationships().stream()
                .map(Relationship::getName).collect(Collectors.toSet());
        addTable(divContent, actions, relationships);

        final String text = String.format("%s - count=%d - in=%d", new Date(),
                coralState.flowFileCount(), coralState.incrementToConsume(0));
        CoralUtils.addChild(body, "div", text, new Attribute("class", "footer"));

        final byte[] xhtml = CoralUtils.toXml(document);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset='UTF-8'");
        response.setContentLength(xhtml.length);
        response.getOutputStream().write(xhtml);
    }

    private void addTable(final Element div, final List<String> actions, final Set<String> relationships) {
        final Element form = CoralUtils.addChild(div, "form",
                new Attribute("action", ""), new Attribute("method", "post"));
        final Element table = CoralUtils.addChild(form, "table", new Attribute("class", "table"));

        final Element thead = CoralUtils.addChild(table, "thead", new Attribute("class", "table"));
        final Element trHead = CoralUtils.addChild(thead, "tr");
        final String[] columns = {"Metadata", "Content", "ID", "Entry Date", "Attributes", "Size", "Action", "Route"};
        for (final String column : columns) {
            CoralUtils.addChild(trHead, "th", column);
        }

        final Element tfoot = CoralUtils.addChild(table, "tfoot", new Attribute("class", "table"));
        final Element trFoot = CoralUtils.addChild(tfoot, "tr");
        final String footer = String.format("%d flowfile(s)", coralState.flowFileCount());
        CoralUtils.addChild(trFoot, "th", footer, new Attribute("colspan", "8"));

        CoralUtils.addChild(table, "tbody", new Attribute("class", "table"));
        for (final FlowFile flowFile : coralState.getFlowFiles()) {
            addRow(table, flowFile, actions, relationships, null);
        }
        for (final CoralFlowFileRoute flowFile : coralState.getFlowFilesRoute()) {
            addRow(table, flowFile.getCoralFlowFile(), null, null, flowFile.getRelationship());
        }
    }

    private void addRow(final Element table, final FlowFile flowFile,
                        final List<String> actions, final Set<String> relationships, final String route) {
        final Element tr = CoralUtils.addChild(table, "tr");

        final Element tdMetadata = CoralUtils.addChild(tr, "td");
        CoralUtils.addChild(tdMetadata, "a", "\u24d8", new Attribute("href", String.format("/flowfile/metadata/%d", flowFile.getId())));
        final Element tdContent = CoralUtils.addChild(tr, "td");
        CoralUtils.addChild(tdContent, "a", "\u25c9", new Attribute("href", String.format("/flowfile/content/%d", flowFile.getId())));

        CoralUtils.addChild(tr, "td", Long.toString(flowFile.getId()), new Attribute("class", "right"));
        CoralUtils.addChild(tr, "td", CoralUtils.toStringZ(new Date(flowFile.getEntryDate())));
        CoralUtils.addChild(tr, "td", Integer.toString(flowFile.getAttributes().size()), new Attribute("class", "right"));
        final CoralFlowFile coralFlowFile = CoralUtils.as(flowFile, CoralFlowFile.class);
        final String size = (coralFlowFile == null) ? Long.toString(flowFile.getSize())
                : (coralFlowFile.isNull() ? "-" :  Long.toString(flowFile.getSize()));
        CoralUtils.addChild(tr, "td", size, new Attribute("class", "right"));

        final Element tdAction = CoralUtils.addChild(tr, "td");
        if (actions == null) {
            tdAction.setTextContent("-");
        } else {
            for (final String action : actions) {
                final String accesskey = action.substring(0, 1);
                final String value = String.format("[%d][%s]", flowFile.getId(), action);
                CoralUtils.addChild(tdAction, "button", action, new Attribute("accesskey", accesskey),
                        new Attribute("type", "submit"), new Attribute("name", "action"), new Attribute("value", value));
            }
        }
        final Element tdRoute = CoralUtils.addChild(tr, "td");
        if (relationships == null) {
            tdRoute.setTextContent(route);
        } else {
            for (final String relationship : relationships) {
                final String accesskey = relationship.substring(0, 1);
                final String value = String.format("[%d][%s]", flowFile.getId(), relationship);
                CoralUtils.addChild(tdRoute, "button", relationship, new Attribute("accesskey", accesskey),
                        new Attribute("type", "submit"), new Attribute("name", "route"), new Attribute("value", value));
            }
        }
    }

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern PATTERN = Pattern.compile("\\[(\\d+)\\]\\[(\\w+)\\]");
}
