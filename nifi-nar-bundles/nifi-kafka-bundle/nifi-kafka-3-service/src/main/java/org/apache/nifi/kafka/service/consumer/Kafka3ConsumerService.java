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
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.consumer.PollingContext;
import org.apache.nifi.kafka.service.api.record.ByteRecord;
import org.apache.nifi.kafka.service.api.record.RecordSummary;
import org.apache.nifi.kafka.service.consumer.pool.ConsumerObjectPool;
import org.apache.nifi.logging.ComponentLog;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Kafka3ConsumerService implements KafkaConsumerService, Closeable {
    private final ComponentLog componentLog;

    private final ConsumerObjectPool consumerObjectPool;

    public Kafka3ConsumerService(final ComponentLog componentLog, final Properties properties) {
        this.componentLog = Objects.requireNonNull(componentLog, "Component Log required");
        this.consumerObjectPool = new ConsumerObjectPool(properties);
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
            return runConsumerFunction((consumer) ->
                consumer.partitionsFor(topic)
                        .stream()
                        .map(partitionInfo -> new PartitionState(partitionInfo.topic(), partitionInfo.partition()))
                        .collect(Collectors.toList())
            );
    }

    @Override
    public void close() {
        consumerObjectPool.close();
    }

    private <T> T runConsumerFunction(final Function<Consumer<byte[], byte[]>, T> consumerFunction) throws IllegalStateException {
        Consumer<byte[], byte[]> consumer = null;
        try {
            consumer = consumerObjectPool.borrowObject();
            return consumerFunction.apply(consumer);
        } catch (final Exception e) {
            throw new RuntimeException("Borrow Consumer failed", e);
        } finally {
            if (consumer != null) {
                try {
                    consumerObjectPool.returnObject(consumer);
                } catch (final Exception e) {
                    componentLog.warn("Return Consumer failed", e);
                }
            }
        }
    }
}
