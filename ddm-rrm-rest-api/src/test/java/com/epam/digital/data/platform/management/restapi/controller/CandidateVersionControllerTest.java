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

package com.epam.digital.data.platform.management.restapi.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.restapi.model.CreateVersionRequest;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto.DataModelFileStatus;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto.DataModelFileType;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto.ChangedFileStatus;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionChangesDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

@ControllerTest(CandidateVersionController.class)
@DisplayName("Version candidates controller tests")
class CandidateVersionControllerTest {

  @MockBean
  VersionManagementServiceImpl versionManagementService;
  MockMvc mockMvc;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/candidates should return 200 with all versions")
  @SneakyThrows
  void getVersionListTest() {
    final var expectedChangeInfoResponse = VersionInfoShortDto.builder()
        .number(1)
        .subject("JohnDoe's version candidate")
        .description("Version candidate to change form")
        .build();

    Mockito.doReturn(List.of(expectedChangeInfoResponse))
        .when(versionManagementService).getVersionsList();

    mockMvc.perform(
        get("/versions/candidates")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.[0].id", is("1")),
        jsonPath("$.[0].name", is("JohnDoe's version candidate")),
        jsonPath("$.[0].description", is("Version candidate to change form"))
    ).andDo(document("versions/candidates/GET"));

    Mockito.verify(versionManagementService).getVersionsList();
  }

  @Test
  @DisplayName("POST /versions/candidates should return 200 with new created version info")
  @SneakyThrows
  void createNewVersionTest() {
    final var expectedVersionDetails = VersionInfoDto.builder()
        .number(1)
        .subject("JohnDoe's version candidate")
        .description("Version candidate to change form")
        .owner("JohnDoe@epam.com")
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .labels(Map.of("Verified", 1))
        .build();
    Mockito.doReturn(expectedVersionDetails).when(versionManagementService)
        .getVersionDetails("1");

    var request = new CreateVersionRequest();
    request.setName("JohnDoe's version candidate");
    request.setDescription("Version candidate to change form");
    final var dto = CreateChangeInputDto.builder().name(request.getName())
        .description(request.getDescription()).build();
    Mockito.when(versionManagementService.createNewVersion(dto)).thenReturn("1");
    mockMvc.perform(
        post("/versions/candidates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(request))
    ).andExpectAll(
        status().isCreated(),
        header().string(HttpHeaders.LOCATION, "/versions/candidates/1"),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", is("1")),
        jsonPath("$.name", is("JohnDoe's version candidate")),
        jsonPath("$.description", is("Version candidate to change form")),
        jsonPath("$.author", is("JohnDoe@epam.com")),
        jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
        jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
        jsonPath("$.hasConflicts", is(false)),
        jsonPath("$.inspections", nullValue()),
        jsonPath("$.validations[0].result", is("SUCCESS"))
    ).andDo(document("versions/candidates/POST"));

    Mockito.verify(versionManagementService).getVersionDetails("1");
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId} should return 200 with version info")
  @SneakyThrows
  void getVersionDetailsTest() {
    var expectedVersionDetails = VersionInfoDto.builder()
        .number(1)
        .subject("JohnDoe's version candidate")
        .description("Version candidate to change form")
        .owner("JohnDoe@epam.com")
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .labels(Map.of("Verified", 1))
        .build();
    Mockito.doReturn(expectedVersionDetails)
        .when(versionManagementService).getVersionDetails("1");

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}", "1")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", is("1")),
        jsonPath("$.name", is("JohnDoe's version candidate")),
        jsonPath("$.description", is("Version candidate to change form")),
        jsonPath("$.author", is("JohnDoe@epam.com")),
        jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
        jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
        jsonPath("$.hasConflicts", is(false)),
        jsonPath("$.inspections", nullValue()),
        jsonPath("$.validations[0].result", is("SUCCESS"))
    ).andDo(document("versions/candidates/{versionCandidateId}/GET"));

    Mockito.verify(versionManagementService).getVersionDetails("1");
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/decline should return 200")
  @SneakyThrows
  void declineTest() {
    Mockito.doNothing().when(versionManagementService).decline("1");

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/decline", "1")
    ).andExpect(
        status().isOk()
    ).andDo(document("versions/candidates/{versionCandidateId}/decline/POST"));

    Mockito.verify(versionManagementService).decline("1");
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/submit should return 200")
  @SneakyThrows
  void submitTest() {
    Mockito.doNothing().when(versionManagementService).submit("1");

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/submit", "1")
    ).andExpect(
        status().isOk()
    ).andDo(document("versions/candidates/{versionCandidateId}/submit/POST"));

    Mockito.verify(versionManagementService).submit("1");
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/rebase should return 200")
  @SneakyThrows
  void rebaseTest() {
    Mockito.doNothing().when(versionManagementService).rebase("1");

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/rebase", "1")
    ).andExpect(
        status().isOk()
    ).andDo(document("versions/candidates/{versionCandidateId}/rebase/GET"));

    Mockito.verify(versionManagementService).rebase("1");
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/changes should return 200 with all changes info")
  @SneakyThrows
  void getChangesTest() {
    final var expectedChangedForm = EntityChangesInfoDto.builder()
        .name("formToBeUpdated")
        .title("JohnDoe's form")
        .status(ChangedFileStatus.CHANGED)
        .build();
    final var expectedChangedProcess = EntityChangesInfoDto.builder()
        .name("newProcess")
        .title("JohnDoe's process")
        .status(ChangedFileStatus.NEW)
        .build();
    final var expectedChangedDataModelFiles = DataModelChangesInfoDto.builder()
        .name("createTables")
        .fileType(DataModelFileType.TABLES_FILE)
        .status(DataModelFileStatus.CHANGED)
        .build();
    final var expectedChangedGroups = EntityChangesInfoDto.builder()
        .title("JohnDoe's group")
        .status(ChangedFileStatus.NEW)
        .build();
    final var expectedChanges = VersionChangesDto.builder()
        .changedForms(List.of(expectedChangedForm))
        .changedBusinessProcesses(List.of(expectedChangedProcess))
        .changedDataModelFiles(List.of(expectedChangedDataModelFiles))
        .changedGroups(List.of(expectedChangedGroups))
        .build();

    Mockito.doReturn(expectedChanges)
        .when(versionManagementService).getVersionChanges("1");

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/changes", "1")
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.changedForms", hasSize(1)),
        jsonPath("$.changedBusinessProcesses", hasSize(1)),
        jsonPath("$.changedForms[0].name", equalTo("formToBeUpdated")),
        jsonPath("$.changedForms[0].title", equalTo("JohnDoe's form")),
        jsonPath("$.changedForms[0].status", equalTo("CHANGED")),
        jsonPath("$.changedBusinessProcesses[0].name", equalTo("newProcess")),
        jsonPath("$.changedBusinessProcesses[0].title", equalTo("JohnDoe's process")),
        jsonPath("$.changedBusinessProcesses[0].status", equalTo("NEW")),
        jsonPath("$.changedGroups[0].title", equalTo("JohnDoe's group")),
        jsonPath("$.changedGroups[0].status", equalTo("NEW"))
    ).andDo(document("versions/candidates/{versionCandidateId}/changes/GET"));

    Mockito.verify(versionManagementService).getVersionChanges("1");
  }
}
