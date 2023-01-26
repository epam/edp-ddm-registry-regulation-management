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

import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.groups.model.GroupChangesDetails;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileStatus;
import com.epam.digital.data.platform.management.model.dto.DataModelFileType;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto.ChangedFileStatus;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("VersionManagementService#getVersionChanges(String)")
class VersionManagementServiceGetVersionChangesTest extends VersionManagementServiceBaseTest {

  @Test
  @DisplayName("should return an object with all found changes")
  @SneakyThrows
  void getVersionChangesTest() {
    final var changeId = RandomString.make();

    mockFormList(changeId);
    mockBpList(changeId);
    mockDataModelList(changeId);
    var group = GroupChangesDetails.builder()
        .name("bp-grouping.yml")
        .status(FileStatus.NEW)
        .build();
    Mockito.when(groupService.getChangesByVersion(changeId)).thenReturn(group);
    final var actualVersionChanges = managementService.getVersionChanges(changeId);

    Assertions.assertThat(actualVersionChanges).isNotNull();
    Assertions.assertThat(actualVersionChanges.getChangedForms())
        .containsAll(expectedFormChanges());
    Assertions.assertThat(actualVersionChanges.getChangedBusinessProcesses())
        .containsAll(expectedBpChanges());
    Assertions.assertThat(actualVersionChanges.getChangedDataModelFiles())
        .containsAll(expectedDataModelChanges());
    final var changedGroups = actualVersionChanges.getChangedGroups();
    Assertions.assertThat(changedGroups.get(0).getStatus().name()).isEqualTo(group.getStatus().name());
    Assertions.assertThat(changedGroups.get(0).getName()).isEqualTo(group.getName());

    Mockito.verify(formService).getChangedFormsListByVersion(changeId);
    Mockito.verify(businessProcessService).getChangedProcessesByVersion(changeId);
    Mockito.verify(dataModelService).listDataModelFiles(changeId);
    Mockito.verify(groupService).getChangesByVersion(changeId);
  }

  private static List<DataModelChangesInfoDto> expectedDataModelChanges() {
    return List.of(
        DataModelChangesInfoDto.builder()
            .name("newDataModelFile")
            .fileType(null)
            .status(DataModelChangesInfoDto.DataModelFileStatus.NEW)
            .build(),
        DataModelChangesInfoDto.builder()
            .name("createTables")
            .fileType(DataModelChangesInfoDto.DataModelFileType.TABLES_FILE)
            .status(DataModelChangesInfoDto.DataModelFileStatus.CHANGED)
            .build()
    );
  }

  private static List<EntityChangesInfoDto> expectedBpChanges() {
    return List.of(
        EntityChangesInfoDto.builder()
            .name("business-process")
            .title("Really test name")
            .status(ChangedFileStatus.NEW)
            .build(),
        EntityChangesInfoDto.builder()
            .name("changed-business-process")
            .title("Changed test name")
            .status(ChangedFileStatus.CHANGED)
            .build(),
        EntityChangesInfoDto.builder()
            .name("deleted-business-process")
            .title("Deleted test name")
            .status(ChangedFileStatus.DELETED)
            .build()
    );
  }

  private static List<EntityChangesInfoDto> expectedFormChanges() {
    return List.of(
        EntityChangesInfoDto.builder()
            .name("new_form")
            .status(ChangedFileStatus.NEW)
            .title("New Form")
            .build(),
        EntityChangesInfoDto.builder()
            .name("changed_form")
            .status(ChangedFileStatus.CHANGED)
            .title("Changed Form")
            .build(),
        EntityChangesInfoDto.builder()
            .name("deleted_form")
            .status(ChangedFileStatus.DELETED)
            .title("Deleted Form")
            .build()
    );
  }

  private void mockDataModelList(String changeId) {
    var dataModelChanges = List.of(
        DataModelFileDto.builder()
            .fileName("newDataModelFile")
            .type(null)
            .status(DataModelFileStatus.NEW)
            .build(),
        DataModelFileDto.builder()
            .fileName("currentDataModelFile")
            .type(null)
            .status(DataModelFileStatus.UNCHANGED)
            .build(),
        DataModelFileDto.builder()
            .fileName("createTables")
            .type(DataModelFileType.TABLES_FILE)
            .status(DataModelFileStatus.CHANGED)
            .build()
    );
    Mockito.doReturn(dataModelChanges).when(dataModelService).listDataModelFiles(changeId);
  }

  private void mockBpList(String changeId) {
    var bpList = List.of(
        BusinessProcessInfoDto.builder()
            .name("business-process")
            .title("Really test name")
            .status(FileStatus.NEW)
            .build(),
        BusinessProcessInfoDto.builder()
            .name("changed-business-process")
            .title("Changed test name")
            .status(FileStatus.CHANGED)
            .build(),
        BusinessProcessInfoDto.builder()
            .name("unchanged-business-process")
            .title("Unchanged test name")
            .status(FileStatus.UNCHANGED)
            .build(),
        BusinessProcessInfoDto.builder()
            .name("deleted-business-process")
            .title("Deleted test name")
            .status(FileStatus.DELETED)
            .build()
    );
    Mockito.doReturn(bpList).when(businessProcessService).getChangedProcessesByVersion(changeId);
  }

  private void mockFormList(String changeId) {
    var formList = List.of(
        FormInfoDto.builder()
            .name("new_form")
            .status(FileStatus.NEW)
            .title("New Form")
            .build(),
        FormInfoDto.builder()
            .name("changed_form")
            .status(FileStatus.CHANGED)
            .title("Changed Form")
            .build(),
        FormInfoDto.builder()
            .name("Current form")
            .status(FileStatus.UNCHANGED)
            .title("Current form")
            .build(),
        FormInfoDto.builder()
            .name("deleted_form")
            .status(FileStatus.DELETED)
            .title("Deleted Form")
            .build()
    );
    Mockito.doReturn(formList).when(formService).getChangedFormsListByVersion(changeId);
  }
}
