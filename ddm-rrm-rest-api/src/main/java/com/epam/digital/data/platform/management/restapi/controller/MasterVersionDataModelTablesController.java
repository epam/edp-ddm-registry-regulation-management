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
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
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

@Tag(description = "Registry regulations master version data-model tables file management Rest API",  name = "master-version-data-model-tables-api")
@RestController
@RequestMapping("/versions/master/data-model/tables")
@RequiredArgsConstructor
@Slf4j
public class MasterVersionDataModelTablesController {

  private final DataModelFileManagementService dataModelFileManagementService;

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Operation(
      summary = "Get data-model tables file content from master version",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a XML representation of the _content of the data-model tables_ file from the master version.",
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
              description = "OK. Tables file content retrieved successfully",
              content = @Content(
                  mediaType = MediaType.TEXT_PLAIN_VALUE,
                  examples = {
                      @ExampleObject(value = "<?xml version=\"1.1\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                          "<databaseChangeLog\n" +
                          "  xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n" +
                          "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "  xmlns:ext=\"http://www.liquibase.org/xml/ns/dbchangelog-ext\"\n" +
                          "  xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd\n" +
                          "        http://www.liquibase.org/xml/ns/dbchangelog-ext https://nexus-public-mdtu-ddm-edp-cicd.apps.cicd2.mdtu-ddm.projects.epam.com/repository/extensions/com/epam/digital/data/platform/liquibase-ext-schema/latest/liquibase-ext-schema-latest.xsd\">\n" +
                          "\n" +
                          "  <changeSet author=\"registry owner\" id=\"table first_registry_table\">\n" +
                          "    <createTable tableName=\"first_registry_table\" ext:historyFlag=\"true\"\n" +
                          "      remarks=\"Найперша таблиця реєстру\">\n" +
                          "      <column name=\"first_registry_table_id\" type=\"UUID\" defaultValueComputed=\"uuid_generate_v4()\">\n" +
                          "        <constraints nullable=\"false\" primaryKey=\"true\" primaryKeyName=\"pk_first_registry_table_id\"/>\n" +
                          "      </column>\n" +
                          "      <column name=\"code\" type=\"TEXT\" remarks=\"Код запису\">\n" +
                          "        <constraints nullable=\"false\" unique=\"true\"/>\n" +
                          "      </column>\n" +
                          "      <column name=\"name\" type=\"TEXT\" remarks=\"Назва запису\">\n" +
                          "        <constraints nullable=\"false\"/>\n" +
                          "      </column>\n" +
                          "    </createTable>\n" +
                          "  </changeSet>\n" +
                          "</databaseChangeLog>")
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
              responseCode = "404",
              description = "Tables file doesn't exists in master version",
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
  @GetMapping
  public ResponseEntity<String> getTablesFileContent() {
    var versionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Getting tables file content from master version '{}' started", versionId);
    final var fileContent = dataModelFileManagementService.getTablesFileContent(versionId);
    log.info("There were found tables file content for master version '{}'", versionId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .body(fileContent);
  }
}
