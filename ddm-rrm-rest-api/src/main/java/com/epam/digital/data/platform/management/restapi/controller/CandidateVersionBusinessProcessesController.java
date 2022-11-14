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

import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.validation.businessProcess.BusinessProcess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Registry regulations version-candidate Business processes management Rest API")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/versions/candidates/{versionCandidateId}/business-processes")
public class CandidateVersionBusinessProcessesController {

  private final BusinessProcessService businessProcessService;

  @Operation(description = "Get business processes list",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = BusinessProcessDetailsShort.class)))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping
  public ResponseEntity<List<BusinessProcessDetailsShort>> getBusinessProcessesBuVersionId(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId) {
    log.info("Started getting business processes from {} version candidate", versionCandidateId);
    var response = businessProcessService.getProcessesByVersion(versionCandidateId).stream()
        .map(e -> BusinessProcessDetailsShort.builder()
            .name(e.getName())
            .title(e.getTitle())
            .created(e.getCreated())
            .updated(e.getUpdated())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} business processes from {} version candidate", response.size(),
        versionCandidateId);
    return ResponseEntity.ok().body(response);
  }

  @Operation(description = "Create new business process",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "201",
              description = "Created",
              content = @Content(mediaType = MediaType.TEXT_XML_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "409",
              description = "Conflict",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "422",
              description = "Unprocessable Entity",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @PostMapping("/{businessProcessName}")
  public ResponseEntity<String> createBusinessProcess(
      @RequestBody @BusinessProcess String businessProcess,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Name of the new process to be created", required = true) String businessProcessName) {
    log.info("Started creating business process {} for {} version candidate", businessProcessName,
        versionCandidateId);
    businessProcessService.createProcess(businessProcessName, businessProcess, versionCandidateId);
    log.info("Finished creating business process {} for {} version candidate. Retrieving process",
        businessProcessName, versionCandidateId);
    var response = businessProcessService.getProcessContent(businessProcessName,
        versionCandidateId);
    log.info("Finished getting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    return ResponseEntity.created(URI.create(
            String.format("/versions/candidates/%s/business-processes/%s", versionCandidateId,
                businessProcessName)))
        .contentType(MediaType.TEXT_XML)
        .body(response);
  }

  @Operation(description = "Get business process",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.TEXT_XML_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class)))})

  @GetMapping("/{businessProcessName}")
  public ResponseEntity<String> getBusinessProcess(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName) {
    log.info("Started getting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    var response = businessProcessService.getProcessContent(businessProcessName,
        versionCandidateId);
    log.info("Finished getting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .body(response);
  }

  @Operation(description = "Update business process",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.TEXT_XML_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "422",
              description = "Unprocessable Entity",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @PutMapping("/{businessProcessName}")
  public ResponseEntity<String> updateBusinessProcess(
      @RequestBody @BusinessProcess String businessProcess,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName) {
    log.info("Started updating business process {} for {} version candidate", businessProcessName,
        versionCandidateId);
    businessProcessService.updateProcess(businessProcess, businessProcessName, versionCandidateId);
    log.info(
        "Finished updating business process {} for {} version candidate. Retrieving this process",
        businessProcessName, versionCandidateId);
    var response = businessProcessService.getProcessContent(businessProcessName,
        versionCandidateId);
    log.info("Finished getting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .body(response);
  }

  @Operation(description = "Delete business process",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "204",
              description = "No Content",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @DeleteMapping("/{businessProcessName}")
  public ResponseEntity<String> deleteBusinessProcess(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName) {
    log.info("Started deleting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    businessProcessService.deleteProcess(businessProcessName, versionCandidateId);
    log.info("Finished deleting business process {} from {} version candidate", businessProcessName,
        versionCandidateId);
    return ResponseEntity.noContent().build();
  }

}
