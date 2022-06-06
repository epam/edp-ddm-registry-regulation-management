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

import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.epam.digital.data.platform.management.groups.service.GroupService;
import com.epam.digital.data.platform.management.groups.validation.BpGroupingValidator;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Tag(description = "Registry regulations candidate version Groups management Rest API", name = "candidate-version-business-process-groups-api")
@RestController
@RequestMapping("/versions/candidates/{versionCandidateId}/business-process-groups")
@RequiredArgsConstructor
public class CandidateVersionGroupsController {

  private final GroupService groupService;
  private final BpGroupingValidator validator;

  @Operation(
      summary = "Get business process groups for candidate",
      description = "### Endpoint purpose:\n This endpoint is used to retrieve a list of JSON representations of _business process groups_ for the version candidate.",
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
              description = "OK. Successful retrieval of business process groups",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BusinessProcessGroupsResponse.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"groups\": [\n" +
                          "    {\n" +
                          "      \"name\": \"Перша група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    },\n" +
                          "    {\n" +
                          "      \"name\": \"Друга група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    },\n" +
                          "    {\n" +
                          "      \"name\": \"Третя група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    }\n" +
                          "  ],\n" +
                          "  \"ungrouped\": [\n" +
                          "    {\n" +
                          "      \"id\": \"bp-4-process_definition_id\",\n" +
                          "      \"name\": \"John Doe added new component\"\n" +
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @GetMapping
  public ResponseEntity<BusinessProcessGroupsResponse> getBusinessProcessGroups(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Started getting business process groups from version candidate with id {}",
        versionCandidateId);
    final var groupsByVersion = groupService.getGroupsByVersion(versionCandidateId);
    log.info("Found business process groups for version candidate with id {}", versionCandidateId);
    return ResponseEntity.ok().body(groupsByVersion);
  }


  @Operation(
      summary = "Save business process groups for version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used to create/update a _business process groups_ for the version candidate. A conflict can arise when two or more commits have made changes to the same part of a file. This can happen when two developers are working on the same branch at the same time, and both make changes to the same piece of code without being aware of each other's changes. ### Group validation: \nBefore saving the new _bp groups_, the server validates it. The _groups_ must be a __yaml__ document and must have a __\"groups\"__ field. Also the field __\"groups.name\"__ must be present, unique and valid (name is match with regex). Also _groups.processDefinitions_  field cannot be empty.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"groups\": [\n" +
                      "    {\n" +
                      "      \"name\": \"Перша група\",\n" +
                      "      \"processDefinitions\": [\n" +
                      "        \"bp-1-process_definition_id\"\n" +
                      "      ]\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"name\": \"Четверта група\",\n" +
                      "      \"processDefinitions\": [\n" +
                      "        \"bp-3-process_definition_id\"\n" +
                      "      ]\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"name\": \"Третя група\"\n" +
                      "    }\n" +
                      "  ],\n" +
                      "  \"ungrouped\": [\n" +
                      "    \"bp-4-process_definition_id\",\n" +
                      "    \"bp-5-process_definition_id\"\n" +
                      "  ]\n" +
                      "}")
              }
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Business process groups successfully created/updated",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BusinessProcessGroupsResponse.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"groups\": [\n" +
                          "    {\n" +
                          "      \"name\": \"Перша група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    },\n" +
                          "    {\n" +
                          "      \"name\": \"Друга група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    },\n" +
                          "    {\n" +
                          "      \"name\": \"Третя група\",\n" +
                          "      \"processDefinitions\": []\n" +
                          "    }\n" +
                          "  ],\n" +
                          "  \"ungrouped\": [\n" +
                          "    {\n" +
                          "      \"id\": \"bp-4-process_definition_id\",\n" +
                          "      \"name\": \"John Doe added new component\"\n" +
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
              responseCode = "409",
              description = "Conflict. It means that bp group file content already has been updated/deleted.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity",
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
  public ResponseEntity<BusinessProcessGroupsResponse> saveBusinessProcessGroups(
      @RequestBody GroupListDetails bpGroups,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Called #saveBusinessProcessGroups() for {} version candidate", versionCandidateId);
    validator.validate(bpGroups);
    groupService.save(versionCandidateId, bpGroups);
    log.info("Updated groups for {} version candidate", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(groupService.getGroupsByVersion(versionCandidateId));
  }

  @Operation(
      summary = "Rollback business process groups for version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for rolling back a __bp groups__ from the __version-candidate__. It is intended for situations where a __bp groups__ needs to be reverted to a prior version, such as to mitigate data corruption or to restore a previous state.",
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
              description = "OK. Business process groups successfully rolled back.",
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
  @PostMapping("/rollback")
  public ResponseEntity<String> rollbackBusinessProcessGroups(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId) {
    log.info("Started rollback bp-grouping file from {} version candidate", versionCandidateId);
    groupService.rollbackBusinessProcessGroups(versionCandidateId);
    log.info("Finished rolling back bp-grouping file from the {} version candidate",
        versionCandidateId);
    return ResponseEntity.ok().build();
  }

}
