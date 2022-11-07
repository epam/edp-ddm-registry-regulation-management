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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@DisplayName("JGitService#getDates")
class JGitServiceGetFilesDatesTest extends AbstractJGitServiceTest {

  static final String REPOSITORY_NAME = RandomString.make();
  static final String FILE_PATH = RandomString.make();

  @Autowired
  CacheManager cacheManager;

  File repo;
  @Mock
  Git git;
  @Mock
  LogCommand logCommand;

  @BeforeEach
  @SneakyThrows
  @Override
  void setUp() {
    super.setUp();

    repo = new File(tempDir, REPOSITORY_NAME);
    Assertions.assertThat(repo.mkdirs()).isTrue();
    Mockito.doReturn(git).when(jGitWrapper).open(repo);
    Mockito.doReturn(logCommand).when(git).log();
    Mockito.doReturn(logCommand).when(logCommand).addPath(FILE_PATH);
  }

  @AfterEach
  void tearDown() {
    cacheManager.getCacheNames()
        .stream()
        .map(cacheManager::getCache)
        .filter(Objects::nonNull)
        .forEach(Cache::clear);
  }

  @Test
  @DisplayName("should return dto with dates for file if it exists")
  @SneakyThrows
  void getFormDatesTest() {
    final var lastCommitBuilder = new CommitBuilder();
    lastCommitBuilder.setTreeId(new ObjectId(1, 2, 3, 4, 5));
    lastCommitBuilder.setAuthor(new PersonIdent("committer1", "committer1@epam.com",
        LocalDateTime.of(2022, 11, 7, 11, 17).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    lastCommitBuilder.setCommitter(lastCommitBuilder.getAuthor());
    final var lastCommit = RevCommit.parse(lastCommitBuilder.build());

    final var firstCommitBuilder = new CommitBuilder();
    firstCommitBuilder.setTreeId(new ObjectId(6, 7, 8, 9, 0));
    firstCommitBuilder.setAuthor(new PersonIdent("committer2", "committer2@epam.com",
        LocalDateTime.of(2022, 10, 30, 17, 53).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    firstCommitBuilder.setCommitter(firstCommitBuilder.getAuthor());
    final var firstCommit = RevCommit.parse(firstCommitBuilder.build());

    Mockito.doReturn(List.of(lastCommit, firstCommit)).when(logCommand).call();

    final var actualDates = jGitService.getDates(REPOSITORY_NAME, FILE_PATH);

    Assertions.assertThat(actualDates)
        .isNotNull()
        .hasFieldOrPropertyWithValue("create", LocalDateTime.of(2022, 10, 30, 17, 53))
        .hasFieldOrPropertyWithValue("update", LocalDateTime.of(2022, 11, 7, 11, 17));

    final var cachedDates = jGitService.getDates(REPOSITORY_NAME, FILE_PATH);
    Assertions.assertThat(cachedDates)
        .isSameAs(actualDates);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should return null for file if it doesn't exist")
  @SneakyThrows
  void getFormDatesTest_noCommitsFound() {
    Mockito.doReturn(List.of()).when(logCommand).call();

    final var actualDates = jGitService.getDates(REPOSITORY_NAME, FILE_PATH);

    Assertions.assertThat(actualDates)
        .isNull();
    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw IllegalStateException if faced NoHeadException")
  @SneakyThrows
  void getFormDatesTest_noHeadException() {
    Mockito.doThrow(NoHeadException.class).when(logCommand).call();

    Assertions.assertThatThrownBy(
            () -> jGitService.getDates(REPOSITORY_NAME, FILE_PATH))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("HEAD reference doesn't exists")
        .hasCauseInstanceOf(NoHeadException.class);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw GitCommandException if faced unknown GitApiException")
  @SneakyThrows
  void getFormDatesTest_gitApiException() {
    final var ex = new GitAPIException("Unknown git api exception") {
    };
    Mockito.doThrow(ex).when(logCommand).call();

    Assertions.assertThatThrownBy(
            () -> jGitService.getDates(REPOSITORY_NAME, FILE_PATH))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Could not execute log command: Unknown git api exception")
        .hasCause(ex);

    verifyMockInvocations();
  }

  @Test
  @DisplayName("should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
  @SneakyThrows
  void getFormDatesTest_repoNotExist() {
    final var repoName = RandomString.make();
    final var filePath = RandomString.make();

    Assertions.assertThatThrownBy(() -> jGitService.getDates(repoName, filePath))
        .isInstanceOf(RepositoryNotFoundException.class)
        .hasMessage("Repository %s doesn't exists", repoName)
        .hasNoCause();

    Mockito.verify(jGitWrapper, never()).open(Mockito.any());
  }

  @SneakyThrows
  void verifyMockInvocations() {
    Mockito.verify(jGitWrapper).open(repo);
    Mockito.verify(git).log();
    Mockito.verify(logCommand).addPath(FILE_PATH);
    Mockito.verify(logCommand).call();
  }
}