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
package org.apache.nifi.kafka.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.json.JsonRecordSetWriter;
import org.apache.nifi.json.JsonTreeReader;
import org.apache.nifi.kafka.service.Kafka3ConnectionService;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.RecordReaderFactory;
import org.apache.nifi.serialization.RecordSetWriterFactory;
import org.apache.nifi.util.TestRunner;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public abstract class AbstractKafkaBaseIT {

    public static final String IMAGE_NAME = "confluentinc/cp-kafka:7.3.2";

    protected static final long TIMESTAMP = System.currentTimeMillis();

    protected static final String CONNECTION_SERVICE_ID = Kafka3ConnectionService.class.getSimpleName();

    protected static final Duration DURATION_POLL = Duration.ofMillis(1000L);

    protected static final KafkaContainer kafkaContainer;

    static {
        kafkaContainer = new KafkaContainer(DockerImageName.parse(IMAGE_NAME));
        kafkaContainer.start();
    }

    protected static ObjectMapper objectMapper;

    @BeforeAll
    protected static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    protected String addKafkaConnectionService(final TestRunner runner) throws InitializationException {
        final KafkaConnectionService connectionService = new Kafka3ConnectionService();
        runner.addControllerService(CONNECTION_SERVICE_ID, connectionService);
        runner.setProperty(connectionService, Kafka3ConnectionService.BOOTSTRAP_SERVERS, kafkaContainer.getBootstrapServers());
        runner.setProperty(connectionService, Kafka3ConnectionService.MAX_POLL_RECORDS, "1000");
        runner.enableControllerService(connectionService);
        return CONNECTION_SERVICE_ID;
    }

    protected String addRecordReaderService(final TestRunner runner) throws InitializationException {
        final String readerId = "record-reader";
        final RecordReaderFactory readerService = new JsonTreeReader();
        runner.addControllerService(readerId, readerService);
        runner.enableControllerService(readerService);
        runner.setProperty(readerId, readerId);
        return readerId;
    }

    protected String addRecordWriterService(final TestRunner runner) throws InitializationException {
        final String writerId = "record-writer";
        final RecordSetWriterFactory writerService = new JsonRecordSetWriter();
        runner.addControllerService(writerId, writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(writerId, writerId);
        return writerId;
    }

    protected String addRecordKeyWriterService(final TestRunner runner) throws InitializationException {
        final String writerId = "record-key-writer";
        final RecordSetWriterFactory writerService = new JsonRecordSetWriter();
        runner.addControllerService(writerId, writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(writerId, writerId);
        return writerId;
    }
}
