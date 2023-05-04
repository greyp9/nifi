package org.apache.nifi.kafka.processors.producer.key;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class AttributeKeyFactory implements KeyFactory {
    private final Map<String, String> attributes;
    private final String keyAttribute;
    private final String keyAttributeEncoding;

    public AttributeKeyFactory(final Map<String, String> attributes,
                               final String keyAttribute,
                               final String keyAttributeEncoding) {
        this.attributes = attributes;
        this.keyAttribute = keyAttribute;
        this.keyAttributeEncoding = Optional.ofNullable(keyAttributeEncoding).orElse(StandardCharsets.UTF_8.name());
    }

    @Override
    public byte[] getKey() throws UnsupportedEncodingException {
        final String keyAttributeValue = (keyAttribute == null) ? null : attributes.get(keyAttribute);
        return (keyAttributeValue == null) ? null : keyAttributeValue.getBytes(keyAttributeEncoding);
    }
}
