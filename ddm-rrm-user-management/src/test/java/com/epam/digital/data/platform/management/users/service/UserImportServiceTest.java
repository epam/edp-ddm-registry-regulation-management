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

package com.epam.digital.data.platform.management.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.management.osintegration.exception.GetProcessingException;
import com.epam.digital.data.platform.management.osintegration.service.VaultService;
import com.epam.digital.data.platform.management.security.model.SecurityContext;
import com.epam.digital.data.platform.management.users.exception.CephInvocationException;
import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.exception.VaultInvocationException;
import com.epam.digital.data.platform.management.users.model.CephFileDto;
import com.epam.digital.data.platform.management.users.model.CephFileInfoDto;
import com.epam.digital.data.platform.management.users.model.ValidationResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(SpringExtension.class)
class UserImportServiceTest {

  static final UUID CEPH_ENTITY_ID = UUID.fromString("10e23e2a-6830-42a6-bf21-d0a4a90b5706");
  static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";
  static final String FILE_BUCKET = "FILE_BUCKET";

  @Mock
  CephService cephService;

  @Mock
  UserInfoService userInfoService;

  @Mock
  VaultService vaultService;

  @Mock
  ValidatorService validatorService;

  UserImportService userImportService;

  MockMultipartFile file = new MockMultipartFile("file", "users.csv", MediaType.MULTIPART_FORM_DATA_VALUE, "test".getBytes());


  @BeforeEach
  void init() {
    this.userImportService = new UserImportServiceImpl(cephService, userInfoService, FILE_BUCKET, vaultService, validatorService);
  }

  @Test
  void validSaveFileToStorage() {
    var contentStr = "test";
    var validationResult = new ValidationResult();
    validationResult.setFileName(file.getOriginalFilename());
    validationResult.setSize(String.valueOf(file.getSize()));
    when(userInfoService.createUsername("userToken")).thenReturn("userName");
    when(vaultService.encrypt(contentStr)).thenReturn(contentStr);
    when(validatorService.validate(file)).thenReturn(validationResult);

    userImportService.storeFile(file, new SecurityContext("userToken"));

    verify(cephService).put(eq(FILE_BUCKET), anyString(), eq(CEPH_OBJECT_CONTENT_TYPE), any(), any());
  }

  @Test
  void storeFileShouldThrowUploadProcessingExceptionWithEmptyFile() {
    when(validatorService.validate(file))
        .thenThrow(new FileLoadProcessingException("File cannot be saved to Ceph - file is null or empty"));

    assertThatCode(() -> userImportService.storeFile(file, new SecurityContext()))
        .isInstanceOf(FileLoadProcessingException.class)
        .hasMessage("File cannot be saved to Ceph - file is null or empty");

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

    assertThatCode(() -> userImportService.delete(stringCephEntity))
        .isInstanceOf(CephInvocationException.class)
        .hasMessage("Failed delete file to ceph, cephKey: " + stringCephEntity);

  }

  @Test
  void validGetFilesInfoFromStorage() {
    final String stringCephEntity = CEPH_ENTITY_ID.toString();
    final Set<String> setOfKeys = Set.of(stringCephEntity);
    final var expectedFilesInfo = new CephFileInfoDto(stringCephEntity, "users.csv", 1L);
    final CephObjectMetadata cephObjectMetadata = new CephObjectMetadata();
    cephObjectMetadata.setUserMetadata(Map.of("id", stringCephEntity,
        "name",
        new String(Base64.getEncoder().encode("users.csv".getBytes(StandardCharsets.UTF_8))),
        "username", "userName",
        "size", "1"));
    when(cephService.getKeys(FILE_BUCKET, StringUtils.EMPTY)).thenReturn(setOfKeys);
    when(cephService.getMetadata(FILE_BUCKET, setOfKeys)).thenReturn(Collections.singletonList(cephObjectMetadata));
    when(userInfoService.createUsername("userToken")).thenReturn("userName");

    var filesInfo = userImportService.getFileInfo(new SecurityContext("userToken"));

    verify(cephService).getKeys(FILE_BUCKET, StringUtils.EMPTY);
    verify(cephService).getMetadata(FILE_BUCKET, setOfKeys);
    assertThat(filesInfo).isEqualTo(expectedFilesInfo);
  }

  @Test
  void getShouldThrowGetProcessingExceptionWithAnyErrorFromCeph() {
    when(userInfoService.createUsername("userToken")).thenReturn("userName");
    when(cephService.getKeys(FILE_BUCKET, StringUtils.EMPTY)).thenThrow(new RuntimeException());

    assertThatCode(() -> userImportService.getFileInfo(new SecurityContext("userToken")))
        .isInstanceOf(CephInvocationException.class)
        .hasMessage("Failed retrieve files info");
  }

  @Test
  @SneakyThrows
  void validDownloadFileFromStorage() {
    final byte[] contentBytes = "test".getBytes();
    final String fileName = "test.csv";
    final String encodedFileName = new String(Base64.getEncoder().encode(fileName.getBytes(StandardCharsets.UTF_8)));
    var cephServiceResponse = CephObject.builder()
        .content(new ByteArrayInputStream(contentBytes))
        .metadata(CephObjectMetadata.builder().userMetadata(Map.of("name", encodedFileName)).build())
        .build();
    when(cephService.get(FILE_BUCKET, CEPH_ENTITY_ID.toString()))
        .thenReturn(Optional.of(cephServiceResponse));
    when(vaultService.decrypt("test")).thenReturn("test");

    CephFileDto cephFileDto = userImportService.downloadFile(CEPH_ENTITY_ID.toString());

    assertThat(cephFileDto.getContent().readAllBytes()).isEqualTo(contentBytes);
    assertThat(cephFileDto.getFileName()).isEqualTo(fileName);
    assertThat(cephFileDto.getContentLength()).isEqualTo(cephServiceResponse.getMetadata().getContentLength());
  }

  @Test
  void downloadShouldThrowCephInvocationExceptionWithAnyErrorFromCeph() {
    String stringCephEntity = CEPH_ENTITY_ID.toString();
    when(cephService.get(FILE_BUCKET, stringCephEntity)).thenThrow(RuntimeException.class);

    assertThatCode(() -> userImportService.downloadFile(stringCephEntity))
        .isInstanceOf(CephInvocationException.class)
        .hasMessage("Failed download file from ceph, cephKey: " + stringCephEntity);
  }

  @Test
  void downloadShouldThrowGetProcessingExceptionWhenNoFilenameInMetadata() {
    String stringCephEntity = CEPH_ENTITY_ID.toString();
    var cephServiceResponse = CephObject.builder()
        .content(new ByteArrayInputStream("test".getBytes()))
        .metadata(CephObjectMetadata.builder().userMetadata(Collections.emptyMap()).build())
        .build();
    when(cephService.get(FILE_BUCKET, stringCephEntity)).thenReturn(Optional.of(cephServiceResponse));

    assertThatCode(() -> userImportService.downloadFile(stringCephEntity))
        .isInstanceOf(GetProcessingException.class)
        .hasMessage("Failed download file from ceph - missed file name, cephKey: " + stringCephEntity);

  }

  @Test
  void storeFileShouldThrowVaultInvocationExceptionWithErrorFromVault() {
    var validationResult = new ValidationResult();
    validationResult.setFileName(file.getOriginalFilename());
    when(validatorService.validate(file)).thenReturn(validationResult);

    assertThatCode(() -> userImportService.storeFile(file, new SecurityContext()))
        .isInstanceOf(VaultInvocationException.class)
        .hasMessage("Exception during Vault content encryption");

  }
}