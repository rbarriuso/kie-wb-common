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

package org.kie.workbench.common.stunner.backend.service;

import java.io.IOException;
import java.io.InputStream;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.errai.marshalling.server.ServerMarshalling;
import org.kie.workbench.common.stunner.core.definition.service.DiagramMetadataMarshaller;
import org.kie.workbench.common.stunner.core.diagram.Metadata;

@ApplicationScoped
public class ErraiDiagramMetadataMarshaller implements DiagramMetadataMarshaller<Metadata> {

    @Override
    public Metadata unmarshall(final InputStream input) throws IOException {
        Metadata result = (Metadata) ServerMarshalling.fromJSON(input);
        return result;
    }

    @Override
    public String marshall(final Metadata metadata) throws IOException {
        String result = ServerMarshalling.toJSON(metadata);
        return result;
    }
}
