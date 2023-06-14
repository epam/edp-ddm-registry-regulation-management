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
package com.epam.digital.data.platform.management.groups.service;

import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.groups.TestUtils;
import com.epam.digital.data.platform.management.groups.exception.GroupsParseException;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessDefinition;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupDetails;
import com.epam.digital.data.platform.management.groups.model.GroupDetailsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class GroupServiceTest {

  private static final String VERSION_ID = "version";
  private static final String GROUPS_PATH = "bp-grouping/bp-grouping.yml";
  private static final String BP_GROUPING_CONTENT = TestUtils.getContent("bp-grouping-test.yml");
  private static final String INCORRECT_BP_GROUPING_CONTENT = TestUtils.getContent(
      "bp-grouping-test-incorrect.yml");
  @Mock
  private VersionContextComponentManager versionContextComponentManager;
  @Mock
  private VersionedFileRepository repository;
  @Mock
  private BusinessProcessService businessProcessService;
  @Mock
  CacheService cacheService;

  @InjectMocks
  private GroupServiceImpl groupService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    Mockito.when(
            versionContextComponentManager.getComponent(VERSION_ID, VersionedFileRepository.class))
        .thenReturn(repository);
  }

  @Test
  void getGroupsTest() {
    Mockito.when(repository.readFile(GROUPS_PATH))
        .thenReturn(BP_GROUPING_CONTENT);
    final var expectedResponse = BusinessProcessInfoDto.builder()
        .path("/bpmn/John_Does_process.bpmn")
        .name("bp-4-process_definition_id")
        .title("John Doe added new component")
        .created(LocalDateTime.of(2022, 11, 3, 11, 45))
        .updated(LocalDateTime.of(2022, 11, 4, 13, 16))
        .build();

    Mockito.doReturn(List.of(expectedResponse))
        .when(businessProcessService).getProcessesByVersion(VERSION_ID);
    final var groupsByVersion = groupService.getGroupsByVersion(VERSION_ID);

    final var expected = BusinessProcessGroupsResponse.builder()
        .groups(List.of(GroupDetailsResponse.builder()
                .name("Перша група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder()
                .name("Друга група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder().name("Третя група").processDefinitions(new ArrayList<>())
                .build()))
        .ungrouped(List.of(
            BusinessProcessDefinition.builder().id("bp-4-process_definition_id").name("John Doe added new component").build()))
        .build();
    Assertions.assertThat(groupsByVersion).isEqualTo(expected);
  }

  private static GroupListDetails getGroupListDetails() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").processDefinitions(new ArrayList<>()).build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  @Test
  void shouldThrowException() {
    Mockito.when(repository.readFile(GROUPS_PATH)).thenReturn(INCORRECT_BP_GROUPING_CONTENT);
    Assertions.assertThatCode(() -> groupService.getGroupsByVersion(VERSION_ID))
        .isInstanceOf(GroupsParseException.class)
        .hasMessage("Could not process bp-grouping file");
  }

  @Test
  @SneakyThrows
  void saveSuccessTest() {
    Mockito.doNothing().when(repository).writeFile(GROUPS_PATH, BP_GROUPING_CONTENT);
    Assertions.assertThatCode(() -> groupService.save(VERSION_ID, getGroupListDetails()))
        .doesNotThrowAnyException();

    Mockito.verify(versionContextComponentManager)
        .getComponent(VERSION_ID, VersionedFileRepository.class);
    var mapper = new YAMLMapper(new YAMLFactory()).disable(
        YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    Mockito.verify(repository)
        .writeFile(GROUPS_PATH, mapper.writeValueAsString(getGroupListDetails()));
  }

  @Test
  @SneakyThrows
  void getChangesTest() {
    var fileInfo = VersionedFileInfoDto.builder()
        .name("bp-grouping")
        .path("bp-grouping/bp-grouping.yml")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    Mockito.when(repository.getFileList(GROUPS_PATH)).thenReturn(List.of(fileInfo));
    Mockito.when(cacheService.getConflictsCache(VERSION_ID)).thenReturn(List.of("bp-grouping/bp-grouping.yml"));

    final var changesByVersion = groupService.getChangesByVersion(VERSION_ID);

    Assertions.assertThat(changesByVersion).isNotNull();
    Assertions.assertThat(changesByVersion.getName()).isEqualTo("bp-grouping.yml");
    Assertions.assertThat(changesByVersion.getStatus()).isEqualTo(FileStatus.NEW);
    Assertions.assertThat(changesByVersion.isConflicted()).isTrue();
  }

  @Test
  @SneakyThrows
  void getChangesCurrentTest() {
    var fileInfo = VersionedFileInfoDto.builder()
        .name("no-grouping")
        .build();
    Mockito.when(repository.getFileList(GROUPS_PATH)).thenReturn(List.of(fileInfo));

    final var changesByVersion = groupService.getChangesByVersion(VERSION_ID);

    Assertions.assertThat(changesByVersion).isNull();
  }

  @Test
  @SneakyThrows
  void deleteProcessDefinitionTest() {
    Mockito.when(repository.readFile(GROUPS_PATH)).thenReturn(BP_GROUPING_CONTENT);
    final var processDefId = "bp-1-process_definition_id";
    groupService.deleteProcessDefinition(processDefId, VERSION_ID);

    var mapper = new YAMLMapper(new YAMLFactory()).disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    Mockito.verify(repository)
        .writeFile(GROUPS_PATH, mapper.writeValueAsString(GroupListDetails.builder()
            .groups(List.of(GroupDetails.builder().name("Перша група")
                    .processDefinitions(List.of("bp-2-process_definition_id"))
                    .build(),
                GroupDetails.builder().name("Друга група")
                    .processDefinitions(List.of("bp-3-process_definition_id")).build(),
                GroupDetails.builder().name("Третя група").processDefinitions(new ArrayList<>()).build()))
            .ungrouped(List.of("bp-4-process_definition_id")).build()));
  }

  @Test
  @SneakyThrows
  void rollbackBusinessProcessGroupsTest() {
    groupService.rollbackBusinessProcessGroups(VERSION_ID);

    Mockito.verify(versionContextComponentManager)
        .getComponent(VERSION_ID, VersionedFileRepository.class);
    Mockito.verify(repository).rollbackFile(GROUPS_PATH);
  }
}
