/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.versionmanagement.event.listener;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.event.ApplicationStartedEventListener;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;

@RequiredArgsConstructor
public class ApplicationStartedEventVersionManagementListener implements
    ApplicationStartedEventListener {

  private final VersionManagementService versionManagementService;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final FormService formService;
  private final BusinessProcessService businessProcessService;
  private final JGitService jGitService;

  @Override
  public void handleApplicationStartedEvent(ApplicationStartedEvent event) {
    var versionsCandidatesStream = versionManagementService.getVersionsList().stream()
        .map(VersionInfoShortDto::getNumber).map(String::valueOf);

    var masterVersion = gerritPropertiesConfig.getHeadBranch();
    var versions = Stream.concat(Stream.of(masterVersion), versionsCandidatesStream)
        .collect(Collectors.toList());

    var executor = Executors.newFixedThreadPool(versions.size());

    versions.forEach(version -> executor.submit(() -> {
      // clone repository if it still not present on file system
      jGitService.cloneRepoIfNotExist(version);
      // fill etag caches
      formService.getFormListByVersion(version);
      businessProcessService.getProcessesByVersion(version);
    }));

    executor.shutdown();
  }
}
