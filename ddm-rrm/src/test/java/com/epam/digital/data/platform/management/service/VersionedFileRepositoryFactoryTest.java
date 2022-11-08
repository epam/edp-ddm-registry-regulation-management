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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.service.impl.HeadFileRepositoryImpl;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryFactoryImpl;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryImpl;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VersionedFileRepositoryFactoryTest {

  @Mock
  private JGitService jGitService;
  @Mock
  private GerritService gerritService;

  @Mock
  private GerritPropertiesConfig config;
  @InjectMocks
  private VersionedFileRepositoryFactoryImpl factory;

  @Test
  @SneakyThrows
  void getRepositoryVersionedTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    ChangeInfoDto changeInfo = new  ChangeInfoDto();
    changeInfo.setChangeId("1");
    final ChangeInfoDto mock = Mockito.mock(ChangeInfoDto.class);
    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(mock);
    Mockito.when(gerritService.getMRByNumber("version")).thenReturn(changeInfo);
    VersionedFileRepository repo = factory.getRepoByVersion("version");
    Assertions.assertInstanceOf(VersionedFileRepositoryImpl.class, repo);
  }

  @Test
  @SneakyThrows
  void getRepositoryHeadTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    VersionedFileRepository repo = factory.getRepoByVersion("master");
    Assertions.assertInstanceOf(HeadFileRepositoryImpl.class, repo);
  }

  @Test
  @SneakyThrows
  void getAvailReposTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    ChangeInfoDto changeInfo = new  ChangeInfoDto();
    changeInfo.setChangeId("1");
    final ChangeInfoDto mock = Mockito.mock(ChangeInfoDto.class);
    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(mock);
    Mockito.when(gerritService.getMRByNumber("version")).thenReturn(changeInfo);
    factory.getRepoByVersion("version");
    factory.getRepoByVersion("master");
    Map<String, VersionedFileRepository> repositories = factory.getAvailableRepos();
    Assertions.assertEquals(2, repositories.entrySet().size());
  }
}
