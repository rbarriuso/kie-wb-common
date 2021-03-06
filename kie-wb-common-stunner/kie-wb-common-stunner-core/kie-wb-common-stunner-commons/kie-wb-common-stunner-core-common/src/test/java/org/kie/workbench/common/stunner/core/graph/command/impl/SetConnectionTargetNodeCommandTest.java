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

package org.kie.workbench.common.stunner.core.graph.command.impl;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnector;
import org.kie.workbench.common.stunner.core.rule.RuleEvaluationContext;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.rule.context.CardinalityContext;
import org.kie.workbench.common.stunner.core.rule.context.ConnectorCardinalityContext;
import org.kie.workbench.common.stunner.core.rule.context.EdgeCardinalityContext;
import org.kie.workbench.common.stunner.core.rule.context.GraphConnectionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.kie.workbench.common.stunner.core.TestingGraphUtils.verifyConnection;
import static org.kie.workbench.common.stunner.core.TestingGraphUtils.verifyConnectorCardinality;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SetConnectionTargetNodeCommandTest extends AbstractGraphCommandTest {

    private static final String NODE_UUID = "nodeUUID";
    private static final String LAST_TARGET_NODE_UUID = "lastTargetNodeUUID";
    private static final String SOURCET_UUID = "nodeSourceUUID";
    private static final String EDGE_UUID = "edgeUUID";
    private static final Integer MAGNET_INDEX = 1;

    private Node node;
    private Node lastTargetNode;
    private Node source;
    private Edge edge;
    private ViewConnector edgeContent;
    private SetConnectionTargetNodeCommand tested;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        super.init(500,
                   500);
        node = mockNode(NODE_UUID);
        lastTargetNode = mockNode(LAST_TARGET_NODE_UUID);
        source = mockNode(SOURCET_UUID);
        edge = mockEdge(EDGE_UUID);
        edgeContent = mock(ViewConnector.class);
        graphNodes.add(node);
        when(graphIndex.getNode(eq(NODE_UUID))).thenReturn(node);
        when(graphIndex.getNode(eq(LAST_TARGET_NODE_UUID))).thenReturn(lastTargetNode);
        when(graphIndex.getNode(eq(SOURCET_UUID))).thenReturn(source);
        when(graphIndex.getEdge(eq(EDGE_UUID))).thenReturn(edge);
        when(edge.getContent()).thenReturn(edgeContent);
        when(edge.getSourceNode()).thenReturn(source);
        when(edge.getTargetNode()).thenReturn(lastTargetNode);
        this.tested = new SetConnectionTargetNodeCommand(node,
                                                         edge,
                                                         MAGNET_INDEX);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllow() {
        CommandResult<RuleViolation> result = tested.allow(graphCommandExecutionContext);
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
        final ArgumentCaptor<RuleEvaluationContext> contextCaptor = ArgumentCaptor.forClass(RuleEvaluationContext.class);
        verify(ruleManager,
               times(3)).evaluate(eq(ruleSet),
                                  contextCaptor.capture());
        final List<RuleEvaluationContext> contexts = contextCaptor.getAllValues();
        assertEquals(3,
                     contexts.size());
        verifyConnection((GraphConnectionContext) contexts.get(0),
                         edge,
                         source,
                         node);

        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(1),
                                   graph,
                                   lastTargetNode,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.DELETE));
        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(2),
                                   graph,
                                   node,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.ADD));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllowNoRules() {
        when(graphCommandExecutionContext.getRuleManager()).thenReturn(null);
        CommandResult<RuleViolation> result = tested.allow(graphCommandExecutionContext);
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
        verify(ruleManager,
               times(0)).evaluate(eq(ruleSet),
                                  any(RuleEvaluationContext.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllowNoTargetConnection() {
        this.tested = new SetConnectionTargetNodeCommand(null,
                                                         edge,
                                                         MAGNET_INDEX);
        CommandResult<RuleViolation> result = tested.allow(graphCommandExecutionContext);
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
        final ArgumentCaptor<RuleEvaluationContext> contextCaptor = ArgumentCaptor.forClass(RuleEvaluationContext.class);
        verify(ruleManager,
               times(2)).evaluate(eq(ruleSet),
                                  contextCaptor.capture());
        final List<RuleEvaluationContext> contexts = contextCaptor.getAllValues();
        assertEquals(2,
                     contexts.size());
        verifyConnection((GraphConnectionContext) contexts.get(0),
                         edge,
                         null,
                         null);

        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(1),
                                   graph,
                                   lastTargetNode,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.DELETE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute() {
        final List lastTargetInEdges = mock(List.class);
        final List sourceOutEdges = mock(List.class);
        final List targetInEdges = mock(List.class);
        when(source.getOutEdges()).thenReturn(sourceOutEdges);
        when(lastTargetNode.getInEdges()).thenReturn(lastTargetInEdges);
        when(node.getInEdges()).thenReturn(targetInEdges);
        CommandResult<RuleViolation> result = tested.execute(graphCommandExecutionContext);
        final ArgumentCaptor<RuleEvaluationContext> contextCaptor = ArgumentCaptor.forClass(RuleEvaluationContext.class);
        verify(ruleManager,
               times(3)).evaluate(eq(ruleSet),
                                  contextCaptor.capture());
        final List<RuleEvaluationContext> contexts = contextCaptor.getAllValues();
        assertEquals(3,
                     contexts.size());
        verifyConnection((GraphConnectionContext) contexts.get(0),
                         edge,
                         source,
                         node);

        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(1),
                                   graph,
                                   lastTargetNode,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.DELETE));
        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(2),
                                   graph,
                                   node,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.ADD));
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
        verify(lastTargetInEdges,
               times(1)).remove(eq(edge));
        verify(targetInEdges,
               times(1)).add(eq(edge));
        verify(edgeContent,
               times(1)).setTargetMagnetIndex(eq(MAGNET_INDEX));
        verify(edge,
               times(1)).setTargetNode(eq(node));
        verify(targetInEdges,
               times(0)).remove(any(Edge.class));
        verify(sourceOutEdges,
               times(0)).add(any(Edge.class));
        verify(graphIndex,
               times(0)).removeEdge(any(Edge.class));
        verify(graphIndex,
               times(0)).addEdge(any(Edge.class));
        verify(graphIndex,
               times(0)).addNode(any(Node.class));
        verify(graphIndex,
               times(0)).removeNode(any(Node.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteNoMagnets() {
        this.tested = new SetConnectionTargetNodeCommand(node,
                                                         edge);
        final List lastTargetInEdges = mock(List.class);
        final List sourceOutEdges = mock(List.class);
        final List targetInEdges = mock(List.class);
        when(source.getOutEdges()).thenReturn(sourceOutEdges);
        when(lastTargetNode.getInEdges()).thenReturn(lastTargetInEdges);
        when(node.getInEdges()).thenReturn(targetInEdges);
        CommandResult<RuleViolation> result = tested.execute(graphCommandExecutionContext);
        final ArgumentCaptor<RuleEvaluationContext> contextCaptor = ArgumentCaptor.forClass(RuleEvaluationContext.class);
        verify(ruleManager,
               times(3)).evaluate(eq(ruleSet),
                                  contextCaptor.capture());
        final List<RuleEvaluationContext> contexts = contextCaptor.getAllValues();
        assertEquals(3,
                     contexts.size());
        verifyConnection((GraphConnectionContext) contexts.get(0),
                         edge,
                         source,
                         node);

        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(1),
                                   graph,
                                   lastTargetNode,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.DELETE));
        verifyConnectorCardinality((ConnectorCardinalityContext) contexts.get(2),
                                   graph,
                                   node,
                                   edge,
                                   EdgeCardinalityContext.Direction.INCOMING,
                                   Optional.of(CardinalityContext.Operation.ADD));
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
        verify(lastTargetInEdges,
               times(1)).remove(eq(edge));
        verify(targetInEdges,
               times(1)).add(eq(edge));
        verify(edgeContent,
               times(0)).setTargetMagnetIndex(eq(MAGNET_INDEX));
        verify(edge,
               times(1)).setTargetNode(eq(node));
        verify(targetInEdges,
               times(0)).remove(any(Edge.class));
        verify(sourceOutEdges,
               times(0)).add(any(Edge.class));
        verify(graphIndex,
               times(0)).removeEdge(any(Edge.class));
        verify(graphIndex,
               times(0)).addEdge(any(Edge.class));
        verify(graphIndex,
               times(0)).addNode(any(Node.class));
        verify(graphIndex,
               times(0)).removeNode(any(Node.class));
    }
}
