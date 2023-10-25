package org.apache.nifi.kafka.service.api.producer;

public class ProducerRecordMetadata {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;

    public ProducerRecordMetadata(final String topic, final int partition, final long offset, final long timestamp) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
