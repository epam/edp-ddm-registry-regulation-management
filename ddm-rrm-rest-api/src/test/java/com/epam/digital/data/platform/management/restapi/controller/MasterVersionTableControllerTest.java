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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.management.restapi.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.service.ReadDataBaseTablesService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
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

@ControllerTest({MasterVersionTableController.class, ApplicationExceptionHandler.class})
@DisplayName("Tables in master version controller test")
class MasterVersionTableControllerTest {

  private static final String HEAD_BRANCH = "master";

  @MockBean
  ReadDataBaseTablesService tableService;
  @MockBean
  MessageResolver messageResolver;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  MockMvc mockMvc;

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    Mockito.doReturn(HEAD_BRANCH).when(gerritPropertiesConfig).getHeadBranch();
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Nested
  @ControllerTest({MasterVersionTableController.class, ApplicationExceptionHandler.class})
  @DisplayName("GET /versions/master/tables")
  class MasterVersionTableListTablesControllerTest {

    @Test
    @DisplayName("should return 200 with all found tables")
    @SneakyThrows
    void getTablesTest() {
      var expectedTablesResponse = TableShortInfoDto.builder()
          .name("John Doe's table")
          .description("John Doe get table")
          .objectReference(true)
          .build();
      Mockito.doReturn(List.of(expectedTablesResponse))
          .when(tableService).listTables(HEAD_BRANCH);

      mockMvc.perform(
          get("/versions/master/tables")
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.[0].name", is("John Doe's table")),
          jsonPath("$.[0].description", is("John Doe get table")),
          jsonPath("$.[0].objectReference", is(true))
      ).andDo(document("versions/master/tables/GET"));

      Mockito.verify(tableService).listTables(HEAD_BRANCH);
    }

    @Test
    @DisplayName("should return 500 it couldn't connect to data base")
    @SneakyThrows
    void listTablesTest_registryDataBaseConnectionException() {
      Mockito.doThrow(RegistryDataBaseConnectionException.class)
          .when(tableService).listTables(HEAD_BRANCH);

      mockMvc.perform(
          get("/versions/master/tables")
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).listTables(HEAD_BRANCH);
    }
  }

  @Nested
  @ControllerTest({MasterVersionTableController.class, ApplicationExceptionHandler.class})
  @DisplayName("GET /versions/master/tables/{tableName}")
  class MasterVersionTableGetTableByNameControllerTest {

    @Test
    @DisplayName("should return 200 with table info")
    @SneakyThrows
    void getTableTest() {
      final var tableName = "John_Does_table";

      final var expectedTablesResponse = new TableInfoDto();
      expectedTablesResponse.setName(tableName);
      expectedTablesResponse.setDescription("John Doe get table");
      expectedTablesResponse.setObjectReference(true);
      Mockito.doReturn(expectedTablesResponse)
          .when(tableService).getTable(HEAD_BRANCH, tableName);

      mockMvc.perform(
          get("/versions/master/tables/{tableName}", tableName)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is(tableName)),
          jsonPath("$.description", is("John Doe get table")),
          jsonPath("$.objectReference", is(true))
      ).andDo(document("versions/master/tables/{tableName}/GET"));

      Mockito.verify(tableService).getTable(HEAD_BRANCH, tableName);
    }

    @Test
    @DisplayName("should return 404 it table doesn't exist")
    @SneakyThrows
    void getTableNotFoundException() {
      final var tableName = "tableName";
      Mockito.doThrow(new TableNotFoundException("Table tableName wasn't found"))
          .when(tableService).getTable(HEAD_BRANCH, tableName);
      Mockito.doReturn("localized message").when(messageResolver)
          .getMessage(FileValidatorErrorMessageTitle.TABLE_NOT_FOUND_EXCEPTION);

      mockMvc.perform(
          get("/versions/master/tables/{tableName}", tableName)
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("TABLE_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details").value(is("Table tableName wasn't found")),
          jsonPath("$.localizedMessage").value(is("localized message")),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).getTable(HEAD_BRANCH, tableName);
    }

    @Test
    @DisplayName("should return 500 it couldn't connect to data base")
    @SneakyThrows
    void getTableTest_RegistryDataBaseConnectionException() {
      final var tableName = "tableName";
      Mockito.doThrow(RegistryDataBaseConnectionException.class)
          .when(tableService).getTable(HEAD_BRANCH, tableName);

      mockMvc.perform(
          get("/versions/master/tables/{tableName}", tableName)
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(tableService).getTable(HEAD_BRANCH, tableName);
    }
  }
}
