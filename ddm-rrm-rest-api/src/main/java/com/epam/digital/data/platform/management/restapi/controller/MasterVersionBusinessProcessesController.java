/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.restapi.controller;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import com.epam.digital.data.platform.management.groups.service.GroupService;
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
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Registry regulations master Business processes management Rest API")
@RestController
@Validated
@RequestMapping("/versions/master/business-processes")
@RequiredArgsConstructor
public class MasterVersionBusinessProcessesController {

  private final BusinessProcessService businessProcessService;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final GroupService groupService;

  @Operation(description = "Get business processes list for master version", parameters = {
      @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string"))},
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
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping
  public ResponseEntity<List<BusinessProcessDetailsShort>> getBusinessProcessesFromMaster() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting business processes from master");
    var response = businessProcessService.getProcessesByVersion(masterVersionId).stream()
        .map(e -> BusinessProcessDetailsShort.builder()
            .name(e.getName())
            .title(e.getTitle())
            .created(e.getCreated())
            .updated(e.getUpdated())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} business processes in master", response.size());
    return ResponseEntity.ok().body(response);
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
              content = @Content(mediaType = MediaType.TEXT_XML_VALUE,
                  schema = @Schema(implementation = Map.class))),
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
  @GetMapping("/{businessProcessName}")
  public ResponseEntity<String> getBusinessProcess(
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting {} business process from master", businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName, masterVersionId);
    log.info("Finished getting {} business process from master", businessProcessName);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(description = "Update business process",
      parameters = {
          @Parameter(in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")),
          @Parameter(in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string")
          )
      },

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
  @PutMapping("/{businessProcessName}")
  public ResponseEntity<String> updateBusinessProcess(
      @RequestBody @BusinessProcess String businessProcess,
      @PathVariable @Parameter(description = "Process name", required = true)
      String businessProcessName,
      @RequestHeader HttpHeaders headers) {

    log.info("Started updating business process {} for master", businessProcessName);

    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");

    businessProcessService.updateProcess(businessProcess, businessProcessName, masterVersionId,
        eTag);
    log.info(
        "Finished updating business process {} for master. Retrieving this process",
        businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName, masterVersionId);
    log.info("Finished getting business process {} from {} version candidate", businessProcessName,
        masterVersionId);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
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
      @PathVariable @Parameter(description = "Name of the new process to be created", required = true) String businessProcessName) {
    log.info("Started creating business process {} for master version", businessProcessName);
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();

    businessProcessService.createProcess(businessProcessName, businessProcess, masterVersionId);
    log.info("Finished creating business process {} for master version. Retrieving process",
        businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName,
        masterVersionId);
    log.info("Finished getting business process {} from master version", businessProcessName);
    return ResponseEntity.created(URI.create(
            String.format("/versions/master/business-processes/%s",
                businessProcessName)))
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(description = "Delete business process",
      parameters = {
          @Parameter(in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")),
          @Parameter(in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string")
          )
      },
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
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName,
      @RequestHeader HttpHeaders headers) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");

    log.info("Started deleting business process {} from master", businessProcessName);
    businessProcessService.deleteProcess(businessProcessName, masterVersionId, eTag);
    groupService.deleteProcessDefinition(businessProcessName, masterVersionId);
    log.info("Finished deleting business process {} from master", businessProcessName);
    return ResponseEntity.noContent().build();
  }
}
