/*
 * Copyright 2022 EPAM Systems.
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

import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.settings.model.SettingsInfoDto;
import com.epam.digital.data.platform.management.settings.service.SettingService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(description = "Registry regulations version candidates settings Rest API",  name = "candidate-version-settings-api")
@RestController
@RequestMapping("/versions/candidates/{versionCandidateId}/settings")
@RequiredArgsConstructor
@Slf4j
public class CandidateVersionSettingsController {

  private final SettingService settingService;

  @Operation(
      summary = "Get settings for version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representations of existing _settings_ for version candidate",
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
              description = "OK. Settings information retrieved successfully",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = SettingsInfoDto.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"themeFile\": \"white-theme.js\",\n" +
                          "  \"title\": \"mdtuddm\",\n" +
                          "  \"titleFull\": \"<Назва реєстру>\",\n" +
                          "  \"supportEmail\": \"support@registry.gov.ua\"\n" +
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
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @GetMapping
  public ResponseEntity<SettingsInfoDto> getSettings(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Called #getSettings() for {} version candidate", versionCandidateId);
    var setting = settingService.getSettings(versionCandidateId);
    log.info("Got settings for {} version candidate", versionCandidateId);
    return ResponseEntity.ok(setting);
  }

  @Operation(
      summary = "Update settings for version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used to update/create a _settings_ for the version candidate. A conflict can arise when two or more commits have made changes to the same part of a file. This can happen when two developers are working on the same branch at the same time, and both make changes to the same piece of code without being aware of each other's changes.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = SettingsInfoDto.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"themeFile\": \"white-theme.js\",\n" +
                      "  \"title\": \"mdtuddm\",\n" +
                      "  \"titleFull\": \"<Назва реєстру>\",\n" +
                      "  \"supportEmail\": \"support@registry.gov.ua\"\n" +
                      "}"
                  )
              })
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Settings information updated successfully",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = SettingsInfoDto.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"themeFile\": \"white-theme.js\",\n" +
                          "  \"title\": \"mdtuddm\",\n" +
                          "  \"titleFull\": \"<Назва реєстру>\",\n" +
                          "  \"supportEmail\": \"support@registry.gov.ua\"\n" +
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
              description = "Conflict. It means that settings file content already has been updated/deleted.",
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
  @PutMapping
  public ResponseEntity<SettingsInfoDto> updateSettings(
      @RequestBody SettingsInfoDto settings,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true)
      String versionCandidateId) {
    log.info("Called #updateSettings() for {} version candidate", versionCandidateId);
    settingService.updateSettings(versionCandidateId, settings);
    log.info("Updated settings for {} version candidate", versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(settingService.getSettings(versionCandidateId));
  }

}
