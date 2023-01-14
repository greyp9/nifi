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

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.TriggerWhenEmpty;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFile;
import org.apache.nifi.processors.standard.coral.core.CoralFlowFileRoute;
import org.apache.nifi.processors.standard.coral.core.CoralState;
import org.apache.nifi.processors.standard.coral.core.ServerFactory;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@CapabilityDescription("Provide interactivity to manually debug a NiFi flow")
@Tags({"debug", "routing"})
@TriggerWhenEmpty
public class Coral extends AbstractProcessor {

    public static final PropertyDescriptor PORT = new PropertyDescriptor.Builder()
            .name("Port")
            .description("The (SSL) port to listen on for incoming connections")
            .required(true)
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .defaultValue("18443")
            .build();
    public static final PropertyDescriptor RELATIONSHIPS = new PropertyDescriptor.Builder()
            .name("Relationships")
            .description("The (dynamic) set of outgoing relationships")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .defaultValue("Outgoing")
            .build();

    private CoralState coralState;
    private Server server;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(PORT);
        properties.add(RELATIONSHIPS);
        return properties;
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final Set<Relationship> set = new HashSet<>();
        set.add(REL_OUTGOING);
        relationships = new AtomicReference<>(set);
    }

    @Override
    public void onPropertyModified(final PropertyDescriptor descriptor, final String oldValue, final String newValue) {
        if (descriptor.equals(RELATIONSHIPS)) {
            relationships.set(toRelationships(newValue));
        }
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        coralState = new CoralState(getRelationships());
        final int port = context.getProperty(PORT).asInteger();
        server = new ServerFactory().create(coralState, port);

        try {
            server.start();
        } catch (Exception e) {
            getLogger().error("Server failed to start", e);
        }
    }

    @SuppressWarnings("unused")
    @OnUnscheduled
    public void OnUnscheduled(final ProcessContext context) {
        try {
            server.stop();
        } catch (Exception e) {
            getLogger().error("Server failed to stop", e);
        } finally {
            server = null;
        }
        coralState = null;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        boolean consume = false;
        if (coralState.shouldConsume()) {
            final FlowFile flowFile = session.get();
            if (flowFile != null) {
                coralState.consumeFlowFile(toCoral(session, flowFile));
                session.remove(flowFile);
                session.commit();
                consume = true;
            } else {
                session.rollback();
            }
        }

        final List<CoralFlowFileRoute> flowFilesRouted = coralState.drainTo();
        final boolean produce = !flowFilesRouted.isEmpty();
        for (final CoralFlowFileRoute flowFile : flowFilesRouted) {
            final FlowFile flowFileIt = fromCoral(session, flowFile.getCoralFlowFile());
            session.transfer(flowFileIt, asRelationship(flowFile.getRelationship()));
        }
        if (produce) {
            session.commit();
        }

        if ((!consume) && (!produce)) {
            context.yield();
        }
    }

    private CoralFlowFile toCoral(final ProcessSession session, final FlowFile flowFile) {
        try {
            final long entryDate = flowFile.getEntryDate();
            final Map<String, String> attributes = new HashMap<>(flowFile.getAttributes());
            attributes.put("ffId", Long.toString(flowFile.getId()));
            try (final InputStream read = session.read(flowFile)) {
                final byte[] data = IOUtils.toByteArray(read);
                return coralState.create(entryDate, attributes, data);
            }
        } catch (final IOException e) {
            throw new ProcessException(e);
        }
    }

    private FlowFile fromCoral(final ProcessSession session, final CoralFlowFile coralFlowFile) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, out -> out.write(coralFlowFile.getData()));
        return session.putAllAttributes(flowFile, coralFlowFile.getAttributes());
    }

    private Set<Relationship> toRelationships(final String config) {
        final Set<Relationship> relationshipsUpdate = new HashSet<>();
        final String[] names = config.split(",");
        if (names.length == 0) {
            relationshipsUpdate.add(REL_OUTGOING);
        } else {
            for (String name : names) {
                relationshipsUpdate.add(new Relationship.Builder().name(name).build());
            }
        }
        return relationshipsUpdate;
    }

    private Relationship asRelationship(final String name) {
        return getRelationships().stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
    }

    public static final Relationship REL_OUTGOING = new Relationship.Builder().name("Outgoing").description("Default Relationship").build();

    private AtomicReference<Set<Relationship>> relationships = new AtomicReference<>();

    @Override
    public Set<Relationship> getRelationships() {
        return relationships.get();
    }
}
