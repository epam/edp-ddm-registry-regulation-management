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

import static com.epam.digital.data.platform.management.util.TestUtils.getContent;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.config.XmlParserConfig;
import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExists;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.impl.BusinessProcessServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
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

  @Captor
  private ArgumentCaptor<String> captor;

  @Mock
  private VersionedFileRepositoryFactory repositoryFactory;
  @Mock
  private VersionedFileRepository repository;
  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;
  @Autowired
  private DocumentBuilder documentBuilder;
  private BusinessProcessServiceImpl businessProcessService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    businessProcessService = new BusinessProcessServiceImpl(repositoryFactory,
        gerritPropertiesConfig, documentBuilder);
    Mockito.when(repositoryFactory.getRepoByVersion(VERSION_ID)).thenReturn(repository);
  }

  @Test
  @SneakyThrows
  void getBusinessProcessesListByVersionTest() {
    FileResponse newBusinessProcess = FileResponse.builder()
        .name("business-process")
        .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    FileResponse deletedProcess = FileResponse.builder().status(FileStatus.DELETED).build();

    Mockito.when(repository.getFileList("bpmn"))
        .thenReturn(List.of(newBusinessProcess, deletedProcess));
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);

    List<BusinessProcessResponse> expectedBusinessProcessesList = businessProcessService.getProcessesByVersion(
        VERSION_ID);
    BusinessProcessResponse expectedBusinessProcess = BusinessProcessResponse.builder()
        .name("business-process")
        .title("Really test name")
        .path("bpmn/business-process." + BPMN_FILE_EXTENSION)
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
        .updated(LocalDateTime.of(2022, 10, 3, 14, 41, 20, 128000000))
        .build();

    Assertions.assertThat(expectedBusinessProcessesList).hasSize(1)
        .element(0).isEqualTo(expectedBusinessProcess);
  }

  @Test
  @SneakyThrows
  void createBusinessProcessTest() {
    Mockito.when(repository.isFileExists("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(false);
    Assertions.assertThatCode(
            () -> businessProcessService.createProcess("business-process", PROCESS_CONTENT, VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), captor.capture());
    String response = captor.getValue();
    Diff documentDiff = DiffBuilder
        .compare(PROCESS_CONTENT)
        .withTest(response)
        .withAttributeFilter(
            attr -> !attr.getName().equals("rrm:modified") && !attr.getName().equals("rrm:created"))
        .build();
    Assertions.assertThat(documentDiff.hasDifferences()).isFalse();
  }

  @Test
  @SneakyThrows
  void createBusinessProcess_alreadyExistsTest() {
    Mockito.when(repository.isFileExists("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(true);
    Assertions.assertThatThrownBy(
            () -> businessProcessService.createProcess("business-process", PROCESS_CONTENT, VERSION_ID))
        .isInstanceOf(BusinessProcessAlreadyExists.class);

    Mockito.verify(repository, never()).writeFile(any(), any());
  }

  @Test
  @SneakyThrows
  void getBusinessProcessContentTest() {
    Mockito.when(repository.readFile("bpmn/business-process." + BPMN_FILE_EXTENSION))
        .thenReturn(PROCESS_CONTENT);

    var actualBusinessProcessContent = businessProcessService.getProcessContent("business-process",
        VERSION_ID);

    Assertions.assertThat(actualBusinessProcessContent)
        .isEqualTo(PROCESS_CONTENT);
  }

  @Test
  @SneakyThrows
  void updateBusinessProcessNoErrorTest() {
    Assertions.assertThatCode(() -> businessProcessService.updateProcess(PROCESS_CONTENT, "business-process", VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository).isFileExists("bpmn/business-process.bpmn");
    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), anyString());
  }

  @Test
  @SneakyThrows
  void updateBusinessProcessTest() {
    Assertions.assertThatCode(
            () -> businessProcessService.updateProcess(PROCESS_CONTENT, "business-process", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository)
        .writeFile(eq("bpmn/business-process." + BPMN_FILE_EXTENSION), captor.capture());
    String response = captor.getValue();
    Diff documentDiff = DiffBuilder
        .compare(PROCESS_CONTENT)
        .withTest(response)
        .withAttributeFilter(
            attr -> !attr.getName().equals("rrm:modified") && !attr.getName().equals("rrm:created"))
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
}
