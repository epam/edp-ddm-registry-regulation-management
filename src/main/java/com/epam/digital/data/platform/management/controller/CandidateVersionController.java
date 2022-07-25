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

import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.CreateVersionResponse;
import com.epam.digital.data.platform.management.model.dto.Version;
import com.epam.digital.data.platform.management.model.dto.VersionDetailedResponse;
import com.epam.digital.data.platform.management.model.exception.DetailedErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Candidate version Rest API")
@RestController
@RequestMapping("/versions/candidates")
public class CandidateVersionController {

  @Operation(summary = "Get versions list",
          parameters = {@Parameter(in= ParameterIn.HEADER,name = "X-Access-Token", schema = @Schema(type = "string"))}, responses = {
          @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Version.class)))),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
  })
  @GetMapping
  public ResponseEntity<List<Version>> getVersionsList() throws Exception {
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Create new version", parameters = {@Parameter(in= ParameterIn.HEADER,name = "X-Access-Token", schema = @Schema(type = "string"))}, responses = {
          @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateVersionResponse.class))),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "422", description = "Unprocessable Entity", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class)))
  })
  @PostMapping()
  public ResponseEntity<String> createNewVersion(@RequestBody CreateVersionRequest requestDto) throws Exception {

    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Get version details by id", parameters = {
          @Parameter(in= ParameterIn.HEADER,name = "X-Access-Token", schema = @Schema(type = "string"))}, responses = {
          @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = VersionDetailedResponse.class))),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class)))
  })
  @GetMapping("/{versionCandidateId}")
  public ResponseEntity<VersionDetailedResponse> getVersionDetails(@PathVariable String versionCandidateId) throws Exception {
    return ResponseEntity.ok().build();
  }

}
