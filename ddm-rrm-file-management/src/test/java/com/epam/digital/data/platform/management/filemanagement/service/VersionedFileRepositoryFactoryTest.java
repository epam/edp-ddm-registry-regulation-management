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
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("VersionedFileRepositoryFactory Test")
class VersionedFileRepositoryFactoryTest {

  private static final String HEAD_BRANCH = "master";

  @Mock
  private JGitService jGitService;
  @Mock
  private GerritService gerritService;

  @Mock
  private GerritPropertiesConfig config;
  @InjectMocks
  private VersionedFileRepositoryFactory factory;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(HEAD_BRANCH).when(config).getHeadBranch();
  }

  @Test
  @DisplayName("should create component with type VersionedFileRepositoryImpl for version candidate")
  void getRepositoryVersionedTest() {
    var refs = RandomString.make();
    var changeId = RandomString.make();
    var version = "version";
    var changeInfo = new ChangeInfoDto();
    changeInfo.setChangeId(changeId);
    changeInfo.setRefs(refs);

    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(changeInfo);
    Mockito.when(gerritService.getMRByNumber(version)).thenReturn(changeInfo);

    var repo = factory.createComponent(version);
    Assertions.assertThat(repo).isInstanceOf(VersionedFileRepositoryImpl.class);

    Mockito.verify(jGitService).cloneRepoIfNotExist(version);
    Mockito.verify(gerritService).getChangeInfo(changeInfo.getChangeId());
    Mockito.verify(jGitService).fetch(version, changeInfo.getRefs());
  }

  @Test
  @DisplayName("should create component with type HeadFileRepositoryImpl for head branch version")
  void getRepositoryHeadTest() {
    var repo = factory.createComponent(HEAD_BRANCH);

    Assertions.assertThat(repo).isInstanceOf(HeadFileRepositoryImpl.class);

    Mockito.verify(jGitService).cloneRepoIfNotExist(HEAD_BRANCH);
  }

  @Test
  @DisplayName("recreate should be true if repository doesn't exists and false otherwise")
  void shouldBeRecreatedTest() {
    var version = "version";

    Mockito.doReturn(false).when(jGitService).repoExists(HEAD_BRANCH);
    Mockito.doReturn(true).when(jGitService).repoExists(version);

    Assertions.assertThat(factory.shouldBeRecreated(HEAD_BRANCH)).isTrue();
    Assertions.assertThat(factory.shouldBeRecreated(version)).isFalse();
  }

  @Test
  @DisplayName("componentType should be VersionedFileRepository")
  void getComponentTypeTest() {
    Assertions.assertThat(factory.getComponentType()).isEqualTo(VersionedFileRepository.class);
  }
}
