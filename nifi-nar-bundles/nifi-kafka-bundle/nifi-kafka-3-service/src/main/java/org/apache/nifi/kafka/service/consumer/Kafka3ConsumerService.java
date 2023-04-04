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
package org.apache.nifi.kafka.service.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.consumer.PollingContext;
import org.apache.nifi.kafka.service.api.record.ByteRecord;
import org.apache.nifi.kafka.service.api.record.RecordSummary;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Kafka3ConsumerService implements KafkaConsumerService {
    private final Consumer<byte[], byte[]> consumer;

    public Kafka3ConsumerService(final String bootstrapServers) {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        final ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
        this.consumer = new KafkaConsumer<>(properties, deserializer, deserializer);
    }

    @Override
    public void commit(RecordSummary recordSummary) {
    }

    @Override
    public Iterable<ByteRecord> poll(PollingContext pollingContext) {
        return null;
    }

    @Override
    public List<PartitionState> getPartitionStates(final String topic) {
        final List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
        return partitionInfos.stream()
                .map(p -> new PartitionState(p.topic(), p.partition()))
                .collect(Collectors.toList());
    }
}
