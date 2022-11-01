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

import com.epam.digital.data.platform.management.exception.ReadingRepositoryException;
import com.epam.digital.data.platform.management.exception.SettingsProcessingException;
import com.epam.digital.data.platform.management.exception.WritingRepositoryException;
import com.epam.digital.data.platform.management.model.dto.GlobalSettingsInfo;

public interface GlobalSettingService {

  /**
   * Return global settings for certain version
   * @param versionCandidateId version we need to get settings from
   * @return settings for specified version
   * @throws ReadingRepositoryException when could not read settings files
   * @throws SettingsProcessingException when settings files has invalid structure
   */
  GlobalSettingsInfo getGlobalSettings(String versionCandidateId);

  /**
   * Update global settings for certain version
   * @param versionCandidateId version we need to update settings for
   * @param settings new settings to be updated
   * @throws ReadingRepositoryException when could not read settings files
   * @throws SettingsProcessingException when settings files has invalid structure
   * @throws WritingRepositoryException when could not write settings files
   */
  void updateSettings(String versionCandidateId, GlobalSettingsInfo settings);

}
