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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteOldRepositoryScheduler {

  private final GerritService gerritService;
  private final JGitService jGitService;

  @Scheduled(cron = "${scheduled.cleanRepositoriesCron}", zone = "${scheduled.cleanRepositoriesTimezone}")
  public void deleteOldRepositories() throws RestApiException {
    List<String> closedMrs = gerritService.getClosedMrIds();

    closedMrs.forEach(mr -> {
      log.info("Try to clear repository with id: " + mr);
      try {
        jGitService.deleteRepo(mr);
      } catch (Exception e) {
        log.error("Error during deleting repository with id: " + mr);
      }
    });
  }

}
