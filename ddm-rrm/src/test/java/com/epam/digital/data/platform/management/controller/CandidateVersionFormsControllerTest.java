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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.impl.FormServiceImpl;
import com.epam.digital.data.platform.management.util.TestUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest(CandidateVersionFormsController.class)
@DisplayName("Forms in version candidates controller tests")
class CandidateVersionFormsControllerTest {

  MockMvc mockMvc;
  @MockBean
  FormServiceImpl formService;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/forms should return 200 with list of all forms")
  @SneakyThrows
  void getFormsByVersionIdTest() {
    var expectedFormResponse = FormResponse.builder()
        .name("john-does-form")
        .title("John Doe added new component")
        .path("/")
        .status(FileStatus.CHANGED)
        .created(LocalDateTime.of(2022, 7, 29, 18, 55))
        .updated(LocalDateTime.of(2022, 7, 29, 18, 56))
        .build();

    Mockito.doReturn(List.of(expectedFormResponse))
        .when(formService).getFormListByVersion("1");

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/forms", "1")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.[0].name", is("john-does-form")),
        jsonPath("$.[0].title", is("John Doe added new component")),
        jsonPath("$.[0].created", is("2022-07-29T18:55:00.000Z")),
        jsonPath("$.[0].updated", is("2022-07-29T18:56:00.000Z"))
    ).andDo(document("versions/candidates/{versionCandidateId}/forms/GET"));

    Mockito.verify(formService).getFormListByVersion("1");
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/forms/{formName} should return 201 with form content")
  @SneakyThrows
  void formCreateTest() {
    final var versionCandidateId = "1";
    final var formName = "john-does-form";
    final var expectedFormContent = TestUtils.getContent("controller/john-does-form.json");

    Mockito.doReturn(expectedFormContent)
        .when(formService).getFormContent(formName, versionCandidateId);

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/forms/{formName}",
            versionCandidateId, formName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(expectedFormContent)
    ).andExpectAll(
        status().isCreated(),
        header().string(HttpHeaders.LOCATION, "/versions/candidates/1/forms/john-does-form"),
        content().contentType(MediaType.APPLICATION_JSON),
        content().json(expectedFormContent)
    ).andDo(document("versions/candidates/{versionCandidateId}/forms/{formName}/POST"));

    Mockito.verify(formService).createForm(formName, expectedFormContent, versionCandidateId);
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/forms/{formName} should return 200 with form content")
  @SneakyThrows
  void getFormTest() {
    final var versionCandidateId = "1";
    final var formName = "john-does-form";
    final var expectedFormContent = TestUtils.getContent("controller/john-does-form.json");

    Mockito.doReturn(expectedFormContent)
        .when(formService).getFormContent(formName, versionCandidateId);

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/forms/{formName}",
            versionCandidateId, formName)
    ).andExpectAll(
        status().isOk(),
        content().json(expectedFormContent)
    ).andDo(document("versions/candidates/{versionCandidateId}/forms/{formName}/GET"));

    Mockito.verify(formService).getFormContent(formName, versionCandidateId);
  }

  @Test
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/forms/{formName} should return 200 with form content")
  @SneakyThrows
  void updateFormTest() {
    final var versionCandidateId = "1";
    final var formName = "john-does-form";
    final var expectedFormContent = TestUtils.getContent("controller/john-does-form.json");

    Mockito.doReturn(expectedFormContent)
        .when(formService).getFormContent(formName, versionCandidateId);

    mockMvc.perform(
        put("/versions/candidates/{versionCandidateId}/forms/{formName}",
            versionCandidateId, formName)
            .contentType(MediaType.APPLICATION_JSON)
            .content(expectedFormContent)
    ).andExpectAll(
        status().isOk(),
        content().json(expectedFormContent)
    ).andDo(document("versions/candidates/{versionCandidateId}/forms/{formName}/PUT"));

    Mockito.verify(formService).updateForm(expectedFormContent, formName, versionCandidateId);
  }

  @Test
  @DisplayName("DELETE /versions/candidates/{versionCandidateId}/forms/{formName} should return 204")
  @SneakyThrows
  void deleteFormTest() {
    Mockito.doNothing().when(formService).deleteForm("john-does-form", "1");

    mockMvc.perform(delete("/versions/candidates/{versionCandidateId}/forms/{formName}",
        "1", "john-does-form")
    ).andExpect(
        status().isNoContent()
    ).andDo(document("versions/candidates/{versionCandidateId}/forms/{formName}/DELETE"));

    Mockito.verify(formService).deleteForm("john-does-form", "1");
  }
}
