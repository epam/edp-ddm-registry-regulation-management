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

import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.validation.ExistingVersionCandidate;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(description = "Registry regulations version-candidate data-model tables file management Rest API", name = "candidate-version-data-model-tables-api")
@RestController
@RequestMapping("/versions/candidates/{versionCandidateId}/data-model/tables")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CandidateVersionDataModelTablesController {

  private final DataModelFileManagementService dataModelFileManagementService;

  @Operation(
      summary = "Get data-model tables file content from requested version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a XML representation of the _content of the data-model tables_ file from the _version-candidate_.",
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
              description = "Version-candidate doesn't exist or tables file doesn't exists in requested version",
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
  public ResponseEntity<String> getTablesFileContent(
      @ExistingVersionCandidate @PathVariable @Parameter(description = "Version candidate identifier", required = true) Integer versionCandidateId) {
    log.info("Getting tables file content from version '{}' started", versionCandidateId);
    final var fileContent = dataModelFileManagementService.getTablesFileContent(
        String.valueOf(versionCandidateId));
    log.info("There were found tables file content for version '{}'", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .eTag(ETagUtils.getETagFromContent(fileContent))
        .body(fileContent);
  }

  @Operation(
      summary = "Put data-model tables file content to specified version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for updating a XML representation of the _content of the data-model tables_ file from the _version-candidate_. A conflict can arise when two or more commits have made changes to the same part of a file. This can happen when two developers are working on the same branch at the same time, and both make changes to the same piece of code without being aware of each other's changes. In this situation, the system cannot automatically determine which change is the correct one, and will require human intervention to resolve the conflict.",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")),
          @Parameter(
              in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string"))
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(
              mediaType = MediaType.TEXT_PLAIN_VALUE,
              schema = @Schema(type = "string"),
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
                      "      remarks=\"The very first registry table\">\n" +
                      "      <column name=\"first_registry_table_id\" type=\"UUID\" defaultValueComputed=\"uuid_generate_v4()\">\n" +
                      "        <constraints nullable=\"false\" primaryKey=\"true\" primaryKeyName=\"pk_first_registry_table_id\"/>\n" +
                      "      </column>\n" +
                      "      <column name=\"code\" type=\"TEXT\" remarks=\"Row code\">\n" +
                      "        <constraints nullable=\"false\" unique=\"true\"/>\n" +
                      "      </column>\n" +
                      "      <column name=\"name\" type=\"TEXT\" remarks=\"Row name\">\n" +
                      "        <constraints nullable=\"false\"/>\n" +
                      "      </column>\n" +
                      "    </createTable>\n" +
                      "  </changeSet>\n" +
                      "</databaseChangeLog>")
              }
          )),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Tables file content updated successfully",
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
              description = "Version-candidate doesn't exist",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "409",
              description = "Conflict. It means that tables file content already has been updated/deleted.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. Tables file content is not valid.",
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
  @PutMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public ResponseEntity<String> putTablesFileContent(
      @ExistingVersionCandidate @PathVariable @Parameter(description = "Version candidate identifier", required = true) Integer versionCandidateId,
      @RequestBody String tablesFileContent,
      @RequestHeader HttpHeaders headers) {
    var versionId = String.valueOf(versionCandidateId);
    log.info("Putting tables file content to version '{}' started", versionId);
    var eTag = headers.getFirst("If-Match");
    dataModelFileManagementService.putTablesFileContent(versionId, tablesFileContent, eTag);
    log.debug("Tables file content in version '{}' updated, reading it again for response.",
        versionId);
    final var updatedFileContent = dataModelFileManagementService.getTablesFileContent(versionId);
    log.info("There were updated tables file content for version '{}'", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .eTag(ETagUtils.getETagFromContent(updatedFileContent))
        .body(updatedFileContent);
  }

  @Operation(
      summary = "Rollback data-model tables file content to specified version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for rolling back a __tables file content__ from the __version-candidate__. It is intended for situations where a __tables file content__ needs to be reverted to a prior version, such as to mitigate data corruption or to restore a previous state.",
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
              description = "OK. Tables file content successfully rolled back.",
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
              description = "Version-candidate doesn't exist",
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
  public ResponseEntity<String> rollbackTables(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId) {
    log.info("Started rollback data-model tables file content from {} version candidate",
        versionCandidateId);
    dataModelFileManagementService.rollbackTables(versionCandidateId);
    log.info("Finished rolling back data-model tables file content from the {} version candidate",
        versionCandidateId);
    return ResponseEntity.ok().build();
  }
}
