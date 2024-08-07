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

package org.apache.nifi.web.api.dto.provenance;

import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "latestProvenanceEvents")
public class LatestProvenanceEventsDTO {
    private String componentId;
    private List<ProvenanceEventDTO> provenanceEvents;

    /**
     * @return the ID of the component whose latest events were fetched
     */
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(final String componentId) {
        this.componentId = componentId;
    }

    /**
     * @return the latest provenance events that were recorded for the associated component
     */
    public List<ProvenanceEventDTO> getProvenanceEvents() {
        return provenanceEvents;
    }

    public void setProvenanceEvents(final List<ProvenanceEventDTO> provenanceEvents) {
        this.provenanceEvents = provenanceEvents;
    }
}
