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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.validation.DDMExtensionChangelogFile;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

/**
 * Service that provides methods for accessing data-model files in different versions
 */
@Validated
public interface DataModelFileManagementService {

  /**
   * Returns content of file that contains tables changes declaration. File path has to be
   * configured as {@code registry-regulation-management.data-model.tables-file-path} property
   *
   * @param versionId id of a version to search file content in
   * @return the content of file (cannot be null)
   *
   * @throws DataModelFileNotFoundInVersionException in case if file doesn't exist
   */
  @NonNull
  String getTablesFileContent(@NonNull String versionId);

  /**
   * Creates or updates file that contains tables changes declaration. File path has to be
   * configured as {@code registry-regulation-management.data-model.tables-file-path} property
   *
   * @param versionId   id of a version to update file content in
   * @param fileContent the new file content of the file
   */
  void putTablesFileContent(@NonNull String versionId,
      @NonNull @DDMExtensionChangelogFile String fileContent);
}
