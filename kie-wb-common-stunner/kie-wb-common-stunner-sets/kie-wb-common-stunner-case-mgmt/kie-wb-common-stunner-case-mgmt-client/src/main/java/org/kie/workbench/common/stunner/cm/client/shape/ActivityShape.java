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

package org.kie.workbench.common.stunner.cm.client.shape;

import org.kie.workbench.common.stunner.bpmn.definition.BaseTask;
import org.kie.workbench.common.stunner.cm.client.shape.view.ActivityView;
import org.kie.workbench.common.stunner.core.client.shape.MutationContext;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.shapes.def.RectangleShapeDef;

public class ActivityShape<W> extends AbstractCMContainerShape<W, RectangleShapeDef<W>, ActivityView> {

    public ActivityShape(final RectangleShapeDef<W> shapeDef,
                         final ActivityView view) {
        super(shapeDef,
              view);
    }

    @Override
    public void applyProperties(final Node<View<W>, Edge> element,
                                final MutationContext mutationContext) {
        super.applyProperties(element,
                              mutationContext);
        final Double width = BaseTask.BaseTaskBuilder.WIDTH;
        final Double height = BaseTask.BaseTaskBuilder.HEIGHT;
        getDefViewHandler().getViewHandler().applySize(width,
                                                       height,
                                                       mutationContext);
    }
}
