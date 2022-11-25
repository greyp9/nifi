package org.apache.nifi.processors.standard.coral.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class CoralFlowFileCreate {
    private final Map<String, String> attributes;
    private final ByteArrayOutputStream content;

    public CoralFlowFileCreate() {
        this.attributes = new HashMap<>();
        this.content = new ByteArrayOutputStream();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public ByteArrayOutputStream getContent() {
        return content;
    }

    public void reset() throws IOException {
        attributes.clear();
        content.reset();
    }

    public void setAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    public void setContent(final byte[] data) throws IOException {
        content.reset();
        content.write(data);
    }
}
