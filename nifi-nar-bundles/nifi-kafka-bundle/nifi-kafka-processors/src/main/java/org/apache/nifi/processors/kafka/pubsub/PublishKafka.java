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
package org.apache.nifi.processors.kafka.pubsub;

import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.apache.nifi.kafka.service.api.producer.ProducerConfiguration;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.VerifiableProcessor;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.nifi.expression.ExpressionLanguageScope.NONE;

public class PublishKafka extends AbstractProcessor implements VerifiableProcessor {

    public static final PropertyDescriptor TOPIC = new PropertyDescriptor.Builder()
            .name("topic")
            .displayName("Topic Name")
            .description("The name of the Kafka Topic to publish to.")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .build();

    public static final PropertyDescriptor CONNECTION_SERVICE = new PropertyDescriptor.Builder()
            .name("connection.service")
            .displayName("Connection Service")
            .description("The controller service that specifies the Kafka endpoint for published FlowFiles.")
            .identifiesControllerService(KafkaConnectionService.class)
            .expressionLanguageSupported(NONE)
            .required(true)
            .build();

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
    }

    @Override
    public List<ConfigVerificationResult> verify(final ProcessContext context, final ComponentLog verificationLogger,
                                                 final Map<String, String> attributes) {
        final List<ConfigVerificationResult> verificationResults = new ArrayList<>();

        final String topic = context.getProperty(TOPIC).getValue();
        final KafkaConnectionService connectionService =
                context.getProperty(CONNECTION_SERVICE).asControllerService(KafkaConnectionService.class);
        final KafkaProducerService producerService = connectionService.getProducerService(new ProducerConfiguration());

        final ConfigVerificationResult.Builder verificationPartitions = new ConfigVerificationResult.Builder()
                .verificationStepName("Determine Topic Partitions");
        try {
            final List<PartitionState> partitionStates = producerService.getPartitionStates(topic);
            verificationPartitions
                .outcome(ConfigVerificationResult.Outcome.SUCCESSFUL)
                .explanation("Determined that there are " + partitionStates.size() + " partitions for topic " + topic);
        } catch (final Exception e) {
            getLogger().error("Failed to determine Partition Information for Topic {} in order to verify configuration", topic, e);
            verificationPartitions
                .outcome(ConfigVerificationResult.Outcome.FAILED)
                .explanation("Could not fetch Partition Information: " + e);
        }
        verificationResults.add(verificationPartitions.build());

        return verificationResults;
    }
}
