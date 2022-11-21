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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@DisplayName("JGitService#getFileContent")
class JGitServiceGetFileContentTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();

  File directory;

  @Mock
  Git git;
  @Mock
  Repository repository;

  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    directory = new File(tempDir, REPO_NAME);

    Assertions.assertThat(directory.mkdirs()).isTrue();
    Mockito.doReturn(git).when(jGitWrapper).open(directory);
    Mockito.doReturn(repository).when(git).getRepository();
  }

  @Test
  @DisplayName("should return found file content")
  @SneakyThrows
  void getFileContentTest() {
    var treeWalk = Mockito.mock(TreeWalk.class);

    var objectId = Mockito.mock(ObjectId.class);
    Mockito.doReturn(objectId).when(treeWalk).getObjectId(0);

    Mockito.doReturn(treeWalk).when(jGitWrapper).getTreeWalk(repository, FILE_PATH);

    var objectLoader = Mockito.mock(ObjectLoader.class);
    Mockito.doReturn(objectLoader).when(repository).open(objectId);

    var expectedFileContent = RandomString.make();
    Mockito.when(objectLoader.getCachedBytes()).thenReturn(expectedFileContent.getBytes());

    var actualFileContent = jGitService.getFileContent(REPO_NAME, FILE_PATH);
    Assertions.assertThat(actualFileContent)
        .isEqualTo(expectedFileContent);

    verifyMockInvocations();
    Mockito.verify(treeWalk).getObjectId(0);
    Mockito.verify(repository).open(objectId);
    Mockito.verify(objectLoader).getCachedBytes();
  }

  @Test
  @DisplayName("should return null if path isn't found in repository")
  @SneakyThrows
  void getFileContentTest_treeWalkNull() {
    Mockito.doReturn(null).when(jGitWrapper).getTreeWalk(repository, FILE_PATH);

    var actualFileContent = jGitService.getFileContent(REPO_NAME, FILE_PATH);
    Assertions.assertThat(actualFileContent)
        .isNull();

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw GitCommandException if IOException occurred")
  @SneakyThrows
  void getFileContentTest_ioException() {
    var treeWalk = Mockito.mock(TreeWalk.class);
    var objectId = Mockito.mock(ObjectId.class);
    Mockito.doReturn(objectId).when(treeWalk).getObjectId(0);

    Mockito.doReturn(treeWalk).when(jGitWrapper).getTreeWalk(repository, FILE_PATH);

    Mockito.doThrow(IOException.class).when(repository).open(objectId);

    Assertions.assertThatThrownBy(
            () -> jGitService.getFileContent(REPO_NAME, FILE_PATH))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during reading file content by path: ")
        .hasCauseInstanceOf(IOException.class);

    verifyMockInvocations();
    Mockito.verify(treeWalk).getObjectId(0);
    Mockito.verify(repository).open(objectId);
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException if path is empty")
  @SneakyThrows
  void getFileContentEmptyPathTest() {
    Mockito.doReturn(null).when(jGitWrapper).getTreeWalk(repository, FILE_PATH);

    Mockito.doCallRealMethod().when(jGitWrapper).getTreeWalk(repository, "");

    Assertions.assertThatThrownBy(() -> jGitService.getFileContent(REPO_NAME, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Empty path not permitted.");
  }

  @Test
  @DisplayName("Should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
  @SneakyThrows
  void getFileContentDirectoryNotExist() {
    final var repoName = RandomString.make();
    final var path = RandomString.make();

    Assertions.assertThatThrownBy(() -> jGitService.getFileContent(repoName, path))
        .isInstanceOf(RepositoryNotFoundException.class)
        .hasMessage("Repository %s doesn't exists", repoName)
        .hasNoCause();

    Mockito.verify(jGitWrapper, never()).open(eq(new File(path)));
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(jGitWrapper).open(directory);
    Mockito.verify(git).getRepository();
    Mockito.verify(jGitWrapper).getTreeWalk(repository, FILE_PATH);
  }
}