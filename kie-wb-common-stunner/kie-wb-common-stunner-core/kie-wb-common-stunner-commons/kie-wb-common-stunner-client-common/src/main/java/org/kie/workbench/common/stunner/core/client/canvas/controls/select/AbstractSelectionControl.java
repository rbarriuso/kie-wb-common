/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.core.client.canvas.controls.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.logging.client.LogConfiguration;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.Canvas;
import org.kie.workbench.common.stunner.core.client.canvas.Layer;
import org.kie.workbench.common.stunner.core.client.canvas.controls.AbstractCanvasHandlerRegistrationControl;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasShapeRemovedEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasClearSelectionEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasElementSelectedEvent;
import org.kie.workbench.common.stunner.core.client.shape.Shape;
import org.kie.workbench.common.stunner.core.client.shape.ShapeState;
import org.kie.workbench.common.stunner.core.client.shape.view.event.MouseClickEvent;
import org.kie.workbench.common.stunner.core.client.shape.view.event.MouseClickHandler;
import org.kie.workbench.common.stunner.core.client.shape.view.event.ViewEventType;
import org.kie.workbench.common.stunner.core.client.shape.view.event.ViewHandler;
import org.kie.workbench.common.stunner.core.graph.Element;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public abstract class AbstractSelectionControl<H extends AbstractCanvasHandler> extends AbstractCanvasHandlerRegistrationControl<H>
        implements SelectionControl<H, Element> {

    private static Logger LOGGER = Logger.getLogger(AbstractSelectionControl.class.getName());

    Event<CanvasElementSelectedEvent> elementSelectedEventEvent;
    Event<CanvasClearSelectionEvent> clearSelectionEventEvent;

    private final List<String> selectedElements = new ArrayList<String>();
    private ViewHandler<?> layerClickHandler;

    @Inject
    public AbstractSelectionControl(final Event<CanvasElementSelectedEvent> elementSelectedEventEvent,
                                    final Event<CanvasClearSelectionEvent> clearSelectionEventEvent) {
        this.elementSelectedEventEvent = elementSelectedEventEvent;
        this.clearSelectionEventEvent = clearSelectionEventEvent;
    }

    /*
        **************************************************************
        *               CANVAS CONTROL METHODS
        ***************************************************************
     */

    @Override
    public void enable(final H canvasHandler) {
        super.enable(canvasHandler);
        final Layer layer = canvasHandler.getCanvas().getLayer();
        // Click event.
        final MouseClickHandler clickHandler = new MouseClickHandler() {

            @Override
            public void handle(final MouseClickEvent event) {
                if (event.isButtonLeft()) {
                    handleLayerClick(!event.isShiftKeyDown());
                }
            }
        };
        layer.addHandler(ViewEventType.MOUSE_CLICK,
                         clickHandler);
        this.layerClickHandler = clickHandler;
    }

    protected abstract void register(final Element element,
                                     final Shape<?> shape);

    @Override
    public void register(final Element element) {
        if (checkNotRegistered(element)) {
            final Shape<?> shape = getCanvas().getShape(element.getUUID());
            if (null != shape) {
                register(element,
                         shape);
            }
        }
    }

    protected void handleElementSelection(final Element element,
                                          final boolean selected,
                                          final boolean clearSelection) {
        if (clearSelection) {
            clearSelection();
        }
        if (selected) {
            log(Level.FINE,
                "Deselect [element=" + element.getUUID() + "]");
            deselect(element);
        } else {
            log(Level.FINE,
                "Select [element=" + element.getUUID() + "]");
            select(element);
        }
    }

    /**
     * When clicking on the layer or on the canvas root element, it's not
     * being added into the selected list but it fires the selection event
     * so other components can process or present their stuff at this point.
     */
    protected void handleLayerClick(final boolean clearSelection) {
        if (clearSelection) {
            clearSelection();
        }
        final String canvasRootUUID = getRootUUID();
        if (null != canvasRootUUID) {
            elementSelectedEventEvent.fire(new CanvasElementSelectedEvent(canvasHandler,
                                                                          canvasRootUUID));
        } else {
            clearSelectionEventEvent.fire(new CanvasClearSelectionEvent(canvasHandler));
        }
    }

    @Override
    protected void doDisable() {
        super.doDisable();
        if (null != layerClickHandler
                && null != getCanvas()
                && null != getCanvas().getLayer()) {
            getCanvas().getLayer().removeHandler(layerClickHandler);
            this.layerClickHandler = null;
        }
    }

    @Override
    public void deregisterAll() {
        super.deregisterAll();
        selectedElements.clear();
    }

    @Override
    public void deregister(final String uuid) {
        super.deregister(uuid);
        selectedElements.remove(uuid);
    }

    @SuppressWarnings("unchecked")
    protected void updateViewShapesState() {
        if (null != getCanvas()) {
            final List<Shape> shapes = getCanvas().getShapes();
            for (final Shape shape : shapes) {
                final boolean isSelected = !selectedElements.isEmpty() && selectedElements.contains(shape.getUUID());
                if (isSelected) {
                    selectShape(shape);
                } else {
                    deselectShape(shape);
                }
            }
            // Batch a show operation.
            getCanvas().draw();
        }
    }

    protected void selectShape(final Shape shape) {
        shape.applyState(ShapeState.SELECTED);
        getCanvas().draw();
    }

    protected void deselectShape(final Shape shape) {
        shape.applyState(ShapeState.NONE);
        getCanvas().draw();
    }
    
    /*
        **************************************************************
        *               SELECTION CONTROL METHODS
        ***************************************************************
     */

    public SelectionControl<H, Element> select(final String uuid,
                                               final boolean fireEvent) {
        selectedElements.add(uuid);
        updateViewShapesState();
        if (fireEvent) {
            elementSelectedEventEvent.fire(new CanvasElementSelectedEvent(canvasHandler,
                                                                          uuid));
        }
        return this;
    }

    @Override
    public SelectionControl<H, Element> select(final Element element) {
        return select(element,
                      true);
    }

    public SelectionControl<H, Element> select(final Element element,
                                               final boolean fireEvent) {
        this.select(element.getUUID(),
                    fireEvent);
        return this;
    }

    public SelectionControl<H, Element> deselect(final String uuid,
                                                 final boolean fireEvent) {
        // GWT.log("***** DESELECTING " + uuid );
        selectedElements.remove(uuid);
        updateViewShapesState();
        if (fireEvent) {
            fireCanvasClear();
        }
        return this;
    }

    @Override
    public SelectionControl<H, Element> deselect(final Element element) {
        return deselect(element,
                        true);
    }

    public SelectionControl<H, Element> deselect(final Element element,
                                                 final boolean fireEvent) {
        return this.deselect(element.getUUID(),
                             fireEvent);
    }

    protected boolean isSelected(final String uuid) {
        return uuid != null && selectedElements.contains(uuid);
    }

    @Override
    public boolean isSelected(final Element element) {
        return null != element && isSelected(element.getUUID());
    }

    @Override
    public Collection<String> getSelectedItems() {
        return Collections.unmodifiableCollection(selectedElements);
    }

    @Override
    public SelectionControl<H, Element> clearSelection() {
        return clearSelection(true);
    }

    public SelectionControl<H, Element> clearSelection(final boolean fireEvent) {
        // De-select all currently selected shapes.
        for (final String uuid : selectedElements) {
            final Shape<?> shape = canvasHandler.getCanvas().getShape(uuid);
            if (null != shape) {
                deselectShape(shape);
            }
        }
        selectedElements.clear();
        if (null != getCanvas()) {
            // Force batch re-show.
            getCanvas().draw();
        }
        if (fireEvent) {
            fireCanvasClear();
        }
        return this;
    }

    void onShapeRemovedEvent(final @Observes CanvasShapeRemovedEvent shapeRemovedEvent) {
        checkNotNull("shapeRemovedEvent",
                     shapeRemovedEvent);
        if (null != getCanvas() && getCanvas().equals(shapeRemovedEvent.getCanvas())) {
            final Shape<?> shape = shapeRemovedEvent.getShape();
            if (selectedElements.contains(shape.getUUID())) {
                this.deselect(shape.getUUID(),
                              false);
            }
        }
    }

    void onCanvasElementSelectedEvent(final @Observes CanvasElementSelectedEvent event) {
        checkNotNull("event",
                     event);
        final String uuid = event.getElementUUID();
        if (null != canvasHandler && canvasHandler.equals(event.getCanvasHandler())) {
            doSelect(uuid);
        }
    }

    private void doSelect(final String uuid) {
        if (!isSelected(uuid) && !uuid.equals(getRootUUID())) {
            this.clearSelection(false);
            this.select(uuid,
                        false);
        }
    }

    void CanvasClearSelectionEvent(final @Observes CanvasClearSelectionEvent event) {
        checkNotNull("event",
                     event);
        if (null != canvasHandler && canvasHandler.equals(event.getCanvasHandler())) {
            this.clearSelection(false);
        }
    }

    protected void fireCanvasClear() {
        clearSelectionEventEvent.fire(new CanvasClearSelectionEvent(canvasHandler));
    }

    protected Canvas getCanvas() {
        return null != canvasHandler ? canvasHandler.getCanvas() : null;
    }

    private String getRootUUID() {
        return canvasHandler.getDiagram().getMetadata().getCanvasRootUUID();
    }

    private void log(final Level level,
                     final String message) {
        if (LogConfiguration.loggingIsEnabled()) {
            LOGGER.log(level,
                       message);
        }
    }
}
