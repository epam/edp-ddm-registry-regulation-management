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

import com.epam.digital.data.platform.management.model.dto.MasterVersionInfoDetailed;
import com.epam.digital.data.platform.management.model.exception.DetailedErrorResponse;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Registry regulations master version management Rest API")
@RestController
@RequestMapping("/versions/master")
@RequiredArgsConstructor
public class MasterVersionController {

  private final VersionManagementService versionManagementService;

  @Operation(description = "Acquire master version full details",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = MasterVersionInfoDetailed.class))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))
      })
  @GetMapping
  public ResponseEntity<MasterVersionInfoDetailed> getMasterVersionInfo() {
    log.info("Started getting master detailed info");
    var masterInfo = versionManagementService.getMasterInfo();
    if (Objects.isNull(masterInfo)) {
      log.info("Master info is null, returning empty info");
      return ResponseEntity.ok()
          .body(MasterVersionInfoDetailed.builder().build());
    }
    var response = MasterVersionInfoDetailed.builder()
        .id(String.valueOf(masterInfo.getNumber()))
        .author(masterInfo.getOwner())
        .description(masterInfo.getDescription())
        .name(masterInfo.getSubject())
        .latestUpdate(masterInfo.getSubmitted())
        .build();
    log.info("Finished getting detailed info about master");
    return ResponseEntity.ok().body(response);
  }
}
