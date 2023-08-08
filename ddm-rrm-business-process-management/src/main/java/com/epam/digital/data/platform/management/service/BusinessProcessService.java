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

import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import java.util.List;

public interface BusinessProcessService {

  List<BusinessProcessInfoDto> getProcessesByVersion(String versionName);

  List<BusinessProcessInfoDto> getChangedProcessesByVersion(String versionName);

  void createProcess(String processName, String content, String versionName);

  String getProcessContent(String processName, String versionName);

  void updateProcess(String content, String processName, String versionName, String eTag);

  /**
   * Rolls back a business process to a specific version.
   *
   * @param processName name of the business process to be rolled back
   * @param versionName name of version candidate
   */
  void rollbackProcess(String processName, String versionName);

  /**
   * Delete business process with eTag validation
   *
   * @param processName    name of business process
   * @param versionName name of version candidate
   * @param eTag entity tag
   */
  void deleteProcess(String processName, String versionName, String eTag);
}
