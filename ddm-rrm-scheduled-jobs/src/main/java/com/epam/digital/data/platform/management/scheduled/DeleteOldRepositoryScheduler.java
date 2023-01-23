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

package com.epam.digital.data.platform.management.scheduled;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContext;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteOldRepositoryScheduler {

  private final VersionContext versionContext;
  private final GerritService gerritService;
  private final JGitService jGitService;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Scheduled(cron = "${scheduled.cleanRepositoriesCron}", zone = "${scheduled.cleanRepositoriesTimezone}")
  public void deleteOldRepositories() {
    try {
      var openedMrs = gerritService.getMRList();
      var repositoriesDirectory = gerritPropertiesConfig.getRepositoryDirectory();

      try (var directories = Files.list(Path.of(repositoriesDirectory))) {
        directories
            .filter(path -> !path.endsWith(gerritPropertiesConfig.getHeadBranch()))
            .filter(path -> openedMrs.stream()
                .map(ChangeInfoDto::getNumber)
                .noneMatch(path::endsWith))
            .map(path -> path.getFileName().toString())
            .forEach(repo -> {
              versionContext.destroyContext(repo);
              jGitService.deleteRepo(repo);
            });
      }
    } catch (Exception e) {
      log.warn("Error during deleting obsolete repositories: {}", e.getMessage());
    }
  }
}
