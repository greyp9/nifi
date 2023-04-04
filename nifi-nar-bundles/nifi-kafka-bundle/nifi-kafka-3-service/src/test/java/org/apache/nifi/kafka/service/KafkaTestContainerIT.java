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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaTestContainerIT {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String IMAGE_NAME = "confluentinc/cp-kafka:7.3.2";

    @Test
    void testKafkaTestContainerStartupAndConnect() {
        try (KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(IMAGE_NAME))) {
            kafka.start();
            final String bootstrapServers = kafka.getBootstrapServers();
            logger.trace("bootstrapServers={}", bootstrapServers);
            assertTrue(bootstrapServers.contains("localhost"));
            kafka.stop();
        }
    }
}
