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
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
    var refs = RandomString.make();
    var changeId = RandomString.make();
    var version = "version";
    var changeInfo = new ChangeInfoDto();
    changeInfo.setChangeId(changeId);
    changeInfo.setRefs(refs);

    Mockito.when(config.getHeadBranch()).thenReturn("master");
    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(changeInfo);
    Mockito.when(gerritService.getMRByNumber(version)).thenReturn(changeInfo);

    VersionedFileRepository repo = factory.getRepoByVersion(version);
    Assertions.assertThat(repo).isInstanceOf(VersionedFileRepositoryImpl.class);

    Mockito.verify(jGitService).cloneRepoIfNotExist(version);
    Mockito.verify(gerritService).getChangeInfo(changeInfo.getChangeId());
    Mockito.verify(jGitService).fetch(version, changeInfo.getRefs());
  }

  @Test
  @SneakyThrows
  void getRepositoryHeadTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    var repo = factory.getRepoByVersion("master");
    Assertions.assertThat(repo).isInstanceOf(HeadFileRepositoryImpl.class);

    Mockito.verify(jGitService).cloneRepoIfNotExist("master");
  }

  @Test
  @SneakyThrows
  void getAvailReposTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    var changeInfo = new ChangeInfoDto();
    changeInfo.setChangeId("1");
    var mock = Mockito.mock(ChangeInfoDto.class);
    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(mock);
    Mockito.when(gerritService.getMRByNumber("version")).thenReturn(changeInfo);
    factory.getRepoByVersion("version");
    factory.getRepoByVersion("master");
    Map<String, VersionedFileRepository> repositories = factory.getAvailableRepos();
    Assertions.assertThat(repositories.entrySet().size()).isEqualTo(2);
  }
}
