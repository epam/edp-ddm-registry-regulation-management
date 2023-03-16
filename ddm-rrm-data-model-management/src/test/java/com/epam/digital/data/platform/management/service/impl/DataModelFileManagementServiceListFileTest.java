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


import static com.epam.digital.data.platform.management.constant.DataModelManagementConstants.DATA_MODEL_FOLDER;

import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.model.dto.DataModelFileDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileStatus;
import com.epam.digital.data.platform.management.model.dto.DataModelFileType;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("DataModelFileService#listDataModelFiles(String)")
class DataModelFileManagementServiceListFileTest extends DataModelFileManagementServiceBaseTest {

  @Test
  void listDataModelFiles_happyPath() {
    final var versionId = "42";

    Mockito.doReturn(List.of(
        VersionedFileInfoDto.builder()
            .name("unchanged_file_name")
            .path("data-model/unchanged_file_name.xml")
            .status(FileStatus.UNCHANGED)
            .build(),
        VersionedFileInfoDto.builder()
            .name("new_file_name")
            .path("data-model/new_file_name.xml")
            .status(FileStatus.NEW)
            .build(),
        VersionedFileInfoDto.builder()
            .name(DataModelFileType.TABLES_FILE.getFileName())
            .path(TABLES_FILE_PATH)
            .status(FileStatus.CHANGED)
            .build()
    )).when(versionedFileRepository).getFileList(DATA_MODEL_FOLDER);

    var expectedDataModelFiles = List.of(
        DataModelFileDto.builder()
            .fileName(DataModelFileType.TABLES_FILE.getFileName())
            .status(DataModelFileStatus.CHANGED)
            .type(DataModelFileType.TABLES_FILE)
            .build(),
        DataModelFileDto.builder()
            .fileName("new_file_name")
            .status(DataModelFileStatus.NEW)
            .type(null)
            .build(),
        DataModelFileDto.builder()
            .fileName("unchanged_file_name")
            .status(DataModelFileStatus.UNCHANGED)
            .type(null)
            .build()
    );

    var actualDataModelFiles = dataModelFileManagementService.listDataModelFiles(versionId);
    Assertions.assertThat(actualDataModelFiles).containsAll(expectedDataModelFiles);

    Mockito.verify(versionContextComponentManager)
        .getComponent(versionId, VersionedFileRepository.class);
    Mockito.verify(versionedFileRepository).getFileList(DATA_MODEL_FOLDER);
  }
}
