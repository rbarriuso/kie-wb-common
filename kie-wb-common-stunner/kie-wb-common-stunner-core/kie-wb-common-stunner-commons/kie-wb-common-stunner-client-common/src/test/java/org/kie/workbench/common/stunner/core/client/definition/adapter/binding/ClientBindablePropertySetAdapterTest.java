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

package org.kie.workbench.common.stunner.core.client.definition.adapter.binding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClientBindablePropertySetAdapterTest extends AbstractClientBindableAdapterTest {

    private ClientBindablePropertySetAdapter clientBindablePropertySetAdapter;

    @Before
    @Override
    public void init() {
        super.init();

        clientBindablePropertySetAdapter = new ClientBindablePropertySetAdapter(translationService);
    }

    @Test
    public void testFunctionallity() {
        String description = clientBindablePropertySetAdapter.getName(model);

        verify(translationService).getPropertySetName(model.getClass().getName());

        assertEquals(PROPERTY_SET_NAME,
                     description);
    }
}
