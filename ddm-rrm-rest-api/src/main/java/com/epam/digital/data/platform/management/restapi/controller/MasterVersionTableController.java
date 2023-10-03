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
import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.model.TableInfo;
import com.epam.digital.data.platform.management.restapi.model.TableInfoShort;
import com.epam.digital.data.platform.management.restapi.service.BuildStatusService;
import com.epam.digital.data.platform.management.service.ReadDataBaseTablesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(description = "Registry regulations master version tables management Rest API",  name = "master-version-tables-api")
@RestController
@RequestMapping("/versions/master/tables")
@RequiredArgsConstructor
@Slf4j
public class MasterVersionTableController {

  private final ControllerMapper controllerMapper;

  private final ReadDataBaseTablesService tableService;

  private final GerritPropertiesConfig gerritPropertiesConfig;

  private final BuildStatusService buildStatusService;

  @Operation(
      summary = "\"Get a list of tables with brief details for the master version",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a list of JSON representations of __tables__ directly from the __master__ version, containing only brief information about each _table_. If you need to retrieve full details of a single _table_ based on its __tableName__, you can use the [GET](#master-version-tables-api/getTable) endpoint.",
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
              description = "OK. Tables successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = TableInfoShort.class)),
                  examples = {
                      @ExampleObject(value = "[\n" +
                          "  {\n" +
                          "    \"name\": \"John Doe's table\",\n" +
                          "    \"description\": \"John Doe get table\",\n" +
                          "    \"objectReference\": true\n" +
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
          )
      })
  @GetMapping
  public ResponseEntity<List<TableInfoShort>> getTables() {
    var versionId = gerritPropertiesConfig.getHeadBranch();
    var isSuccessCandidateVersionBuild = buildStatusService.isSuccessMasterVersionBuild();
    log.info("Getting list of tables for master version '{}' started", versionId);
    final var tables = tableService.listTables(versionId, isSuccessCandidateVersionBuild);
    log.info("There were found {} tables for master version '{}'", tables.size(), versionId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(controllerMapper.toTableInfosShort(tables));
  }

  @Operation(
      summary = "Get specific table full details",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representation of a __table__ directly from the __master__ version. This operation retrieves a single _table_ based on the specified __tableName__. If you need to retrieve list of _tables_, you can use the [GET](#master-version-tables-api/getTables) endpoint.",
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
              description = "OK. Table successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = TableInfo.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"name\": \"ExampleTable\",\n" +
                          "  \"description\": \"Example description\",\n" +
                          "  \"objectReference\": true,\n" +
                          "  \"columns\": {\n" +
                          "    \"id\": {\n" +
                          "      \"name\": \"id\",\n" +
                          "      \"description\": \"Table column id\",\n" +
                          "      \"type\": \"INTEGER\",\n" +
                          "      \"defaultValue\": \"0\",\n" +
                          "      \"notNullFlag\": true\n" +
                          "    },\n" +
                          "    \"name\": {\n" +
                          "      \"name\": \"name\",\n" +
                          "      \"description\": \"Table column name\",\n" +
                          "      \"type\": \"VARCHAR\",\n" +
                          "      \"defaultValue\": null,\n" +
                          "      \"notNullFlag\": true\n" +
                          "    }\n" +
                          "  },\n" +
                          "  \"foreignKeys\": {\n" +
                          "    \"fk_example\": {\n" +
                          "      \"name\": \"fk_example\",\n" +
                          "      \"targetTable\": \"AnotherTable\",\n" +
                          "      \"columnPairs\": [\n" +
                          "        {\n" +
                          "          \"sourceColumnName\": \"id\",\n" +
                          "          \"targetColumnName\": \"example_id\"\n" +
                          "        }\n" +
                          "      ]\n" +
                          "    }\n" +
                          "  },\n" +
                          "  \"primaryKey\": {\n" +
                          "    \"name\": \"pk_example\",\n" +
                          "    \"columns\": [\n" +
                          "      {\n" +
                          "        \"name\": \"id\",\n" +
                          "        \"sorting\": \"ASC\"\n" +
                          "      }\n" +
                          "    ]\n" +
                          "  },\n" +
                          "  \"uniqueConstraints\": {\n" +
                          "    \"uk_example\": {\n" +
                          "      \"name\": \"uk_example\",\n" +
                          "      \"columns\": [\n" +
                          "        {\n" +
                          "          \"name\": \"name\",\n" +
                          "          \"sorting\": \"ASC\"\n" +
                          "        }\n" +
                          "      ]\n" +
                          "    }\n" +
                          "  },\n" +
                          "  \"indices\": {\n" +
                          "    \"idx_example\": {\n" +
                          "      \"name\": \"idx_example\",\n" +
                          "      \"columns\": [\n" +
                          "        {\n" +
                          "          \"name\": \"id\",\n" +
                          "          \"sorting\": \"ASC\"\n" +
                          "        },\n" +
                          "        {\n" +
                          "          \"name\": \"name\",\n" +
                          "          \"sorting\": \"DESC\"\n" +
                          "        }\n" +
                          "      ]\n" +
                          "    }\n" +
                          "  }\n" +
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
  @GetMapping("/{tableName}")
  public ResponseEntity<TableInfo> getTable(
      @PathVariable @Parameter(description = "Table name", required = true) String tableName) {
    var versionId = gerritPropertiesConfig.getHeadBranch();
    var isSuccessCandidateVersionBuild = buildStatusService.isSuccessMasterVersionBuild();
    log.info("Getting table by name '{}' for master version '{}' started", tableName, versionId);
    final var tableInfoDto = tableService.getTable(versionId, tableName, isSuccessCandidateVersionBuild);
    log.info("Table '{}' was found in master version '{}'", tableName, versionId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(controllerMapper.toTableInfo(tableInfoDto));
  }
}
