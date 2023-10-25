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
package org.apache.nifi.kafka.service.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.common.ServiceConfiguration;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.apache.nifi.kafka.service.api.producer.ProducerConfiguration;
import org.apache.nifi.kafka.service.api.producer.PublishContext;
import org.apache.nifi.kafka.service.api.producer.RecordSummary;
import org.apache.nifi.kafka.service.api.record.KafkaRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class Kafka3ProducerService implements KafkaProducerService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Producer<byte[], byte[]> producer;
    private final ServiceConfiguration serviceConfiguration;
    private final ProducerConfiguration producerConfiguration;

    public Kafka3ProducerService(final Properties properties,
                                 final ServiceConfiguration serviceConfiguration,
                                 final ProducerConfiguration producerConfiguration) {
        final ByteArraySerializer serializer = new ByteArraySerializer();
        this.producer = new KafkaProducer<>(properties, serializer, serializer);
        this.serviceConfiguration = serviceConfiguration;
        this.producerConfiguration = producerConfiguration;
        if (producerConfiguration.getUseTransactions()) {
            producer.initTransactions();
        }
    }

    @Override
    public RecordSummary send(final Iterator<KafkaRecord> kafkaRecords, final PublishContext publishContext) {
        return producerConfiguration.getUseTransactions()
                ? sendTransaction(kafkaRecords, publishContext)
                : sendNoTransaction(kafkaRecords, publishContext);
    }

    private RecordSummary sendTransaction(final Iterator<KafkaRecord> kafkaRecords, final PublishContext publishContext) {
        final ProducerCallback callback = new ProducerCallback();
        try {
            producer.beginTransaction();
            logger.trace("sendTransaction():begin");
            while (kafkaRecords.hasNext()) {
                producer.send(toProducerRecord(kafkaRecords.next(), publishContext), callback);
                callback.send();
            }
            logger.trace("sendTransaction():inFlight");
            producer.flush();
            producer.commitTransaction();
            logger.trace("sendTransaction():committed");
        } catch (final Exception e) {
            producer.abortTransaction();
            logger.trace("sendTransaction():aborted");
        }
        return callback.waitComplete(serviceConfiguration.getMaxAckWaitMillis());
    }

    private RecordSummary sendNoTransaction(final Iterator<KafkaRecord> kafkaRecords, final PublishContext publishContext) {
        final ProducerCallback callback = new ProducerCallback();
        logger.trace("sendNoTransaction():start");
        while (kafkaRecords.hasNext()) {
            producer.send(toProducerRecord(kafkaRecords.next(), publishContext), callback);
            callback.send();
        }
        logger.trace("sendNoTransaction():inFlight");
        producer.flush();
        logger.trace("sendNoTransaction():flushed");
        return callback.waitComplete(serviceConfiguration.getMaxAckWaitMillis());
    }

    private ProducerRecord<byte[], byte[]> toProducerRecord(final KafkaRecord kafkaRecord, final PublishContext publishContext) {
        final String topic = Optional.ofNullable(kafkaRecord.getTopic()).orElse(publishContext.getTopic());
        final Integer partition = Optional.ofNullable(kafkaRecord.getPartition()).orElse(publishContext.getPartition());
        return new ProducerRecord<>(topic, partition, kafkaRecord.getTimestamp(), kafkaRecord.getKey(), kafkaRecord.getValue(), toKafkaHeadersNative(kafkaRecord));
    }

    @Override
    public List<PartitionState> getPartitionStates(final String topic) {
        final List<PartitionInfo> partitionInfos = producer.partitionsFor(topic);
        return partitionInfos.stream()
                .map(p -> new PartitionState(p.topic(), p.partition()))
                .collect(Collectors.toList());
    }

    private List<Header> toKafkaHeadersNative(final KafkaRecord kafkaRecord) {
        return kafkaRecord.getHeaders().stream()
                .map(h -> new RecordHeader(h.key(), h.value()))
                .collect(Collectors.toList());
    }
}
