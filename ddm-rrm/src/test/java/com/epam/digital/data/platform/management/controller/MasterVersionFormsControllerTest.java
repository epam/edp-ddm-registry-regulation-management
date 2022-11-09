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

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.util.TestUtils;
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

@ControllerTest(MasterVersionFormsController.class)
@DisplayName("Forms in master version controller tests")
class MasterVersionFormsControllerTest {

  static final String HEAD_BRANCH = "master";

  MockMvc mockMvc;

  @MockBean
  FormService formService;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    Mockito.lenient().doReturn(HEAD_BRANCH).when(gerritPropertiesConfig).getHeadBranch();
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/master/forms should return 200 with list of all forms")
  @SneakyThrows
  void getFormsFromMaster() {
    var expectedFormResponse = FormInfoDto.builder()
        .name("John Doe's form")
        .path("forms/John Doe's form.json")
        .title("John Doe added new component")
        .status(FileStatus.CURRENT)
        .created(LocalDateTime.of(2022, 7, 29, 15, 6))
        .updated(LocalDateTime.of(2022, 7, 29, 15, 7))
        .build();
    Mockito.doReturn(List.of(expectedFormResponse))
        .when(formService).getFormListByVersion(HEAD_BRANCH);

    mockMvc.perform(
        get("/versions/master/forms")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.[0].name", is("John Doe's form")),
        jsonPath("$.[0].title", is("John Doe added new component")),
        jsonPath("$.[0].created", is("2022-07-29T15:06:00.000Z")),
        jsonPath("$.[0].updated", is("2022-07-29T15:07:00.000Z"))
    ).andDo(document("versions/master/forms/GET"));

    Mockito.verify(formService).getFormListByVersion(HEAD_BRANCH);
  }

  @Test
  @DisplayName("GET /versions/master/forms/{formName} should return 200 with form content")
  @SneakyThrows
  void getFormFromMaster() {
    final var formName = "john-does-form";
    final var expectedFormContent = TestUtils.getContent("controller/john-does-form.json");
    Mockito.doReturn(expectedFormContent)
        .when(formService).getFormContent(formName, HEAD_BRANCH);

    mockMvc.perform(
        get("/versions/master/forms/{formName}", formName)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        content().json(expectedFormContent)
    ).andDo(document("versions/master/forms/{formName}/GET"));

    Mockito.verify(formService).getFormContent(formName, HEAD_BRANCH);
  }
}
