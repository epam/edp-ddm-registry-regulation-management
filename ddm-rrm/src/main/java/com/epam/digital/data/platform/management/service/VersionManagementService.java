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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.Nullable;

public interface VersionManagementService {

  /**
   * Get versions list
   */
  List<ChangeInfoDetailedDto> getVersionsList();


  @Nullable
  ChangeInfoDetailedDto getMasterInfo();

  /**
   * Details of head master
   */
  List<String> getDetailsOfHeadMaster(String path) throws IOException;

  /**
   * Details of current version
   */
  List<VersionedFileInfo> getVersionFileList(String versionName);

  /**
   * Create new version
   */
  String createNewVersion(CreateVersionRequest subject);

  ChangeInfoDetailedDto getVersionDetails(String versionName);

  /**
   * Decline version by name
   */
  void decline(String versionName);

  /**
   * Mark reviewed the version
   */
  boolean markReviewed(String versionName);

  /**
   * Submit version by name
   */
  void submit(String versionName);

  /**
   * Updates version with changes from head branch
   *
   * @param versionName version to update
   */
  void rebase(String versionName);

  VersionChanges getVersionChanges(String versionCandidateId);
//
//    /**
//     * Add robot comment
//     */
//    void robotComment(String versionName, String robotId, String robotRunId, String comment, String message, String filePath);
}
