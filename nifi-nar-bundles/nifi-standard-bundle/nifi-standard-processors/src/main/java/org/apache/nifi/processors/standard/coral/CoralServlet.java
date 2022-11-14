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

import org.apache.nifi.flowfile.FlowFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class CoralServlet  extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CoralState coralState;

    @Override
    public void init() throws ServletException {
        super.init();
        coralState = (CoralState) getServletContext().getAttribute(CoralState.class.getName());
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        logger.info(request.getRequestURI());

        final String queryString = request.getQueryString();
        if ((queryString != null) && queryString.contains("in")) {
            coralState.incrementIn(1);
        }
        if ((queryString != null) && queryString.contains("out")) {
            coralState.incrementOut(1);
        }

        if (request.getRequestURI().equals("/favicon.ico")) {
            doGetFavicon(response);
        } else {
            doGetHtml(response);
        }
    }

    private void doGetFavicon(final HttpServletResponse response) throws IOException {
        final byte[] icon = CoralUtils.toBytes(getClass(), FAVICON);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("image/x-icon");
        response.setContentLength(icon.length);
        response.getOutputStream().write(icon);
    }

    private void doGetHtml(final HttpServletResponse response) throws IOException {
        final Document document = CoralUtils.create("html");
        final Element head = CoralUtils.addChild(document.getDocumentElement(), "head");
        CoralUtils.addChild(head, "title", "Coral - NiFi");
        final Element body = CoralUtils.addChild(document.getDocumentElement(), "body");
        final Element div = CoralUtils.addChild(body, "div", "Coral Content - NiFi");
        addTable(div);

        final String text = String.format("HELLO WORLD at %s - count=%d - in=%d - out=%d", new Date(),
                coralState.flowFileCount(), coralState.incrementIn(0), coralState.incrementOut(0));
        CoralUtils.addChild(body, "div", text);

        final byte[] xhtml = CoralUtils.toXml(document);
        logger.info(new String(xhtml, StandardCharsets.UTF_8));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset='UTF-8'");
        response.setContentLength(xhtml.length);
        response.getOutputStream().write(xhtml);
    }

    private void addTable(final Element div) throws IOException {
        final Element table = CoralUtils.addChild(div, "table");
        for (final FlowFile flowFile : coralState.getFlowFiles()) {
            addRow(table, flowFile);
        }
    }

    private void addRow(final Element table, final FlowFile flowFile) throws IOException {
        final Element tr = CoralUtils.addChild(table, "tr");
        final Element td = CoralUtils.addChild(tr, "td");
        td.setTextContent(flowFile.toString());
    }

    private static final String HTML = "org/apache/nifi/processors/standard/coral/coral.html";
    private static final String FAVICON = "org/apache/nifi/processors/standard/coral/nifi16.ico";
}
