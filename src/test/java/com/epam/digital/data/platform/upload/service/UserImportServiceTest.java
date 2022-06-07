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

package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.upload.exception.CephInvocationException;
import com.epam.digital.data.platform.upload.exception.GetProcessingException;
import com.epam.digital.data.platform.upload.exception.ImportProcessingException;
import com.epam.digital.data.platform.upload.model.dto.CephEntityReadDto;
import com.epam.digital.data.platform.upload.model.dto.CephFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {
  static final UUID CEPH_ENTITY_ID = UUID.fromString("10e23e2a-6830-42a6-bf21-d0a4a90b5706");
  static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";
  static final String FILE_BUCKET = "FILE_BUCKET";

  @Mock
  CephService cephService;

  MockMultipartFile file = new MockMultipartFile("file", "users.csv", MediaType.MULTIPART_FORM_DATA_VALUE, "test".getBytes());

  UserImportService userImportService;

  @BeforeEach
  void init() {
    this.userImportService = new UserImportService(cephService, FILE_BUCKET, new ObjectMapper());
  }

  @Test
  void validSaveFileToStorage() {
    userImportService.storeFile(file);

    verify(cephService).put(eq(FILE_BUCKET), any(), eq(CEPH_OBJECT_CONTENT_TYPE), any(), any());
  }

  @Test
  void shouldConvertAnyExceptionToUploadProcessingException() {
    when(cephService.put(any(), any(), any(), any(), any())).thenThrow(RuntimeException.class);

    var exception = assertThrows(CephInvocationException.class,
            () -> userImportService.storeFile(file));

    assertThat(exception.getMessage()).isEqualTo("Failed saving file to ceph");
  }

  @Test
  void storeFileShouldThrowUploadProcessingExceptionWithEmptyFile() {
    MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            StringUtils.EMPTY.getBytes());

    var exception = assertThrows(ImportProcessingException.class,
            () -> userImportService.storeFile(emptyFile));

    assertThat(exception.getMessage()).isEqualTo("File cannot be saved to Ceph - file is null or empty");
  }

  @Test
  void validDeleteFileFromStorage() {
    final String stringCephEntity = CEPH_ENTITY_ID.toString();

    userImportService.delete(stringCephEntity);

    verify(cephService).delete(FILE_BUCKET, Set.of(stringCephEntity));
  }

  @Test
  void deleteShouldThrowDeleteProcessingExceptionWithAnyErrorFromCeph() {
    final String stringCephEntity = CEPH_ENTITY_ID.toString();
    doThrow(RuntimeException.class).when(cephService).delete(FILE_BUCKET, Set.of(stringCephEntity));

    var exception = assertThrows(CephInvocationException.class,
            () -> userImportService.delete(stringCephEntity));

    assertThat(exception.getMessage()).isEqualTo("Failed delete file to ceph, cephKey: " + stringCephEntity);
  }

  @Test
  void validGetFilesInfoFromStorage() {
    final String stringCephEntity = CEPH_ENTITY_ID.toString();
    final Set<String> setOfKeys = Set.of(stringCephEntity);
    final CephEntityReadDto expectedFilesInfo = new CephEntityReadDto(stringCephEntity, "users.csv");
    final CephObjectMetadata cephObjectMetadata = new CephObjectMetadata();
    cephObjectMetadata.setUserMetadata(Map.of("id", stringCephEntity, "name", "users.csv"));
    when(cephService.getKeys(FILE_BUCKET, StringUtils.EMPTY)).thenReturn(setOfKeys);
    when(cephService.getMetadata(FILE_BUCKET, setOfKeys)).thenReturn(Collections.singletonList(cephObjectMetadata));

    CephEntityReadDto filesInfo = userImportService.getFileInfo();

    verify(cephService).getKeys(FILE_BUCKET, StringUtils.EMPTY);
    verify(cephService).getMetadata(FILE_BUCKET, setOfKeys);
    assertEquals(expectedFilesInfo, filesInfo);
  }

  @Test
  void getShouldThrowGetProcessingExceptionWithAnyErrorFromCeph() {
    when(cephService.getKeys(any(), any())).thenThrow(RuntimeException.class);

    var exception = assertThrows(CephInvocationException.class,
            () -> userImportService.getFileInfo());

    assertThat(exception.getMessage()).isEqualTo("Failed retrieve files info");
  }

  @Test
  @SneakyThrows
  void validDownloadFileFromStorage() {
    final byte[] contentBytes = "test".getBytes();
    var cephServiceResponse = CephObject.builder()
            .content(new ByteArrayInputStream(contentBytes))
            .metadata(CephObjectMetadata.builder().userMetadata(Map.of("name", "test.csv")).build())
            .build();
    when(cephService.get(FILE_BUCKET, CEPH_ENTITY_ID.toString()))
            .thenReturn(Optional.of(cephServiceResponse));

    CephFileDto cephFileDto = userImportService.downloadFile(CEPH_ENTITY_ID.toString());

    assertArrayEquals(contentBytes, cephFileDto.getContent().readAllBytes());
    assertEquals(cephServiceResponse.getMetadata().getUserMetadata().get("name"), cephFileDto.getFileName());
    assertEquals(cephServiceResponse.getMetadata().getContentLength(), cephFileDto.getContentLength());
  }

  @Test
  void downloadShouldThrowCephInvocationExceptionWithAnyErrorFromCeph() {
    String stringCephEntity = CEPH_ENTITY_ID.toString();
    when(cephService.get(FILE_BUCKET, stringCephEntity)).thenThrow(RuntimeException.class);

    var exception = assertThrows(CephInvocationException.class,
            () -> userImportService.downloadFile(stringCephEntity));

    assertThat(exception.getMessage()).isEqualTo("Failed download file from ceph, cephKey: " + stringCephEntity);
  }

  @Test
  void downloadShouldThrowGetProcessingExceptionWhenNoFilenameInMetadata() {
    String stringCephEntity = CEPH_ENTITY_ID.toString();
    var cephServiceResponse = CephObject.builder()
            .content(new ByteArrayInputStream("test".getBytes()))
            .metadata(CephObjectMetadata.builder().userMetadata(Collections.emptyMap()).build())
            .build();
    when(cephService.get(FILE_BUCKET, stringCephEntity)).thenReturn(Optional.of(cephServiceResponse));

    var exception = assertThrows(GetProcessingException.class,
            () -> userImportService.downloadFile(stringCephEntity));

    assertThat(exception.getMessage())
            .isEqualTo("Failed download file from ceph - missed file name, cephKey: " + stringCephEntity);
  }
}