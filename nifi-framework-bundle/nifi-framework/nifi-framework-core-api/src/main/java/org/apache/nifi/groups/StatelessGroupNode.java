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

package org.apache.nifi.groups;

import org.apache.nifi.connectable.Connectable;
import org.apache.nifi.controller.ProcessScheduler;
import org.apache.nifi.controller.ScheduledState;
import org.apache.nifi.controller.SchedulingAgentCallback;
import org.apache.nifi.controller.scheduling.LifecycleState;
import org.apache.nifi.controller.scheduling.SchedulingAgent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface StatelessGroupNode extends Connectable {

    void initialize(StatelessGroupNodeInitializationContext initializationContext);

    ProcessGroup getProcessGroup();

    void start(ScheduledExecutorService executorService, SchedulingAgentCallback schedulingAgentCallback, LifecycleState lifecycleState);

    CompletableFuture<Void> stop(ProcessScheduler processScheduler, ScheduledExecutorService executor, SchedulingAgent schedulingAgent, LifecycleState scheduleState);

    ScheduledState getCurrentState();

    ScheduledState getDesiredState();

    void setDesiredState(ScheduledState desiredState);

    long getBoredYieldDuration(TimeUnit timeUnit);
}
