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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.service.GroupService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(description = "Registry regulations Master version Groups management Rest API",  name = "master-version-business-process-groups-api")
@RestController
@RequestMapping("/versions/master/business-process-groups")
@RequiredArgsConstructor
public class MasterVersionGroupsController {

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final GroupService groupService;

  @Operation(
      summary = "Get business process groups for master version",
      description = "### Endpoint purpose:\n This endpoint is used to retrieve a list of JSON representations of _business process groups_ for the master version.",
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
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
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
                  }
              )
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
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)
              )
          )
      }
  )
  @GetMapping
  public ResponseEntity<BusinessProcessGroupsResponse> getBusinessProcessGroups() {
    log.info("Started getting business process groups from master");
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    final var groupsByVersion = groupService.getGroupsByVersion(masterVersionId);
    log.info("Found business process groups for master version");
    return ResponseEntity.ok().body(groupsByVersion);
  }
}
