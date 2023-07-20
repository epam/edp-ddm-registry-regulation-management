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

import java.io.File;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#commitAndSubmit")
class JGitServiceCommitAndSubmitTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();
  static final String FILE_CONTENT = RandomString.make();

  static final String REPO_URL = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASS = RandomString.make();
  static final String HEAD_BRANCH = RandomString.make();
  static final String COMMIT_MESSAGE = "created file ";
  static final String PRIVATE_SUBMIT = "%private,submit";

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

    Mockito.doReturn(commitCommand).when(git).commit();
    Mockito.doReturn(commitCommand).when(commitCommand).setMessage(COMMIT_MESSAGE + FILE_PATH);
    Mockito.doReturn(commitCommand).when(commitCommand).setInsertChangeId(true);

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
        .setRefSpecs(Mockito.refEq(new RefSpec("HEAD:refs/for/" + HEAD_BRANCH + PRIVATE_SUBMIT)));
  }

  @Test
  @DisplayName("should 'git add' file, find last commit, commit, add remote and push with submit parameter")
  @SneakyThrows
  void testAmend() {
    jGitService.commitAndSubmit(REPO_NAME, FILE_PATH, FILE_CONTENT);

    Assertions.assertThat(Path.of(repoDir.getPath(), FILE_PATH))
        .exists()
        .content().isEqualTo(FILE_CONTENT);

    Mockito.verify(git).add();
    Mockito.verify(addCommand).addFilepattern(FILE_PATH);
    Mockito.verify(addCommand).call();

    Mockito.verify(git).status();
    Mockito.verify(status).isClean();
    Mockito.verify(statusCommand).call();

    Mockito.verify(git).commit();
    Mockito.verify(commitCommand).setMessage(COMMIT_MESSAGE + FILE_PATH);
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
        .setRefSpecs(Mockito.refEq(new RefSpec("HEAD:refs/for/" + HEAD_BRANCH + PRIVATE_SUBMIT)));
    Mockito.verify(pushCommand).call();
  }
}
