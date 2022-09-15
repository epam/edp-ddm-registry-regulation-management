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

import com.epam.digital.data.platform.management.config.XmlParserConfig;
import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExists;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.impl.BusinessProcessServiceImpl;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.xml.parsers.DocumentBuilder;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@Import(XmlParserConfig.class)
@ExtendWith(SpringExtension.class)
public class BusinessProcessServiceTest {

  private static final String VERSION_ID = "version";
  private static final String PROCESS_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_0ffpt5r\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\" modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\">" +
      "  <bpmn:process id=\"Process_00mzcs7\" name=\"Really test name\" isExecutable=\"true\">" +
      "    <bpmn:startEvent id=\"StartEvent_1\" />" +
      "  </bpmn:process>" +
      "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">" +
      "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_00mzcs7\">" +
      "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">" +
      "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\" />" +
      "      </bpmndi:BPMNShape>" +
      "    </bpmndi:BPMNPlane>" +
      "  </bpmndi:BPMNDiagram>" +
      "</bpmn:definitions>";

  @Mock
  private VersionedFileRepositoryFactory repositoryFactory;
  @Mock
  private VersionedFileRepository repository;
  @Autowired
  private DocumentBuilder documentBuilder;
  private BusinessProcessServiceImpl businessProcessService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    businessProcessService = new BusinessProcessServiceImpl(repositoryFactory, documentBuilder);
    Mockito.when(repositoryFactory.getRepoByVersion(VERSION_ID)).thenReturn(repository);
  }

  @Test
  @SneakyThrows
  void getBusinessProcessesListByVersionTest() {
    FileResponse newBusinessProcess = FileResponse.builder()
        .name("business-process")
        .path("business-processes/business-process.xml")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    FileResponse deletedProcess = FileResponse.builder().status(FileStatus.DELETED).build();

    Mockito.when(repository.getFileList("business-processes")).thenReturn(List.of(newBusinessProcess, deletedProcess));
    Mockito.when(repository.readFile("business-processes/business-process.xml")).thenReturn(PROCESS_CONTENT);

    List<BusinessProcessResponse> expectedBusinessProcessesList = businessProcessService.getProcessesByVersion(VERSION_ID);
    BusinessProcessResponse expectedBusinessProcess = BusinessProcessResponse.builder()
        .name("business-process")
        .title("Really test name")
        .path("business-processes/business-process.xml")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();

    Assertions.assertThat(expectedBusinessProcessesList).hasSize(1)
        .element(0).isEqualTo(expectedBusinessProcess);
  }

  @Test
  @SneakyThrows
  void createBusinessProcessTest() {
    Mockito.when(repository.isFileExists("business-processes/business-process.xml")).thenReturn(false);
    Assertions.assertThatCode(() -> businessProcessService.createProcess("business-process", PROCESS_CONTENT, VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository).writeFile("business-processes/business-process.xml", PROCESS_CONTENT);
  }

  @Test
  @SneakyThrows
  void createBusinessProcess_alreadyExistsTest() {
    Mockito.when(repository.isFileExists("business-processes/business-process.xml")).thenReturn(true);
    Assertions.assertThatThrownBy(() -> businessProcessService.createProcess("business-process", PROCESS_CONTENT, VERSION_ID))
        .isInstanceOf(BusinessProcessAlreadyExists.class);

    Mockito.verify(repository, never()).writeFile(any(), any());
  }

  @Test
  @SneakyThrows
  void getBusinessProcessContentTest() {
    Mockito.when(repository.readFile("business-processes/business-process.xml")).thenReturn(PROCESS_CONTENT);

    var actualBusinessProcessContent = businessProcessService.getProcessContent("business-process", VERSION_ID);

    Assertions.assertThat(actualBusinessProcessContent)
        .isEqualTo(PROCESS_CONTENT);
  }

  @Test
  @SneakyThrows
  void updateBusinessProcessTest() {
    Assertions.assertThatCode(() -> businessProcessService.updateProcess(PROCESS_CONTENT, "business-process", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).writeFile("business-processes/business-process.xml", PROCESS_CONTENT);
  }

  @Test
  @SneakyThrows
  void deleteProcessTest() {
    Assertions.assertThatCode(() -> businessProcessService.deleteProcess("business-process", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).deleteFile("business-processes/business-process.xml");
  }
}
