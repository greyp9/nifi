package org.apache.nifi.kafka.processors.producer.value;

import java.io.IOException;

/**
 * Implementations of converters from FlowFile data to Kafka record value.
 */
public interface ValueFactory {
    byte[] getValue() throws IOException;
}
