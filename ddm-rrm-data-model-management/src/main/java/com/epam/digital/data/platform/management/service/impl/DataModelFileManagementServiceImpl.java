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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.DataModelConfigurationProperties;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataModelFileManagementServiceImpl implements DataModelFileManagementService {

  private final DataModelConfigurationProperties dataModelProps;
  private final VersionContextComponentManager versionContextComponentManager;

  @Override
  @NonNull
  public String getTablesFileContent(@NonNull String versionId) {
    var tablesFilePath = dataModelProps.getTablesFilePath();
    log.debug("Reading content from file '{}' for version '{}'", tablesFilePath, versionId);

    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);

    log.trace("Checking if file '{}' exists in version '{}' repository", tablesFilePath, versionId);
    if (!repo.isFileExists(tablesFilePath)) {
      throw new DataModelFileNotFoundInVersionException(tablesFilePath, versionId);
    }

    log.trace("File '{}' exists in version '{}' repository. Reading content...", tablesFilePath,
        versionId);
    var tableFileContent = Objects.requireNonNull(repo.readFile(tablesFilePath),
        "File content cannot be null");

    log.debug("File '{}' content was read for version '{}', content length - '{}'", tablesFilePath,
        versionId, tableFileContent.length());
    return tableFileContent;
  }

  @Override
  public void putTablesFileContent(@NonNull String versionId, @NonNull String fileContent) {
    var tablesFilePath = dataModelProps.getTablesFilePath();
    log.debug("Putting content to file '{}' for version '{}'", tablesFilePath, versionId);

    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);

    repo.writeFile(tablesFilePath, fileContent);
    log.debug("File '{}' content was updated in version '{}', new content length - '{}'",
        tablesFilePath, versionId, fileContent.length());
  }
}
