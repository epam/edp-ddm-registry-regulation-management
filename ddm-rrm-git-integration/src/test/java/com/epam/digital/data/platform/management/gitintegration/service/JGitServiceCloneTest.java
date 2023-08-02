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

package com.epam.digital.data.platform.management.gitintegration.service;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import java.io.File;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#clone")
class JGitServiceCloneTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String REPO_URL = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASSWORD = RandomString.make();

  File directory;
  @Mock
  CloneCommand cloneCommand;

  @BeforeEach
  @Override
  void setUp() {
    super.setUp();

    directory = new File(tempDir, REPO_NAME);

    Mockito.doReturn(cloneCommand).when(jGitWrapper).cloneRepository();
    Mockito.doReturn(USER).when(gerritPropertiesConfig).getUser();
    Mockito.doReturn(PASSWORD).when(gerritPropertiesConfig).getPassword();
    Mockito.doReturn(REPO_URL).when(gerritPropertiesConfig).getUrl();
    Mockito.doReturn(REPO_NAME).when(gerritPropertiesConfig).getRepository();

    Mockito.doReturn(cloneCommand).when(cloneCommand).setURI(REPO_URL + "/" + REPO_NAME);
    Mockito.doReturn(cloneCommand).when(cloneCommand).setDirectory(directory);
    Mockito.doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));
    Mockito.doReturn(cloneCommand).when(cloneCommand).setCloneAllBranches(true);

    Mockito.doReturn("master").when(gerritPropertiesConfig).getHeadBranch();
  }

  @Test
  @DisplayName("should not clone if repo already exists")
  @SneakyThrows
  void testCloneRepository_repoAlreadyExists() {
    Assertions.assertThat(directory.createNewFile()).isTrue();

    jGitService.cloneRepoIfNotExist(REPO_NAME);

    Mockito.verify(jGitWrapper, Mockito.never()).cloneRepository();
  }

  @Test
  @DisplayName("should clone repo doesn't exist")
  @SneakyThrows
  void testCloneRepository() {
    final var git = Mockito.mock(Git.class);
    Mockito.doReturn(git).when(cloneCommand).call();

    jGitService.cloneRepoIfNotExist(REPO_NAME);

    verifyMockInvocations();
    Mockito.verify(git).close();
  }

  @Test
  @DisplayName("should throw IllegalStateException if there is invalid remote")
  @SneakyThrows
  void testCloneRepository_invalidRemote() {
    Mockito.doThrow(InvalidRemoteException.class).when(cloneCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(REPO_NAME))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Remote that is configured under \"gerrit\" prefix is invalid: ")
        .hasCauseInstanceOf(InvalidRemoteException.class);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should retry and throw GitCommandException if there is transport exception")
  @SneakyThrows
  void testCloneRepository_transportException() {
    Mockito.doThrow(TransportException.class).when(cloneCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(REPO_NAME))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during cloning repository %s: ", REPO_NAME)
        .hasCauseInstanceOf(GitAPIException.class);

    Mockito.verify(cloneCommand, Mockito.times(3)).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if there is unknown git exception")
  @SneakyThrows
  void testCloneRepository_gitApiException() {
    Mockito.doThrow(new GitAPIException("message") {
    }).when(cloneCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(REPO_NAME))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during cloning repository %s: ", REPO_NAME)
        .hasCauseInstanceOf(GitAPIException.class);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw NPE if clone command returned null instead of Git (not possible in real)")
  @SneakyThrows
  void testCloneRepository_cloneReturnNull() {
    Mockito.doReturn(null).when(cloneCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(REPO_NAME))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("CloneCommand#call cannot be null");

    verifyMockInvocations();
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(jGitWrapper).cloneRepository();
    Mockito.verify(cloneCommand).setURI(REPO_URL + "/" + REPO_NAME);
    Mockito.verify(cloneCommand).setDirectory(directory);
    Mockito.verify(cloneCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));
    Mockito.verify(cloneCommand).setCloneAllBranches(true);
    Mockito.verify(cloneCommand).call();
  }
}
