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
package org.apache.nifi.kafka.service.consumer.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.Objects;
import java.util.Properties;

/**
 * Pooled Object Factory for Kafka Consumers
 */
class ConsumerPooledObjectFactory extends BasePooledObjectFactory<Consumer<byte[], byte[]>> {
    private final Properties consumerProperties;

    /**
     * Consumer Pooled Object Factory constructor with Kafka Consumer Properties
     *
     * @param consumerProperties Kafka Consumer Properties
     */
    ConsumerPooledObjectFactory(final Properties consumerProperties) {
        this.consumerProperties = Objects.requireNonNull(consumerProperties, "Consumer Properties required");
    }

    @Override
    public Consumer<byte[], byte[]> create() {
        final ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
        return new KafkaConsumer<>(consumerProperties, deserializer, deserializer);
    }

    /**
     * Wrap Kafka Consumer using Default Pooled Object for tracking
     *
     * @param consumer Kafka Consumer
     * @return Pooled Object wrapper
     */
    @Override
    public PooledObject<Consumer<byte[], byte[]>> wrap(final Consumer<byte[], byte[]> consumer) {
        Objects.requireNonNull(consumer, "Consumer required");
        return new DefaultPooledObject<>(consumer);
    }

    /**
     * Destroy Pooled Object closes wrapped Kafka Consumer
     *
     * @param pooledObject Pooled Object with Consumer to be closed
     */
    @Override
    public void destroyObject(final PooledObject<Consumer<byte[], byte[]>> pooledObject) {
        Objects.requireNonNull(pooledObject, "Pooled Object required");
        final Consumer<byte[], byte[]> consumer = pooledObject.getObject();
        consumer.close();
    }
}
