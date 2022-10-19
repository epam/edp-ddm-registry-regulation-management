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

package com.epam.digital.data.platform.management.controller;

import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.service.impl.BusinessProcessServiceImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ControllerTest(CandidateVersionBusinessProcessesController.class)
public class CandidateVersionBusinessProcessControllerTest {

  private static final String CANDIDATE_VERSION_ID = "id1";
  private static final String BASE_REQUEST = "/versions/candidates/id1/business-processes";
  private MockMvc mockMvc;
  private static final String BUSINESS_PROCESS_ID = "John_Does_process";
  private static final String BUSINESS_PROCESS_NAME = "John Doe added new component";
  private static final String BUSINESS_PROCESS_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\" modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\">\n" +
      "  <bpmn:process id=\"" + BUSINESS_PROCESS_ID + "\" name=\"" + BUSINESS_PROCESS_NAME + "\" isExecutable=\"true\">\n" +
      "    <bpmn:startEvent id=\"StartEvent_1\" />\n" +
      "  </bpmn:process>\n" +
      "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n" +
      "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"" + BUSINESS_PROCESS_ID + "\">\n" +
      "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n" +
      "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\" />\n" +
      "      </bpmndi:BPMNShape>\n" +
      "    </bpmndi:BPMNPlane>\n" +
      "  </bpmndi:BPMNDiagram>\n" +
      "</bpmn:definitions>";

  protected final static Map<String, String> BPMN_NAMESPACES = Map.of(
      "bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL",
      "bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI",
      "dc", "http://www.omg.org/spec/DD/20100524/DC",
      "modeler", "http://camunda.org/schema/modeler/1.0");

  @MockBean
  private BusinessProcessServiceImpl businessProcessService;

  @RegisterExtension
  private final RestDocumentationExtension restDocumentation = new RestDocumentationExtension();

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
                    RestDocumentationContextProvider restDocumentation) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  @SneakyThrows
  public void getBusinessProcess() {
    Mockito.when(businessProcessService.getProcessContent(BUSINESS_PROCESS_ID, CANDIDATE_VERSION_ID))
        .thenReturn(BUSINESS_PROCESS_CONTENT);

    mockMvc.perform(get(BASE_REQUEST + "/" + BUSINESS_PROCESS_ID))
        .andExpectAll(
            status().isOk(),
            content().contentType("text/xml"),
            xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(BUSINESS_PROCESS_ID),
            xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(BUSINESS_PROCESS_NAME)
        ).andDo(document("versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/GET"));
  }

  @Test
  @SneakyThrows
  public void getBusinessProcessesByVersionId() {
    Mockito.when(businessProcessService.getProcessesByVersion(CANDIDATE_VERSION_ID))
        .thenReturn(List.of(BusinessProcessResponse.builder()
                .path("/bpmn/" + BUSINESS_PROCESS_ID + ".bpmn")
                .name(BUSINESS_PROCESS_ID)
                .title(BUSINESS_PROCESS_NAME)
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
            .build()));

    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST, CANDIDATE_VERSION_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$", hasSize(1)))
        .andDo(document("versions/candidates/{versionCandidateId}/business-processes/GET"));
  }

  @Test
  @SneakyThrows
  public void createBusinessProcess() {
    Mockito.when(businessProcessService.getProcessContent(BUSINESS_PROCESS_ID, CANDIDATE_VERSION_ID))
        .thenReturn(BUSINESS_PROCESS_CONTENT);
    mockMvc.perform(MockMvcRequestBuilders.post(
            BASE_REQUEST + "/{businessProcessName}", BUSINESS_PROCESS_ID)
        .contentType(MediaType.TEXT_XML).content(BUSINESS_PROCESS_CONTENT)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isCreated(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(BUSINESS_PROCESS_ID),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(BUSINESS_PROCESS_NAME))
        .andDo(document("versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST"));

    Mockito.verify(businessProcessService).createProcess(BUSINESS_PROCESS_ID, BUSINESS_PROCESS_CONTENT, CANDIDATE_VERSION_ID);
  }

  @Test
  @SneakyThrows
  public void updateBusinessProcess() {
    Mockito.when(businessProcessService.getProcessContent(BUSINESS_PROCESS_ID, CANDIDATE_VERSION_ID))
            .thenReturn(BUSINESS_PROCESS_CONTENT);
    mockMvc.perform(MockMvcRequestBuilders.put(
            BASE_REQUEST + "/{businessProcessName}", BUSINESS_PROCESS_ID)
        .contentType(MediaType.TEXT_XML).content(BUSINESS_PROCESS_CONTENT)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isOk(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(BUSINESS_PROCESS_ID),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(BUSINESS_PROCESS_NAME))
        .andDo(document("versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT"));

    Mockito.verify(businessProcessService).updateProcess(BUSINESS_PROCESS_CONTENT, BUSINESS_PROCESS_ID, CANDIDATE_VERSION_ID);
  }

  @Test
  @SneakyThrows
  public void deleteBusinessProcess() {
    mockMvc.perform(MockMvcRequestBuilders.delete(
            BASE_REQUEST + "/{businessProcessName}", BUSINESS_PROCESS_ID))
        .andExpect(status().isNoContent())
        .andDo(document("versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/DELETE"));

    Mockito.verify(businessProcessService).deleteProcess(BUSINESS_PROCESS_ID, CANDIDATE_VERSION_ID);
  }
}
