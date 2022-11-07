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
package com.epam.digital.data.platform.management.gitintegration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import java.io.File;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGit service unit test")
class JGitServiceTest extends AbstractJGitServiceTest {

  @Mock
  private Git git;
  @Mock
  private CheckoutCommand checkoutCommand;
  @Mock
  private FetchCommand fetchCommand;

  @Test
  @SneakyThrows
  void testAmendRepositoryNotExist() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    final var filecontent = RandomString.make();
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class)
        .isThrownBy(
            () -> jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath,
                filecontent));
  }

  @Test
  @SneakyThrows
  void testAmendEmptyInput() {

    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    final var filecontent = RandomString.make();
    var repo = new File(tempDir, repositoryName);
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(gitFileService.writeFile(repositoryName, filecontent, filepath))
        .thenReturn(null);

    jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath, filecontent);
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void deleteFalseTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    var repo = new File(tempDir, repositoryName);
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    jGitService.delete(repositoryName, filepath, refs, commitMessage, changeId);
    Mockito.verify(checkoutCommand).call();
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void deleteRepoNotExistTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class).isThrownBy(
        () -> jGitService.delete(repositoryName, filepath, refs, commitMessage, changeId));
  }
}
