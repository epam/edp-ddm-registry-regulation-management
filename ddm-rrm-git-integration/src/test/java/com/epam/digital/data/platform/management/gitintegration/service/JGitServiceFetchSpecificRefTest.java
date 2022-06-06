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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#fetch")
class JGitServiceFetchSpecificRefTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASSWORD = RandomString.make();
  static final String REFS = RandomString.make();

  File directory;

  @Mock
  Git git;
  @Mock
  FetchCommand fetchCommand;
  @Mock
  CheckoutCommand checkoutCommand;


  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    directory = new File(tempDir, REPO_NAME);

    Assertions.assertThat(directory.mkdirs()).isTrue();
    Mockito.doReturn(git).when(jGitWrapper).open(directory);
    Mockito.doReturn(USER).when(gerritPropertiesConfig).getUser();
    Mockito.doReturn(PASSWORD).when(gerritPropertiesConfig).getPassword();

    Mockito.doReturn(fetchCommand).when(git).fetch();
    Mockito.doReturn(fetchCommand).when(fetchCommand).setRefSpecs(REFS);
    Mockito.doReturn(fetchCommand)
        .when(fetchCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));

    Mockito.doReturn(checkoutCommand).when(git).checkout();
    Mockito.doReturn(checkoutCommand).when(checkoutCommand).setName(Constants.FETCH_HEAD);
  }

  @Test
  @DisplayName("should call fetch refs and checkout commands")
  @SneakyThrows
  void testFetch() {
    jGitService.fetch(REPO_NAME, REFS);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if there is invalid remote")
  @SneakyThrows
  void testFetch_invalidRemote() {
    var invalidRemote = new InvalidRemoteException("invalid remote");
    Mockito.when(fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(REPO_NAME, REFS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default remote \"origin\" cannot be invalid")
        .hasCauseInstanceOf(InvalidRemoteException.class);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand, Mockito.never()).call();
  }

  @DisplayName("should throw IllegalStateException if there is checkout conflict")
  @ParameterizedTest
  @ValueSource(classes = {
      CheckoutConflictException.class,
      RefAlreadyExistsException.class,
      RefNotFoundException.class,
      InvalidRefNameException.class
  })
  @SneakyThrows
  void testResetHeadBranchToRemote_checkoutConflict(
      Class<? extends GitAPIException> checkoutConflictType) {
    Mockito.doThrow(checkoutConflictType)
        .when(checkoutCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.fetch(REPO_NAME, REFS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Checkout on FETCH_HEAD must not throw such exception: ")
        .hasCauseInstanceOf(checkoutConflictType);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @DisplayName("should retry and throw GitCommandException if there is transport exception")
  @SneakyThrows
  void testResetHeadBranchToRemote_transportException() {
    Mockito.doThrow(TransportException.class)
        .when(fetchCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.fetch(REPO_NAME, REFS))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred while fetching: ")
        .hasCauseInstanceOf(TransportException.class);

    verifyMockInvocations();
    Mockito.verify(fetchCommand, Mockito.times(3)).call();
    Mockito.verify(checkoutCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if there is unknown git exception during fetch")
  @SneakyThrows
  void testFetch_fetchGitCommandException() {
    var gitAPIException = new GitAPIException("some git API exception") {
    };
    Mockito.doThrow(gitAPIException)
        .when(fetchCommand).call();
    Assertions.assertThatThrownBy(() -> jGitService.fetch(REPO_NAME, REFS))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while fetching: some git API exception")
        .hasCause(gitAPIException);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if there is unknown git exception during checkout")
  @SneakyThrows
  void testFetch_checkoutGitCommandException() {
    var gitAPIException = new GitAPIException("some git API exception") {
    };

    Mockito.doThrow(gitAPIException)
        .when(checkoutCommand).call();
    Assertions.assertThatThrownBy(() -> jGitService.fetch(REPO_NAME, REFS))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while checkout: some git API exception")
        .hasCause(gitAPIException);

    verifyMockInvocations();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if couldn't open the repo due to any IOException")
  @SneakyThrows
  void testFetch_couldNotOpenRepo() {
    var repoName = RandomString.make();
    var ref = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    Mockito.doThrow(IOException.class).when(jGitWrapper).open(file);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, ref))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during repository opening: ")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("Should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
  @SneakyThrows
  void testFetchDirectoryNotExist() {
    var repoName = RandomString.make();
    final String refs = RandomString.make();
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(RepositoryNotFoundException.class)
        .hasMessage("Repository " + repoName + " doesn't exists")
        .hasNoCause();

    Mockito.verify(jGitWrapper, never()).open(eq(new File(String.format("%s/%s", repoName, refs))));
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(jGitWrapper).open(directory);

    Mockito.verify(git).fetch();
    Mockito.verify(fetchCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));
    Mockito.verify(fetchCommand).setRefSpecs(REFS);

    Mockito.verify(git, Mockito.atMostOnce()).reset();
    Mockito.verify(checkoutCommand, Mockito.atMostOnce()).setName(Constants.FETCH_HEAD);

    Mockito.verify(git).close();
  }
}
