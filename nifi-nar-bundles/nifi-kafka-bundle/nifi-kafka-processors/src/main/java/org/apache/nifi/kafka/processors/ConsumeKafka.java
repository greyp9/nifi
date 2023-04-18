package org.apache.nifi.kafka.processors;

import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.consumer.ConsumerConfiguration;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.VerifiableProcessor;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.nifi.expression.ExpressionLanguageScope.NONE;

@Tags({"kafka", "consumer", "record"})
public class ConsumeKafka extends AbstractProcessor implements VerifiableProcessor {

    static final PropertyDescriptor CONNECTION_SERVICE = new PropertyDescriptor.Builder()
            .name("Kafka Connection Service")
            .displayName("Kafka Connection Service")
            .description("Provides connections to Kafka Broker for publishing Kafka Records")
            .identifiesControllerService(KafkaConnectionService.class)
            .expressionLanguageSupported(NONE)
            .required(true)
            .build();

    static final PropertyDescriptor TOPIC_NAME = new PropertyDescriptor.Builder()
            .name("Topic Name")
            .displayName("Topic Name")
            .description("Name of the Kafka Topic from which the Processor consumes Kafka Records")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .build();

    private static final List<PropertyDescriptor> DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(
            CONNECTION_SERVICE,
            TOPIC_NAME
    ));

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
    }

    @Override
    public List<ConfigVerificationResult> verify(final ProcessContext context, final ComponentLog verificationLogger, final Map<String, String> attributes) {
        final List<ConfigVerificationResult> verificationResults = new ArrayList<>();

        final KafkaConnectionService connectionService = context.getProperty(CONNECTION_SERVICE).asControllerService(KafkaConnectionService.class);
        final KafkaConsumerService consumerService = connectionService.getConsumerService(new ConsumerConfiguration());

        final ConfigVerificationResult.Builder verificationPartitions = new ConfigVerificationResult.Builder()
                .verificationStepName("Verify Topic Partitions");

        final String topicName = context.getProperty(TOPIC_NAME).evaluateAttributeExpressions(attributes).getValue();
        try {
            final List<PartitionState> partitionStates = consumerService.getPartitionStates(topicName);
            verificationPartitions
                    .outcome(ConfigVerificationResult.Outcome.SUCCESSFUL)
                    .explanation(String.format("Partitions [%d] found for Topic [%s]", partitionStates.size(), topicName));
        } catch (final Exception e) {
            getLogger().error("Topic [%s] Partition verification failed", topicName, e);
            verificationPartitions
                    .outcome(ConfigVerificationResult.Outcome.FAILED)
                    .explanation(String.format("Topic [%s] Partition access failed: %s", topicName, e));
        }
        verificationResults.add(verificationPartitions.build());

        return verificationResults;
    }
}
