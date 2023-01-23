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
package com.epam.digital.data.platform.management.filemanagement.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionBeanFactory;
import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionedFileRepositoryFactory implements
    VersionBeanFactory<VersionedFileRepository> {

  private final GerritPropertiesConfig config;
  private final JGitService jGitService;
  private final GerritService gerritService;
  private final FileManagementMapper mapper;

  @Override
  @NonNull
  public VersionedFileRepository createBean(@NonNull String versionId) {
    var repo = config.getHeadBranch().equals(versionId)
        ? new HeadFileRepositoryImpl(versionId, jGitService, gerritService, mapper)
        : new VersionedFileRepositoryImpl(versionId, jGitService, gerritService, mapper);

    repo.updateRepository();
    return repo;
  }

  @Override
  public boolean shouldBeRecreated(@NonNull String versionId) {
    return !jGitService.repoExists(versionId);
  }

  @Override
  @NonNull
  public Class<VersionedFileRepository> getBeanType() {
    return VersionedFileRepository.class;
  }
}
