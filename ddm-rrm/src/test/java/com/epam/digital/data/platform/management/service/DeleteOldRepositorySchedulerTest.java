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

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.service.JGitServiceImpl;
import com.epam.digital.data.platform.management.service.impl.DeleteOldRepositoryScheduler;
import com.epam.digital.data.platform.management.service.impl.GerritServiceImpl;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteOldRepositorySchedulerTest {

  @Mock
  private JGitServiceImpl jGitService;
  @Mock
  private GerritServiceImpl gerritService;

  @InjectMocks
  private DeleteOldRepositoryScheduler scheduler;

  @Test
  @SneakyThrows
  void deleteOldRepositoriesSuccessTest() {
    var repo = RandomString.make();
    var ids = List.of(repo);
    Mockito.when(gerritService.getClosedMrIds()).thenReturn(ids);
    scheduler.deleteOldRepositories();
    Mockito.verify(jGitService).deleteRepo(repo);
  }

  @Test
  @SneakyThrows
  void noClosedMrsTest() {
    List<String> ids = new ArrayList<>();
    Mockito.when(gerritService.getClosedMrIds()).thenReturn(ids);
    scheduler.deleteOldRepositories();
    Mockito.verify(jGitService, Mockito.never()).deleteRepo(any());
  }

  @Test
  @SneakyThrows
  void exceptionThrownTest() {
    Mockito.when(gerritService.getClosedMrIds()).thenThrow(RestApiException.class);
    Assertions.assertThatThrownBy(() -> scheduler.deleteOldRepositories())
        .isInstanceOf(RestApiException.class);
  }

  @Test
  @SneakyThrows
  void exceptionNotThrownTest() {
    var repo = RandomString.make();
    var ids = List.of(repo);
    Mockito.when(gerritService.getClosedMrIds()).thenReturn(ids);
    Mockito.doThrow(GitCommandException.class).when(jGitService).deleteRepo(repo);
    Assertions.assertThatCode(() -> scheduler.deleteOldRepositories())
        .doesNotThrowAnyException();
  }
}
