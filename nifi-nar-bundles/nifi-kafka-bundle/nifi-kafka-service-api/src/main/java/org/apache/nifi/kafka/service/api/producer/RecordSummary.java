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
package org.apache.nifi.kafka.service.api.producer;

import org.apache.nifi.flowfile.FlowFile;

import java.util.List;

public class RecordSummary {
    private final boolean success;
    private final long sentCount;
    private final long acknowledgedCount;
    private final long failedCount;

    private final List<FlowFile> flowFiles;
    private final List<ProducerRecordMetadata> metadatas;
    private final List<Exception> exceptions;

    public RecordSummary(final boolean success, final long sentCount, final long acknowledgedCount, final long failedCount,
                         final List<FlowFile> flowFiles, final List<ProducerRecordMetadata> metadatas, final List<Exception> exceptions) {
        this.success = success;
        this.sentCount = sentCount;
        this.acknowledgedCount = acknowledgedCount;
        this.failedCount = failedCount;
        this.flowFiles = flowFiles;
        this.metadatas = metadatas;
        this.exceptions = exceptions;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getSentCount() {
        return sentCount;
    }

    public long getAcknowledgedCount() {
        return acknowledgedCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public List<FlowFile> getFlowFiles() {
        return flowFiles;
    }

    public List<ProducerRecordMetadata> getMetadatas() {
        return metadatas;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }
}
