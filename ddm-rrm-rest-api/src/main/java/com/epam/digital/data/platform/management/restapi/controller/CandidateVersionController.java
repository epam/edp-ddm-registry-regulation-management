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

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.CreateVersionRequest;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.model.VersionChangesInfo;
import com.epam.digital.data.platform.management.restapi.model.VersionInfo;
import com.epam.digital.data.platform.management.restapi.model.VersionInfoDetailed;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Registry regulations version-candidate management Rest API")
@RestController
@RequestMapping("/versions/candidates")
@RequiredArgsConstructor
public class CandidateVersionController {

  private final VersionManagementService versionManagementService;
  private final ControllerMapper controllerMapper;

  @Operation(description = "Get list of existing opened version-candidates",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = VersionInfo.class)))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
      })
  @GetMapping
  public ResponseEntity<List<VersionInfo>> getVersionsList() {
    log.info("Started getting versions list");
    var response = versionManagementService.getVersionsList()
        .stream().map(c -> VersionInfo.builder()
            .id(String.valueOf(c.getNumber()))
            .name(c.getSubject())
            .description(c.getDescription())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} version candidates", response.size());
    return ResponseEntity.ok(response);
  }

  @Operation(description = "Abandon the existing opened version-candidate. After this operation the version-candidate won't take any changes anymore.",
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
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @PostMapping("/{versionCandidateId}/decline")
  public ResponseEntity<String> declineVersionCandidate(
      @PathVariable @Parameter(description = "Version candidate identifier to abandon", required = true) String versionCandidateId) {
    log.info("Declining {} version candidate", versionCandidateId);
    versionManagementService.decline(versionCandidateId);
    log.info("{} version candidate was declined", versionCandidateId);
    return ResponseEntity.ok().build();
  }

  @Operation(description = "Integrate version-candidate changes into master version of registry regulation",
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
          @ApiResponse(responseCode = "409",
              description = "Conflict",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @PostMapping("/{versionCandidateId}/submit")
  public ResponseEntity<String> submitVersionCandidate(
      @PathVariable @Parameter(description = "Version candidate identifier to be merged into master version", required = true) String versionCandidateId) {
    log.info("Started submitting {} version candidate", versionCandidateId);
    versionManagementService.markReviewed(versionCandidateId);
    log.info("Version candidate {} was marked reviewed", versionCandidateId);
    versionManagementService.submit(versionCandidateId);
    log.info("Version candidate {} was submitted", versionCandidateId);
    return ResponseEntity.ok().build();
  }


  @Operation(description = "Create new version-candidate from current state of master version.",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "201",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionInfoDetailed.class))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "422",
              description = "Unprocessable Entity",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @PostMapping
  public ResponseEntity<VersionInfoDetailed> createNewVersion(
      @RequestBody CreateVersionRequest requestDto) {
    log.info("Creating new version with name {}", requestDto.getName());
    final CreateChangeInputDto changeInputDto = controllerMapper.toDto(requestDto);
    var versionId = versionManagementService.createNewVersion(changeInputDto);
    var changeInfo = versionManagementService.getVersionDetails(versionId);
    log.info("Version candidate with name {} was created: version id - {}", requestDto.getName(),
        versionId);
    return ResponseEntity.created(URI.create("/versions/candidates/" + versionId))
        .body(controllerMapper.toVersionInfoDetailed(changeInfo));
  }

  @Operation(description = "Acquire version-candidate full details",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionInfoDetailed.class))),
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
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @GetMapping("/{versionCandidateId}")
  public ResponseEntity<VersionInfoDetailed> getVersionDetails(
      @PathVariable @Parameter(description = "Version-candidate identifier", required = true) String versionCandidateId) {
    log.info("Started getting detailed info about {} version candidate", versionCandidateId);
    var response = controllerMapper.toVersionInfoDetailed(
        versionManagementService.getVersionDetails(versionCandidateId));
    log.info("Finished getting detailed info about {} version candidate", versionCandidateId);
    return ResponseEntity.ok().body(response);
  }

  @Operation(description = "Get version changes by version-candidate id",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          required = true,
          description = "Token used for endpoint security",
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionChangesInfo.class))),
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
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @GetMapping("/{versionCandidateId}/changes")
  public ResponseEntity<VersionChangesInfo> getVersionChanges(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Getting changes for version {}", versionCandidateId);
    var versionChanges = versionManagementService.getVersionChanges(versionCandidateId);
    log.info("Version changes for version {} found", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(controllerMapper.toVersionChangesInfo(versionChanges));
  }


  @Operation(description = "Rebase changes from master version",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          required = true,
          description = "Token used for endpoint security",
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
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
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @GetMapping("/{versionCandidateId}/rebase")
  public ResponseEntity<Void> rebase(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Started version candidate {} rebase", versionCandidateId);
    versionManagementService.rebase(versionCandidateId);
    log.info("Version candidate {} successfully rebased", versionCandidateId);
    return ResponseEntity.ok().build();
  }
}
