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
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JGitService#repoExists")
class JGitServiceRepoExistsTest extends AbstractJGitServiceTest {

  static final String REPO_NAME = RandomString.make();

  File repoDir;

  @Override
  @BeforeEach
  @SneakyThrows
  void setUp() {
    super.setUp();
    repoDir = new File(tempDir, REPO_NAME);
    Assertions.assertThat(repoDir.mkdirs()).isTrue();
  }

  @Test
  @DisplayName("should return true if directory exists")
  @SneakyThrows
  void testRepoExists() {
    Assertions.assertThat(repoDir).exists();

    Assertions.assertThat(jGitService.repoExists(REPO_NAME)).isTrue();
  }

  @Test
  @DisplayName("should return false if directory doesn't exist")
  @SneakyThrows
  void testRepoExists_false() {
    FileUtils.forceDelete(repoDir);
    Assertions.assertThat(repoDir).doesNotExist();

    Assertions.assertThat(jGitService.repoExists(REPO_NAME)).isFalse();
  }
}
