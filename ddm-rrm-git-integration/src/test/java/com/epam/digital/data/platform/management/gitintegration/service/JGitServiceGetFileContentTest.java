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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("JGitService#getFileContent")
class JGitServiceGetFileContentTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();

  File file;

  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    File directory = new File(tempDir, REPO_NAME);
    Assertions.assertThat(directory.mkdirs()).isTrue();

    file = new File(directory, FILE_PATH);
    Assertions.assertThat(file.createNewFile()).isTrue();
  }

  @Test
  @DisplayName("should return found file content")
  @SneakyThrows
  void getFileContentTest() {
    var expectedFileContent = RandomString.make();
    Mockito.when(jGitWrapper.readFileContent(file.toPath())).thenReturn(expectedFileContent);

    var actualFileContent = jGitService.getFileContent(REPO_NAME, FILE_PATH);
    Assertions.assertThat(actualFileContent)
        .isEqualTo(expectedFileContent);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should return null if path isn't found in repository")
  @SneakyThrows
  void getFileContentTest_treeWalkNull() {
    Mockito.doReturn(null).when(jGitWrapper).readFileContent(file.toPath());

    var actualFileContent = jGitService.getFileContent(REPO_NAME, FILE_PATH);
    Assertions.assertThat(actualFileContent)
        .isNull();

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw GitCommandException if IOException occurred")
  @SneakyThrows
  void getFileContentTest_ioException() {
    Mockito.doThrow(IOException.class).when(jGitWrapper).readFileContent(file.toPath());

    Assertions.assertThatThrownBy(
            () -> jGitService.getFileContent(REPO_NAME, FILE_PATH))
        .isInstanceOf(GitCommandException.class)
        .hasMessageContaining("Exception occurred during reading file content by path: ")
        .hasCauseInstanceOf(IOException.class);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException if path is empty")
  @SneakyThrows
  void getFileContentEmptyPathTest() {
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
    Mockito.verify(jGitWrapper).readFileContent(file.toPath());
  }
}