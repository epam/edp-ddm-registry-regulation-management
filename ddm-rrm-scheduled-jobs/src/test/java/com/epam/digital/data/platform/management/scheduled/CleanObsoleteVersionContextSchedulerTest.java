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
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoShortDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritServiceImpl;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.service.JGitServiceImpl;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class CleanObsoleteVersionContextSchedulerTest {

  @TempDir
  private File tempDir;

  @Mock
  private JGitServiceImpl jGitService;
  @Mock
  private GerritServiceImpl gerritService;
  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;
  @Mock
  private VersionContextComponentManager context;

  @InjectMocks
  private CleanObsoleteVersionContextScheduler scheduler;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(tempDir.getAbsolutePath()).when(gerritPropertiesConfig)
        .getRepositoryDirectory();
    Mockito.doReturn("").when(gerritPropertiesConfig).getHeadBranch();
  }

  @Test
  @SneakyThrows
  void cleanObsoleteVersionContextsTest() {
    var repo = RandomString.make();
    Files.createDirectory(Path.of(tempDir.getAbsolutePath(), repo));
    Mockito.when(gerritService.getMRList()).thenReturn(List.of());

    scheduler.cleanObsoleteVersionContexts();

    Mockito.verify(jGitService).deleteRepo(repo);
    Mockito.verify(context).destroyContext(repo);
  }

  @Test
  @SneakyThrows
  void cleanObsoleteVersionContexts_noHeadBranchDeleting() {
    var headBranch = RandomString.make();
    Files.createDirectory(Path.of(tempDir.getAbsolutePath(), headBranch));
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(headBranch);
    Mockito.when(gerritService.getMRList()).thenReturn(List.of());

    scheduler.cleanObsoleteVersionContexts();

    Mockito.verifyNoInteractions(jGitService);
  }

  @Test
  @SneakyThrows
  void cleanObsoleteVersionContexts_noObsoleteRepos() {
    var repo = RandomString.make();
    Files.createDirectory(Path.of(tempDir.getAbsolutePath(), repo));
    var changeInfo = new ChangeInfoShortDto();
    changeInfo.setNumber(repo);
    var mrList = List.of(changeInfo);
    Mockito.when(gerritService.getMRList()).thenReturn(mrList);

    scheduler.cleanObsoleteVersionContexts();

    Mockito.verifyNoInteractions(jGitService);
  }

  @Test
  @SneakyThrows
  void cleanObsoleteVersionContexts_gerritCommunication() {
    Mockito.doThrow(GerritCommunicationException.class).when(gerritService).getMRList();
    Assertions.assertThatCode(() -> scheduler.cleanObsoleteVersionContexts())
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void exceptionNotThrownTest() {
    var repo = RandomString.make();
    Mockito.when(gerritService.getMRList()).thenReturn(List.of());
    Mockito.doThrow(GitCommandException.class).when(jGitService).deleteRepo(repo);

    Assertions.assertThatCode(() -> scheduler.cleanObsoleteVersionContexts())
        .doesNotThrowAnyException();
  }
}
