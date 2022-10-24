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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.GitCommandException;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * Provides methods for working with git service.
 */
public interface JGitService {

  /**
   * Clone repository by version
   *
   * @param versionName version identifier
   * @throws GitCommandException in case of clone repository failure
   */
  void cloneRepo(String versionName);

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
   * @param versionName   name of the specified version
   * @param changeInfoDto dto with ref info
   * @throws GitCommandException in case if it couldn't open repo or fetch or reset git command
   *                             failures
   */
  void fetch(@NonNull String versionName, @NonNull ChangeInfoDto changeInfoDto);

  List<String> getFilesInPath(String versionName, String path) throws Exception;

  FileDatesDto getDates(String versionName, String filePath);

  String getFileContent(String versionName, String filePath) throws Exception;

  void amend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception;

  void delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception;

  void deleteRepo(String repoName) throws IOException;
}
