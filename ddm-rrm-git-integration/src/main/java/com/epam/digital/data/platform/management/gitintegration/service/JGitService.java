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

import com.epam.digital.data.platform.management.gitintegration.exception.GitFileNotFoundException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;

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
   * @throws GitCommandException         in case if it couldn't open repo or fetch or reset git
   *                                     command failures
   * @throws RepositoryNotFoundException in case if head-branch repository doesn't exist
   */
  void resetHeadBranchToRemote();

  /**
   * Fetches and checkouts repository for specified version to remote state
   *
   * @param repositoryName name of the specified repository
   * @param refs           ref info
   * @throws GitCommandException         in case if it couldn't open repo or fetch or checkout git
   *                                     command failures
   * @throws RepositoryNotFoundException in case if repository doesn't exist
   */
  void fetch(@NonNull String repositoryName, @NonNull String refs);

  /**
   * Returns list files by path from repository
   *
   * @param repositoryName name of the specified repository
   * @param path           non-empty file location
   * @return {@link List} of {@link String} with files' names
   *
   * @throws GitCommandException         in case if it couldn't open repo or facing IOException
   * @throws RepositoryNotFoundException in case if repository doesn't exist
   * @throws IllegalArgumentException    if path is empty
   */
  @NonNull
  List<String> getFilesInPath(@NonNull String repositoryName, @NonNull String path);

  /**
   * Returns list of conflict file names by repository name
   *
   * @param repositoryName name of the specified repository
   * @return {@link List} of {@link String} with files' names
   */
  @NonNull
  List<String> getConflicts(@NonNull String repositoryName);

  /**
   * Get creation and update date of file from git log
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location
   * @return {@link FileDatesDto dates information} or null if file in path doesn't exist
   *
   * @throws GitCommandException         in case if it couldn't open repo or git log command
   *                                     failure
   * @throws RepositoryNotFoundException in case if repository doesn't exist
   */
  @Nullable
  FileDatesDto getDates(@NonNull String repositoryName, @NonNull String filePath);

  /**
   * Returns file content by path from repository
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location
   * @return {@link String} content of file
   *
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or facing IOException
   */
  @Nullable
  String getFileContent(@NonNull String repositoryName, @NonNull String filePath);

  /**
   * Amend commit with file and push to refs for head-branch. It requires that repository already is
   * checkout on FETCH_HEAD for successful push to repo
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location on FileSystem
   * @param fileContent    content of file
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or add, log, commit,
   *                                     remote add or push git command failures
   */
  void amend(@NonNull String repositoryName, @NonNull String filePath, @NonNull String fileContent);

  /**
   * Delete file and push to refs for head-branch. It requires that repository already is checkout
   * on FETCH_HEAD for successful push to repo
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location on FileSystem
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or rm, log, commit, remote
   *                                     add or push git command failures
   */
  void delete(@NonNull String repositoryName, @NonNull String filePath);

  /**
   * Delete repository from FileSystem
   *
   * @param repositoryName name of the specified repository
   * @throws GitCommandException in case of repository deletion errors
   */
  void deleteRepo(String repositoryName);

  /**
   * Checks if repository exists on FileSystem
   *
   * @param repositoryName name of the specified repository
   * @return true if repo exists and false otherwise
   */
  boolean repoExists(String repositoryName);

  /**
   * Commit file and push to refs for head-branch. It requires that repository already is
   * checkout on FETCH_HEAD for successful push to repo
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location on FileSystem
   * @param fileContent    content of file
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitCommandException         in case if it couldn't open repo or add, log, commit,
   *                                     remote add or push git command failures
   */
  void commitAndSubmit(@NonNull String repositoryName, @NonNull String filePath, @NonNull String fileContent);

  /**
   * Revert the modified file to the state of the commit from which the branch was created. It
   * requires that repository already is checkout on FETCH_HEAD for successful push to repo
   *
   * @param repositoryName name of the specified repository
   * @param filePath       file location on FileSystem
   * @throws RepositoryNotFoundException if repository not exists
   * @throws GitFileNotFoundException    in case when attempting to access a file in a Git
   *                                     repository that does not exist
   * @throws GitCommandException         in case if it couldn't open repo or add, log, commit,
   *                                     checkout, remote add or push git command failures
   */
  void rollbackFile(@NonNull String repositoryName, @NonNull String filePath);
}
