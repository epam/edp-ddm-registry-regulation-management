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

import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.util.List;
import java.util.stream.Collectors;
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

  @Scheduled(cron = "${scheduled.repositoryRefreshCron}", zone = "${scheduled.repositoryRefreshTimezone}")
  public void refresh() {
    log.debug("Refreshing repositories started");
    List<ChangeInfoDto> mrList = gerritService.getMRList().stream()
        .filter(mr -> !mr.getMergeable()).collect(Collectors.toList());

    for (ChangeInfoDto changeInfo : mrList) {
      try {
        String changeId = changeInfo.getChangeId();
        log.debug("Refreshing repository {}", changeInfo.getNumber());
        gerritService.rebase(changeId);
        ChangeInfoDto changeInfoDto = gerritService.getChangeInfo(changeId);
        jGitService.fetch(changeInfoDto.getNumber(), changeInfoDto.getRefs());
      } catch (Exception e) {
        log.error("Error during repository refresh", e);
      }
    }

    log.debug("Refreshing head branch repository");
    try {
      jGitService.resetHeadBranchToRemote();
    } catch (Exception e) {
      log.error("Error during head branch repository refresh", e);
    }

    log.debug("Refreshing repositories finished");
  }
}
