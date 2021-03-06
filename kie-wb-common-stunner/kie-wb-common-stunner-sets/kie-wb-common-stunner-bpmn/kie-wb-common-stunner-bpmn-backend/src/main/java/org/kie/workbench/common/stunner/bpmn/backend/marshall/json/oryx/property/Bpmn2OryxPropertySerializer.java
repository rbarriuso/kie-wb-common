/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.bpmn.backend.marshall.json.oryx.property;

import org.kie.workbench.common.stunner.core.definition.property.PropertyType;

/**
 * Serializer for the different property values expected by oryx/jbpmdesigner marshallers.
 * @param <T> The type for the value.
 */
public interface Bpmn2OryxPropertySerializer<T> {

    boolean accepts(final PropertyType type);

    T parse(final Object property,
            final String value);

    String serialize(final Object property,
                     final T value);
}
