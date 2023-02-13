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

import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("DataModelFileService#getTablesFileContent(String)")
class DataModelFileManagementServiceGetTablesFileContentTest extends
    DataModelFileManagementServiceBaseTest {

  @Test
  @DisplayName("should return tables file content if tables file exists")
  void getTableFileContent_happyPath() {
    final var versionId = RandomString.make();
    final var expectedFileContent = RandomString.make();

    Mockito.doReturn(true).when(versionedFileRepository).isFileExists(TABLES_FILE_PATH);
    Mockito.doReturn(expectedFileContent).when(versionedFileRepository).readFile(TABLES_FILE_PATH);

    final var actualFileContent = dataModelFileManagementService.getTablesFileContent(versionId);

    Assertions.assertThat(actualFileContent)
        .isSameAs(expectedFileContent);

    Mockito.verify(versionContextComponentManager)
        .getComponent(versionId, VersionedFileRepository.class);
    Mockito.verify(versionedFileRepository).isFileExists(TABLES_FILE_PATH);
    Mockito.verify(versionedFileRepository).readFile(TABLES_FILE_PATH);
  }

  @Test
  @DisplayName("should throw DataModelFileNotFoundInVersionException if tables file doesn't exist")
  void getTableFileContent_noFileFound() {
    final var versionId = RandomString.make();

    Mockito.doReturn(false).when(versionedFileRepository).isFileExists(TABLES_FILE_PATH);

    Assertions.assertThatThrownBy(() -> dataModelFileManagementService.getTablesFileContent(versionId))
        .isInstanceOf(DataModelFileNotFoundInVersionException.class)
        .hasMessage("Data-model file %s is not found in version %s", TABLES_FILE_PATH, versionId);

    Mockito.verify(versionContextComponentManager)
        .getComponent(versionId, VersionedFileRepository.class);
    Mockito.verify(versionedFileRepository).isFileExists(TABLES_FILE_PATH);
    Mockito.verify(versionedFileRepository, Mockito.never()).readFile(Mockito.anyString());
  }

  @Test
  @DisplayName("should throw NullPointerException if tables file exists but content is null")
  void getTableFileContent_contentIsNull() {
    final var versionId = RandomString.make();

    Mockito.doReturn(true).when(versionedFileRepository).isFileExists(TABLES_FILE_PATH);
    Mockito.doReturn(null).when(versionedFileRepository).readFile(TABLES_FILE_PATH);

    Assertions.assertThatThrownBy(() -> dataModelFileManagementService.getTablesFileContent(versionId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("File content cannot be null");

    Mockito.verify(versionContextComponentManager)
        .getComponent(versionId, VersionedFileRepository.class);
    Mockito.verify(versionedFileRepository).isFileExists(TABLES_FILE_PATH);
    Mockito.verify(versionedFileRepository).readFile(TABLES_FILE_PATH);
  }
}
