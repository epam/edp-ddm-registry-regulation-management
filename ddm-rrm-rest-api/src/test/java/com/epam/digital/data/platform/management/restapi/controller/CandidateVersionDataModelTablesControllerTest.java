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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.management.restapi.util.TestUtils;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import com.epam.digital.data.platform.management.validation.DDMExtensionChangelogFile;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.metadata.ConstraintDescriptor;
import lombok.SneakyThrows;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest({
    CandidateVersionDataModelTablesController.class,
    ApplicationExceptionHandler.class
})
@DisplayName("Data-model tables file in version-candidate controller test")
class CandidateVersionDataModelTablesControllerTest {

  public static final Integer VERSION_CANDIDATE_ID = 42;
  public static final String VERSION_CANDIDATE_ID_STRING = String.valueOf(VERSION_CANDIDATE_ID);

  @MockBean
  DataModelFileManagementService fileService;
  @MockBean
  VersionManagementService versionManagementService;
  @MockBean
  MessageSource messageSource;
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
  @DisplayName("GET /versions/candidates/{versionCandidateId}/data-model/tables")
  @ControllerTest({
      CandidateVersionDataModelTablesController.class,
      ApplicationExceptionHandler.class
  })
  class CandidateVersionDataModelTablesControllerGetFileContentTest {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void getFileContent_happyPath() {
      final var expectedTableContent = TestUtils.getContent("controller/createTables.xml");

      Mockito.doReturn(expectedTableContent).when(fileService)
          .getTablesFileContent(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedTableContent.getBytes(StandardCharsets.UTF_8))
      ).andDo(document("versions/candidates/{versionCandidateId}/data-model/tables/GET"));

      Mockito.verify(fileService).getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getFileContent_badRequest() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", "master")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );

      Mockito.verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getFileContent_versionCandidateNotFound() {
      Mockito.doThrow(GerritChangeNotFoundException.class).when(versionManagementService)
          .getVersionDetails(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("CHANGE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("getTablesFileContent.versionCandidateId: Version candidate does not exist.")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("should return 404 if tables file not found")
    @SneakyThrows
    void getFileContent_fileNotFound() {
      final var exception = new DataModelFileNotFoundInVersionException("createTables.xml",
          "42");

      Mockito.doThrow(exception).when(fileService)
          .getTablesFileContent(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("DATA_MODEL_FILE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("Data-model file createTables.xml is not found in version 42")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath()
      );

      Mockito.verify(fileService).getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }

    @Test
    @DisplayName("should return 500 at any unexpected error")
    @SneakyThrows
    void getFileContent_unexpectedError() {
      Mockito.doThrow(RuntimeException.class)
          .when(fileService).getTablesFileContent(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("RUNTIME_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(fileService).getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }
  }

  @Nested
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/data-model/tables")
  @ControllerTest({
      CandidateVersionDataModelTablesController.class,
      ApplicationExceptionHandler.class
  })
  class CandidateVersionDataModelTablesControllerPutFileContentTest {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void putFileContent_happyPath() {
      final var expectedTableContent = TestUtils.getContent("controller/createTables.xml");
      final var eTag = RandomString.make();

      Mockito.doReturn(expectedTableContent).when(fileService)
          .getTablesFileContent(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
              .header("IF-Match", eTag)
              .contentType(MediaType.APPLICATION_XML)
              .content(expectedTableContent)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedTableContent.getBytes(StandardCharsets.UTF_8))
      ).andDo(document("versions/candidates/{versionCandidateId}/data-model/tables/GET"));

      Mockito.verify(fileService)
          .putTablesFileContent(VERSION_CANDIDATE_ID_STRING, expectedTableContent, eTag);
      Mockito.verify(fileService).getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void putFileContent_badRequest() {
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", "master")
              .contentType(MediaType.APPLICATION_XML)
              .content("any content")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );

      Mockito.verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void putFileContent_versionCandidateNotFound() {
      Mockito.doThrow(GerritChangeNotFoundException.class).when(versionManagementService)
          .getVersionDetails(VERSION_CANDIDATE_ID_STRING);

      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
              .contentType(MediaType.APPLICATION_XML)
              .content("anyContent")
      ).andExpectAll(
          status().isNotFound(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("CHANGE_NOT_FOUND")),
          jsonPath("$.details").value(
              is("putTablesFileContent.versionCandidateId: Version candidate does not exist.")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("should return 422 if faced ConstraintViolationException")
    @SneakyThrows
    void putFileContent_constraintViolationException() {
      final var eTag = RandomString.make();
      // mock exception
      var exception = Mockito.mock(ConstraintViolationException.class);
      var constraintViolation = Mockito.mock(ConstraintViolation.class);
      var constraintDescriptor = Mockito.mock(ConstraintDescriptor.class);
      var annotation = Mockito.mock(DDMExtensionChangelogFile.class);
      Mockito.doReturn(annotation).when(constraintDescriptor).getAnnotation();
      Mockito.doReturn(constraintDescriptor).when(constraintViolation).getConstraintDescriptor();
      Mockito.doReturn(Set.of(constraintViolation)).when(exception).getConstraintViolations();
      Mockito.doReturn("ExtendedChangelogFile message").when(exception).getMessage();

      var expectedTableContent = RandomString.make();

      Mockito.doThrow(exception)
          .when(fileService)
          .putTablesFileContent(VERSION_CANDIDATE_ID_STRING, expectedTableContent, eTag);

      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
              .header("IF-Match", eTag)
              .contentType(MediaType.APPLICATION_XML)
              .content(expectedTableContent)
      ).andExpectAll(
          status().isUnprocessableEntity(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("INVALID_CHANGELOG")),
          jsonPath("$.details").value(is("ExtendedChangelogFile message")),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(fileService)
          .putTablesFileContent(VERSION_CANDIDATE_ID_STRING, expectedTableContent, eTag);
      Mockito.verify(fileService, Mockito.never())
          .getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }

    @Test
    @DisplayName("should return 500 at any unexpected error")
    @SneakyThrows
    void putFileContent_unexpectedError() {
      final var expectedTableContent = TestUtils.getContent("controller/createTables.xml");
      final var eTag = RandomString.make();
      Mockito.doThrow(RuntimeException.class)
          .when(fileService)
          .putTablesFileContent(VERSION_CANDIDATE_ID_STRING, expectedTableContent, eTag);

      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", VERSION_CANDIDATE_ID)
              .header("IF-Match", eTag)
              .contentType(MediaType.APPLICATION_XML)
              .content(expectedTableContent)
      ).andExpectAll(
          status().isInternalServerError(),
          jsonPath("$.traceId").hasJsonPath(),
          jsonPath("$.code").value(is("RUNTIME_ERROR")),
          jsonPath("$.details").doesNotHaveJsonPath(),
          jsonPath("$.localizedMessage").doesNotHaveJsonPath(),
          content().contentType(MediaType.APPLICATION_JSON)
      );

      Mockito.verify(fileService)
          .putTablesFileContent(VERSION_CANDIDATE_ID_STRING, expectedTableContent, eTag);
      Mockito.verify(fileService, Mockito.never())
          .getTablesFileContent(VERSION_CANDIDATE_ID_STRING);
    }
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/data-model/tables/rollback should return 200")
  @SneakyThrows
  void rollbackFormTest() {
    Mockito.doNothing().when(fileService).rollbackTables(VERSION_CANDIDATE_ID_STRING);

    mockMvc.perform(post("/versions/candidates/{versionCandidateId}/data-model/tables/rollback",
        VERSION_CANDIDATE_ID_STRING)
    ).andExpect(
        status().isOk()
    ).andDo(document("versions/candidates/{versionCandidateId}/data-model/tables/rollback/POST"));

    Mockito.verify(fileService).rollbackTables(VERSION_CANDIDATE_ID_STRING);
  }
}
