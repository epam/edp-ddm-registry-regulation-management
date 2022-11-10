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
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#amend")
class JGitServiceAmendTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();
  static final String FILE_CONTENT = RandomString.make();

  static final String REPO_URL = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASS = RandomString.make();
  static final String HEAD_BRANCH = RandomString.make();
  static final String COMMIT_MESSAGE = RandomString.make();

  File repoDir;

  @Mock
  Git git;
  @Mock
  AddCommand addCommand;
  @Mock
  StatusCommand statusCommand;
  @Mock
  Status status;
  @Mock
  LogCommand logCommand;
  RevCommit lastCommit;
  @Mock
  CommitCommand commitCommand;
  @Mock
  RemoteAddCommand remoteAddCommand;
  @Mock
  PushCommand pushCommand;

  @Override
  @BeforeEach
  @SneakyThrows
  void setUp() {
    super.setUp();

    Mockito.doReturn(REPO_URL).when(gerritPropertiesConfig).getUrl();
    Mockito.doReturn(REPO_NAME).when(gerritPropertiesConfig).getRepository();
    Mockito.doReturn(USER).when(gerritPropertiesConfig).getUser();
    Mockito.doReturn(PASS).when(gerritPropertiesConfig).getPassword();
    Mockito.doReturn(HEAD_BRANCH).when(gerritPropertiesConfig).getHeadBranch();

    repoDir = new File(tempDir, REPO_NAME);
    Assertions.assertThat(repoDir.mkdirs()).isTrue();

    Mockito.doReturn(git).when(jGitWrapper).open(repoDir);

    Mockito.doReturn(addCommand).when(git).add();
    Mockito.doReturn(addCommand).when(addCommand).addFilepattern(FILE_PATH);

    Mockito.doReturn(statusCommand).when(git).status();
    Mockito.doReturn(false).when(status).isClean();
    Mockito.doReturn(status).when(statusCommand).call();

    Mockito.doReturn(logCommand).when(git).log();
    final var lastCommitBuilder = new CommitBuilder();
    lastCommitBuilder.setMessage(COMMIT_MESSAGE);
    lastCommitBuilder.setTreeId(new ObjectId(1, 2, 3, 4, 5));
    lastCommitBuilder.setAuthor(new PersonIdent("committer", "committer@epam.com",
        LocalDateTime.of(2022, 11, 10, 13, 40).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    lastCommitBuilder.setCommitter(lastCommitBuilder.getAuthor());
    lastCommit = RevCommit.parse(lastCommitBuilder.build());
    Mockito.doReturn(List.of(lastCommit)).when(logCommand).call();

    Mockito.doReturn(commitCommand).when(git).commit();
    Mockito.doReturn(commitCommand).when(commitCommand).setAmend(true);
    Mockito.doReturn(commitCommand).when(commitCommand).setMessage(COMMIT_MESSAGE);

    Mockito.doReturn(remoteAddCommand).when(git).remoteAdd();
    Mockito.doReturn(remoteAddCommand).when(remoteAddCommand)
        .setName(Constants.DEFAULT_REMOTE_NAME);
    Mockito.doReturn(remoteAddCommand).when(remoteAddCommand)
        .setUri(new URIish(REPO_URL + "/" + REPO_NAME).setPass(PASS).setUser(USER));

    Mockito.doReturn(pushCommand).when(git).push();
    Mockito.doReturn(pushCommand).when(pushCommand)
        .setCredentialsProvider(Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASS)));
    Mockito.doReturn(pushCommand).when(pushCommand).setRemote(Constants.DEFAULT_REMOTE_NAME);
    Mockito.doReturn(pushCommand).when(pushCommand)
        .setRefSpecs(Mockito.refEq(new RefSpec("HEAD:refs/for/" + HEAD_BRANCH)));
  }

  @Test
  @DisplayName("should 'git add' file, find last commit, commit with amend, add remote and push")
  @SneakyThrows
  void testAmend() {
    jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT);

    Assertions.assertThat(Path.of(repoDir.getPath(), FILE_PATH))
        .exists()
        .content().isEqualTo(FILE_CONTENT);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(status).isClean();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).setAmend(true);
    Mockito.verify(commitCommand).setMessage(COMMIT_MESSAGE);
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(remoteAddCommand).setName(Constants.DEFAULT_REMOTE_NAME);
    Mockito.verify(remoteAddCommand)
        .setUri(new URIish(REPO_URL + "/" + REPO_NAME).setPass(PASS).setUser(USER));
    Mockito.verify(remoteAddCommand).call();

    Mockito.verify(git).push();
    Mockito.verify(pushCommand)
        .setCredentialsProvider(Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASS)));
    Mockito.verify(pushCommand).setRemote(Constants.DEFAULT_REMOTE_NAME);
    Mockito.verify(pushCommand)
        .setRefSpecs(Mockito.refEq(new RefSpec("HEAD:refs/for/" + HEAD_BRANCH)));
    Mockito.verify(pushCommand).call();
  }

  @Test
  @DisplayName("should 'git add' file and do nothing if status is clean")
  @SneakyThrows
  void testAmendCleanStatus() {
    Mockito.doReturn(true).when(status).isClean();

    jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT);

    Assertions.assertThat(Path.of(repoDir.getPath(), FILE_PATH))
        .exists()
        .content().isEqualTo(FILE_CONTENT);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(status).isClean();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git, Mockito.never()).log();
    Mockito.verify(logCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw RepositoryNotFoundException if no repository exists")
  @SneakyThrows
  void testAmendRepositoryNotExist() {
    final var repositoryName = RandomString.make();
    final var filepath = RandomString.make();
    final var fileContent = RandomString.make();

    Assertions.assertThatThrownBy(() -> jGitService.amend(repositoryName, filepath, fileContent))
        .isInstanceOf(RepositoryNotFoundException.class)
        .hasMessage("Repository %s doesn't exists", repositoryName)
        .hasNoCause();
  }

  @Test
  @DisplayName("should throw GitCommandException if IOException occurred during writing content to file")
  @SneakyThrows
  void testAmend_IOExceptionOccurred() {
    Assertions.assertThat(new File(repoDir, FILE_PATH).mkdirs()).isTrue();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during writing content to file %s: ", FILE_PATH)
        .hasCauseInstanceOf(IOException.class);

    Mockito.verify(git, Mockito.never()).add();
    Mockito.verify(git, Mockito.never()).status();
    Mockito.verify(git, Mockito.never()).log();
    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(git, Mockito.never()).push();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on add command")
  @SneakyThrows
  void testAmend_addCommandException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(addCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Could not execute add/rm command: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git, Mockito.never()).status();
    Mockito.verify(statusCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).log();
    Mockito.verify(logCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if NoWorkTreeException occurred on status command")
  @SneakyThrows
  void testAmend_statusCommandNoWorkTreeException() {
    Mockito.doThrow(NoWorkTreeException.class).when(statusCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Work tree mustn't disappear: ")
        .hasCauseInstanceOf(NoWorkTreeException.class);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git, Mockito.never()).log();
    Mockito.verify(logCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on status command")
  @SneakyThrows
  void testAmend_statusCommandUnknownGitApiException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(statusCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Could not execute status command: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git, Mockito.never()).log();
    Mockito.verify(logCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if NoHeadException occurred on log command")
  @SneakyThrows
  void testAmend_logCommandNoHeadException() {
    Mockito.doThrow(NoHeadException.class).when(logCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Log/commit command doesn't expected to throw such exception: ")
        .hasCauseInstanceOf(NoHeadException.class);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on log command")
  @SneakyThrows
  void testAmend_logCommandUnknownGitApiException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(logCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred during amending commit: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git, Mockito.never()).commit();
    Mockito.verify(commitCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @ParameterizedTest
  @ValueSource(classes = {
      AbortedByHookException.class,
      ConcurrentRefUpdateException.class,
      NoHeadException.class,
      NoMessageException.class,
      ServiceUnavailableException.class,
      UnmergedPathsException.class,
      WrongRepositoryStateException.class
  })
  @DisplayName("should throw IllegalStateException if one of the exceptions occurred on commit command")
  @SneakyThrows
  void testAmend_commitCommandIllegalStateException(Class<? extends GitAPIException> exType) {
    Mockito.doThrow(exType).when(commitCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Log/commit command doesn't expected to throw such exception: ")
        .hasCauseInstanceOf(exType);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on commit command")
  @SneakyThrows
  void testAmend_commitCommandUnknownGitApiException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(commitCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred during amending commit: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git, Mockito.never()).remoteAdd();
    Mockito.verify(remoteAddCommand, Mockito.never()).call();

    Mockito.verify(git, Mockito.never()).push();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on add remote command")
  @SneakyThrows
  void testAmend_remoteAddCommandUnknownGitApiException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(remoteAddCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Could not execute add-remote/push command: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(git).push();

    Mockito.verify(remoteAddCommand).call();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if invalid url is configured")
  @SneakyThrows
  void testAmend_remoteAddInvalidURI() {
    Mockito.doReturn("").when(gerritPropertiesConfig).getUrl();
    Mockito.doReturn("").when(gerritPropertiesConfig).getRepository();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Repository url that is configured under \"gerrit\" prefix is invalid")
        .hasNoCause();

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(git, Mockito.never()).push();

    Mockito.verify(remoteAddCommand, Mockito.never()).call();
    Mockito.verify(pushCommand, Mockito.never()).call();
  }

  @Test
  @DisplayName("should throw IllegalStateException if InvalidRemoteException occurred on push command")
  @SneakyThrows
  void testAmend_pushCommandInvalidRemoteException() {
    Mockito.doThrow(InvalidRemoteException.class).when(pushCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Remote that is configured under \"gerrit\" prefix is invalid: ")
        .hasCauseInstanceOf(InvalidRemoteException.class);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(git).push();

    Mockito.verify(remoteAddCommand).call();
    Mockito.verify(pushCommand).call();
  }

  @Test
  @DisplayName("should retry and throw GitCommandException if TransportException occurred on push command")
  @SneakyThrows
  void testAmend_pushCommandTransportException() {
    Mockito.doThrow(TransportException.class).when(pushCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Could not execute add-remote/push command: ")
        .hasCauseInstanceOf(TransportException.class);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(git).push();

    Mockito.verify(remoteAddCommand).call();
    Mockito.verify(pushCommand, Mockito.times(3)).call();
  }

  @Test
  @DisplayName("should throw GitCommandException if unknown GitApiException occurred on push command")
  @SneakyThrows
  void testAmend_pushCommandUnknownGitApiException() {
    var ex = new GitAPIException("unknown exception") {
    };
    Mockito.doThrow(ex).when(pushCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.amend(REPO_NAME, FILE_PATH, FILE_CONTENT))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Could not execute add-remote/push command: unknown exception")
        .hasCause(ex);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).log();
    Mockito.verify(logCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).call();

    Mockito.verify(git).remoteAdd();
    Mockito.verify(git).push();

    Mockito.verify(remoteAddCommand).call();
    Mockito.verify(pushCommand).call();
  }
}
