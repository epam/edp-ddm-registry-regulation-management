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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(UserImportController.class)
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
        .apply(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  @SneakyThrows
  void handleFileUpload() {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "test".getBytes());
    var cephEntity = new CephFileInfoDto(CEPH_ENTITY_ID.toString(), file.getOriginalFilename(), file.getSize());
    when(userImportService.storeFile(file, new SecurityContext())).thenReturn(cephEntity);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isCreated(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.id", is(cephEntity.getId().toString())))
        .andDo(document("batch-loads/users/POST"));
  }

  @Test
  @SneakyThrows
  void getFilesInfo() {
    final String fileName = "users.csv";
    final var expectedFilesInfo = new CephFileInfoDto(CEPH_ENTITY_ID.toString(), fileName, 1L);
    when(userImportService.getFileInfo(any())).thenReturn(expectedFilesInfo);

    mockMvc.perform(get(BASE_URL))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.id", is(CEPH_ENTITY_ID.toString())),
                    jsonPath("$.name", is(fileName)),
                    jsonPath("$.size", is(1)))
        .andDo(document("batch-loads/users/GET"));

  }

  @Test
  @SneakyThrows
  void deleteFile() {
    mockMvc.perform(delete(BASE_URL + "/{id}", CEPH_ENTITY_ID.toString()))
            .andExpectAll(status().isNoContent())
        .andDo(document("batch-loads/users/{id}/DELETE"));
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

  @Test
  @SneakyThrows
  void startImport() {
    mockMvc.perform(post(BASE_URL + "/imports"))
            .andExpectAll(status().isAccepted())
        .andDo(document("batch-loads/users/imports/POST"));
  }
}