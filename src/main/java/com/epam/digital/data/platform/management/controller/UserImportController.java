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

import com.epam.digital.data.platform.management.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.model.dto.CephFileInfoDto;
import com.epam.digital.data.platform.management.service.OpenShiftService;
import com.epam.digital.data.platform.management.service.UserImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
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
  private final OpenShiftService openShiftService;

  public UserImportController(UserImportService userImportService, OpenShiftService openShiftService) {
    this.userImportService = userImportService;
    this.openShiftService = openShiftService;
  }

  @Operation(
      description = "Store file endpoint",
      parameters = @Parameter(in = ParameterIn.QUERY,
      name = "securityContext",
      schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.ALL_VALUE,
                  schema = @Schema(implementation = CephFileInfoDto.class)))})
  @PostMapping
  public ResponseEntity<CephFileInfoDto> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                          @HttpSecurityContext SecurityContext securityContext) {
    log.info("handleFileUpload called");
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(userImportService.storeFile(file, securityContext));
  }

  @Operation(description = "Get file information",
      parameters = @Parameter(in = ParameterIn.QUERY,
      name = "securityContext",
      schema = @Schema(type = "string")),
      responses = {
        @ApiResponse(responseCode = "200",
          description = "OK",
          content = @Content(mediaType = MediaType.ALL_VALUE,
            schema = @Schema(implementation = CephFileInfoDto.class)))})
  @GetMapping
  public ResponseEntity<CephFileInfoDto> getFileInfo(@HttpSecurityContext SecurityContext securityContext) {
    log.info("getFilesInfo called");
    return ResponseEntity.ok().body(userImportService.getFileInfo(securityContext));
  }


  @Operation(
      description = "Delete file endpoint",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.ALL_VALUE,
                  schema = @Schema(implementation = CephFileInfoDto.class)))})
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(@PathVariable("id") @Parameter(required = true, description = "Resource identifier") String id) {
    log.info("deleteFile called");
    userImportService.delete(id);
    return ResponseEntity.noContent().build();
  }

  /*
  TODO [MDTUDDM-12911] Will be available in the next release. Required: extend 'admin-portal-encryption-only-role' to decrypt data.
  @GetMapping("/{id}")
  public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) {
    log.info("downloadFile called");
    CephFileDto cephObject = userImportService.downloadFile(id);
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(cephObject.getContentLength())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    String.format(ATTACHMENT_HEADER_VALUE, cephObject.getFileName()))
            .body(new InputStreamResource(cephObject.getContent()));
  }*/

  @Operation(description = "Start import endpoint",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.ALL_VALUE))})
  @PostMapping("/imports")
  public ResponseEntity<Void> imports(@HttpSecurityContext SecurityContext securityContext) {
    log.info("imports called");
    openShiftService.startImport(securityContext);
    return ResponseEntity.accepted().build();
  }
}
