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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(description = "Registry regulations Master version settings Rest API",  name = "master-version-settings-api")
@RestController
@RequestMapping("/versions/master/settings")
@RequiredArgsConstructor
@Slf4j
public class MasterVersionSettingsController {

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final SettingService settingService;

  @Operation(
      summary = "Get settings for master version",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representations of existing _settings_ for master version",
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @GetMapping
  public ResponseEntity<SettingsInfoDto> getSettings() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Called #getSettings() for master");
    var settings = settingService.getSettings(masterVersionId);
    log.info("Got settings for master");
    return ResponseEntity.ok(settings);
  }
}
