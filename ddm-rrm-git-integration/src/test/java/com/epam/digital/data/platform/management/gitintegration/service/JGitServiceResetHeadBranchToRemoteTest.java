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

import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#resetHeadBranchToRemote")
class JGitServiceResetHeadBranchToRemoteTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASSWORD = RandomString.make();

  File directory;

  @Mock
  Git git;
  @Mock
  FetchCommand fetchCommand;
  @Mock
  ResetCommand resetCommand;

  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    directory = new File(tempDir, REPO_NAME);

    Assertions.assertThat(directory.mkdirs()).isTrue();
    Mockito.doReturn(git).when(jGitWrapper).open(directory);
    Mockito.doReturn(REPO_NAME).when(gerritPropertiesConfig).getHeadBranch();
    Mockito.doReturn(USER).when(gerritPropertiesConfig).getUser();
    Mockito.doReturn(PASSWORD).when(gerritPropertiesConfig).getPassword();

    Mockito.doReturn(fetchCommand).when(git).fetch();
    Mockito.doReturn(fetchCommand)
        .when(fetchCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));

    Mockito.doReturn(resetCommand).when(git).reset();
    Mockito.doReturn(resetCommand).when(resetCommand).setMode(ResetType.HARD);
    Mockito.doReturn(resetCommand)
        .when(resetCommand).setRef(Constants.DEFAULT_REMOTE_NAME + "/" + REPO_NAME);
  }


  @Test
  @DisplayName("should call fetch all and reset commands")
  @SneakyThrows
  void testResetHeadBranchToRemote() {
    jGitService.resetHeadBranchToRemote();

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(resetCommand).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if there is invalid remote")
  @SneakyThrows
  void testResetHeadBranchToRemote_invalidRemote() {
    var invalidRemote = new InvalidRemoteException("invalid remote");
    Mockito.when(fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default remote \"origin\" cannot be invalid")
        .hasCauseInstanceOf(InvalidRemoteException.class);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(resetCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if there is checkout conflict")
  @SneakyThrows
  void testResetHeadBranchToRemote_checkoutConflict() {
    Mockito.doThrow(CheckoutConflictException.class)
        .when(resetCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Hard reset must not face any conflicts: ")
        .hasCauseInstanceOf(CheckoutConflictException.class);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(resetCommand).call();
  }

  @Test
  @DisplayName("should retry and throw GitCommandException if there is transport exception")
  @SneakyThrows
  void testResetHeadBranchToRemote_transportException() {
    Mockito.doThrow(TransportException.class)
        .when(fetchCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred while fetching: ")
        .hasCauseInstanceOf(TransportException.class);

    verifyMockInvocations();
    Mockito.verify(fetchCommand, Mockito.times(3)).call();
    Mockito.verify(resetCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if couldn't open the repo due to any IOException")
  @SneakyThrows
  void testResetHeadBranchToRemote_couldNotOpenRepo() {
    var repoName = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);

    Mockito.doThrow(IOException.class).when(jGitWrapper).open(file);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during repository opening: ")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("should throw GitCommandException if there is unknown git exception during fetch")
  @SneakyThrows
  void testResetHeadBranchToRemote_fetchGitCommandException() {
    var gitAPIException = new GitAPIException("some git API exception") {
    };
    Mockito.doThrow(gitAPIException)
        .when(fetchCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while fetching: some git API exception")
        .hasCause(gitAPIException);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(resetCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if there is unknown git exception during reset")
  @SneakyThrows
  void testResetHeadBranchToRemote_resetGitCommandException() {
    var gitAPIException = new GitAPIException("some git API exception") {
    };
    Mockito.doThrow(gitAPIException)
        .when(resetCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred during hard reset on origin %s: some git API exception",
            REPO_NAME)
        .hasCause(gitAPIException);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(resetCommand).call();
  }

  @Test
  @DisplayName("should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
  @SneakyThrows
  void testResetHeadBranchToRemoteDirectoryNotExist() {
    var headBranch = RandomString.make();
    var repo = new File(tempDir, headBranch);
    Assertions.assertThat(repo.exists()).isFalse();

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(headBranch);

    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(RepositoryNotFoundException.class)
        .hasNoCause()
        .hasMessage("Repository " + headBranch + " doesn't exists");

    Mockito.verify(jGitWrapper, never()).open(repo);
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(jGitWrapper).open(directory);

    Mockito.verify(git).fetch();
    Mockito.verify(fetchCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));

    Mockito.verify(git, Mockito.atMostOnce()).reset();
    Mockito.verify(resetCommand, Mockito.atMostOnce()).setMode(ResetType.HARD);
    Mockito.verify(resetCommand, Mockito.atMostOnce())
        .setRef(Constants.DEFAULT_REMOTE_NAME + "/" + REPO_NAME);

    Mockito.verify(git).close();
  }
}