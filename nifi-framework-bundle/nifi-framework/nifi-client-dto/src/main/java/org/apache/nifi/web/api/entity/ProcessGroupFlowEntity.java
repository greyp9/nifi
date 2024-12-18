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
package org.apache.nifi.web.api.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.nifi.web.api.dto.PermissionsDTO;
import org.apache.nifi.web.api.dto.RevisionDTO;
import org.apache.nifi.web.api.dto.flow.ProcessGroupFlowDTO;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * A serialized representation of this class can be placed in the entity body of a request or response to or from the API. This particular entity holds a reference to a ProcessGroupFlowDTO.
 */
@XmlRootElement(name = "processGroupFlowEntity")
public class ProcessGroupFlowEntity extends Entity {

    private RevisionDTO revision;
    private PermissionsDTO permissions;
    private ProcessGroupFlowDTO processGroupFlow;

    /**
     * @return revision for this component
     */
    @Schema(description = "The revision for this process group."
    )
    public RevisionDTO getRevision() {
        return revision;
    }

    public void setRevision(RevisionDTO revision) {
        this.revision = revision;
    }

    /**
     * The permissions for this component.
     *
     * @return The permissions
     */
    @Schema(description = "The access policy for this process group."
    )
    public PermissionsDTO getPermissions() {
        return permissions;
    }

    public void setPermissions(PermissionsDTO permissions) {
        this.permissions = permissions;
    }

    /**
     * The ProcessGroupFlowDTO that is being serialized.
     *
     * @return The ProcessGroupFlowDTO object
     */
    public ProcessGroupFlowDTO getProcessGroupFlow() {
        return processGroupFlow;
    }

    public void setProcessGroupFlow(ProcessGroupFlowDTO flow) {
        this.processGroupFlow = flow;
    }

}
