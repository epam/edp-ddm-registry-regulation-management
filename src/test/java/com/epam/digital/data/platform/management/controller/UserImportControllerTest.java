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

import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.model.dto.CephFileInfoDto;
import com.epam.digital.data.platform.management.service.OpenShiftService;
import com.epam.digital.data.platform.management.service.impl.UserImportServiceImpl;
import com.epam.digital.data.platform.management.util.TestUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SecuredControllerTest(UserImportController.class)
class UserImportControllerTest {

  static final String BASE_URL = "/batch-loads/users";
  static final String HEADER_VALUE = "attachment; filename=\"test\"";
  static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
  static final UUID CEPH_ENTITY_ID = UUID.fromString("10e23e2a-6830-42a6-bf21-d0a4a90b5706");

  MockMvc mockMvc;

  @MockBean
  UserImportServiceImpl userImportService;

  @MockBean
  OpenShiftService openShiftService;

  @RegisterExtension
  final RestDocumentationExtension restDocumentation = new RestDocumentationExtension();

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .apply(springSecurity()).build();
  }

  @SneakyThrows
  void handleFileUpload(String tokenPath, ResultMatcher... matchers) {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "users.csv",
        MediaType.MULTIPART_FORM_DATA_VALUE,
        "test".getBytes());
    var cephEntity = new CephFileInfoDto(CEPH_ENTITY_ID.toString(), file.getOriginalFilename(),
        file.getSize());
    when(userImportService.storeFile(file, new SecurityContext())).thenReturn(cephEntity);

    mockMvc.perform(multipart(BASE_URL).file(file)
            .with(authentication(tokenPath)))
        .andExpectAll(matchers);
  }

  @Test
  void validHandleFileUpload() {
    handleFileUpload("user-management-role-user-token",
        status().isCreated(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", is(CEPH_ENTITY_ID.toString())));
  }

  @Test
  void handleFileUploadShouldReturn403WithoutRequiredRole() {
    handleFileUpload("user-token", status().isForbidden());
  }

  @SneakyThrows
  void getFilesInfo(String tokenPath, ResultMatcher... matchers) {

    final var expectedFilesInfo = new CephFileInfoDto(CEPH_ENTITY_ID.toString(), "users.csv", 1L);
    when(userImportService.getFileInfo(any())).thenReturn(expectedFilesInfo);

    mockMvc.perform(get(BASE_URL).with(authentication(tokenPath)))
        .andExpectAll(matchers);

  }

  @Test
  void validGetFilesInfo() {
    getFilesInfo("user-management-role-user-token",
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", is(CEPH_ENTITY_ID.toString())),
        jsonPath("$.name", is("users.csv")),
        jsonPath("$.size", is(1)));
  }

  @Test
  void getFilesInfoShouldReturn403WithoutRequiredRole() {
    getFilesInfo("user-token", status().isForbidden());
  }

  @SneakyThrows
  void deleteFile(String tokenPath, ResultMatcher... matchers) {
    mockMvc.perform(delete(BASE_URL + "/{id}", CEPH_ENTITY_ID.toString())
            .with(authentication(tokenPath)))
        .andExpectAll(matchers);
  }

  @Test
  void validDeleteFile() {
    deleteFile("user-management-role-user-token", status().isNoContent());
  }

  @Test
  void deleteFileShouldReturn403WithoutRequiredRole() {
    deleteFile("user-token", status().isForbidden());
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


  @SneakyThrows
  void startImport(String tokenPath, ResultMatcher... matchers) {
    mockMvc.perform(post(BASE_URL + "/imports")
            .with(authentication(tokenPath)))
        .andExpectAll(matchers);
  }

  @Test
  void validStartImport() {
    startImport("user-management-role-user-token", status().isAccepted());
  }

  @Test
  void startImportShouldReturn403WithoutRequiredRole() {
    startImport("user-token", status().isForbidden());
  }

  private RequestPostProcessor authentication(String tokenPath) {
    return request -> {
      request.addHeader("x-access-token", getAuthToken(tokenPath));
      return request;
    };
  }

  private String getAuthToken(String path) {
    return TestUtils.getContent(path);
  }
}