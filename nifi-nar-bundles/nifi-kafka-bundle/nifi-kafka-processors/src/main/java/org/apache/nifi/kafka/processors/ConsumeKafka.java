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

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.kafka.processors.common.KafkaUtils;
import org.apache.nifi.kafka.processors.consumer.ProcessingStrategy;
import org.apache.nifi.kafka.processors.consumer.bundle.ByteRecordBundler;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.common.OffsetSummary;
import org.apache.nifi.kafka.service.api.common.PartitionState;
import org.apache.nifi.kafka.service.api.common.TopicPartitionSummary;
import org.apache.nifi.kafka.service.api.consumer.AutoOffsetReset;
import org.apache.nifi.kafka.service.api.consumer.ConsumerConfiguration;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.consumer.PollingContext;
import org.apache.nifi.kafka.service.api.consumer.PollingSummary;
import org.apache.nifi.kafka.service.api.record.ByteRecord;
import org.apache.nifi.kafka.shared.attribute.KafkaFlowFileAttribute;
import org.apache.nifi.kafka.shared.property.KeyEncoding;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.VerifiableProcessor;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.provenance.ProvenanceReporter;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.serialization.MalformedRecordException;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.serialization.RecordReaderFactory;
import org.apache.nifi.serialization.RecordSetWriter;
import org.apache.nifi.serialization.RecordSetWriterFactory;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.RecordSet;
import org.apache.nifi.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.nifi.expression.ExpressionLanguageScope.NONE;

@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@Tags({"kafka", "consumer", "record"})
public class ConsumeKafka extends AbstractProcessor implements VerifiableProcessor {

    static final AllowableValue TOPIC_NAME = new AllowableValue("names", "names", "Topic is a full topic name or comma separated list of names");
    static final AllowableValue TOPIC_PATTERN = new AllowableValue("pattern", "pattern", "Topic is a regex using the Java Pattern syntax");

    static final PropertyDescriptor CONNECTION_SERVICE = new PropertyDescriptor.Builder()
            .name("Kafka Connection Service")
            .displayName("Kafka Connection Service")
            .description("Provides connections to Kafka Broker for publishing Kafka Records")
            .identifiesControllerService(KafkaConnectionService.class)
            .expressionLanguageSupported(NONE)
            .required(true)
            .build();

    static final PropertyDescriptor GROUP_ID = new PropertyDescriptor.Builder()
            .name("Group ID")
            .displayName("Group ID")
            .description("Kafka Consumer Group Identifier corresponding to Kafka group.id property")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(NONE)
            .build();

    static final PropertyDescriptor TOPICS = new PropertyDescriptor.Builder()
            .name("topic")
            .displayName("Topic Name(s)")
            .description("The name of the Kafka Topic(s) from which the Processor consumes Kafka Records. More than one can be supplied if comma separated.")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .build();

    static final PropertyDescriptor TOPIC_TYPE = new PropertyDescriptor.Builder()
            .name("topic_type")
            .displayName("Topic Name Format")
            .description("Specifies whether the Topic(s) provided are a comma separated list of names or a single regular expression")
            .required(true)
            .allowableValues(TOPIC_NAME, TOPIC_PATTERN)
            .defaultValue(TOPIC_NAME)
            .build();

    static final PropertyDescriptor RECORD_READER = new PropertyDescriptor.Builder()
            .name("record-reader")
            .displayName("Record Reader")
            .description("The Record Reader to use for incoming Kafka messages")
            .identifiesControllerService(RecordReaderFactory.class)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .build();

    static final PropertyDescriptor RECORD_WRITER = new PropertyDescriptor.Builder()
            .name("record-writer")
            .displayName("Record Writer")
            .description("The Record Writer to use in order to serialize the outgoing FlowFiles")
            .identifiesControllerService(RecordSetWriterFactory.class)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .build();

    static final PropertyDescriptor AUTO_OFFSET_RESET = new PropertyDescriptor.Builder()
            .name("Auto Offset Reset")
            .displayName("Auto Offset Reset")
            .description("Automatic offset configuration applied when no previous consumer offset found corresponding to Kafka auto.offset.reset property")
            .required(true)
            .allowableValues(AutoOffsetReset.class)
            .defaultValue(AutoOffsetReset.LATEST.getValue())
            .expressionLanguageSupported(NONE)
            .build();

    static final PropertyDescriptor KEY_ATTRIBUTE_ENCODING = new PropertyDescriptor.Builder()
            .name("key-attribute-encoding")
            .displayName("Key Attribute Encoding")
            .description("FlowFiles that are emitted have an attribute named '" + KafkaFlowFileAttribute.KAFKA_KEY + "'. This property dictates how the value of the attribute should be encoded.")
            .required(true)
            .defaultValue(KeyEncoding.UTF8)
            .allowableValues(KeyEncoding.class)
            .build();

    static final PropertyDescriptor MESSAGE_DEMARCATOR = new PropertyDescriptor.Builder()
            .name("message-demarcator")
            .displayName("Message Demarcator")
            .required(false)
            .addValidator(Validator.VALID)
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .description("Since KafkaConsumer receives messages in batches, you have an option to output FlowFiles which contains "
                    + "all Kafka messages in a single batch for a given topic and partition and this property allows you to provide a string (interpreted as UTF-8) to use "
                    + "for demarcating apart multiple Kafka messages. This is an optional property and if not provided each Kafka message received "
                    + "will result in a single FlowFile which  "
                    + "time it is triggered. To enter special character such as 'new line' use CTRL+Enter or Shift+Enter depending on the OS")
            .build();

    static final PropertyDescriptor SEPARATE_BY_KEY = new PropertyDescriptor.Builder()
            .name("separate-by-key")
            .displayName("Separate By Key")
            .description("If true, and the <Message Demarcator> property is set, two messages will only be added to the same FlowFile if both of the Kafka Messages have identical keys.")
            .required(false)
            .allowableValues("true", "false")
            .defaultValue("false")
            .build();


    static final PropertyDescriptor PROCESSING_STRATEGY = new PropertyDescriptor.Builder()
            .name("Processing Strategy")
            .displayName("Processing Strategy")
            .description("Strategy for processing Kafka Records and writing serialized output to FlowFiles")
            .required(true)
            .allowableValues(ProcessingStrategy.class)
            .defaultValue(ProcessingStrategy.FLOW_FILE.getValue())
            .expressionLanguageSupported(NONE)
            .build();

    static final PropertyDescriptor HEADER_ENCODING = new PropertyDescriptor.Builder()
            .name("Header Encoding")
            .displayName("Header Encoding")
            .description("Character encoding applied when reading Kafka Record Header values and writing FlowFile attributes")
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
            .defaultValue(StandardCharsets.UTF_8.name())
            .required(true)
            .build();

    static final PropertyDescriptor HEADER_NAME_PATTERN = new PropertyDescriptor.Builder()
            .name("Header Name Pattern")
            .displayName("Header Name Pattern")
            .description("Regular Expression Pattern applied to Kafka Record Header Names for selecting Header Values to be written as FlowFile attributes")
            .addValidator(StandardValidators.REGULAR_EXPRESSION_VALIDATOR)
            .required(false)
            .build();

    static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles containing one or more serialized Kafka Records")
            .build();

    private static final List<PropertyDescriptor> DESCRIPTORS = List.of(
            CONNECTION_SERVICE,
            GROUP_ID,
            TOPICS,
            TOPIC_TYPE,
            MESSAGE_DEMARCATOR,
            SEPARATE_BY_KEY,
            RECORD_READER,
            RECORD_WRITER,
            AUTO_OFFSET_RESET,
            PROCESSING_STRATEGY,
            HEADER_ENCODING,
            HEADER_NAME_PATTERN,
            KEY_ATTRIBUTE_ENCODING
            //COMMIT_OFFSETS,  // to be implemented
            //KEY_FORMAT,
            //KEY_RECORD_READER,
            //MAX_UNCOMMITTED_TIME,
            //OUTPUT_STRATEGY,
    );

    private static final Set<Relationship> RELATIONSHIPS = Collections.singleton(SUCCESS);

    private static final String TRANSIT_URI_FORMAT = "kafka://%s/%s";

    private KafkaConsumerService consumerService;

    private Charset headerEncoding;

    private Pattern headerNamePattern;

    private KeyEncoding keyEncoding;

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        final KafkaConnectionService connectionService = context.getProperty(CONNECTION_SERVICE).asControllerService(KafkaConnectionService.class);
        consumerService = connectionService.getConsumerService(new ConsumerConfiguration());
        headerEncoding = Charset.forName(context.getProperty(HEADER_ENCODING).getValue());

        final String headerNamePatternProperty = context.getProperty(HEADER_NAME_PATTERN).getValue();
        if (StringUtils.isNotBlank(headerNamePatternProperty)) {
            headerNamePattern = Pattern.compile(headerNamePatternProperty);
        } else {
            headerNamePattern = null;
        }
        keyEncoding = context.getProperty(KEY_ATTRIBUTE_ENCODING).asAllowableValue(KeyEncoding.class);
    }

    @OnStopped
    public void onStopped() {
        // discard reference; leave controller service state intact
        consumerService = null;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        final PollingContext pollingContext = getPollingContext(context);

        final Iterator<ByteRecord> consumerRecords = consumerService.poll(pollingContext).iterator();
        if (consumerRecords.hasNext()) {
            processConsumerRecords(context, session, pollingContext, consumerRecords);
        } else {
            getLogger().debug("No Kafka Records consumed: {}", pollingContext);
            context.yield();
        }
    }

    @Override
    public List<ConfigVerificationResult> verify(final ProcessContext context, final ComponentLog verificationLogger, final Map<String, String> attributes) {
        final List<ConfigVerificationResult> verificationResults = new ArrayList<>();

        final KafkaConnectionService connectionService = context.getProperty(CONNECTION_SERVICE).asControllerService(KafkaConnectionService.class);
        final KafkaConsumerService consumerService = connectionService.getConsumerService(new ConsumerConfiguration());

        final ConfigVerificationResult.Builder verificationPartitions = new ConfigVerificationResult.Builder()
                .verificationStepName("Verify Topic Partitions");

        final PollingContext pollingContext = getPollingContext(context);

        try {
            final List<PartitionState> partitionStates = consumerService.getPartitionStates(pollingContext);
            verificationPartitions
                    .outcome(ConfigVerificationResult.Outcome.SUCCESSFUL)
                    .explanation(String.format("Partitions [%d] found for Topics %s", partitionStates.size(), pollingContext.getTopics()));
        } catch (final Exception e) {
            getLogger().error("Topics {} Partition verification failed", pollingContext.getTopics(), e);
            verificationPartitions
                    .outcome(ConfigVerificationResult.Outcome.FAILED)
                    .explanation(String.format("Topics %s Partition access failed: %s", pollingContext.getTopics(), e));
        }
        verificationResults.add(verificationPartitions.build());

        return verificationResults;
    }

    private void processConsumerRecords(final ProcessContext context, final ProcessSession session, final PollingContext pollingContext, final Iterator<ByteRecord> consumerRecords) {
        final ProcessingStrategy processingStrategy = ProcessingStrategy.valueOf(context.getProperty(PROCESSING_STRATEGY).getValue());
        // model this switch on the existing implementation at `ConsumerLease.processRecords()`
        if (ProcessingStrategy.FLOW_FILE == processingStrategy) {
            processInputFlowFile(session, pollingContext, consumerRecords);
        } else if (ProcessingStrategy.DEMARCATOR == processingStrategy) {
            final Iterator<ByteRecord> iteratorDemarcator = transformDemarcator(context, consumerRecords);
            processInputFlowFile(session, pollingContext, iteratorDemarcator);
        } else if (ProcessingStrategy.RECORD == processingStrategy) {
            processInputRecords(context, session, pollingContext, consumerRecords);
        } else {
            throw new ProcessException(String.format("Processing Strategy not supported [%s]", processingStrategy));
        }
    }

    private Iterator<ByteRecord> transformDemarcator(final ProcessContext context, final Iterator<ByteRecord> consumerRecords) {
        final PropertyValue propertyValueDemarcator = context.getProperty(ConsumeKafka.MESSAGE_DEMARCATOR);
        if (propertyValueDemarcator.isSet()) {
            final byte[] demarcator = propertyValueDemarcator.evaluateAttributeExpressions().getValue().getBytes(StandardCharsets.UTF_8);
            final boolean separateByKey = context.getProperty(SEPARATE_BY_KEY).asBoolean();
            return new ByteRecordBundler(demarcator, separateByKey, keyEncoding, headerNamePattern, headerEncoding).bundle(consumerRecords);
        } else {
            return consumerRecords;
        }
    }

    private void processInputRecords(final ProcessContext context, final ProcessSession session, final PollingContext pollingContext, final Iterator<ByteRecord> consumerRecords) {
        final RecordReaderFactory readerFactory = context.getProperty(RECORD_READER).asControllerService(RecordReaderFactory.class);
        final RecordSetWriterFactory writerFactory = context.getProperty(RECORD_WRITER).asControllerService(RecordSetWriterFactory.class);
        try {
            while (consumerRecords.hasNext()) {
                final ByteRecord consumerRecord = consumerRecords.next();
                final byte[] valueIn = consumerRecord.getValue();
                int recordCount = 0;
                if (valueIn.length > 0) {
                    final InputStream in = new ByteArrayInputStream(valueIn);
                    final Map<String, String> attributes = KafkaUtils.toAttributes(consumerRecord, keyEncoding, headerNamePattern, headerEncoding);
                    final RecordReader reader = readerFactory.createRecordReader(attributes, in, valueIn.length, getLogger());
                    FlowFile flowFile = session.create();
                    flowFile = session.putAllAttributes(flowFile, attributes);
                    try (final OutputStream rawOut = session.write(flowFile)) {
                        final RecordSet recordSet = reader.createRecordSet();
                        final RecordSchema schema = writerFactory.getSchema(attributes, recordSet.getSchema());
                        final RecordSetWriter writer = writerFactory.createWriter(getLogger(), schema, rawOut, attributes);
                        Record record;
                        writer.beginRecordSet();
                        while ((record = recordSet.next()) != null) {
                            ++recordCount;
                            writer.write(record);
                        }
                        writer.finishRecordSet();
                        writer.flush();
                        final ProvenanceReporter provenanceReporter = session.getProvenanceReporter();
                        final String transitUri = String.format(TRANSIT_URI_FORMAT, consumerRecord.getTopic(), consumerRecord.getPartition());
                        provenanceReporter.receive(flowFile, transitUri);
                    }
                    flowFile = session.putAttribute(flowFile, "record.count", String.valueOf(recordCount));
                    session.transfer(flowFile, SUCCESS);
                }
            }
            session.commitAsync();
        } catch (MalformedRecordException | SchemaNotFoundException | IOException e) {
            throw new ProcessException(e);
        }
    }

    private void processInputFlowFile(final ProcessSession session, final PollingContext pollingContext, final Iterator<ByteRecord> consumerRecords) {
        final Map<TopicPartitionSummary, OffsetSummary> offsets = new LinkedHashMap<>();

        while (consumerRecords.hasNext()) {
            final ByteRecord consumerRecord = consumerRecords.next();

            final byte[] value = consumerRecord.getValue();
            FlowFile flowFile = session.create();
            flowFile = session.write(flowFile, outputStream -> outputStream.write(value));

            final Map<String, String> attributes = KafkaUtils.toAttributes(consumerRecord, keyEncoding, headerNamePattern, headerEncoding);
            flowFile = session.putAllAttributes(flowFile, attributes);

            final ProvenanceReporter provenanceReporter = session.getProvenanceReporter();
            final String transitUri = String.format(TRANSIT_URI_FORMAT, consumerRecord.getTopic(), consumerRecord.getPartition());
            provenanceReporter.receive(flowFile, transitUri);

            session.transfer(flowFile, SUCCESS);

            final TopicPartitionSummary topicPartitionSummary = new TopicPartitionSummary(consumerRecord.getTopic(), consumerRecord.getPartition());
            final long offset = consumerRecord.getOffset();
            final OffsetSummary offsetSummary = offsets.computeIfAbsent(topicPartitionSummary, (summary) -> new OffsetSummary(offset));
            offsetSummary.setOffset(offset);
        }

        final PollingSummary pollingSummary;
        if (pollingContext.getTopicPattern().isPresent()) {
            pollingSummary = new PollingSummary(pollingContext.getGroupId(), pollingContext.getTopicPattern().get(), pollingContext.getAutoOffsetReset(), offsets);
        } else {
            pollingSummary = new PollingSummary(pollingContext.getGroupId(), pollingContext.getTopics(), pollingContext.getAutoOffsetReset(), offsets);
        }
        session.commitAsync(() -> consumerService.commit(pollingSummary));
    }

    private PollingContext getPollingContext(final ProcessContext context) {
        final String groupId = context.getProperty(GROUP_ID).getValue();
        final String offsetReset = context.getProperty(AUTO_OFFSET_RESET).getValue();
        final AutoOffsetReset autoOffsetReset = AutoOffsetReset.valueOf(offsetReset.toUpperCase());
        final String topics = context.getProperty(TOPICS).evaluateAttributeExpressions().getValue();
        final String topicType = context.getProperty(TOPIC_TYPE).getValue();

        final PollingContext pollingContext;
        if (topicType.equals(TOPIC_PATTERN.getValue())) {
            final Pattern topicPattern = Pattern.compile(topics.trim());
            pollingContext = new PollingContext(groupId, topicPattern, autoOffsetReset);
        } else if (topicType.equals(TOPIC_NAME.getValue())) {
            final Collection<String> topicList = KafkaUtils.toTopicList(topics);
            pollingContext = new PollingContext(groupId, topicList, autoOffsetReset);
        } else {
            getLogger().error("Subscription type has an unknown value {}", topicType);
            return null;
        }
        return pollingContext;
    }
}
