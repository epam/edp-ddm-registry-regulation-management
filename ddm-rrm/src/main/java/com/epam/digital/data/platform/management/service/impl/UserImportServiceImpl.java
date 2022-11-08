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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.management.exception.CephInvocationException;
import com.epam.digital.data.platform.management.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.exception.VaultInvocationException;
import com.epam.digital.data.platform.management.model.dto.CephFileDto;
import com.epam.digital.data.platform.management.model.dto.CephFileInfoDto;
import com.epam.digital.data.platform.management.osintegration.exception.GetProcessingException;
import com.epam.digital.data.platform.management.osintegration.service.VaultService;
import com.epam.digital.data.platform.management.security.model.SecurityContext;
import com.epam.digital.data.platform.management.service.UserImportService;
import com.epam.digital.data.platform.management.service.UserInfoService;
import com.epam.digital.data.platform.management.service.ValidatorService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserImportServiceImpl implements UserImportService {

  private static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";
  private static final String USERNAME = "username";
  private static final String NAME = "name";
  private static final String SIZE = "size";
  private static final String ID = "id";

  private final CephService userImportCephService;
  private final UserInfoService userInfoService;
  @Value("${user-import-ceph.bucket}")
  private final String userImportFileBucket;
  private final VaultService vaultService;
  private final ValidatorService validatorService;

  @Override
  public CephFileInfoDto storeFile(MultipartFile file, SecurityContext securityContext) {
    var validationResult = validatorService.validate(file);

    String encodedFileName;
    try {
      encodedFileName = new String(Base64.getEncoder()
          .encode(validationResult.getFileName().getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new FileLoadProcessingException("Cannot read file name", e);
    }

    String username = userInfoService.createUsername(securityContext.getAccessToken());

    byte[] encryptedContent = getEncryptedContent(file);

    String existingId = getFileInfo(securityContext).getId();
    var cephKey = StringUtils.defaultIfBlank(existingId, UUID.randomUUID().toString());

    saveFileToCeph(cephKey, encryptedContent, encodedFileName, username,
        validationResult.getSize());

    return new CephFileInfoDto(cephKey, validationResult.getFileName(), file.getSize());
  }

  @Override
  public CephFileInfoDto getFileInfo(SecurityContext securityContext) {
    String username = userInfoService.createUsername(securityContext.getAccessToken());

    try {
      Set<String> keys = userImportCephService.getKeys(userImportFileBucket, StringUtils.EMPTY);
      if (keys.isEmpty()) {
        return new CephFileInfoDto();
      }

      return userImportCephService.getMetadata(userImportFileBucket, keys)
          .stream()
          .filter(cephObjectMetadata -> StringUtils.equals(
              cephObjectMetadata.getUserMetadata().get(USERNAME), username))
          .findFirst()
          .map(cephObjectMetadata -> mapToDto(cephObjectMetadata.getUserMetadata()))
          .orElse(new CephFileInfoDto());
    } catch (Exception e) {
      throw new CephInvocationException("Failed retrieve files info", e);
    }
  }

  @Override
  public void delete(String cephKey) {
    try {
      userImportCephService.delete(userImportFileBucket, Set.of(cephKey));
    } catch (Exception e) {
      throw new CephInvocationException("Failed delete file to ceph, cephKey: " + cephKey, e);
    }
  }

  @Override
  public CephFileDto downloadFile(String cephKey) {
    Optional<CephObject> cephObjectOptional;

    try {
      cephObjectOptional = userImportCephService.get(userImportFileBucket, cephKey);
    } catch (Exception e) {
      throw new CephInvocationException("Failed download file from ceph, cephKey: " + cephKey, e);
    }

    var cephObject = cephObjectOptional.orElseThrow(
        () -> new GetProcessingException("File not found in Ceph: " + cephKey));

    var fileName = Optional.ofNullable(cephObject.getMetadata().getUserMetadata().get(NAME))
        .orElseThrow(() -> new GetProcessingException(
            "Failed download file from ceph - missed file name, cephKey: " + cephKey));

    var decodedFileName = new String(Base64.getDecoder().decode(fileName), StandardCharsets.UTF_8);

    var decodedInputStream = decodeContent(cephObject.getContent());

    return new CephFileDto(decodedFileName, decodedInputStream,
        cephObject.getMetadata().getContentLength());
  }

  private byte[] getEncryptedContent(MultipartFile file) {
    try {
      return vaultService.encrypt(new String(file.getBytes(), StandardCharsets.UTF_8))
          .getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new VaultInvocationException("Exception during Vault content encryption", e);
    }
  }

  private CephFileInfoDto mapToDto(Map<String, String> userMetadata) {
    return CephFileInfoDto
        .builder()
        .id(userMetadata.getOrDefault(ID, StringUtils.EMPTY))
        .name(new String(
            Base64.getDecoder().decode(userMetadata.getOrDefault(NAME, StringUtils.EMPTY))))
        .size(Long.parseLong(userMetadata.getOrDefault(SIZE, "0")))
        .build();
  }

  private InputStream decodeContent(InputStream content) {
    try {
      return IOUtils.toInputStream(
          vaultService.decrypt(new String(content.readAllBytes(), StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new VaultInvocationException("Exception during Vault content decryption", e);
    }
  }

  private void saveFileToCeph(String cephKey, byte[] content, String originalFilename,
      String username, String originalFileSize) {
    log.info("Storing file to Ceph. Key: {}, Name: {}", cephKey, originalFilename);
    try {
      userImportCephService.put(
          userImportFileBucket,
          cephKey,
          CEPH_OBJECT_CONTENT_TYPE,
          Map.of(NAME, originalFilename, ID, cephKey, USERNAME, username, SIZE, originalFileSize),
          new ByteArrayInputStream(content));
    } catch (Exception e) {
      throw new CephInvocationException("Failed saving file to ceph", e);
    }
  }
}
