/*
 * Copyright 2023 EPAM Systems.
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
import com.epam.digital.data.platform.management.restapi.validation.ExistingVersionCandidate;
import com.epam.digital.data.platform.management.service.DataModelFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Registry regulations master version data-model tables file management Rest API")
@RestController
@RequestMapping("/versions/candidates/{versionCandidateId}/data-model/tables")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CandidateVersionDataModelTablesController {

  private final DataModelFileService dataModelFileService;

  @Operation(description = "Get data-model tables file content from requested version-candidate", parameters = {
      @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string"))},
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Version-candidate doesn't exist or tables file doesn't exists in requested version",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping
  public ResponseEntity<String> getTablesFileContent(
      @ExistingVersionCandidate @PathVariable @Parameter(description = "Version candidate identifier", required = true) Integer versionCandidateId) {
    log.info("Getting tables file content from version '{}' started", versionCandidateId);
    final var fileContent = dataModelFileService.getTablesFileContent(
        String.valueOf(versionCandidateId));
    log.info("There were found tables file content for version '{}'", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .body(fileContent);
  }
}
