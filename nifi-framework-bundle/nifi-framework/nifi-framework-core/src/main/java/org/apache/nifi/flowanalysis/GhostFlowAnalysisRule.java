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

package org.apache.nifi.flowanalysis;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.reporting.InitializationException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GhostFlowAnalysisRule implements FlowAnalysisRule {

    private String id;
    private String canonicalClassName;

    public void setIdentifier(final String id) {
        this.id = id;
    }

    public void setCanonicalClassName(final String canonicalClassName) {
        this.canonicalClassName = canonicalClassName;
    }

    @Override
    public Collection<ValidationResult> validate(final ValidationContext context) {
        return Collections.singleton(new ValidationResult.Builder()
            .input("Any Property")
            .subject("Missing Flow Analysis Rule")
            .valid(false)
            .explanation("Flow Analysis Rule is of type " + canonicalClassName + ", but this is not a valid Flow Analysis Rule type")
            .build());
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(final String name) {
        return buildDescriptor(name);
    }

    private PropertyDescriptor buildDescriptor(final String propertyName) {
        return new PropertyDescriptor.Builder()
            .name(propertyName)
            .description(propertyName)
            .required(true)
            .sensitive(true)
            .build();
    }

    @Override
    public void onPropertyModified(final PropertyDescriptor descriptor, String oldValue, String newValue) {
    }

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public String getIdentifier() {
        return id;
    }

    @Override
    public String toString() {
        return "GhostFlowAnalysisRule[id=" + id + "]";
    }

    @Override
    public void initialize(FlowAnalysisRuleInitializationContext context) throws InitializationException {

    }
}
