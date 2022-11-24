/*
 * Copyright 2022 EPAM Systems.
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


import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEvent;
import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEventListener;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Async listener of the {@link VersionCandidateCreatedEvent}.
 * <p>
 * It clones the repository for the version candidate right after its creation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VersionCandidateCreatedEventGitListener implements
    VersionCandidateCreatedEventListener {

  private final JGitService gitService;

  @Override
  public void handleVersionCandidateCreatedEvent(VersionCandidateCreatedEvent event) {
    var versionCandidateNumber = event.getVersionCandidateNumber();
    log.debug("Handling version candidate {} created event", versionCandidateNumber);
    try {
      gitService.cloneRepoIfNotExist(versionCandidateNumber);
    } catch (Throwable e) {
      log.error("Version candidate {} creation handling have been failed...",
          versionCandidateNumber, e);
    }
  }
}
