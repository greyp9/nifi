package org.apache.nifi.processors.standard.coral.core;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public final class CoralFlowFileCursor {
    private final Map<String, String> attributes;
    private final ByteArrayOutputStream content;

    public CoralFlowFileCursor() {
        this.attributes = new HashMap<>();
        this.content = new ByteArrayOutputStream();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public ByteArrayOutputStream getContent() {
        return content;
    }

    public void reset() {
        attributes.clear();
        content.reset();
    }

    public void setAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    public void setContent(final byte[] data) {
        content.reset();
        content.write(data, 0, data.length);
    }

    public void set(final CoralFlowFile flowFile) {
        reset();
        attributes.putAll(flowFile.getAttributes());
        setContent(flowFile.getData());
    }
}
