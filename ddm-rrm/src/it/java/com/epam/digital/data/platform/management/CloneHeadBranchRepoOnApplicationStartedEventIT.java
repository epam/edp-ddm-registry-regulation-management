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

package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@DisplayName("Clone head branch repo on application start event IT")
class CloneHeadBranchRepoOnApplicationStartedEventIT extends BaseIT {

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;
  @Autowired
  private GerritPropertiesConfig gerritPropertiesConfig;

  private ApplicationStartedEvent event;

  @BeforeEach
  @Override
  @SneakyThrows
  void setUp() {
    super.setUp();
    var springApplication = Mockito.mock(SpringApplication.class);
    event = Mockito.mock(ApplicationStartedEvent.class);
    Mockito.doReturn(springApplication)
        .when(event).getSpringApplication();
    Mockito.doReturn(RegistryRegulationManagementApplication.class)
        .when(springApplication).getMainApplicationClass();
  }

  @Test
  @DisplayName("should clone repository if remote repo exists")
  @SneakyThrows
  void testCloneHeadBranchRepo() {
    // Clearing head repo
    FileUtils.forceDelete(context.getHeadRepo());
    Assertions.assertThat(context.getHeadRepo()).doesNotExist();

    // publish ApplicationStartedEvent
    applicationEventPublisher.publishEvent(event);

    // Verify that head branch repo was successfully cloned
    Assertions.assertThat(context.getHeadRepo()).exists();
  }

  @Test
  @DisplayName("should rethrow exception if remote repo doesn't exist")
  @SneakyThrows
  void testCloneHeadBranchRepo_exceptionOccurred() {
    // Clearing whole test directory and remote repo
    FileUtils.forceDelete(context.getRemoteHeadRepo());
    FileUtils.forceDelete(context.getTestDirectory());
    Assertions.assertThat(context.getRemoteHeadRepo()).doesNotExist();
    Assertions.assertThat(context.getHeadRepo()).doesNotExist();

    // publish ApplicationStartedEvent
    Assertions.assertThatThrownBy(() -> applicationEventPublisher.publishEvent(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Remote that is configured under \"gerrit\" prefix is invalid: "
            + "Invalid remote: origin");

    // Verify that without remote repo there wasn't cloned head branch repo
    Assertions.assertThat(context.getHeadRepo()).doesNotExist();
  }
}
