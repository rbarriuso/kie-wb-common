/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.forms.jbpm.server.service.formGeneration.impl.runtime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.forms.jbpm.server.service.formGeneration.model.Client;
import org.kie.workbench.common.forms.jbpm.server.service.formGeneration.model.Expense;
import org.kie.workbench.common.forms.jbpm.server.service.formGeneration.model.Line;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NestedFormsBPMNRuntimeFormDefinitionGeneratorServiceTest extends BPMNRuntimeFormDefinitionGeneratorServiceTest {

    @Override
    public void setup() {
        super.setup();
    }

    @Test
    public void testCreateNewProcessFormNestedFormsFromGeneralClassLoader() {
        launchNestedFormsTest();
    }

    @Test
    public void testCreateNewProcessFormNestedFormsFromProjectClassLoader() throws ClassNotFoundException {
        when(source.loadClass(anyString())).then(invocationOnMock -> loadClass(invocationOnMock.getArguments()[0].toString()));

        launchNestedFormsTest();
    }

    protected Class loadClass(String className) {
        if (Expense.class.equals(className)) {
            return Expense.class;
        } else if (Client.class.equals(className)) {
            return Client.class;
        } else if (Line.class.equals(className)) {
            return Line.class;
        }
        return null;
    }
}
