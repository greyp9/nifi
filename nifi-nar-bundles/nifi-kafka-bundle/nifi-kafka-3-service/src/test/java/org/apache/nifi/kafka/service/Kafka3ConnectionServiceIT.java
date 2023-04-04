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
package org.apache.nifi.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Kafka3ConnectionServiceIT {
    private static final String IMAGE_NAME = "confluentinc/cp-kafka:7.3.2";

    private static final String TEST_TOPIC = "nifi-" + System.currentTimeMillis();

    private static KafkaContainer kafka;

    @BeforeAll
    static void beforeAll() throws ExecutionException, InterruptedException, TimeoutException {
        kafka = new KafkaContainer(DockerImageName.parse(IMAGE_NAME));
        kafka.start();

        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (final AdminClient adminClient = AdminClient.create(properties)) {
            final int numPartitions = 1;
            final short replicationFactor = 1;
            final NewTopic newTopic = new NewTopic(TEST_TOPIC, numPartitions, replicationFactor);
            final CreateTopicsResult topics = adminClient.createTopics(Collections.singleton(newTopic));
            final KafkaFuture<Void> topicFuture = topics.values().get(TEST_TOPIC);
            topicFuture.get(1, TimeUnit.SECONDS);
        }
    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
    }

    @Test
    void testProducerFromConnectionService() {
        final KafkaConnectionService connectionService = new Kafka3ConnectionService(kafka.getBootstrapServers());
        final KafkaProducerService producerService = connectionService.getProducerService(null);
        final List<PartitionState> partitionStates = producerService.getPartitionStates(TEST_TOPIC);
        assertEquals(1, partitionStates.size());
        final PartitionState partitionState = partitionStates.iterator().next();
        assertEquals(TEST_TOPIC, partitionState.getTopic());
        assertEquals(0, partitionState.getPartition());
    }


    @Test
    void testConsumerFromConnectionService() {
        final KafkaConnectionService connectionService = new Kafka3ConnectionService(kafka.getBootstrapServers());
        final KafkaConsumerService consumerService = connectionService.getConsumerService(null);
        final List<PartitionState> partitionStates = consumerService.getPartitionStates(TEST_TOPIC);
        assertEquals(1, partitionStates.size());
        final PartitionState partitionState = partitionStates.iterator().next();
        assertEquals(TEST_TOPIC, partitionState.getTopic());
        assertEquals(0, partitionState.getPartition());
    }
}
