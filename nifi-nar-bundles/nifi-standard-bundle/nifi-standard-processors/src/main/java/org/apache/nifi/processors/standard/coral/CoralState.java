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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class CoralState {
    public final AtomicInteger input;
    public final AtomicInteger output;
    private final List<FlowFile> flowFiles;

    public CoralState() {
        this.input = new AtomicInteger(0);
        this.output = new AtomicInteger(0);
        this.flowFiles = Collections.synchronizedList(new LinkedList<>());
    }

    public boolean shouldConsume() {
        return (input.get() > 0);
    }

    public boolean shouldProduce() {
        return (output.get() > 0);
    }

    public int incrementIn(final int amount) {
        return input.addAndGet(amount);
    }

    public int incrementOut(final int amount) {
        return output.addAndGet(amount);
    }

    public int flowFileCount() {
        return flowFiles.size();
    }

    public boolean addFlowFile(final FlowFile flowFile) {
        final boolean toAdd = ((flowFile != null) && (input.get() > 0));
        if (toAdd) {
            flowFiles.add(flowFile);
            input.decrementAndGet();
        }
        return toAdd;
    }

    public Optional<FlowFile> removeFlowFile() {
        final boolean toRemove = (output.get() > 0);
        Optional<FlowFile> flowFile = Optional.empty();
        if (toRemove) {
            flowFile = flowFiles.stream().findFirst();
        }
        if (flowFile.isPresent()) {
            output.decrementAndGet();
            flowFiles.remove(flowFile.get());
        }
        return flowFile;
    }
}
