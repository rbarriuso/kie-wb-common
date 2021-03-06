/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.projecteditor.client.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.guvnor.common.services.project.builder.model.BuildMessage;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.builder.service.BuildService;
import org.guvnor.common.services.project.client.repositories.ConflictingRepositoriesPopup;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.common.services.project.model.GAV;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.service.DeploymentMode;
import org.guvnor.common.services.project.service.GAVAlreadyExistsException;
import org.guvnor.structure.repositories.Repository;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.workbench.common.screens.projecteditor.client.editor.DeploymentScreenPopupViewImpl;
import org.kie.workbench.common.screens.projecteditor.client.resources.ProjectEditorResources;
import org.kie.workbench.common.screens.server.management.service.SpecManagementService;
import org.kie.workbench.common.services.shared.project.KieProject;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.uberfire.backend.vfs.Path;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mocks.EventSourceMock;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.events.NotificationEvent;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(GwtMockitoTestRunner.class)
public class BuildExecutorTest {

    @Mock
    private DeploymentScreenPopupViewImpl deploymentScreenPopupView;

    @Mock
    private SpecManagementService specManagementServiceMock;
    private Caller<SpecManagementService> specManagementService;

    @Mock
    private BuildService buildServiceMock;
    private Caller<BuildService> buildService;

    @Mock
    private EventSourceMock<BuildResults> buildResultsEvent;

    @Mock
    private EventSourceMock<NotificationEvent> notificationEvent;

    @Mock
    private ConflictingRepositoriesPopup conflictingRepositoriesPopup;

    @Mock
    private ProjectContext context;

    @Mock
    protected Repository repository;

    @Mock
    protected KieProject project;

    @Mock
    protected Path pomPath;

    @Mock
    private BuildExecutor.View view;

    private BuildExecutor buildExecutor;

    @Before
    public void setup() {
        specManagementService = new CallerMock<>(specManagementServiceMock);
        buildService = spy(new CallerMock<>(buildServiceMock));

        final POM pom = new POM(new GAV("groupId",
                                        "artifactId",
                                        "version"));
        mockBuildService(buildServiceMock);
        mockProjectContext(pom,
                           repository,
                           project,
                           pomPath);

        buildExecutor = new BuildExecutor(deploymentScreenPopupView,
                                          specManagementService,
                                          buildService,
                                          buildResultsEvent,
                                          notificationEvent,
                                          conflictingRepositoriesPopup,
                                          context);
        buildExecutor.init(view);
    }

    @Test
    public void testBuildCommand() {
        buildExecutor.triggerBuild();

        verifyNotification(ProjectEditorResources.CONSTANTS.BuildSuccessful(),
                           NotificationEvent.NotificationType.SUCCESS);
        verifyBusyShowHideAnyString(1,
                                    1,
                                    ProjectEditorResources.CONSTANTS.Building());
    }

    @Test
    public void testBuildCommandFail() {
        BuildMessage message = mock(BuildMessage.class);
        List<BuildMessage> messages = new ArrayList<BuildMessage>();
        messages.add(message);

        BuildResults results = mock(BuildResults.class);
        when(results.getErrorMessages()).thenReturn(messages);

        when(buildServiceMock.build(any(KieProject.class))).thenReturn(results);

        buildExecutor.triggerBuild();

        verifyNotification(ProjectEditorResources.CONSTANTS.BuildFailed(),
                           NotificationEvent.NotificationType.ERROR);
        verifyBusyShowHideAnyString(1,
                                    1,
                                    ProjectEditorResources.CONSTANTS.Building());
    }

    @Test
    public void testBuildAndDeployCommandSingleServerTemplate() {
        final ServerTemplate serverTemplate = new ServerTemplate("id",
                                                                 "name");
        when(specManagementServiceMock.listServerTemplates()).thenReturn(Collections.singletonList(serverTemplate));

        buildExecutor.triggerBuildAndDeploy();

        ArgumentCaptor<ContainerSpec> containerSpecArgumentCaptor = ArgumentCaptor.forClass(ContainerSpec.class);
        verify(specManagementServiceMock).saveContainerSpec(eq(serverTemplate.getId()),
                                                            containerSpecArgumentCaptor.capture());
        final ContainerSpec containerSpec = containerSpecArgumentCaptor.getValue();
        assertEquals(project.getPom().getGav().getArtifactId(),
                     containerSpec.getContainerName());

        verifyNotification(ProjectEditorResources.CONSTANTS.BuildSuccessful(),
                           NotificationEvent.NotificationType.SUCCESS);
        verifyNotification(ProjectEditorResources.CONSTANTS.DeploySuccessful(),
                           NotificationEvent.NotificationType.SUCCESS);
        verify(notificationEvent,
               times(2)).fire(any(NotificationEvent.class));
        verifyBusyShowHideAnyString(1,
                                    1);
    }

    @Test
    public void testBuildAndDeployCommandSingleServerTemplateContainerExists() {
        final String containerId = project.getPom().getGav().getArtifactId() + "_" + project.getPom().getGav().getVersion();
        final String containerName = project.getPom().getGav().getArtifactId();
        final ServerTemplate serverTemplate = new ServerTemplate("id",
                                                                 "name");
        serverTemplate.addContainerSpec(new ContainerSpec(containerId,
                                                          containerName,
                                                          null,
                                                          null,
                                                          null,
                                                          null));
        when(specManagementServiceMock.listServerTemplates()).thenReturn(Collections.singletonList(serverTemplate));

        buildExecutor.triggerBuildAndDeploy();

        verify(deploymentScreenPopupView).setValidateExistingContainerCallback(any(DeploymentScreenPopupViewImpl.ValidateExistingContainerCallback.class));
        verify(deploymentScreenPopupView).setContainerId(containerId);
        verify(deploymentScreenPopupView).setContainerAlias(containerName);
        verify(deploymentScreenPopupView).setStartContainer(true);
        verify(deploymentScreenPopupView).configure(any(com.google.gwt.user.client.Command.class));
        verify(deploymentScreenPopupView).show();
        verifyNoMoreInteractions(deploymentScreenPopupView);
    }

    @Test
    public void testBuildAndDeployCommandMultipleServerTemplate() {
        final String containerId = project.getPom().getGav().getArtifactId() + "_" + project.getPom().getGav().getVersion();
        final String containerName = project.getPom().getGav().getArtifactId();
        final ServerTemplate serverTemplate1 = new ServerTemplate("id1",
                                                                  "name1");
        final ServerTemplate serverTemplate2 = new ServerTemplate("id2",
                                                                  "name2");

        when(specManagementServiceMock.listServerTemplates()).thenReturn(Arrays.asList(serverTemplate1,
                                                                                       serverTemplate2));

        buildExecutor.triggerBuildAndDeploy();

        verify(deploymentScreenPopupView).setValidateExistingContainerCallback(any(DeploymentScreenPopupViewImpl.ValidateExistingContainerCallback.class));
        verify(deploymentScreenPopupView).setContainerId(containerId);
        verify(deploymentScreenPopupView).setContainerAlias(containerName);
        verify(deploymentScreenPopupView).setStartContainer(true);
        verify(deploymentScreenPopupView).addServerTemplates(eq(Sets.newHashSet("id1",
                                                                                "id2")));
        verify(deploymentScreenPopupView).configure(any(com.google.gwt.user.client.Command.class));
        verify(deploymentScreenPopupView).show();
        verifyNoMoreInteractions(deploymentScreenPopupView);
    }

    @Test
    public void testBuildAndDeployCommand() {
        buildExecutor.triggerBuildAndDeploy();

        verifyNotification(ProjectEditorResources.CONSTANTS.BuildSuccessful(),
                           NotificationEvent.NotificationType.SUCCESS);
        verify(notificationEvent,
               times(1)).fire(any(NotificationEvent.class));
        verifyBusyShowHideAnyString(1,
                                    1);
    }

    @Test
    public void testBuildAndDeployCommandFail() {
        BuildMessage message = mock(BuildMessage.class);
        List<BuildMessage> messages = new ArrayList<>();
        messages.add(message);

        BuildResults results = mock(BuildResults.class);
        when(results.getErrorMessages()).thenReturn(messages);

        when(buildServiceMock.buildAndDeploy(any(KieProject.class),
                                             any(DeploymentMode.class))).thenReturn(results);

        buildExecutor.triggerBuildAndDeploy();

        verifyNotification(ProjectEditorResources.CONSTANTS.BuildFailed(),
                           NotificationEvent.NotificationType.ERROR);
        verifyBusyShowHideAnyString(1,
                                    1,
                                    ProjectEditorResources.CONSTANTS.Building());
    }

    @Test
    public void testAlreadyRunningBuild() {
        when(buildService.call(any(RemoteCallback.class),
                               any(ErrorCallback.class))).thenAnswer(invocationOnMock -> {
            // not calling callback causes building is still set to true
            return buildServiceMock;
        });

        buildExecutor.triggerBuild();
        buildExecutor.triggerBuild();

        verify(view,
               times(1)).showABuildIsAlreadyRunning();
        verify(notificationEvent,
               never()).fire(any(NotificationEvent.class));
        verifyBusyShowHideAnyString(1,
                                    0);
    }

    @Test
    public void testAlreadyRunningBuildAndDeploy() {
        when(buildService.call(any(RemoteCallback.class),
                               any(ErrorCallback.class))).thenAnswer(invocationOnMock -> {
            // not calling callback causes building is still set to true
            return buildServiceMock;
        });

        buildExecutor.triggerBuildAndDeploy();
        buildExecutor.triggerBuildAndDeploy();

        verify(view,
               times(1)).showABuildIsAlreadyRunning();
        verify(notificationEvent,
               never()).fire(any(NotificationEvent.class));
        verifyBusyShowHideAnyString(1,
                                    0);
    }

    @Test
    public void testBuildManagedRepository() throws Exception {
        final Map<String, Object> env = new HashMap<String, Object>() {
            {
                put("managed",
                    true);
            }
        };
        when(repository.getEnvironment()).thenReturn(env);

        buildExecutor.triggerBuild();

        verify(buildServiceMock,
               times(1)).build(eq(project));
        verifyBusyShowHideAnyString(1,
                                    1,
                                    ProjectEditorResources.CONSTANTS.Building());
    }

    @Test
    public void testBuildNotManagedRepositoryNonClashingGAV() throws Exception {
        final Map<String, Object> env = new HashMap<String, Object>() {
            {
                put("managed",
                    false);
            }
        };
        when(repository.getEnvironment()).thenReturn(env);

        buildExecutor.triggerBuild();

        verify(buildServiceMock,
               times(1)).build(eq(project));
        verifyBusyShowHideAnyString(1,
                                    1,
                                    ProjectEditorResources.CONSTANTS.Building());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuildNotManagedRepositoryClashingGAV() throws Exception {
        final Map<String, Object> env = new HashMap<String, Object>() {
            {
                put("managed",
                    false);
            }
        };
        when(repository.getEnvironment()).thenReturn(env);

        doThrow(GAVAlreadyExistsException.class).when(buildServiceMock).buildAndDeploy(eq(project),
                                                                                       eq(DeploymentMode.VALIDATED));

        final GAV gav = project.getPom().getGav();
        final ArgumentCaptor<Command> commandArgumentCaptor = ArgumentCaptor.forClass(Command.class);

        buildExecutor.triggerBuildAndDeploy();

        verify(buildServiceMock,
               times(1)).buildAndDeploy(eq(project),
                                        eq(DeploymentMode.VALIDATED));
        verify(conflictingRepositoriesPopup,
               times(1)).setContent(eq(gav),
                                    any(Set.class),
                                    commandArgumentCaptor.capture());
        verify(conflictingRepositoriesPopup,
               times(1)).show();

        assertNotNull(commandArgumentCaptor.getValue());

        //Emulate User electing to force save
        commandArgumentCaptor.getValue().execute();

        verify(conflictingRepositoriesPopup,
               times(1)).hide();

        verify(buildServiceMock,
               times(1)).buildAndDeploy(eq(project),
                                        eq(DeploymentMode.FORCED));
        verify(view,
               times(2)).showBusyIndicator(eq(ProjectEditorResources.CONSTANTS.Building()));
        verify(view,
               times(2)).hideBusyIndicator();
    }

    private void verifyNotification(final String message,
                                    final NotificationEvent.NotificationType type) {
        verify(notificationEvent).fire(argThat(new ArgumentMatcher<NotificationEvent>() {
            @Override
            public boolean matches(final Object argument) {
                final NotificationEvent event = (NotificationEvent) argument;
                final String notification = event.getNotification();
                final NotificationEvent.NotificationType type = event.getType();

                return notification.equals(message) && type.equals(type);
            }
        }));
    }

    private void verifyBusyShowHideAnyString(final int show,
                                             final int hide) {
        verifyBusyShowHideAnyString(show,
                                    hide,
                                    null);
    }

    private void verifyBusyShowHideAnyString(final int show,
                                             final int hide,
                                             final String message) {
        if (message != null) {
            verify(view,
                   times(show)).showBusyIndicator(message);
        } else {
            verify(view,
                   times(show)).showBusyIndicator(anyString());
        }

        verify(view,
               times(hide)).hideBusyIndicator();
    }

    private void mockProjectContext(final POM pom,
                                    final Repository repository,
                                    final KieProject project,
                                    final Path pomPath) {
        when(context.getActiveRepository()).thenReturn(repository);
        when(context.getActiveBranch()).thenReturn("master");
        when(repository.getAlias()).thenReturn("repository");

        when(project.getProjectName()).thenReturn("project");
        when(project.getPomXMLPath()).thenReturn(pomPath);
        when(project.getPom()).thenReturn(pom);
        when(project.getRootPath()).thenReturn(mock(Path.class));
        when(pomPath.getFileName()).thenReturn("pom.xml");
        when(context.getActiveProject()).thenReturn(project);
    }

    private void mockBuildService(final BuildService buildService) {
        when(buildService.build(any(KieProject.class))).thenReturn(new BuildResults());
        when(buildService.buildAndDeploy(any(KieProject.class),
                                         any(DeploymentMode.class))).thenReturn(new BuildResults());
    }
}
