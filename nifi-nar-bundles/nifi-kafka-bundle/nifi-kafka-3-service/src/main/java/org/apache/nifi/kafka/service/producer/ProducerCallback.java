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
package org.apache.nifi.kafka.service.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.nifi.kafka.service.api.producer.ProducerRecordMetadata;
import org.apache.nifi.kafka.service.api.producer.RecordSummary;
import org.apache.nifi.kafka.shared.util.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ProducerCallback implements Callback {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicLong sentCount;
    private final AtomicLong acknowledgedCount;
    private final AtomicLong failedCount;
    private final List<ProducerRecordMetadata> metadatas;
    private final List<Exception> exceptions;
    private final Notifier notifier;

    public ProducerCallback() {
        this.sentCount = new AtomicLong(0L);
        this.acknowledgedCount = new AtomicLong(0L);
        this.failedCount = new AtomicLong(0L);
        this.metadatas = new ArrayList<>();
        this.exceptions = new ArrayList<>();
        this.notifier = new Notifier();
    }

    public long send() {
        return sentCount.incrementAndGet();
    }

    @Override
    public void onCompletion(final RecordMetadata metadata, final Exception exception) {
        if (exception == null) {
            acknowledgedCount.addAndGet(1L);
            metadatas.add(toProducerRecordMetadata(metadata));
        } else {
            failedCount.addAndGet(1L);
            exceptions.add(exception);
        }
        notifier.notifyWaiter();
    }

    private static ProducerRecordMetadata toProducerRecordMetadata(final RecordMetadata m) {
        return new ProducerRecordMetadata(m.topic(), m.partition(), m.offset(), m.timestamp());
    }

    public RecordSummary waitComplete(final long maxAckWaitMillis) {
        logger.trace("waitComplete():start");
        final Supplier<Boolean> conditionComplete = () -> (acknowledgedCount.get() == sentCount.get());
        final boolean success = notifier.waitForCondition(conditionComplete, maxAckWaitMillis);
        logger.trace("waitComplete():finish - {}", success);
        return new RecordSummary(success, sentCount.get(), acknowledgedCount.get(), failedCount.get(), metadatas, exceptions);
    }
}
