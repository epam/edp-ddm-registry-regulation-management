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

package com.epam.digital.data.platform.management.gitintegration.event.listener;

import com.epam.digital.data.platform.management.core.config.AsyncConfig;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.config.RetryConfig;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DisplayName("ApplicationStartedEventGitListener#handleApplicationStartedEvent")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    AsyncConfig.class,
    RetryConfig.class,
    ApplicationStartedEventGitListener.class
})
class ApplicationStartedEventGitListenerTest {

  private static final String HEAD_BRANCH = "master";

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  @MockBean
  JGitService gitService;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;

  private ApplicationStartedEvent event;

  @BeforeAll
  static void setClass() {
    System.setProperty("registry-regulation-management.retry.head-branch-cloning-delay", "1");
  }

  @BeforeEach
  void setUp() {
    Mockito.doReturn(HEAD_BRANCH)
        .when(gerritPropertiesConfig).getHeadBranch();

    var springApplication = Mockito.mock(SpringApplication.class);
    event = Mockito.mock(ApplicationStartedEvent.class);
    Mockito.doReturn(springApplication).when(event).getSpringApplication();
    Mockito.doReturn(getClass()).when(springApplication).getMainApplicationClass();
  }

  @Test
  @DisplayName("should call cloning repository in async")
  @SneakyThrows
  void handleApplicationStartedEvent() {
    Mockito.doNothing().when(gitService).cloneRepoIfNotExist(HEAD_BRANCH);

    applicationEventPublisher.publishEvent(event);

    Mockito.verify(gitService, Mockito.timeout(1000)).cloneRepoIfNotExist(HEAD_BRANCH);
  }

  @Test
  @DisplayName("should retry cloning repository if failed in any case")
  @SneakyThrows
  void handleVersionCandidateCreatedEvent_exceptionThrow() {
    Mockito
        .doThrow(GitCommandException.class)
        .doThrow(IllegalStateException.class)
        .doNothing()
        .when(gitService).cloneRepoIfNotExist(HEAD_BRANCH);

    applicationEventPublisher.publishEvent(event);

    Mockito.verify(gitService, Mockito.timeout(1000).times(3)).cloneRepoIfNotExist(HEAD_BRANCH);
  }
}

