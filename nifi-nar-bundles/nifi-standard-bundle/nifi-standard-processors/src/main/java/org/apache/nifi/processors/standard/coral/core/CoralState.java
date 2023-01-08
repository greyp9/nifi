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

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CoralState {
    private final AtomicLong nextFlowFileId;
    private final AtomicInteger countToConsume;
    private final LinkedBlockingQueue<CoralFlowFile> flowFiles;
    private final Set<Relationship> relationships;
    private final LinkedBlockingQueue<CoralFlowFileRoute> flowFilesRoute;
    private final CoralFlowFileCursor flowFileCursor;

    public CoralState(final Set<Relationship> relationships) {
        this.nextFlowFileId = new AtomicLong(0);
        this.countToConsume = new AtomicInteger(0);
        this.flowFiles = new LinkedBlockingQueue<>();
        this.relationships = relationships;
        this.flowFilesRoute = new LinkedBlockingQueue<>();
        this.flowFileCursor = new CoralFlowFileCursor();
    }

    public Set<Relationship> getRelationships() {
        return relationships;
    }

    public boolean shouldConsume() {
        return (countToConsume.get() > 0);
    }

    public int incrementToConsume(final int amount) {
        return countToConsume.addAndGet(amount);
    }

    public List<FlowFile> getFlowFiles() {
        return new ArrayList<>(flowFiles);
    }

    public List<CoralFlowFileRoute> getFlowFilesRoute() {
        return new ArrayList<>(flowFilesRoute);
    }

    public CoralFlowFileCursor getFlowFileCursor() { return flowFileCursor; }

    public int flowFileCount() {
        return flowFiles.size();
    }

    public CoralFlowFile create(long entryDate, Map<String, String> attributes, byte[] data) {
        return new CoralFlowFile(nextFlowFileId.incrementAndGet(), entryDate, attributes, data);
    }

    public void createFlowFile(final CoralFlowFile flowFile) {
        flowFiles.add(flowFile);
    }

    public void consumeFlowFile(final CoralFlowFile flowFile) {
        flowFiles.add(flowFile);
        countToConsume.decrementAndGet();
    }

    public Optional<CoralFlowFile> getFlowFile(final String idString) {
        final long id = Long.parseLong(idString);
        return flowFiles.stream().filter(ff -> ff.getId() == id).findFirst();
    }

    public void routeFlowFile(final String idString, final String relationship) {
        final long id = Long.parseLong(idString);
        final Optional<CoralFlowFile> flowFile = flowFiles.stream().filter(ff -> ff.getId() == id).findFirst();
        flowFile.ifPresent(ff -> {
                    flowFiles.remove(ff);
                    flowFilesRoute.add(new CoralFlowFileRoute(ff, relationship));
                }
        );
    }

    public void actionFlowFile(final String idString, final String action) {
        if ("CLONE".equals(action)) {
            cloneFlowFile(idString);
        } else if ("DROP".equals(action)) {
            dropFlowFile(idString);
        }
    }

    private void cloneFlowFile(final String idString) {
        final long id = Long.parseLong(idString);
        final Optional<CoralFlowFile> flowFile = flowFiles.stream().filter(ff -> ff.getId() == id).findFirst();
        flowFile.ifPresent(ff -> createFlowFile(create(System.currentTimeMillis(), ff.getAttributes(), ff.getData())));
    }

    private void dropFlowFile(final String idString) {
        final long id = Long.parseLong(idString);
        final Optional<CoralFlowFile> flowFile = flowFiles.stream().filter(ff -> ff.getId() == id).findFirst();
        flowFile.ifPresent(flowFiles::remove);
    }

    public List<CoralFlowFileRoute> drainTo() {
        List<CoralFlowFileRoute> routed = new ArrayList<>();
        flowFilesRoute.drainTo(routed);
        return routed;
    }
}
