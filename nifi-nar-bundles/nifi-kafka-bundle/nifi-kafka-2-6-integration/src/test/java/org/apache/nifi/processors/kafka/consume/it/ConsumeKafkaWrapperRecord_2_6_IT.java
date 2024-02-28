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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.nifi.kafka.shared.property.KeyFormat;
import org.apache.nifi.kafka.shared.property.OutputStrategy;
import org.apache.nifi.processors.kafka.pubsub.ConsumeKafkaRecord_2_6;
import org.apache.nifi.processors.kafka.pubsub.KafkaProcessor;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumeKafkaWrapperRecord_2_6_IT extends ConsumeKafka_2_6_BaseIT {
    private static final String TEST_RESOURCE = "org/apache/nifi/processors/kafka/publish/ff.json";
    private static final int TEST_RECORD_COUNT = 3;

    private static final String TOPIC = ConsumeKafkaWrapperRecord_2_6_IT.class.getName();
    private static final String GROUP_ID = ConsumeKafkaWrapperRecord_2_6_IT.class.getSimpleName();

    @Test
    public void testKafkaTestContainerProduceConsumeOne() throws ExecutionException, InterruptedException, IOException, InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(ConsumeKafkaRecord_2_6.class);
        runner.setValidateExpressionUsage(false);
        final URI uri = URI.create(kafka.getBootstrapServers());
        addRecordReaderService(runner);
        addRecordWriterService(runner);
        addKeyRecordReaderService(runner);
        runner.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", uri.getHost(), uri.getPort()));
        runner.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        runner.setProperty(KafkaProcessor.OUTPUT_STRATEGY, OutputStrategy.USE_WRAPPER);
        runner.setProperty(KafkaProcessor.KEY_FORMAT, KeyFormat.RECORD);
        runner.setProperty("topic", TOPIC);
        runner.setProperty("group.id", GROUP_ID);

        runner.run(1, false, true);
        final String messageKey = "{\"id\": 0,\"name\": \"K\"}";
        final String message = new String(IOUtils.toByteArray(Objects.requireNonNull(
                getClass().getClassLoader().getResource(TEST_RESOURCE))), StandardCharsets.UTF_8);
        final List<Header> headersPublish = Collections.singletonList(
                new RecordHeader("header1", "value1".getBytes(StandardCharsets.UTF_8)));
        produceOne(TOPIC, null, messageKey, message, headersPublish);
        final long pollUntil = System.currentTimeMillis() + DURATION_POLL.toMillis();
        while (System.currentTimeMillis() < pollUntil) {
            runner.run(1, false, false);
        }
        runner.run(1, true, false);

        runner.assertTransferCount(KafkaProcessor.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(KafkaProcessor.REL_SUCCESS);
        assertEquals(1, flowFiles.size());
        final MockFlowFile flowFile = flowFiles.getFirst();
        runner.getLogger().trace(flowFile.getContent());
        flowFile.assertAttributeEquals("record.count", Long.toString(TEST_RECORD_COUNT));

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNodeTree = objectMapper.readTree(flowFile.getContent());
        final ArrayNode arrayNode = assertInstanceOf(ArrayNode.class, jsonNodeTree);

        final Iterator<JsonNode> elements = arrayNode.elements();
        assertEquals(TEST_RECORD_COUNT, arrayNode.size());
        while (elements.hasNext()) {
            final ObjectNode wrapper = assertInstanceOf(ObjectNode.class, elements.next());

            final ObjectNode key = assertInstanceOf(ObjectNode.class, wrapper.get("key"));
            assertEquals(2, key.size());
            assertEquals(0, key.get("id").asInt());
            assertEquals("K", key.get("name").asText());

            final ObjectNode value = assertInstanceOf(ObjectNode.class, wrapper.get("value"));
            assertTrue(Arrays.asList(1, 2, 3).contains(value.get("id").asInt()));
            assertTrue(Arrays.asList("A", "B", "C").contains(value.get("name").asText()));

            final ObjectNode headers = assertInstanceOf(ObjectNode.class, wrapper.get("headers"));
            assertEquals(1, headers.size());
            assertEquals("value1", headers.get("header1").asText());

            final ObjectNode metadata = assertInstanceOf(ObjectNode.class, wrapper.get("metadata"));
            assertEquals(TOPIC, metadata.get("topic").asText());
            assertEquals(0, metadata.get("partition").asInt());
            assertEquals(0, metadata.get("offset").asInt());
            assertTrue(metadata.get("timestamp").isIntegralNumber());

            final List<ProvenanceEventRecord> provenanceEvents = runner.getProvenanceEvents();
            assertEquals(1, provenanceEvents.size());
            final ProvenanceEventRecord provenanceEvent = provenanceEvents.getFirst();
            assertEquals(ProvenanceEventType.RECEIVE, provenanceEvent.getEventType());
        }
    }
}
