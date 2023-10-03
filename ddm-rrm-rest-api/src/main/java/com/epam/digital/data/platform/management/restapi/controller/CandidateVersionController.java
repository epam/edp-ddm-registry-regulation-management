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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.epam.digital.data.platform.management.restapi.model.BuildType;
import com.epam.digital.data.platform.management.restapi.model.ResultValues;
import com.epam.digital.data.platform.management.restapi.service.BuildStatusService;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(description = "Registry regulations version-candidate management Rest API", name = "candidate-version-api")
@RestController
@RequestMapping("/versions/candidates")
@RequiredArgsConstructor
public class CandidateVersionController {

  private final VersionManagementService versionManagementService;
  private final ControllerMapper controllerMapper;
  private final BuildStatusService buildStatusService;

  @Operation(
      summary = "Get list of existing opened version-candidates",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a list of JSON representations of __version information__ from the __version-candidate__, containing only brief information about each __version information__. If you need to retrieve full details of a single __version information__ based on its __versionCandidateId__, you can use the [GET](#candidate-version-api/getVersionDetails) endpoint.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Version details successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = VersionInfo.class)),
                  examples = {
                      @ExampleObject(value = "[\n" +
                          "  {\n" +
                          "    \"id\": \"1\",\n" +
                          "    \"name\": \"JohnDoe's version candidate\",\n" +
                          "    \"description\": \"Version candidate to change form\"\n" +
                          "  }\n" +
                          "]"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
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

  @Operation(
      summary = "Abandon the existing opened version-candidate.",
      description = "### Endpoint purpose:\n This endpoint is used to decline an available open __version-candidate__. It is intended for situations where the __candidate version__ is no longer needed. After this operation the __version-candidate__ won't take any changes anymore.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Version candidate successfully abandoned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
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
  @PostMapping("/{versionCandidateId}/decline")
  public ResponseEntity<String> declineVersionCandidate(
      @PathVariable @Parameter(description = "Version candidate identifier to abandon", required = true) String versionCandidateId) {
    log.info("Declining {} version candidate", versionCandidateId);
    versionManagementService.decline(versionCandidateId);
    log.info("{} version candidate was declined", versionCandidateId);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Integrate version-candidate changes into master version of registry regulation",
      description = "### Endpoint purpose:\n This endpoint is used to merge an available open __version-candidate__, identified by the _versionCandidateId_ parameter, into master version of the registry regulation after the changes have been reviewed. Once the merge operation is completed, the __version-candidate__ will no longer accept any new changes. Successful completion of the merge operation is indicated by a _204 No Content_ response. In case of any conflicts between the __version-candidate__ and the _master version_, such as duplicate names for data elements or changes made to data elements already changed in the _master version_, this API returns a __409 Conflict__ HTTP response. In such cases, the resulting _conflict_ must be resolved before attempting the merge operation again.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content. Version candidate successfully merged into master version.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "409",
              description = "Conflict. The same data has been updated or deleted in the master version by another merge commit.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
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


  @Operation(
      summary = "Create new version-candidate from current state of master version.",
      description = "### Endpoint purpose:\n  This endpoint is used to create a new __version-candidate__ from the current state of the _master_ version. The purpose is to allow making changes to the data elements without affecting the stability of the _master_ version. The endpoint requires the `X-Access-Token` header for security. Once the new __version-candidate__ is created, it can be developed independently from other __version-candidates__ or the _master_ version. When the changes are ready, the __version-candidate__ can be merged back into the _master_ version. If the operation is _successful_, the resulting `VersionInfoDetailed` object is returned along with a _`201 Created`_ status code. If the request _fails_ due to invalid input or server issues, a _`4xx` or `5xx`_ HTTP response code may be returned along with a detailed error message.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CreateVersionRequest.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"name\" : \"JohnDoe's version candidate\",\n" +
                      "  \"description\" : \"Version candidate to change form\"\n" +
                      "}")
              }
          )),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "OK. Version candidate successfully created",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionInfoDetailed.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"id\" : \"1\",\n" +
                      "  \"name\" : \"JohnDoe's version candidate\",\n" +
                      "  \"description\" : \"Version candidate to change form\",\n" +
                      "  \"author\" : \"JohnDoe@epam.com\",\n" +
                      "  \"creationDate\" : \"2022-08-10T11:30:00\",\n" +
                      "  \"latestUpdate\" : \"2022-08-10T11:40:00\",\n" +
                      "  \"hasConflicts\" : false,\n" +
                      "  \"inspections\" : null,\n" +
                      "  \"validations\" : [ {\n" +
                      "    \"name\" : \"Validation 1\",\n" +
                      "    \"result\" : \"SUCCESS\",\n" +
                      "    \"message\" : \"Validation passed\"\n" +
                      "  } ]\n" +
                      "}")
              })
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. Version request is not valid",
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

  @Operation(
      summary = "Acquire version-candidate full details",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representations of _version information_ from the __version-candidate__. This operation retrieves a single __version information__ based on the specified __versionCandidateId__ with full details. If you need to retrieve a list of __version information__ with brief details, you can use the [GET](#candidate-version-api/getVersionsList) endpoint.",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Version details successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionInfoDetailed.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"id\": \"1\",\n" +
                          "  \"name\": \"JohnDoe's version candidate\",\n" +
                          "  \"description\": \"Version candidate to change form\",\n" +
                          "  \"author\": \"JohnDoe@epam.com\",\n" +
                          "  \"creationDate\": \"2022-08-10T11:30:00.000Z\",\n" +
                          "  \"latestUpdate\": \"2022-08-10T11:40:00.000Z\",\n" +
                          "  \"hasConflicts\": false,\n" +
                          "  \"inspections\": null,\n" +
                          "  \"validations\": [\n" +
                          "    {\n" +
                          "      \"result\": \"SUCCESS\"\n" +
                          "    }\n" +
                          "  ]\n" +
                          "}"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
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
  @GetMapping("/{versionCandidateId}")
  public ResponseEntity<VersionInfoDetailed> getVersionDetails(
      @PathVariable @Parameter(description = "Version-candidate identifier", required = true) String versionCandidateId) {
    log.info("Started getting detailed info about {} version candidate", versionCandidateId);
    VersionInfoDto versionDetails = versionManagementService.getVersionDetails(versionCandidateId);
    String statusVersionBuild = buildStatusService.getStatusVersionBuild(versionDetails, BuildType.CANDIDATE);
    var response = controllerMapper.toVersionInfoDetailed(
        versionDetails);
    response.getValidations().get(0).setResult(ResultValues.valueOf(statusVersionBuild));
    log.info("Finished getting detailed info about {} version candidate", versionCandidateId);
    return ResponseEntity.ok().body(response);
  }

  @Operation(
      summary = "Get version changes by version-candidate id",
      description = "### Endpoint purpose:\n This operation retrieves _changes_ made to the data elements in a __version-candidate__ compared to the _master_ version. The endpoint allows you to review the changes made in a candidate version before merging with the main version.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          required = true,
          description = "Token used for endpoint security",
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Version changes successfully retrieved",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = VersionChangesInfo.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"changedForms\": [\n" +
                          "    {\n" +
                          "      \"name\": \"formToBeUpdated\",\n" +
                          "      \"title\": \"JohnDoe's form\",\n" +
                          "      \"status\": \"CHANGED\"\n" +
                          "    }\n" +
                          "  ],\n" +
                          "  \"changedBusinessProcesses\": [\n" +
                          "    {\n" +
                          "      \"name\": \"newProcess\",\n" +
                          "      \"title\": \"JohnDoe's process\",\n" +
                          "      \"status\": \"NEW\"\n" +
                          "    }\n" +
                          "  ],\n" +
                          "  \"changedGroups\": [\n" +
                          "    {\n" +
                          "      \"title\": \"JohnDoe's group\",\n" +
                          "      \"status\": \"NEW\"\n" +
                          "    }\n" +
                          "  ]\n" +
                          "}")
                  })
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
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


  @Operation(
      summary = "Rebase changes from master version",
      description = "This operation applies the changes made to the _master_ version onto a __version-candidate__. The purpose is to ensure that the __version candidate__  has all the latest changes from the _master_ version before merging it.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          required = true,
          description = "Token used for endpoint security",
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Rebase was successful",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
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
  @PutMapping("/{versionCandidateId}/rebase")
  public ResponseEntity<Void> rebase(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Started version candidate {} rebase", versionCandidateId);
    versionManagementService.rebase(versionCandidateId);
    log.info("Version candidate {} successfully rebased", versionCandidateId);
    return ResponseEntity.ok().build();
  }
}
