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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/batch-loads/users")
public class UserImportController {
  private static final String ATTACHMENT_HEADER_VALUE = "attachment; filename=\"%s\"";
  private final UserImportService userImportService;

  public UserImportController(UserImportService userImportService) {
    this.userImportService = userImportService;
  }

  @PostMapping
  public ResponseEntity<CephEntityImportDto> handleFileUpload(@RequestParam("file") MultipartFile file) {
    log.info("handleFileUpload called");
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(userImportService.storeFile(file));
  }

  @GetMapping
  public ResponseEntity<CephEntityReadDto> getFileInfo() {
    log.info("getFilesInfo called");
    return ResponseEntity.ok().body(userImportService.getFileInfo());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(@PathVariable("id") String id) {
    log.info("deleteFile called");
    userImportService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) {
    CephFileDto cephObject = userImportService.downloadFile(id);
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(cephObject.getContentLength())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    String.format(ATTACHMENT_HEADER_VALUE, cephObject.getFileName()))
            .body(new InputStreamResource(cephObject.getContent()));
  }

}
