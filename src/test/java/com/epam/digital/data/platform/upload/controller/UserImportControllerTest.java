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

package com.epam.digital.data.platform.upload.controller;

import com.epam.digital.data.platform.upload.model.dto.CephEntityImportDto;
import com.epam.digital.data.platform.upload.model.dto.CephEntityReadDto;
import com.epam.digital.data.platform.upload.model.dto.CephFileDto;
import com.epam.digital.data.platform.upload.service.UserImportService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(UserImportController.class)
class UserImportControllerTest {

  static final String BASE_URL = "/batch-loads/users";
  static final String HEADER_VALUE = "attachment; filename=\"test\"";
  static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
  static final UUID CEPH_ENTITY_ID = UUID.fromString("10e23e2a-6830-42a6-bf21-d0a4a90b5706");

  @Autowired
  MockMvc mockMvc;

  @MockBean
  UserImportService userImportService;

  @Test
  @SneakyThrows
  void handleFileUpload() {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "test".getBytes());
    var cephEntity = new CephEntityImportDto(CEPH_ENTITY_ID);
    when(userImportService.storeFile(file)).thenReturn(cephEntity);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isCreated(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.id", is(cephEntity.getId().toString())));
  }

  @Test
  @SneakyThrows
  void getFilesInfo() {
    final String fileName = "users.csv";
    final CephEntityReadDto expectedFilesInfo = new CephEntityReadDto(CEPH_ENTITY_ID.toString(), fileName);
    when(userImportService.getFileInfo()).thenReturn(expectedFilesInfo);

    mockMvc.perform(get(BASE_URL))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.id", is(CEPH_ENTITY_ID.toString())),
                    jsonPath("$.name", is(fileName)));

  }

  @Test
  @SneakyThrows
  void deleteFile() {
    mockMvc.perform(delete(BASE_URL + "/{id}", CEPH_ENTITY_ID.toString()))
            .andExpectAll(status().isNoContent());
  }

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
  }
}