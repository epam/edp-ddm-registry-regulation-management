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

import java.time.LocalDateTime;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryRefreshScheduler {

  private static final String CONFLICTS_CACHE_NAME = "conflicts";
  private static final String LATEST_REBASE_CACHE_NAME = "latestRebase";
  private final GerritService gerritService;
  private final JGitService jGitService;
  private final CacheManager cacheManager;

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
        var versionId = change.getNumber();
        log.debug("Refreshing repository {}", change.getNumber());
        gerritService.rebase(changeId);
        jGitService.cloneRepoIfNotExist(versionId);
        jGitService.fetch(versionId, change.getRefs());

        Cache conflictCache = cacheManager.getCache(CONFLICTS_CACHE_NAME);
        conflictCache.evictIfPresent(versionId);
        conflictCache.put(versionId, jGitService.getConflicts(versionId));

        Cache rebaseCache = cacheManager.getCache(LATEST_REBASE_CACHE_NAME);
        rebaseCache.evictIfPresent(versionId);
        rebaseCache.put(versionId, LocalDateTime.now());
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
