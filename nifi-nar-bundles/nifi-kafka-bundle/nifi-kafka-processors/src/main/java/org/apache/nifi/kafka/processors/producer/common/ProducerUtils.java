package org.apache.nifi.kafka.processors.producer.common;

import java.io.IOException;

public class ProducerUtils {

    public static void checkMessageSize(final long max, final long actual) throws IOException {
        if (actual > max) {
            throw new IOException(String.format("max.message.size %d exceeded (found %d)", max, actual));
        }
    }
}
