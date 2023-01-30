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

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.mapper.DdmTableMapper;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.management.restapi.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.service.DataModelService;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import data.model.snapshot.model.DdmTable;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest({CandidateVersionTableController.class, ApplicationExceptionHandler.class})
@DisplayName("Tables in version candidate controller test")
class CandidateVersionTableControllerTest {

  @MockBean
  DataModelService tableService;
  @MockBean
  VersionManagementService versionManagementService;
  @MockBean
  MessageResolver messageResolver;
  @MockBean
  DdmTableMapper mapper;
  MockMvc mockMvc;

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Nested
  @ControllerTest({CandidateVersionTableController.class, ApplicationExceptionHandler.class})
  @DisplayName("GET /versions/candidates/{versionCandidateId}/tables")
  class CandidateVersionTableListTablesControllerTest {

    @Test
    @DisplayName("should return 200 with all found tables")
    @SneakyThrows
    void getTablesTest() {
      var versionCandidate = "105";
      var expectedTablesResponse = TableShortInfoDto.builder()
          .name("John Doe's table")
          .description("John Doe get table")
          .objectReference(true)
          .build();
      Mockito.doReturn(List.of(expectedTablesResponse))
          .when(tableService).listTables(versionCandidate);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidate)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.[0].name", is("John Doe's table")),
          jsonPath("$.[0].description", is("John Doe get table")),
          jsonPath("$.[0].objectReference", is(true))
      ).andDo(document("versions/candidates/{versionCandidateId}/tables/GET"));

      Mockito.verify(tableService).listTables(versionCandidate);
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getTable_badRequest() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", "master")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );

      Mockito.verifyNoInteractions(tableService);
    }

    @Test
    @DisplayName("should return 404 it version-candidate doesn't exist")
    @SneakyThrows
    void getTable_versionCandidateNotFound() {
      var versionCandidate = "42";
      Mockito.doThrow(GerritChangeNotFoundException.class).when(versionManagementService)
          .getVersionDetails(versionCandidate);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidate)
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("CHANGE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("getTables.versionCandidateId: Version candidate does not exist.")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verifyNoInteractions(tableService);
    }

    @Test
    @DisplayName("should return 500 it couldn't connect to master version database")
    @SneakyThrows
    void getTable_registryDataBaseConnectionException() {
      var versionCandidate = "142";
      Mockito.doThrow(RegistryDataBaseConnectionException.class)
          .when(tableService).listTables(versionCandidate);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidate)
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).listTables(versionCandidate);
    }
  }

  @Nested
  @ControllerTest({CandidateVersionTableController.class, ApplicationExceptionHandler.class})
  @DisplayName("GET /versions/candidates/{versionCandidateId}/tables/{tableName}")
  class CandidateVersionTableGetTableByNameControllerTest {

    @Test
    @DisplayName("should return 200 with table info")
    @SneakyThrows
    void getTableTest() {
      var versionCandidate = "105";
      final var tableName = "John_Does_table";

      final var expectedTablesResponse = new TableInfoDto();
      expectedTablesResponse.setName(tableName);
      expectedTablesResponse.setDescription("John Doe get table");
      expectedTablesResponse.setObjectReference(true);
      Mockito.doReturn(expectedTablesResponse)
          .when(tableService).getTable(versionCandidate, tableName);
      final var expectedDdmTable = new DdmTable();
      expectedDdmTable.setName(tableName);
      expectedDdmTable.setDescription("John Doe get table");
      expectedDdmTable.setObjectReference(true);
      Mockito.doReturn(expectedDdmTable).when(mapper).convertToDdmTable(expectedTablesResponse);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidate,
              tableName)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is(tableName)),
          jsonPath("$.description", is("John Doe get table")),
          jsonPath("$.objectReference", is(true))
      ).andDo(document("versions/candidates/{versionCandidateId}/{tableName}/GET"));

      Mockito.verify(tableService).getTable(versionCandidate, tableName);
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getTable_badRequest() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", "master",
              "tableName")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );

      Mockito.verifyNoInteractions(tableService);
    }

    @Test
    @DisplayName("should return 404 it version-candidate doesn't exist")
    @SneakyThrows
    void getTable_versionCandidateNotFound() {
      var versionCandidate = "42";
      Mockito.doThrow(GerritChangeNotFoundException.class).when(versionManagementService)
          .getVersionDetails(versionCandidate);
      final var tableName = "tableName";

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidate,
              tableName)
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("CHANGE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("getTable.versionCandidateId: Version candidate does not exist.")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verifyNoInteractions(tableService);
    }

    @Test
    @DisplayName("should return 404 it table doesn't exist")
    @SneakyThrows
    void getTableNotFoundException() {
      var versionCandidate = "42";
      final var tableName = "tableName";
      Mockito.doThrow(new TableNotFoundException("Table tableName wasn't found"))
          .when(tableService).getTable(versionCandidate, tableName);
      Mockito.doReturn("localized message").when(messageResolver)
          .getMessage(FileValidatorErrorMessageTitle.TABLE_NOT_FOUND_EXCEPTION);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidate,
              tableName)
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("TABLE_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details").value(is("Table tableName wasn't found")),
          jsonPath("$.localizedMessage").value(is("localized message")),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).getTable(versionCandidate, tableName);
    }

    @Test
    @DisplayName("should return 500 it couldn't connect to master version database")
    @SneakyThrows
    void getTable_registryDataBaseConnectionException() {
      var versionCandidate = "142";
      final var tableName = "tableName";
      Mockito.doThrow(RegistryDataBaseConnectionException.class)
          .when(tableService).getTable(versionCandidate, tableName);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidate,
              tableName)
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).getTable(versionCandidate, tableName);
    }
  }
}
