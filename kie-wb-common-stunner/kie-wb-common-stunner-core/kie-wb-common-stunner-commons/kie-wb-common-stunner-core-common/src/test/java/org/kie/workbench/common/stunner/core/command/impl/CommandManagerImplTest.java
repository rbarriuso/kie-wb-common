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

package org.kie.workbench.common.stunner.core.command.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandListener;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.omg.CORBA.Object;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommandManagerImplTest {

    @Mock
    private Object context;

    @Mock
    private Command<Object, Object> command;

    @Mock
    private CommandResult<Object> commandResult;

    private CommandManagerImpl<Object, Object> tested;

    @Before
    public void setup() throws Exception {
        tested = new CommandManagerImpl<Object, Object>();
    }

    @Test
    public void testAllow() {
        when(command.allow(context)).thenReturn(commandResult);
        CommandResult<Object> result = tested.allow(context,
                                                    command);
        verify(command,
               times(1)).allow(eq(context));
        verify(command,
               times(0)).execute(anyObject());
        verify(command,
               times(0)).undo(anyObject());
        assertNotNull(result);
        assertEquals(commandResult,
                     result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllowWithListener() {
        CommandListener<Object, Object> listener = mock(CommandListener.class);
        tested.setCommandListener(listener);
        testAllow();
        verify(listener,
               times(1)).onAllow(eq(context),
                                 eq(command),
                                 eq(commandResult));
        verify(listener,
               times(0)).onExecute(anyObject(),
                                   anyObject(),
                                   anyObject());
        verify(listener,
               times(0)).onUndo(anyObject(),
                                anyObject(),
                                anyObject());
    }

    @Test
    public void testExecute() {
        when(command.execute(context)).thenReturn(commandResult);
        CommandResult<Object> result = tested.execute(context,
                                                      command);
        verify(command,
               times(1)).execute(eq(context));
        verify(command,
               times(0)).allow(anyObject());
        verify(command,
               times(0)).undo(anyObject());
        assertNotNull(result);
        assertEquals(commandResult,
                     result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteWithListener() {
        CommandListener<Object, Object> listener = mock(CommandListener.class);
        tested.setCommandListener(listener);
        testExecute();
        verify(listener,
               times(0)).onAllow(anyObject(),
                                 anyObject(),
                                 anyObject());
        verify(listener,
               times(1)).onExecute(eq(context),
                                   eq(command),
                                   eq(commandResult));
        verify(listener,
               times(0)).onUndo(anyObject(),
                                anyObject(),
                                anyObject());
    }

    @Test
    public void testUndo() {
        when(command.undo(context)).thenReturn(commandResult);
        CommandResult<Object> result = tested.undo(context,
                                                   command);
        verify(command,
               times(1)).undo(eq(context));
        verify(command,
               times(0)).execute(anyObject());
        verify(command,
               times(0)).allow(anyObject());
        assertNotNull(result);
        assertEquals(commandResult,
                     result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUndoWithListener() {
        CommandListener<Object, Object> listener = mock(CommandListener.class);
        tested.setCommandListener(listener);
        testUndo();
        verify(listener,
               times(0)).onAllow(anyObject(),
                                 anyObject(),
                                 anyObject());
        verify(listener,
               times(0)).onExecute(anyObject(),
                                   anyObject(),
                                   anyObject());
        verify(listener,
               times(1)).onUndo(eq(context),
                                eq(command),
                                eq(commandResult));
    }
}
