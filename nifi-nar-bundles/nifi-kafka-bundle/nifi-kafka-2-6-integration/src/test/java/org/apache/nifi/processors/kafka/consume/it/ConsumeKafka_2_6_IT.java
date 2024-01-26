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
package org.apache.nifi.processors.kafka.consume.it;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.nifi.processors.kafka.pubsub.ConsumeKafka_2_6;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumeKafka_2_6_IT extends ConsumeKafka_2_6_BaseIT {
    private static final String TEST_RECORD_KEY = "key-" + System.currentTimeMillis();
    private static final String TEST_RECORD_VALUE = "value-" + System.currentTimeMillis();
    private static final String TOPIC = ConsumeKafka_2_6_IT.class.getName();
    private static final String GROUP_ID = ConsumeKafka_2_6_IT.class.getSimpleName();

    @Test
    public void testKafkaTestContainerProduceConsumeOne() throws ExecutionException, InterruptedException {
        testContainerProduceOne();

        final TestRunner runner = TestRunners.newTestRunner(ConsumeKafka_2_6.class);
        runner.setValidateExpressionUsage(false);
        final URI uri = URI.create(kafka.getBootstrapServers());
        runner.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", uri.getHost(), uri.getPort()));
        runner.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        runner.setProperty("topic", TOPIC);
        runner.setProperty("group.id", GROUP_ID);
        runner.run();
        runner.assertTransferCount("success", 1);
    }

    private void testContainerProduceOne() throws ExecutionException, InterruptedException {
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(getProducerProperties())) {
            final ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, TEST_RECORD_KEY, TEST_RECORD_VALUE);
            final Future<RecordMetadata> future = producer.send(record);
            final RecordMetadata metadata = future.get();
            assertEquals(TOPIC, metadata.topic());
            assertTrue(metadata.hasOffset());
            assertEquals(0L, metadata.offset());
        }
    }
}
