/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * Provides methods for working with git service.
 */
public interface JGitService {

  /**
   * Clone repository by version if folder is not exists yet
   *
   * @param repositoryName repository identifier
   * @throws GitCommandException in case of clone repository failure
   */
  void cloneRepoIfNotExist(@NonNull String repositoryName);

  /**
   * Fetches and resets repository for main branch to origin state
   *
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  void resetHeadBranchToRemote();

  /**
   * Fetches and checkouts repository for specified version to remote state
   *
   * @param repositoryName name of the specified repository
   * @param refs           ref info
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  void fetch(@NonNull String repositoryName, @NonNull String refs);

  /**
   * Returns list files by path from repository
   *
   * @param repositoryName name of the specified repository
   * @param path           file location
   * @return {@link List} of {@link String} with files' names
   *
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  List<String> getFilesInPath(String repositoryName, String path);

  /**
   * Get creation and update date of file from git log
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location
   * @return {@link FileDatesDto dates information}
   */
  FileDatesDto getDates(String repositoryName, String filePath);

  /**
   * Returns file content by path from repository
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location
   * @return {@link String} content of file
   *
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or fetch or reset git
   *                                     command failures
   */
  String getFileContent(String repositoryName, String filePath);

  /**
   * Amend commit with file
   *
   * @param repositoryName name of the specified repository
   * @param refs           ref info
   * @param commitMessage  commit message
   * @param changeId       identifier of commit
   * @param filePath       file location on FileSystem
   * @param fileContent    content of file
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or fetch or reset git
   *                                     command failures
   */
  void amend(String repositoryName, String refs, String commitMessage, String changeId,
      String filePath, String fileContent);

  /**
   * Delete file in current gerrit change
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location on FileSystem
   * @param refs           ref info
   * @param commitMessage  commit message
   * @param changeId       identifier of commit
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or fetch or reset git
   *                                     command failures
   */
  void delete(String repositoryName, String filePath, String refs, String commitMessage,
      String changeId);

  /**
   * Delete repository from FileSystem
   *
   * @param repositoryName name of the specified repository
   * @throws GitCommandException in case of repository deletion errors
   */
  void deleteRepo(String repositoryName);
}
