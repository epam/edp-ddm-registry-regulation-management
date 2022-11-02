/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VersionedFileRepositoryFactoryImpl implements VersionedFileRepositoryFactory {

  @Autowired
  private GerritPropertiesConfig config;

  @Autowired
  private JGitService jGitService;

  @Autowired
  private GerritService gerritService;

  private final ConcurrentMap<String, VersionedFileRepository> repositoryMap = new ConcurrentHashMap<>();

  @Override
  public VersionedFileRepository getRepoByVersion(String versionName) {

    return repositoryMap.computeIfAbsent(
        versionName, repo -> {
          var repository = doCreateRepo(versionName);
          repository.pullRepository();
          return repository;
        });
  }

  @Override
  public Map<String, VersionedFileRepository> getAvailableRepos() {
    return ImmutableMap.copyOf(repositoryMap);
  }

  private VersionedFileRepository doCreateRepo(String versionName) {
    if (config.getHeadBranch().equals(versionName)) {
      HeadFileRepositoryImpl repo = new HeadFileRepositoryImpl();
      repo.setVersionName(versionName);
      repo.setJGitService(jGitService);
      return repo;
    } else {
      VersionedFileRepositoryImpl repo = new VersionedFileRepositoryImpl();
      repo.setVersionName(versionName);
      repo.setGerritService(gerritService);
      repo.setJGitService(jGitService);
      return repo;
    }
  }
}
