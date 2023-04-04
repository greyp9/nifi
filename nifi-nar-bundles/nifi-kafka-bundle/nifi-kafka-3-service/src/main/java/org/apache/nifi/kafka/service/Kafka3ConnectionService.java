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

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ControllerServiceInitializationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.kafka.service.api.KafkaConnectionService;
import org.apache.nifi.kafka.service.api.consumer.ConsumerConfiguration;
import org.apache.nifi.kafka.service.api.consumer.KafkaConsumerService;
import org.apache.nifi.kafka.service.api.producer.KafkaProducerService;
import org.apache.nifi.kafka.service.api.producer.ProducerConfiguration;
import org.apache.nifi.kafka.service.consumer.Kafka3ConsumerService;
import org.apache.nifi.kafka.service.producer.Kafka3ProducerService;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Kafka3ConnectionService extends AbstractControllerService implements KafkaConnectionService {

    private final String bootstrapServers;

    public static PropertyDescriptor BOOTSTRAP_SERVERS = new PropertyDescriptor.Builder()
            .name("bootstrap.servers")
            .displayName("Bootstrap Servers")
            .description("Comma-separated list of Kafka Bootstrap Servers in the format host:port")
            .required(true)
            .addValidator(StandardValidators.HOSTNAME_PORT_LIST_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .defaultValue("localhost:9092")
            .build();

    private List<PropertyDescriptor> properties;

    public Kafka3ConnectionService(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    protected void init(final ControllerServiceInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(BOOTSTRAP_SERVERS);
        this.properties = Collections.unmodifiableList(properties);
    }

    @Override
    public KafkaConsumerService getConsumerService(ConsumerConfiguration consumerConfiguration) {
        return new Kafka3ConsumerService(bootstrapServers);
    }

    @Override
    public KafkaProducerService getProducerService(ProducerConfiguration producerConfiguration) {
        return new Kafka3ProducerService(bootstrapServers);
    }
}
