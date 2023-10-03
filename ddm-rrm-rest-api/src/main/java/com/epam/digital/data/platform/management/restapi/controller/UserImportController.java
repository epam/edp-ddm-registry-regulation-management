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

package com.epam.digital.data.platform.management.restapi.controller;

import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.users.model.CephFileInfoDto;
import com.epam.digital.data.platform.management.osintegration.service.OpenShiftService;
import com.epam.digital.data.platform.management.security.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.management.security.annotation.PreAuthorizeUserManagementRole;
import com.epam.digital.data.platform.management.security.model.SecurityContext;
import com.epam.digital.data.platform.management.users.service.UserImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@Tag(description = "Users bulk upload RestAPI",  name = "users-batch-loads-api")
@RestController
@PreAuthorizeUserManagementRole
@RequestMapping("/batch-loads/users")
@RequiredArgsConstructor
public class UserImportController {

  private static final String ATTACHMENT_HEADER_VALUE = "attachment; filename=\"%s\"";
  private final UserImportService userImportService;
  private final OpenShiftService openShiftService;

  @Operation(
      summary = "Store file endpoint",
      description = "### Endpoint purpose: \n This endpoint is used for downloading a file with registry user data. \n ### File validation: \nBefore saving the new _file_ to the storage, the server validates the _file_. The _file_ must be a __csv__ document and must have a non-empty __\"name\"__. Also the __\"file\"__ must not be null and empty. Also _file_ encoding must be UTF-8.\n ### Existing file handling: \n The _file_ in the ceph is tied to the user who uploads it, so when you try to upload a second _file_, the first _file_ in the ceph is overwritten.",
      parameters = @Parameter(
          in = ParameterIn.QUERY,
          name = "securityContext",
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Created. Returns uploaded file metadata",
              content = @Content(mediaType = MediaType.ALL_VALUE,
                  schema = @Schema(implementation = CephFileInfoDto.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"id\": \"123456789\",\n" +
                          "  \"name\": \"example_file.txt\",\n" +
                          "  \"size\": 1024\n" +
                          "}"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @PostMapping
  public ResponseEntity<CephFileInfoDto> handleFileUpload(@RequestParam("file") MultipartFile file,
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("#handleFileUpload() called");
    var response = userImportService.storeFile(file, securityContext);
    log.info("File {} was uploaded", file.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response);
  }

  @Operation(
      summary = "Get file information",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representation of a __file__ metadata. Since the file is mapped to a username, the file information of the user who executed the given endpoint is returned.",
      parameters = @Parameter(
          in = ParameterIn.QUERY,
          name = "securityContext",
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.ALL_VALUE,
                  schema = @Schema(implementation = CephFileInfoDto.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"id\": \"123456789\",\n" +
                          "  \"name\": \"example_file.txt\",\n" +
                          "  \"size\": 1024\n" +
                          "}"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @GetMapping
  public ResponseEntity<CephFileInfoDto> getFileInfo(
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("#getFilesInfo() called");
    var response = userImportService.getFileInfo(securityContext);
    log.info("Finished getting files info");
    return ResponseEntity.ok().body(response);
  }


  @Operation(
      summary = "Delete file endpoint",
      description = "### Endpoint purpose:\n This endpoint is used for deleting a __file__ from storage by id.",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "No content.",
              content = @Content(mediaType = MediaType.ALL_VALUE,
                  schema = @Schema(implementation = CephFileInfoDto.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(
      @PathVariable("id") @Parameter(required = true, description = "Resource identifier") String id) {
    log.info("deleteFile called");
    userImportService.delete(id);
    log.info("File {} was deleted", id);
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

  @Operation(
      summary = "Start import endpoint",
      description = "### Endpoint purpose:\n This endpoint is used for starting the process of importing the downloaded file with registry user data.",
      responses = {
          @ApiResponse(
              responseCode = "202",
              description = "Accepted",
              content = @Content(mediaType = MediaType.ALL_VALUE)
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @PostMapping("/imports")
  public ResponseEntity<Void> imports(@HttpSecurityContext SecurityContext securityContext) {
    log.info("imports called");
    var cephFileDto = userImportService.getFileInfo(securityContext);
    openShiftService.startImport(cephFileDto.getId(), securityContext);
    return ResponseEntity.accepted().build();
  }
}
