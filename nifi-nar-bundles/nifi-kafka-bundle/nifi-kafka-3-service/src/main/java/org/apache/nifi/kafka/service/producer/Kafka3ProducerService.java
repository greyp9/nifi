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
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.common.ServiceConfiguration;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.apache.nifi.kafka.service.api.producer.ProducerConfiguration;
import org.apache.nifi.kafka.service.api.producer.PublishContext;
import org.apache.nifi.kafka.service.api.producer.RecordSummary;
import org.apache.nifi.kafka.service.api.record.KafkaRecord;
import org.apache.nifi.kafka.service.producer.txn.KafkaNonTransactionalProducerWrapper;
import org.apache.nifi.kafka.service.producer.txn.KafkaProducerWrapper;
import org.apache.nifi.kafka.service.producer.txn.KafkaTransactionalProducerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Kafka3ProducerService implements KafkaProducerService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Producer<byte[], byte[]> producer;
    private final ProducerCallback callback;

    private final ServiceConfiguration serviceConfiguration;

    private final KafkaProducerWrapper wrapper;

    public Kafka3ProducerService(final Properties properties,
                                 final ServiceConfiguration serviceConfiguration,
                                 final ProducerConfiguration producerConfiguration) {
        final ByteArraySerializer serializer = new ByteArraySerializer();
        this.producer = new KafkaProducer<>(properties, serializer, serializer);
        this.callback = new ProducerCallback();

        this.serviceConfiguration = serviceConfiguration;

        this.wrapper = producerConfiguration.getUseTransactions()
                ? new KafkaTransactionalProducerWrapper(producer, callback)
                : new KafkaNonTransactionalProducerWrapper(producer, callback);
    }

    @Override
    public void close() {
        producer.close();
    }

    @Override
    public void init() {
        wrapper.init();
        logger.trace("init()");
    }

    @Override
    public void send(final Iterator<KafkaRecord> kafkaRecords, final PublishContext publishContext) {
        wrapper.send(kafkaRecords, publishContext);
        logger.trace("send():inFlight");
    }

    @Override
    public RecordSummary complete() {
        producer.flush();
        wrapper.complete();
        return callback.waitComplete(serviceConfiguration.getMaxAckWaitMillis());
    }

    @Override
    public List<PartitionState> getPartitionStates(final String topic) {
        final List<PartitionInfo> partitionInfos = producer.partitionsFor(topic);
        return partitionInfos.stream()
                .map(p -> new PartitionState(p.topic(), p.partition()))
                .collect(Collectors.toList());
    }
}
