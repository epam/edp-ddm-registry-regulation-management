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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.springframework.util.StreamUtils.copyToString;

import com.epam.digital.data.platform.management.config.XmlParserConfig;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExistsException;
import com.epam.digital.data.platform.management.exception.ProcessNotFoundException;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.mapper.BusinessProcessMapper;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.service.impl.BusinessProcessServiceImpl;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

@Import(XmlParserConfig.class)
@ExtendWith(SpringExtension.class)
public class BusinessProcessServiceTest {

  private static final String VERSION_ID = "version";
  private static final String PROCESS_CONTENT = getContent("bp-sample.bpmn");
  private static final String BPMN_FILE_EXTENSION = "bpmn";

  @Captor private ArgumentCaptor<String> captor;

  @Mock private VersionContextComponentManager versionContextComponentManager;
  @Mock private VersionedFileRepository repository;
  @Mock private VersionedFileRepository masterRepository;
  @Mock private GerritPropertiesConfig gerritPropertiesConfig;
  @Mock private CacheService cacheService;
  @Autowired private DocumentBuilder documentBuilder;

  @Spy
  private BusinessProcessMapper businessProcessMapper =
      Mappers.getMapper(BusinessProcessMapper.class);

  private BusinessProcessServiceImpl businessProcessService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    businessProcessService =
        new BusinessProcessServiceImpl(
            versionContextComponentManager,
            businessProcessMapper,
            gerritPropertiesConfig,
            documentBuilder,
            cacheService);
    Mockito.when(
            versionContextComponentManager.getComponent(VERSION_ID, VersionedFileRepository.class))
        .thenReturn(repository);
    Mockito.when(
            versionContextComponentManager.getComponent(
                gerritPropertiesConfig.getHeadBranch(), VersionedFileRepository.class))
        .thenReturn(masterRepository);
  }

  @Test
  @SneakyThrows
  void getBusinessProcessesListByVersionTest() {
    VersionedFileInfoDto newBusinessProcess =
        VersionedFileInfoDto.builder()
            .name("business-process")
            .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
            .status(FileStatus.NEW)
            .created(LocalDateTime.of(2022, 8, 10, 13, 18))
            .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
            .build();
    VersionedFileInfoDto deletedProcess =
        VersionedFileInfoDto.builder().status(FileStatus.DELETED).build();

    Mockito.when(repository.getFileList("bpmn"))
        .thenReturn(List.of(newBusinessProcess, deletedProcess));
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);

    List<BusinessProcessInfoDto> expectedBusinessProcessesList =
        businessProcessService.getProcessesByVersion(VERSION_ID);
    BusinessProcessInfoDto expectedBusinessProcess =
        BusinessProcessInfoDto.builder()
            .name("business-process")
            .title("Really test name")
            .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
            .status(FileStatus.NEW)
            .created(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
            .updated(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
            .build();

    Assertions.assertThat(expectedBusinessProcessesList)
        .hasSize(1)
        .element(0)
        .isEqualTo(expectedBusinessProcess);
  }

  @Test
  @SneakyThrows
  void getBusinessProcessesByVersionWithInvalidContentTest() {
    VersionedFileInfoDto newBusinessProcess =
        VersionedFileInfoDto.builder()
            .name("business-process")
            .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
            .status(FileStatus.NEW)
            .created(LocalDateTime.of(2022, 8, 10, 13, 18))
            .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
            .build();
    VersionedFileInfoDto deletedProcess =
        VersionedFileInfoDto.builder().status(FileStatus.DELETED).build();

    Mockito.when(repository.getFileList("bpmn"))
        .thenReturn(List.of(newBusinessProcess, deletedProcess));
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn("Invalid content");

    Assertions.assertThatThrownBy(() -> businessProcessService.getProcessesByVersion(VERSION_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse xml document");
  }

  @Test
  @SneakyThrows
  void getChangedBusinessProcessListByVersionTest() {
    VersionedFileInfoDto newBusinessProcess =
        VersionedFileInfoDto.builder()
            .name("business-process")
            .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
            .status(FileStatus.DELETED)
            .created(LocalDateTime.of(2022, 8, 10, 13, 18))
            .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
            .build();

    Mockito.when(repository.getFileList("bpmn")).thenReturn(List.of(newBusinessProcess));
    Mockito.when(masterRepository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);
    Mockito.when(cacheService.getConflictsCache(VERSION_ID))
        .thenReturn(List.of("bpmn/business-process." + BPMN_FILE_EXTENSION));

    List<BusinessProcessInfoDto> expectedBusinessProcessesList =
        businessProcessService.getChangedProcessesByVersion(VERSION_ID);
    BusinessProcessInfoDto expectedBusinessProcess =
        BusinessProcessInfoDto.builder()
            .name("business-process")
            .title("Really test name")
            .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
            .status(FileStatus.DELETED)
            .created(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
            .updated(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
            .conflicted(true)
            .build();

    Assertions.assertThat(expectedBusinessProcessesList)
        .hasSize(1)
        .element(0)
        .isEqualTo(expectedBusinessProcess);
  }

  @Test
  @SneakyThrows
  void createBusinessProcessTest() {
    Mockito.when(repository.isFileExists("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(false);
    Assertions.assertThatCode(
            () ->
                businessProcessService.createProcess(
                    "business-process", PROCESS_CONTENT, VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), captor.capture());
    String response = captor.getValue();
    Diff documentDiff =
        DiffBuilder.compare(PROCESS_CONTENT)
            .withTest(response)
            .withAttributeFilter(
                attr ->
                    !attr.getName().equals("rrm:modified") && !attr.getName().equals("rrm:created"))
            .build();
    Assertions.assertThat(documentDiff.hasDifferences()).isFalse();
  }

  @Test
  @SneakyThrows
  void createBusinessProcess_alreadyExistsTest() {
    Mockito.when(repository.isFileExists("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(true);
    Assertions.assertThatThrownBy(
            () ->
                businessProcessService.createProcess(
                    "business-process", PROCESS_CONTENT, VERSION_ID))
        .isInstanceOf(BusinessProcessAlreadyExistsException.class);

    Mockito.verify(repository, never()).writeFile(eq("business-process"), anyString());
    // check if business process with this name was not created, but not real value
  }

  @Test
  @SneakyThrows
  void createBusinessProcessInvalidContentTest() {
    Assertions.assertThatThrownBy(
            () ->
                businessProcessService.createProcess(
                    "business-process", "Invalid content", VERSION_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse xml document");
  }

  @Test
  @SneakyThrows
  void getBusinessProcessContentTest() {
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);

    var actualBusinessProcessContent =
        businessProcessService.getProcessContent("business-process", VERSION_ID);

    Assertions.assertThat(actualBusinessProcessContent).isEqualTo(PROCESS_CONTENT);
  }

  @Test
  @SneakyThrows
  void getBusinessProcessContentNotFoundTest() {
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(null);

    Assertions.assertThatThrownBy(
            () -> businessProcessService.getProcessContent("business-process", VERSION_ID))
        .isInstanceOf(ProcessNotFoundException.class)
        .hasMessage("Process business-process not found");
  }

  @Test
  @SneakyThrows
  void updateBusinessProcessNoErrorTest() {
    Assertions.assertThatCode(
            () ->
                businessProcessService.updateProcess(
                    PROCESS_CONTENT, "business-process", VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository).isFileExists("bpmn/business-process.bpmn");
    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), anyString());
    // check if there is no error, but not real value
  }

  @Test
  @SneakyThrows
  void updateBusinessProcessTest() {
    Mockito.when(repository.isFileExists("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(true);
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);
    Assertions.assertThatCode(
            () ->
                businessProcessService.updateProcess(
                    PROCESS_CONTENT, "business-process", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), captor.capture());
    String response = captor.getValue();
    Diff documentDiff =
        DiffBuilder.compare(PROCESS_CONTENT)
            .withTest(response)
            .withAttributeFilter(
                attr ->
                    !attr.getName().equals("rrm:modified") && !attr.getName().equals("rrm:created"))
            .build();
    Assertions.assertThat(documentDiff.hasDifferences()).isFalse();
  }

  @Test
  @SneakyThrows
  void deleteProcessTest() {
    Assertions.assertThatCode(
            () -> businessProcessService.deleteProcess("business-process", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).deleteFile("bpmn/business-process." + BPMN_FILE_EXTENSION);
  }

  @SneakyThrows
  public static String getContent(String filePath) {
    return copyToString(
        BusinessProcessServiceTest.class.getClassLoader().getResourceAsStream(filePath),
        StandardCharsets.UTF_8);
  }

  @Test
  @SneakyThrows
  void rollbackProcessTest() {
    businessProcessService.rollbackProcess("business-process", VERSION_ID);

    Mockito.verify(repository)
        .rollbackFile("bpmn/business-process." + BPMN_FILE_EXTENSION);
  }
}
