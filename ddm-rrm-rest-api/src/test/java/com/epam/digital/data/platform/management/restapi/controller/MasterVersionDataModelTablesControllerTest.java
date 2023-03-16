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

import static com.epam.digital.data.platform.management.restapi.controller.MasterVersionDataModelTablesControllerTest.HEAD_BRANCH;
import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.management.restapi.util.TestUtils;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest({
    MasterVersionDataModelTablesController.class,
    ApplicationExceptionHandler.class,
    GerritPropertiesConfig.class
})
@TestPropertySource(properties = {
    "gerrit.head-branch=" + HEAD_BRANCH
})
@DisplayName("Data-model tables file in master version controller test")
class MasterVersionDataModelTablesControllerTest {

  public static final String HEAD_BRANCH = "master";

  @MockBean
  DataModelFileManagementService fileService;
  @MockBean
  MessageResolver messageResolver;
  MockMvc mockMvc;

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Nested
  @DisplayName("GET /versions/master/data-model/tables")
  @ControllerTest({
      MasterVersionDataModelTablesController.class,
      ApplicationExceptionHandler.class,
      GerritPropertiesConfig.class
  })
  @TestPropertySource(properties = {
      "gerrit.head-branch=" + HEAD_BRANCH
  })
  class MasterVersionDataModelTablesControllerGetFileContentTest {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void getFileContent_happyPath() {
      final var expectedTableContent = TestUtils.getContent("controller/createTables.xml");

      Mockito.doReturn(expectedTableContent).when(fileService).getTablesFileContent(HEAD_BRANCH);

      mockMvc.perform(
          get("/versions/master/data-model/tables")
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedTableContent.getBytes(StandardCharsets.UTF_8))
      ).andDo(document("versions/master/data-model/tables/GET"));

      Mockito.verify(fileService).getTablesFileContent(HEAD_BRANCH);
    }

    @Test
    @DisplayName("should return 404 if tables file not found")
    @SneakyThrows
    void getFileContent_fileNotFound() {
      final var exception = new DataModelFileNotFoundInVersionException("createTables.xml",
          "master");

      Mockito.doThrow(exception).when(fileService).getTablesFileContent(HEAD_BRANCH);

      mockMvc.perform(
          get("/versions/master/data-model/tables")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("DATA_MODEL_FILE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("Data-model file createTables.xml is not found in version master")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath()
      );

      Mockito.verify(fileService).getTablesFileContent(HEAD_BRANCH);
    }

    @Test
    @DisplayName("should return 500 at any unexpected error")
    @SneakyThrows
    void listTablesTest_unexpectedError() {
      Mockito.doThrow(RuntimeException.class)
          .when(fileService).getTablesFileContent(HEAD_BRANCH);

      mockMvc.perform(
          get("/versions/master/data-model/tables")
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("RUNTIME_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(fileService).getTablesFileContent(HEAD_BRANCH);
    }
  }
}
