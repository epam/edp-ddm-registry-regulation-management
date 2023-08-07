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

import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.model.TableInfo;
import com.epam.digital.data.platform.management.restapi.model.TableInfoShort;
import com.epam.digital.data.platform.management.restapi.service.BuildStatusService;
import com.epam.digital.data.platform.management.restapi.validation.ExistingVersionCandidate;
import com.epam.digital.data.platform.management.service.ReadDataBaseTablesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Registry regulations version-candidate tables management Rest API")
@RestController
@RequestMapping("/versions/candidates")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CandidateVersionTableController {

  private final ControllerMapper controllerMapper;

  private final ReadDataBaseTablesService tableService;
  private final BuildStatusService buildStatusService;

  @Operation(description = "Get tables list from version-candidate", parameters = {
      @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string"))},
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = TableInfoShort.class)))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Version-candidate not found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping("/{versionCandidateId}/tables")
  public ResponseEntity<List<TableInfoShort>> getTables(
      @ExistingVersionCandidate @PathVariable @Parameter(description = "Version candidate identifier", required = true) Integer versionCandidateId) {
    log.info("Getting list of tables for version-candidate '{}' started", versionCandidateId);
    String versionId = String.valueOf(versionCandidateId);
    boolean isSuccessCandidateVersionBuild = buildStatusService.isSuccessCandidateVersionBuild(versionId);

    final var tables = tableService.listTables(versionId, isSuccessCandidateVersionBuild);
    log.info("There were found {} tables for master version-candidate '{}'", tables.size(),
        versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(controllerMapper.toTableInfosShort(tables));
  }

  @Operation(description = "Get specific table full details from version-candidate",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TableInfo.class))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Version candidate or table not found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping("/{versionCandidateId}/tables/{tableName}")
  public ResponseEntity<TableInfo> getTable(
      @ExistingVersionCandidate @PathVariable @Parameter(description = "Version candidate identifier", required = true) Integer versionCandidateId,
      @PathVariable @Parameter(description = "Table name", required = true) String tableName) {
    log.info("Getting table by name '{}' for version-candidate '{}' started", tableName,
        versionCandidateId);
    String versionId = String.valueOf(versionCandidateId);
    boolean isSuccessCandidateVersionBuild = buildStatusService.isSuccessCandidateVersionBuild(versionId);

    final var tableInfoDto = tableService.getTable(versionId, tableName, isSuccessCandidateVersionBuild);
    log.info("Table '{}' was found in version-candidate '{}'", tableName, versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(controllerMapper.toTableInfo(tableInfoDto));
  }
}
