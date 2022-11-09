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
package com.epam.digital.data.platform.management.filemanagement.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionedFileRepositoryFactoryImpl implements VersionedFileRepositoryFactory {

  private final GerritPropertiesConfig config;

  private final JGitService jGitService;

  private final GerritService gerritService;
  private final FileManagementMapper mapper;

  private final ConcurrentMap<String, VersionedFileRepository> repositoryMap = new ConcurrentHashMap<>();

  @Override
  public VersionedFileRepository getRepoByVersion(String versionName) {

    return repositoryMap.computeIfAbsent(
        versionName, repo -> {
          var repository = doCreateRepo(versionName);
          repository.updateRepository();
          return repository;
        });
  }

  @Override
  public Map<String, VersionedFileRepository> getAvailableRepos() {
    return ImmutableMap.copyOf(repositoryMap);
  }

  private VersionedFileRepository doCreateRepo(String versionName) {
    return config.getHeadBranch().equals(versionName)
        ? new HeadFileRepositoryImpl(versionName, jGitService, gerritService, mapper)
        : new VersionedFileRepositoryImpl(versionName, jGitService, gerritService, mapper);
  }
}
