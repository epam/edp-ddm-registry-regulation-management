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

import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;

import lombok.SneakyThrows;

@DisplayName("JGitService#getConflicts")
class JGitServiceGetConflictsTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String USER = RandomString.make();
  static final String PASSWORD = RandomString.make();
  static final String REFS = RandomString.make();

  File repo;
  @Mock Git git;
  @Mock Repository repository;
  @Mock ObjectId objectId;
  @Mock MergeCommand mergeCommand;
  @Mock MergeResult mergeResult;
  @Mock ResetCommand resetCommand;
  @Mock
  FetchCommand fetchCommand;

  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    repo = new File(tempDir, REPO_NAME);
    Assertions.assertThat(repo.mkdirs()).isTrue();
    Mockito.doReturn(git).when(jGitWrapper).open(repo);
    Mockito.doReturn(repository).when(git).getRepository();
    Mockito.doReturn(objectId).when(repository).resolve(anyString());
    Mockito.doReturn(mergeCommand).when(git).merge();
    Mockito.doReturn(mergeCommand).when(mergeCommand).include(objectId);
    Mockito.doReturn(mergeCommand).when(mergeCommand).setCommit(false);
    Mockito.doReturn(mergeCommand)
        .when(mergeCommand)
        .setFastForward(MergeCommand.FastForwardMode.NO_FF);
    Mockito.doReturn(mergeResult).when(mergeCommand).call();
    Mockito.doReturn(resetCommand).when(git).reset();
    Mockito.doReturn(resetCommand).when(resetCommand).setMode(ResetCommand.ResetType.HARD);

    Mockito.doReturn(USER).when(gerritPropertiesConfig).getUser();
    Mockito.doReturn(PASSWORD).when(gerritPropertiesConfig).getPassword();

    Mockito.doReturn(fetchCommand).when(git).fetch();
    Mockito.doReturn(fetchCommand).when(fetchCommand).setRefSpecs(REFS);
    Mockito.doReturn(fetchCommand)
            .when(fetchCommand).setCredentialsProvider(
                    Mockito.refEq(new UsernamePasswordCredentialsProvider(USER, PASSWORD)));
  }

  @Test
  @DisplayName("should return conflict file names")
  void getConflictsTest() {
    Map<String, int[][]> conflictMap = new HashMap<>();
    String key = "aa/bb";
    conflictMap.put(key, null);
    Mockito.when(mergeResult.getConflicts()).thenReturn(conflictMap);

    var actualFileContent = jGitService.getConflicts(REPO_NAME);
    Assertions.assertThat(actualFileContent.get(0)).isEqualTo(key);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should return empty list")
  void getConflictsTest_conflictsNull() {
    Mockito.when(mergeResult.getConflicts()).thenReturn(null);

    var actualFileContent = jGitService.getConflicts(REPO_NAME);
    Assertions.assertThat(actualFileContent).isEqualTo(Collections.emptyList());

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw GitCommandException if GitAPIException occurred")
  @SneakyThrows
  void getConflictsTest_GitAPIException() {
    Mockito.doThrow(NoHeadException.class).when(mergeCommand).call();

    Assertions.assertThatThrownBy(() -> jGitService.getConflicts(REPO_NAME))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during getting conflicts")
        .hasCauseInstanceOf(NoHeadException.class);
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(mergeCommand).call();
    Mockito.verify(resetCommand).call();
  }
}
