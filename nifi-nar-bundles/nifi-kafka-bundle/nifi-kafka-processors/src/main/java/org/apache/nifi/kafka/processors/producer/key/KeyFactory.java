package org.apache.nifi.kafka.processors.producer.key;

import java.io.UnsupportedEncodingException;

/**
 * Implementations of converters from FlowFile data to Kafka record key.
 */
public interface KeyFactory {
    byte[] getKey() throws UnsupportedEncodingException;
}
