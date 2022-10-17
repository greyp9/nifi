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
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@TriggerWhenEmpty
public class Coral extends AbstractProcessor {
    private CoralState coralState;
    private Server server;

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        coralState = new CoralState();

        // https://www.eclipse.org/jetty/documentation/jetty-9/index.html#jetty-helloworld
        // https://stackoverflow.com/questions/39421686/jetty-pass-object-from-main-method-to-servlet
        server = new Server(18080);
        final ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.setAttribute(coralState.getClass().getName(), coralState);
        contextHandler.addServlet(CoralServlet.class, "/*");
        server.setHandler(contextHandler);

        try {
            server.start();
        } catch (Exception e) {
            getLogger().error("Server failed to start", e);
        }
    }

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
                coralState.addFlowFile(toCoral(session, flowFile));
                session.remove(flowFile);
                session.commit();
                consume = true;
            } else {
                session.rollback();
            }
        }

        boolean produce = false;
        if (coralState.shouldProduce()) {
            final Optional<FlowFile> flowFileOptional = coralState.removeFlowFile();
            if (flowFileOptional.isPresent()) {
                final FlowFile flowFile = fromCoral(session, (CoralFlowFile) flowFileOptional.get());
                session.transfer(flowFile, REL_A);
                session.commit();
                produce = true;
            }
        }

        if ((!consume) && (!produce)) {
            context.yield();
        }
    }

    private CoralFlowFile toCoral(final ProcessSession session, final FlowFile flowFile) {
        try {
            final long id = flowFile.getId();
            final Map<String, String> attributes = flowFile.getAttributes();
            try (final InputStream read = session.read(flowFile)) {
                final byte[] data = IOUtils.toByteArray(read);
                return new CoralFlowFile(id, attributes, data);
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

    public static final Relationship REL_A = new Relationship.Builder().name("A").description("Relationship A").build();
    public static final Relationship REL_B = new Relationship.Builder().name("B").description("Relationship B").build();
    public static final Relationship REL_C = new Relationship.Builder().name("C").description("Relationship C").build();

    public static final Set<Relationship> relationships = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(REL_A, REL_B, REL_C)));

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }
}
