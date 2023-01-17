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


import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.event.ApplicationStartedEventListener;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Async listener of the {@link ApplicationStartedEvent}.
 * <p>
 * Clones the repository for head-branch version right after application is started
 * <p>
 * If the cloning has failed, then tries cloning it again with
 * {@link Integer#MAX_VALUE maximum number} of attempts and configured delay -
 * {@code retry.head-branch-cloning-delay}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartedEventGitListener implements ApplicationStartedEventListener {

  private final JGitService gitService;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Override
  @Retryable(maxAttempts = Integer.MAX_VALUE,
      include = Exception.class,
      backoff = @Backoff(delayExpression = "${registry-regulation-management.retry.head-branch-cloning-delay:300000}"))
  public void handleApplicationStartedEvent(ApplicationStartedEvent event) {
    var headBranchRepo = gerritPropertiesConfig.getHeadBranch();
    log.debug("Cloning repository for head-branch {}", headBranchRepo);
    try {
      gitService.cloneRepoIfNotExist(headBranchRepo);
    } catch (Exception e) {
      log.warn("Head-branch {} cloning handling have been failed: {}", headBranchRepo,
          e.getMessage(), e);
      throw e;
    }
  }
}
