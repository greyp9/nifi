package org.apache.nifi.kafka.service.api.header;

public class KafkaHeader {
    private final String key;
    private final byte[] value;

    public KafkaHeader(final String key, final byte[] value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }
}
