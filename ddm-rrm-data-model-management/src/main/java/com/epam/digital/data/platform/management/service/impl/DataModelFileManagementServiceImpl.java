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

import com.epam.digital.data.platform.management.constant.DataModelManagementConstants;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.mapper.DataModelFileManagementMapper;
import com.epam.digital.data.platform.management.model.dto.DataModelFileDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileType;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataModelFileManagementServiceImpl implements DataModelFileManagementService {

  private final VersionContextComponentManager versionContextComponentManager;
  private final DataModelFileManagementMapper mapper;
  private final CacheService cacheService;

  @Override
  @NonNull
  public String getTablesFileContent(@NonNull String versionId) {
    var tablesFilePath = getTablesFilePath();
    log.debug("Reading content from file '{}' for version '{}'", tablesFilePath, versionId);

    var repo = getVersionedFileRepo(versionId);

    log.trace("Checking if file '{}' exists in version '{}' repository", tablesFilePath, versionId);
    if (!repo.isFileExists(tablesFilePath)) {
      throw new DataModelFileNotFoundInVersionException(tablesFilePath, versionId);
    }

    log.trace(
        "File '{}' exists in version '{}' repository. Reading content...",
        tablesFilePath,
        versionId);
    var tableFileContent =
        Objects.requireNonNull(repo.readFile(tablesFilePath), "File content cannot be null");

    log.debug(
        "File '{}' content was read for version '{}', content length - '{}'",
        tablesFilePath,
        versionId,
        tableFileContent.length());
    return tableFileContent;
  }

  @Override
  public void putTablesFileContent(@NonNull String versionId, @NonNull String fileContent) {
    var tablesFilePath = getTablesFilePath();
    log.debug("Putting content to file '{}' for version '{}'", tablesFilePath, versionId);

    var repo = getVersionedFileRepo(versionId);

    repo.writeFile(tablesFilePath, fileContent);
    log.debug(
        "File '{}' content was updated in version '{}', new content length - '{}'",
        tablesFilePath,
        versionId,
        fileContent.length());
  }

  @Override
  @NonNull
  public List<DataModelFileDto> listDataModelFiles(@NonNull String versionId) {
    log.debug("Getting list of data-model files for version '{}'", versionId);

    var repo = getVersionedFileRepo(versionId);
    var foundFiles = repo.getFileList(DataModelManagementConstants.DATA_MODEL_FOLDER);

    log.debug("Found '{}' files in version '{}'", foundFiles.size(), versionId);

    List<DataModelFileDto> dataModels = new ArrayList<>();
    
    List<String> conflicts = cacheService.getConflictsCache(versionId);
    for (VersionedFileInfoDto versionedFileInfoDto : foundFiles) {
      dataModels.add(
          mapper.toChangedDataModelFileDto(
              versionedFileInfoDto, conflicts.contains(versionedFileInfoDto.getPath())));
    }
    return dataModels;
  }

  @Override
  public void rollbackTables(@NonNull String versionId) {
    var repo = getVersionedFileRepo(versionId);
    repo.rollbackFile(getTablesFilePath());
  }

  private String getTablesFilePath() {
    return getDataModelFilePath(DataModelFileType.TABLES_FILE.getFileName());
  }

  private String getDataModelFilePath(String fileName) {
    return FilenameUtils.normalize(
        String.format("%s/%s.xml", DataModelManagementConstants.DATA_MODEL_FOLDER, fileName));
  }

  private VersionedFileRepository getVersionedFileRepo(String versionId) {
    return versionContextComponentManager.getComponent(versionId, VersionedFileRepository.class);
  }
}
