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

package com.epam.digital.data.platform.management.gitintegration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.gitintegration.exception.GitFileNotFoundException;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#rollback")
class JGitServiceRollbackTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();

  static final String REPO_URL = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASS = RandomString.make();
  static final String HEAD_BRANCH = RandomString.make();
  static final String COMMIT_MESSAGE = RandomString.make();

  File repoDir;
  File fileToRollback;
  RevCommit lastCommit;

  @Mock
  Git git;
  @Mock
  RmCommand rmCommand;
  @Mock
  AddCommand addCommand;
  @Mock
  StatusCommand statusCommand;
  @Mock
  Status status;
  @Mock
  LogCommand logCommand;
  @Mock
  CommitCommand commitCommand;
  @Mock
  RemoteAddCommand remoteAddCommand;
  @Mock
  PushCommand pushCommand;
  @Mock
  CheckoutCommand mockCheckoutCommand;
  @Mock
  TreeWalk treeWalk;
  @Mock
  Repository repository;
  @Mock
  Ref ref;


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
    fileToRollback = new File(repoDir, FILE_PATH);
    Assertions.assertThat(repoDir.mkdirs()).isTrue();
    Assertions.assertThat(fileToRollback.createNewFile()).isTrue();

    Mockito.doReturn(git).when(jGitWrapper).open(repoDir);
    Mockito.doReturn(repository).when(git).getRepository();

    Mockito.doReturn(statusCommand).when(git).status();
    Mockito.doReturn(false).when(status).isClean();
    Mockito.doReturn(status).when(statusCommand).call();

    final var parentCommitBuilder = new CommitBuilder();
    parentCommitBuilder.setMessage(COMMIT_MESSAGE);
    parentCommitBuilder.setTreeId(new ObjectId(5, 6, 7, 8, 9));
    parentCommitBuilder.setAuthor(new PersonIdent("committer2", "committer2@epam.com",
        LocalDateTime.of(2023, 6, 13, 17, 15).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    parentCommitBuilder.setCommitter(parentCommitBuilder.getAuthor());
    var parentCommit = RevCommit.parse(parentCommitBuilder.build());

    final var lastCommitBuilder = new CommitBuilder();
    lastCommitBuilder.setParentId(parentCommit);
    lastCommitBuilder.setMessage(COMMIT_MESSAGE);
    lastCommitBuilder.setTreeId(new ObjectId(1, 2, 3, 4, 5));
    lastCommitBuilder.setAuthor(new PersonIdent("committer", "committer@epam.com",
        LocalDateTime.of(2023, 6, 12, 17, 15).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    lastCommitBuilder.setCommitter(lastCommitBuilder.getAuthor());
    lastCommit = RevCommit.parse(lastCommitBuilder.build());
    Mockito.doReturn(logCommand).when(git).log();
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
  @DisplayName("should check if file exists in parent commit, 'git checkout' file (exists in the parent commit), find last commit, commit with amend, add remote and push")
  @SneakyThrows
  void testRollbackFileThatExistsInParentCommit() {
    Mockito.doReturn(treeWalk).doReturn(treeWalk).when(jGitWrapper)
        .getTreeWalk(eq(repository), eq(FILE_PATH), any());
    Mockito.doReturn(addCommand).when(git).add();
    Mockito.doReturn(addCommand).when(addCommand).addFilepattern(FILE_PATH);
    Mockito.doReturn(mockCheckoutCommand).when(git).checkout();
    Mockito.doReturn(mockCheckoutCommand).when(mockCheckoutCommand)
        .setStartPoint(any(RevCommit.class));
    Mockito.doReturn(mockCheckoutCommand).when(mockCheckoutCommand).addPath(FILE_PATH);
    Mockito.doReturn(ref).when(mockCheckoutCommand).call();

    jGitService.rollbackFile(REPO_NAME, FILE_PATH);

    Assertions.assertThat(Path.of(repoDir.getPath(), FILE_PATH)).exists();

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(status).isClean();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git, Mockito.times(2)).log();
    Mockito.verify(logCommand, Mockito.times(2)).call();

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
  @DisplayName("should check if file exists in parent commit, 'git rm' file (does not exists in the parent commit), find last commit, commit with amend, add remote and push")
  @SneakyThrows
  void testRollbackFileThatDoesNotExistsInParentCommit() {
    Mockito.doReturn(treeWalk).doReturn(null).when(jGitWrapper)
        .getTreeWalk(eq(repository), eq(FILE_PATH), any());
    Mockito.doReturn(rmCommand).when(git).rm();
    Mockito.doReturn(rmCommand).when(rmCommand).addFilepattern(FILE_PATH);

    jGitService.rollbackFile(REPO_NAME, FILE_PATH);

    Assertions.assertThat(Path.of(repoDir.getPath(), FILE_PATH)).doesNotExist();

    Mockito.verify(git).rm();
    Mockito.verify(rmCommand).addFilepattern(FILE_PATH);
    Mockito.verify(rmCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(status).isClean();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git, Mockito.times(2)).log();
    Mockito.verify(logCommand, Mockito.times(2)).call();

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
  @DisplayName("should throw an error when file does not exists (in parent commit and last commit)")
  @SneakyThrows
  void testRollbackFileDoesNotExists() {
    Mockito.doReturn(null).doReturn(null).when(jGitWrapper)
        .getTreeWalk(eq(repository), eq(FILE_PATH), any());

    Assertions.assertThatThrownBy(() -> jGitService.rollbackFile(REPO_NAME, FILE_PATH))
        .isInstanceOf(GitFileNotFoundException.class)
        .hasMessage(String.format("Rollback failed, file %s doesn't exist", FILE_PATH));
  }
}
