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

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsumerPooledObjectFactoryTest {
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092";

    @Mock
    Consumer<byte[], byte[]> mockConsumer;

    ConsumerPooledObjectFactory factory;

    @BeforeEach
    void setFactory() {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        factory = new ConsumerPooledObjectFactory(properties);
    }

    @Test
    void testCreate() {
        final Consumer<byte[], byte[]> consumer = factory.create();

        assertNotNull(consumer);
    }

    @Test
    void testWrap() {
        final PooledObject<Consumer<byte[], byte[]>> pooledObject = factory.wrap(mockConsumer);

        assertNotNull(pooledObject);
    }

    @Test
    void testDestroyObject() {
        final PooledObject<Consumer<byte[], byte[]>> pooledObject = new DefaultPooledObject<>(mockConsumer);

        factory.destroyObject(pooledObject);

        verify(mockConsumer).close();
    }
}
