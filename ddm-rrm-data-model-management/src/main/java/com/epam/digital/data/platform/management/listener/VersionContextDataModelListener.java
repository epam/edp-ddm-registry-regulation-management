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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContext;
import com.epam.digital.data.platform.management.core.event.ApplicationStartedEventListener;
import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEvent;
import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEventListener;
import com.epam.digital.data.platform.management.datasource.PublicDataSource;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Listener that tries to create data sources for version candidate
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VersionContextDataModelListener implements VersionCandidateCreatedEventListener,
    ApplicationStartedEventListener {

  private final VersionContext versionContext;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Override
  @Retryable(maxAttempts = Integer.MAX_VALUE,
      include = Exception.class,
      backoff = @Backoff(delayExpression = "${registry-regulation-management.retry.data-model-context-creating-delay:300000}"))
  public void handleVersionCandidateCreatedEvent(VersionCandidateCreatedEvent event) {
    var versionCandidateId = event.getVersionCandidateNumber();
    createBeans(versionCandidateId);
  }

  @Override
  @Retryable(maxAttempts = Integer.MAX_VALUE,
      include = Exception.class,
      backoff = @Backoff(delayExpression = "${registry-regulation-management.retry.data-model-context-creating-delay:300000}"))
  public void handleApplicationStartedEvent(ApplicationStartedEvent event) {
    var versionId = gerritPropertiesConfig.getHeadBranch();
    createBeans(versionId);
  }

  private void createBeans(String versionId) {
    try {
      versionContext.getBean(versionId, PublicDataSource.class);
      versionContext.getBean(versionId, RegistryDataSource.class);
    } catch (Exception e) {
      log.warn("Exception occurred during creating data sources for version {}: {}",
          versionId, e.getMessage(), e);
      throw e;
    }
  }
}
