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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.model.dto.CephFileInfoDto;
import com.epam.digital.data.platform.management.service.OpenShiftService;
import com.epam.digital.data.platform.management.service.impl.UserImportServiceImpl;
import com.epam.digital.data.platform.management.util.TestUtils;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SecuredControllerTest(UserImportController.class)
@DisplayName("User import controller tests")
class UserImportControllerTest {

  /*
  TODO [MDTUDDM-12911] Will be available in the next release. Required: extend 'admin-portal-encryption-only-role' to decrypt data.
  static final String HEADER_VALUE = "attachment; filename=\"test\"";
  static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
  */
  static final UUID CEPH_ENTITY_ID = UUID.fromString("10e23e2a-6830-42a6-bf21-d0a4a90b5706");

  MockMvc mockMvc;

  @MockBean
  UserImportServiceImpl userImportService;

  @MockBean
  OpenShiftService openShiftService;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .apply(springSecurity())
        .build();
  }

  @Nested
  @SecuredControllerTest(UserImportController.class)
  @DisplayName("POST /batch-loads/users")
  class UserImportControllerFileUploadTest {

    @SneakyThrows
    void handleFileUpload(String tokenPath, ResultHandler resultHandler,
        ResultMatcher... matchers) {
      final var mockMultipartFile = new MockMultipartFile(
          "file",
          "users.csv",
          MediaType.MULTIPART_FORM_DATA_VALUE,
          "test".getBytes());
      final var cephEntity = new CephFileInfoDto(CEPH_ENTITY_ID.toString(),
          mockMultipartFile.getOriginalFilename(),
          mockMultipartFile.getSize());
      Mockito.doReturn(cephEntity)
          .when(userImportService).storeFile(mockMultipartFile, new SecurityContext());

      mockMvc.perform(
          multipart("/batch-loads/users").file(mockMultipartFile)
              .header("x-access-token", TestUtils.getContent(tokenPath))
      ).andExpectAll(
          matchers
      ).andDo(resultHandler);
    }

    @Test
    @DisplayName("should return 201 if user has user-management role")
    void validHandleFileUpload() {
      handleFileUpload("user-management-role-user-token",
          document("batch-loads/users/POST"),
          status().isCreated(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.id", is(CEPH_ENTITY_ID.toString())));
    }

    @Test
    @DisplayName("should return 403 if user doesn't have user-management role")
    void handleFileUploadShouldReturn403WithoutRequiredRole() {
      handleFileUpload("user-token", result -> {
      }, status().isForbidden());
    }
  }

  @Nested
  @SecuredControllerTest(UserImportController.class)
  @DisplayName("GET /batch-loads/users")
  class UserImportControllerGetFilesInfoTest {

    @SneakyThrows
    void getFilesInfo(String tokenPath, ResultHandler resultHandler, ResultMatcher... matchers) {
      final var expectedFilesInfo = new CephFileInfoDto(CEPH_ENTITY_ID.toString(), "users.csv", 1L);

      Mockito.doReturn(expectedFilesInfo)
          .when(userImportService).getFileInfo(new SecurityContext());

      mockMvc.perform(
          get("/batch-loads/users")
              .header("x-access-token", TestUtils.getContent(tokenPath))
      ).andExpectAll(
          matchers
      ).andDo(resultHandler);

    }

    @Test
    @DisplayName("should return 200 if user has user-management role")
    void validGetFilesInfo() {
      getFilesInfo("user-management-role-user-token",
          document("batch-loads/users/GET"),
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.id", is(CEPH_ENTITY_ID.toString())),
          jsonPath("$.name", is("users.csv")),
          jsonPath("$.size", is(1)));
    }

    @Test
    @DisplayName("should return 403 if user doesn't have user-management role")
    void getFilesInfoShouldReturn403WithoutRequiredRole() {
      getFilesInfo("user-token", result -> {
      }, status().isForbidden());
    }
  }

  @Nested
  @SecuredControllerTest(UserImportController.class)
  @DisplayName("DELETE /batch-loads/users/{id}")
  class UserImportControllerFileDeleteTest {

    @SneakyThrows
    void deleteFile(String tokenPath, ResultHandler resultHandler, ResultMatcher... matchers) {
      mockMvc.perform(
          delete("/batch-loads/users/{id}", CEPH_ENTITY_ID.toString())
              .header("x-access-token", TestUtils.getContent(tokenPath))
      ).andExpectAll(
          matchers
      ).andDo(resultHandler);
    }

    @Test
    @DisplayName("should return 204 if user has user-management role")
    void validDeleteFile() {
      deleteFile("user-management-role-user-token",
          document("batch-loads/users/{id}/DELETE"),
          status().isNoContent());

      Mockito.verify(userImportService).delete(CEPH_ENTITY_ID.toString());
    }

    @Test
    @DisplayName("should return 403 if user doesn't have user-management role")
    void deleteFileShouldReturn403WithoutRequiredRole() {
      deleteFile("user-token", result -> {
      }, status().isForbidden());

      Mockito.verify(userImportService, Mockito.never()).delete(anyString());
    }
  }

 /*
  TODO [MDTUDDM-12911] Will be available in the next release. Required: extend 'admin-portal-encryption-only-role' to decrypt data.

  @Test
  @SneakyThrows
  void downloadFile() {
    final String stringCephEntityId = CEPH_ENTITY_ID.toString();
    final String fileName = "test";
    final long contentLength = fileName.getBytes().length;
    final CephFileDto cephFileDto = new CephFileDto(fileName, new ByteArrayInputStream(fileName.getBytes()), contentLength);
    when(userImportService.downloadFile(stringCephEntityId)).thenReturn(cephFileDto);

    mockMvc.perform(get(BASE_URL + "/{id}", stringCephEntityId))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_OCTET_STREAM),
                    header().longValue(CONTENT_LENGTH_HEADER_NAME, contentLength),
                    header().string(HttpHeaders.CONTENT_DISPOSITION, HEADER_VALUE)
            );
  }*/

  @Nested
  @SecuredControllerTest(UserImportController.class)
  @DisplayName("POST /batch-loads/users/imports")
  class UserImportControllerFileImportsTest {

    @SneakyThrows
    void startImport(String tokenPath, ResultHandler resultHandler, ResultMatcher... matchers) {
      mockMvc.perform(
          post("/batch-loads/users/imports")
              .header("x-access-token", TestUtils.getContent(tokenPath))
      ).andExpectAll(
          matchers
      ).andDo(resultHandler);
    }

    @Test
    @DisplayName("should return 202 if user has user-management role")
    void validStartImport() {
      startImport("user-management-role-user-token",
          document("batch-loads/users/imports/POST"),
          status().isAccepted());

      Mockito.verify(openShiftService).startImport(new SecurityContext());
    }

    @Test
    @DisplayName("should return 403 if user doesn't have user-management role")
    void startImportShouldReturn403WithoutRequiredRole() {
      startImport("user-token", result -> {
      }, status().isForbidden());

      Mockito.verify(openShiftService, Mockito.never()).startImport(any());
    }
  }
}