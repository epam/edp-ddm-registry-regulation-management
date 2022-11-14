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

package com.epam.digital.data.platform.management.restapi.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.restapi.util.TestUtils;
import com.epam.digital.data.platform.management.service.impl.BusinessProcessServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest(MasterVersionBusinessProcessesController.class)
@DisplayName("Business process in master version controller tests")
class MasterVersionBusinessProcessControllerTest {

  static final String HEAD_BRANCH = "master";

  @MockBean
  BusinessProcessServiceImpl businessProcessService;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  MockMvc mockMvc;

  @BeforeEach
  void setup(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentationContextProvider) {
    Mockito.lenient().doReturn(HEAD_BRANCH).when(gerritPropertiesConfig).getHeadBranch();
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentationContextProvider))
        .build();
  }

  @Test
  @DisplayName("GET /versions/master/business-processes/{businessProcessName} should return 200 with business process content")
  @SneakyThrows
  void getBusinessProcess() {
    final var processId = "John_Does_process";
    final var expectedProcessContent = TestUtils.getContent("controller/John_Does_process.bpmn");

    Mockito.doReturn(expectedProcessContent)
        .when(businessProcessService).getProcessContent(processId, HEAD_BRANCH);

    mockMvc.perform(
        get("/versions/master/business-processes/{businessProcessName}", processId)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.TEXT_XML),
        content().xml(expectedProcessContent)
    ).andDo(document("versions/master/business-processes/{businessProcessName}/GET"));
  }

  @Test
  @DisplayName("GET /versions/master/business-processes should return 200 with list of all business processes")
  @SneakyThrows
  void getBusinessProcessesByVersionId() {
    final var expectedResponse = BusinessProcessInfoDto.builder()
        .path("/bpmn/John_Does_process.bpmn")
        .name("John_Does_process")
        .title("John Doe added new component")
        .created(LocalDateTime.of(2022, 11, 3, 11, 45))
        .updated(LocalDateTime.of(2022, 11, 4, 13, 16))
        .build();

    Mockito.doReturn(List.of(expectedResponse))
        .when(businessProcessService).getProcessesByVersion(HEAD_BRANCH);

    mockMvc.perform(
        get("/versions/master/business-processes")
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$", hasSize(1)),
        jsonPath("$[0].name", equalTo("John_Does_process")),
        jsonPath("$[0].title", equalTo("John Doe added new component")),
        jsonPath("$[0].created", equalTo("2022-11-03T11:45:00.000Z")),
        jsonPath("$[0].updated", equalTo("2022-11-04T13:16:00.000Z"))
    ).andDo(document("versions/master/business-processes/GET"));
  }
}
