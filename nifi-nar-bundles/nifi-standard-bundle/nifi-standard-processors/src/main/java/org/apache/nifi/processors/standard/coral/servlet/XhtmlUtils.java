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
import org.apache.nifi.processors.standard.coral.core.CoralUtils;
import org.w3c.dom.Element;

import java.util.Date;

public class XhtmlUtils {

    public static Element createFooter(final Element parent, boolean addHomeLink) {
        final Element divFooter = CoralUtils.addChild(parent, "div", new Attribute("class", "footer"));
        final String textFooter = String.format("%s", new Date());
        CoralUtils.addChild(divFooter, "span", textFooter, new Attribute("class", "left"));
        if (addHomeLink) {
            final Element spanHref = CoralUtils.addChild(divFooter, "span", new Attribute("class", "right"));
            CoralUtils.addChild(spanHref, "a", "[\u23cf]", new Attribute("href", "/"));
        }
        return divFooter;
    }
}
