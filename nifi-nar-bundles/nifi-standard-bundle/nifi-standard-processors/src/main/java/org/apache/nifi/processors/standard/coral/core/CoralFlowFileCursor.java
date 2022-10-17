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
package org.apache.nifi.processors.standard.coral.core;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public final class CoralFlowFileCursor {
    private final Map<String, String> attributes;
    private final ByteArrayOutputStream content;

    public CoralFlowFileCursor() {
        this.attributes = new HashMap<>();
        this.content = new ByteArrayOutputStream();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public ByteArrayOutputStream getContent() {
        return content;
    }

    public void reset() {
        attributes.clear();
        content.reset();
    }

    public void setAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    public void setContent(final byte[] data) {
        content.reset();
        content.write(data, 0, data.length);
    }

    public void set(final CoralFlowFile flowFile) {
        reset();
        attributes.putAll(flowFile.getAttributes());
        setContent(flowFile.getData());
    }
}
