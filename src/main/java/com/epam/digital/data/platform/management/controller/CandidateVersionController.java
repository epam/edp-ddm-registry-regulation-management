/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.controller;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.model.dto.VersionInfo;
import com.epam.digital.data.platform.management.model.dto.VersionInfoDetailed;
import com.epam.digital.data.platform.management.model.exception.DetailedErrorResponse;
import com.epam.digital.data.platform.management.service.VersionManagementService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Registry regulations version-candidate management Rest API")
@RestController
@RequestMapping("/versions/candidates")
@RequiredArgsConstructor
public class CandidateVersionController {

  private final VersionManagementService versionManagementService;

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
  public ResponseEntity<List<VersionInfo>> getVersionsList() throws Exception {
    return ResponseEntity.ok(
        versionManagementService.getVersionsList()
            .stream().map(c -> VersionInfo.builder()
                .id(String.valueOf(c.getNumber()))
                .name(c.getSubject())
                .description(c.getDescription())
                .build())
            .collect(Collectors.toList())
    );
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
    versionManagementService.decline(versionCandidateId);
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
  public ResponseEntity<String> submitVersionCandidate(@PathVariable @Parameter(description = "Version candidate identifier to be merged into master version", required = true) String versionCandidateId) {
    versionManagementService.markReviewed(versionCandidateId);
    versionManagementService.submit(versionCandidateId);
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
      @RequestBody CreateVersionRequest requestDto) throws Exception {
    var versionId = versionManagementService.createNewVersion(requestDto);
    var changeInfo = versionManagementService.getVersionDetails(versionId);
    return ResponseEntity.created(URI.create("/versions/candidates/" + versionId))
        .body(mapToVersionInfoDetailed(changeInfo));
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
      @PathVariable @Parameter(description = "Version-candidate identifier", required = true) String versionCandidateId) throws Exception {
    ChangeInfoDetailedDto changeInfoDetailedDto = versionManagementService.getVersionDetails(versionCandidateId);
    return ResponseEntity.ok().body(mapToVersionInfoDetailed(changeInfoDetailedDto));
  }

  @Operation(summary = "Get version changes by id",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionChanges.class))),
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
  public ResponseEntity<VersionChanges> getVersionChanges(@PathVariable String versionCandidateId) throws Exception {
    return ResponseEntity.ok().body(versionManagementService.getVersionChanges(versionCandidateId));
  }

  private VersionInfoDetailed mapToVersionInfoDetailed(ChangeInfoDetailedDto changeInfoDetailedDto) {
    return VersionInfoDetailed.builder()
        .id(String.valueOf(changeInfoDetailedDto.getNumber()))
        .author(changeInfoDetailedDto.getOwner())
        .creationDate(changeInfoDetailedDto.getCreated())
        .description(changeInfoDetailedDto.getDescription())
        .hasConflicts(Boolean.FALSE.equals(changeInfoDetailedDto.getMergeable()))
        .latestUpdate(changeInfoDetailedDto.getUpdated())
        .name(changeInfoDetailedDto.getSubject())
        .build();
  }
}
