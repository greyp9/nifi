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
package org.apache.nifi.processors.standard.coral;

import org.apache.nifi.flowfile.FlowFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CoralFlowFile implements FlowFile {

    private final long id;
    private final Map<String, String> attributes;
    private final byte[] data;

    public CoralFlowFile(final long id, final Map<String, String> attributes, final byte[] data) {
        this.id = id;
        this.attributes = new HashMap<>(attributes);
        this.data = data;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getEntryDate() {
        return 0L;
    }

    @Override
    public long getLineageStartDate() {
        return 0L;
    }

    @Override
    public long getLineageStartIndex() {
        return 0L;
    }

    @Override
    public Long getLastQueueDate() {
        return null;
    }

    @Override
    public long getQueueDateIndex() {
        return 0L;
    }

    @Override
    public boolean isPenalized() {
        return false;
    }

    @Override
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public long getSize() {
        return data.length;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int compareTo(@NotNull FlowFile o) {
        return 0;
    }
}