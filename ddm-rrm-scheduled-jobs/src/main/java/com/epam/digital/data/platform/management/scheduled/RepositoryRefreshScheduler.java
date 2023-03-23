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

package com.epam.digital.data.platform.management.scheduled;

import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryRefreshScheduler {

  private final GerritService gerritService;
  private final JGitService jGitService;

  @Scheduled(cron = "${registry-regulation-management.scheduled.version-candidate-repo-refresh.cron}",
      zone = "${registry-regulation-management.scheduled.version-candidate-repo-refresh.timezone}")
  public void refreshVersionCandidates() {
    log.debug("Refreshing version-candidates' repositories started");
    var mrList = gerritService.getMRList();

    for (var changeInfo : mrList) {
      try {
        var change = gerritService.getMRByNumber(changeInfo.getNumber());
        if (change.getMergeable()) {
          continue;
        }
        var changeId = change.getChangeId();
        log.debug("Refreshing repository {}", change.getNumber());
        gerritService.rebase(changeId);
        jGitService.fetch(change.getNumber(), change.getRefs());
      } catch (Exception e) {
        log.warn("Error during repository refresh: {}", e.getMessage(), e);
      }
    }

    log.debug("Refreshing version-candidates' repositories finished");
  }

  @Scheduled(cron = "${registry-regulation-management.scheduled.master-repo-refresh.cron}",
      zone = "${registry-regulation-management.scheduled.master-repo-refresh.timezone}")
  public void refreshMasterVersion() {
    log.debug("Refreshing head branch repository");
    try {
      jGitService.resetHeadBranchToRemote();
    } catch (Exception e) {
      log.warn("Head branch repository refresh failed: {}", e.getMessage(), e);
    }

    log.debug("Refreshing head branch repository finished");
  }
}
