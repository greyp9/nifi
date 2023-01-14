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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoralServlet extends HttpServlet {
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

        if (request.getRequestURI().equals("/favicon.ico")) {
            doGetFavicon(response);
        } else if (request.getRequestURI().equals("/coral.css")) {
            doGetCss(response);
        } else {
            doGetHtml(response);
        }
    }

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern PATTERN = Pattern.compile("\\[(\\d+)\\]\\[(\\w+)\\]");

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        logger.trace("POST {}", request.getRequestURI());
        final Map<String, String[]> parameters = new LinkedHashMap<>();
        final String contentType = request.getHeader("Content-Type");
        if (contentType == null) {
            logger.trace("no Content-Type header");
        } else if (contentType.equals("application/x-www-form-urlencoded")) {
            parameters.putAll(request.getParameterMap());
        } else if (contentType.startsWith("multipart/form-data")) {
            final Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getName().equals("uploadFile")) {
                    final byte[] bytes = CoralUtils.toBytes(part.getInputStream());
                    logger.trace("UPLOAD: file=[{}], size=[{}], sha256=[{}]",
                            part.getName(), bytes.length, CoralUtils.sha256(bytes));
                }
            }
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
                }
            }
        }
        response.setHeader("Location", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FOUND);
    }

    private void doGetFavicon(final HttpServletResponse response) throws IOException {
        final byte[] icon = CoralUtils.toBytes(getClass(), FAVICON);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("image/x-icon");
        response.setContentLength(icon.length);
        response.getOutputStream().write(icon);
    }

    private void doGetCss(final HttpServletResponse response) throws IOException {
        final byte[] css = CoralUtils.toBytes(getClass(), CSS);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/css");
        response.setContentLength(css.length);
        response.getOutputStream().write(css);
    }

    private void doGetHtml(final HttpServletResponse response) throws IOException {
        final Document document = CoralUtils.create("html");
        final Element head = CoralUtils.addChild(document.getDocumentElement(), "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        CoralUtils.addChild(head, "link", new Attribute("href", "/coral.css"),
                new Attribute("rel", "stylesheet"), new Attribute("type", "text/css"));
        final Element body = CoralUtils.addChild(document.getDocumentElement(), "body");

        // body header
        final Element divHeader = CoralUtils.addChild(body, "div", new Attribute("class", "header"));
        CoralUtils.addChild(divHeader, "h1", "Coral Processor - Flow Debugger - NiFi");
        CoralUtils.addChild(divHeader, "p", "NiFi was built to automate the flow of data between systems.  The Coral "
                + "processor exists to help debug flow mechanics during flow development.");

        final Element divContent = CoralUtils.addChild(body, "div", new Attribute("class", "content"));

        // incoming flowfiles
        final Element divIncoming = CoralUtils.addChild(divContent, "div", new Attribute("id", "incoming"));
        CoralUtils.addChild(divIncoming, "h2", "Incoming FlowFiles");
        CoralUtils.addChild(divIncoming, "p", "Update the Processor FlowFile state via these UI elements:");

        final Element tableIn = CoralUtils.addChild(divIncoming, "table", new Attribute("class", "table"));
        final Element tr1In = CoralUtils.addChild(tableIn, "tr");
        CoralUtils.addChild(tr1In, "td", "FlowFiles may be manually routed from upstream processors.");
        final Element tdInForm = CoralUtils.addChild(tr1In, "td");
        final Element tr2In = CoralUtils.addChild(tableIn, "tr");
        CoralUtils.addChild(tr2In, "td", "FlowFiles may be manually created from user-supplied text.");
        final Element tdInText = CoralUtils.addChild(tr2In, "td");
        final Element tr3In = CoralUtils.addChild(tableIn, "tr");
        CoralUtils.addChild(tr3In, "td", "FlowFiles may be manually created from user-supplied files via file upload.");
        final Element tdInFile = CoralUtils.addChild(tr3In, "td");

        final Element form = CoralUtils.addChild(tdInForm, "form",
                new Attribute("action", ""), new Attribute("method", "post"));
        CoralUtils.addChild(form, "button", "Accept Incoming FlowFile", new Attribute("accesskey", "A"),
                new Attribute("type", "submit"), new Attribute("name", "accept"), new Attribute("value", "flowfile"));
        CoralUtils.addChild(tdInText, "a", "[Create From Text]", new Attribute("href", "/flowfile/create/text"));
        CoralUtils.addChild(tdInFile, "a", "[Create From File Upload]", new Attribute("href", "/flowfile/create/text"));

        // flowfile state
        final Element divViewState = CoralUtils.addChild(divContent, "div", new Attribute("id", "state"));
        CoralUtils.addChild(divViewState, "h2", "FlowFile State");
        CoralUtils.addChild(divViewState, "p", "Current processor FlowFile state:");

        final Element tableState = CoralUtils.addChild(divViewState, "table", new Attribute("class", "table"));
        final Element tr1State = CoralUtils.addChild(tableState, "tr");
        CoralUtils.addChild(tr1State, "td", "Number of FlowFiles held by this processor");
        CoralUtils.addChild(tr1State, "td", Integer.toString(coralState.flowFileCount()));
        final Element tr2State = CoralUtils.addChild(tableState, "tr");
        CoralUtils.addChild(tr2State, "td", "Number of upstream FlowFiles requested by this processor");
        CoralUtils.addChild(tr2State, "td", Integer.toString(coralState.incrementToConsume(0)));

        final Element pDetail = CoralUtils.addChild(divViewState, "p");
        CoralUtils.addChild(pDetail, "a", "View Detail", new Attribute("href", "/flowfiles"));

        XhtmlUtils.createFooter(body, false);

        final byte[] xhtml = CoralUtils.toXml(document);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset='UTF-8'");
        response.setContentLength(xhtml.length);
        response.getOutputStream().write(xhtml);
    }

    private static final String CSS = "org/apache/nifi/processors/standard/coral/coral.css";
    private static final String FAVICON = "org/apache/nifi/processors/standard/coral/nifi16.ico";
}
