/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.listener;

import com.epam.digital.data.platform.management.core.config.AsyncConfig;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.config.RetryConfig;
import com.epam.digital.data.platform.management.core.context.VersionContext;
import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEvent;
import com.epam.digital.data.platform.management.datasource.PublicDataSource;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DisplayName("VersionContextDataModelListener test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    AsyncConfig.class,
    RetryConfig.class,
    VersionContextDataModelListener.class
})
@TestPropertySource(properties = "registry-regulation-management.retry.data-model-context-creating-delay = 1")
class VersionContextDataModelListenerTest {

  private static final String HEAD_BRANCH = "master";
  private static final String VERSION_CANDIDATE = "42";

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  @MockBean
  VersionContext versionContext;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;

  private ApplicationStartedEvent applicationStartedEvent;
  private VersionCandidateCreatedEvent versionCandidateCreatedEvent;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(HEAD_BRANCH)
        .when(gerritPropertiesConfig).getHeadBranch();

    var springApplication = Mockito.mock(SpringApplication.class);
    applicationStartedEvent = Mockito.mock(ApplicationStartedEvent.class);
    Mockito.doReturn(springApplication).when(applicationStartedEvent).getSpringApplication();
    Mockito.doReturn(getClass()).when(springApplication).getMainApplicationClass();

    versionCandidateCreatedEvent = new VersionCandidateCreatedEvent("any", VERSION_CANDIDATE);
  }

  @Test
  @DisplayName("should init creating registry and public data sources on application start")
  @SneakyThrows
  void handleApplicationStartedEvent() {
    Mockito.doReturn(null).when(versionContext).getBean(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.doReturn(null).when(versionContext).getBean(HEAD_BRANCH, PublicDataSource.class);

    applicationEventPublisher.publishEvent(applicationStartedEvent);

    Mockito.verify(versionContext, Mockito.timeout(1000))
        .getBean(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.verify(versionContext, Mockito.timeout(1000))
        .getBean(HEAD_BRANCH, PublicDataSource.class);
  }

  @Test
  @DisplayName("should retry creating data sources on application starting if failed in any case")
  @SneakyThrows
  void handleApplicationStartedEvent_exceptionThrow() {
    Mockito
        .doThrow(RuntimeException.class)
        .doReturn(null)
        .when(versionContext).getBean(HEAD_BRANCH, RegistryDataSource.class);

    applicationEventPublisher.publishEvent(applicationStartedEvent);

    Mockito.verify(versionContext, Mockito.timeout(1000).times(2))
        .getBean(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.verify(versionContext, Mockito.timeout(1000).times(2))
        .getBean(HEAD_BRANCH, PublicDataSource.class);
  }

  @Test
  @DisplayName("should init creating registry and public data sources on creating version-candidate")
  @SneakyThrows
  void handleVersionCandidateCreatedEvent() {
    Mockito.doReturn(null).when(versionContext)
        .getBean(VERSION_CANDIDATE, RegistryDataSource.class);
    Mockito.doReturn(null).when(versionContext).getBean(VERSION_CANDIDATE, PublicDataSource.class);

    applicationEventPublisher.publishEvent(versionCandidateCreatedEvent);

    Mockito.verify(versionContext, Mockito.timeout(1000))
        .getBean(VERSION_CANDIDATE, RegistryDataSource.class);
    Mockito.verify(versionContext, Mockito.timeout(1000))
        .getBean(VERSION_CANDIDATE, PublicDataSource.class);
  }

  @Test
  @DisplayName("should retry creating data sources on creating version-candidate if failed in any case")
  @SneakyThrows
  void handleVersionCandidateCreatedEvent_exceptionThrow() {
    Mockito
        .doThrow(RuntimeException.class)
        .doReturn(null)
        .when(versionContext).getBean(VERSION_CANDIDATE, RegistryDataSource.class);

    applicationEventPublisher.publishEvent(versionCandidateCreatedEvent);

    Mockito.verify(versionContext, Mockito.timeout(1000).times(2))
        .getBean(VERSION_CANDIDATE, RegistryDataSource.class);
    Mockito.verify(versionContext, Mockito.timeout(1000).times(2))
        .getBean(VERSION_CANDIDATE, PublicDataSource.class);
  }
}

