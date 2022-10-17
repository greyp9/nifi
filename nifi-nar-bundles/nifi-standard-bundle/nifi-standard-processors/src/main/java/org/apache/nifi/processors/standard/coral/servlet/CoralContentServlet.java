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

import org.apache.nifi.processors.standard.coral.core.CoralFlowFile;
import org.apache.nifi.processors.standard.coral.core.CoralState;
import org.apache.nifi.processors.standard.coral.core.CoralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoralContentServlet extends HttpServlet {
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
                final String contentTypeParameter = request.getParameter("Content-Type");
                final String contentType = (contentTypeParameter == null) ? "text/plain" : contentTypeParameter;
                final byte[] payload = flowFile.get().getData();
                CoralUtils.toResponse(response, HttpServletResponse.SC_OK, contentType, payload.length, payload);
            } else {
                CoralUtils.toResponse(response, HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            CoralUtils.toResponse(response, HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("/flowfile/content/(.+)");
}
