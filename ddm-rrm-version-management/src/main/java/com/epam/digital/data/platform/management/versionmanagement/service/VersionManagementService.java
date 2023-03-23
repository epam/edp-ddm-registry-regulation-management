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

package com.epam.digital.data.platform.management.versionmanagement.service;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionChangesDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.validation.VersionCandidate;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * Service that is using to work with versions
 */
@Validated
public interface VersionManagementService {

  /**
   * Get versions list
   *
   * @return {@link List} of {@link VersionInfoDto}
   */
  List<VersionInfoShortDto> getVersionsList();


  /**
   * Get information about master version
   *
   * @return {@link VersionInfoDto}
   */
  @Nullable
  VersionInfoDto getMasterInfo();

  /**
   * Details of current version
   *
   * @param versionName version identifier
   * @return {@link List} of {@link VersionedFileInfoDto}
   */
  List<VersionedFileInfoDto> getVersionFileList(String versionName);

  /**
   * Create new version
   *
   * @param subject version creation info
   * @return version identifier
   */
  String createNewVersion(@VersionCandidate CreateChangeInputDto subject);

  /**
   * Get version details by version identifier
   *
   * @param versionName version identifier
   * @return {@link VersionInfoDto}
   */
  VersionInfoDto getVersionDetails(String versionName);

  /**
   * Decline version by version identifier
   *
   * @param versionName version identifier
   */
  void decline(String versionName);

  /**
   * Mark reviewed the version by identifier
   *
   * @param versionName version identifier
   * @return true if change is reviewed otherwise return false
   */
  boolean markReviewed(String versionName);

  /**
   * Submit version by identifier
   *
   * @param versionName version identifier
   */
  void submit(String versionName);

  /**
   * Updates version with changes from head branch
   *
   * @param versionName version identifier
   */
  void rebase(String versionName);

  /**
   * Get info about changes in version
   *
   * @param versionCandidateId version identifier
   * @return {@link VersionChangesDto}
   */
  VersionChangesDto getVersionChanges(String versionCandidateId);
//
//    /**
//     * Add robot comment
//     */
//    void robotComment(String versionName, String robotId, String robotRunId, String comment, String message, String filePath);
}
