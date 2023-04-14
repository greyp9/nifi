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
package org.apache.nifi.kafka.processors.pubsub;

import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.consumer.ConsumerConfiguration;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.apache.nifi.kafka.service.api.producer.ProducerConfiguration;
import org.apache.nifi.processor.VerifiableProcessor;
import org.apache.nifi.processors.kafka.pubsub.PublishKafka;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.apache.nifi.processors.kafka.pubsub.PublishKafka.CONNECTION_SERVICE;
import static org.apache.nifi.processors.kafka.pubsub.PublishKafka.TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPublishKafka {

    private TestRunner runner;

    private static final String TEST_TOPIC = "nifi-" + System.currentTimeMillis();


    @BeforeEach
    public void setRunner() {
        runner = TestRunners.newTestRunner(PublishKafka.class);
    }

    @Test
    public void testVerifiableSuccess() throws InitializationException {
        final PartitionState partitionState0 = new PartitionState(TEST_TOPIC, 0);
        final List<PartitionState> partitionStates = Collections.singletonList(partitionState0);
        final KafkaProducerService producerService = mock(KafkaProducerService.class);
        when(producerService.getPartitionStates(eq(TEST_TOPIC))).thenReturn(partitionStates);

        final KafkaConnectionService connectionService = new MockKafkaConnectionService(producerService);
        final String connectionServiceId = KafkaConnectionService.class.getSimpleName();
        runner.addControllerService(connectionServiceId, connectionService);
        runner.enableControllerService(connectionService);
        runner.setProperty(TOPIC, TEST_TOPIC);
        runner.setProperty(CONNECTION_SERVICE, connectionServiceId);

        final List<ConfigVerificationResult> results = ((VerifiableProcessor) runner.getProcessor())
                .verify(runner.getProcessContext(), runner.getLogger(), Collections.emptyMap());
        assertEquals(1, results.size());

        final ConfigVerificationResult firstResult = results.iterator().next();
        assertEquals(ConfigVerificationResult.Outcome.SUCCESSFUL, firstResult.getOutcome());
        assertNotNull(firstResult.getExplanation());
    }

    @Test
    public void testVerifiableFailure() throws InitializationException {
        final KafkaProducerService producerService = mock(KafkaProducerService.class);
        when(producerService.getPartitionStates(eq(TEST_TOPIC))).thenThrow(new IllegalStateException("unreachable"));

        final KafkaConnectionService connectionService = new MockKafkaConnectionService(producerService);
        final String connectionServiceId = KafkaConnectionService.class.getSimpleName();
        runner.addControllerService(connectionServiceId, connectionService);
        runner.enableControllerService(connectionService);
        runner.setProperty(TOPIC, TEST_TOPIC);
        runner.setProperty(CONNECTION_SERVICE, connectionServiceId);

        final List<ConfigVerificationResult> results = ((VerifiableProcessor) runner.getProcessor())
                .verify(runner.getProcessContext(), runner.getLogger(), Collections.emptyMap());
        assertEquals(1, results.size());

        final ConfigVerificationResult firstResult = results.iterator().next();
        assertEquals(ConfigVerificationResult.Outcome.FAILED, firstResult.getOutcome());
        assertNotNull(firstResult.getExplanation());
    }

    private static class MockKafkaConnectionService extends AbstractControllerService implements KafkaConnectionService {
        private final KafkaProducerService producerService;

        public MockKafkaConnectionService(final KafkaProducerService producerService) {
            this.producerService = producerService;
        }

        @Override
        public KafkaConsumerService getConsumerService(ConsumerConfiguration consumerConfiguration) {
            return null;
        }

        @Override
        public KafkaProducerService getProducerService(ProducerConfiguration producerConfiguration) {
            return producerService;
        }
    }
}
