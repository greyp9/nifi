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
