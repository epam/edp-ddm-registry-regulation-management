/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.restapi.controller;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.model.FormDetailsShort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Registry regulations Master version Forms management Rest API")
@RestController
@RequestMapping("/versions/master/forms")
@RequiredArgsConstructor
public class MasterVersionFormsController {

  private final FormService formService;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final ControllerMapper mapper;

  @Operation(description = "Get lest of forms for master version", parameters = {
      @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string"))},
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = FormDetailsShort.class)))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping
  public ResponseEntity<List<FormDetailsShort>> getFormsFromMaster() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting forms from master");
    var dtos = formService.getFormListByVersion(masterVersionId);
    var response = mapper.toFormDetailsShorts(dtos);
    log.info("Found {} forms in master", response.size());
    return ResponseEntity.ok().body(response);
  }

  @Operation(description = "Get specific form full details",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "200",
              description = "OK",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = Map.class))),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @GetMapping("/{formName}")
  public ResponseEntity<Object> getForm(
      @PathVariable @Parameter(description = "Form name", required = true) String formName) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Getting {} form from master", formName);
    var response = formService.getFormContent(formName, masterVersionId);
    log.info("Finished getting {} form form master", formName);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(description = "Create new form within master",
      parameters = @Parameter(in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")),
      responses = {
          @ApiResponse(responseCode = "201",
              description = "Created",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "409",
              description = "Conflict",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "422",
              description = "Unprocessable Entity",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @PostMapping("/{formName}")
  public ResponseEntity<String> formCreate(@RequestBody String form,
      @PathVariable @Parameter(description = "Name of the new form to be created", required = true) String formName) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();

    log.info("Started creating {} form for master", formName);
    formService.createForm(formName, form, masterVersionId);
    log.info("Form {} was created for master. Retrieving this form", formName);
    var response = formService.getFormContent(formName, masterVersionId);
    log.info("Finished getting {} form from master", formName);
    return ResponseEntity.created(URI.create(
            String.format("/versions/master/forms/%s", formName)))
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(description = "Delete existing form within master",
      parameters = {
          @Parameter(in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")),
          @Parameter(in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string")
          )
      },
      responses = {
          @ApiResponse(responseCode = "204",
              description = "No Content",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))),
          @ApiResponse(responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class)))})
  @DeleteMapping("/{formName}")
  public ResponseEntity<String> deleteForm(
      @PathVariable @Parameter(description = "Name of the form to be deleted", required = true) String formName,
      @RequestHeader HttpHeaders headers) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");
    log.info("Started deleting {} form for master", formName);
    formService.deleteForm(formName, masterVersionId, eTag);
    log.info("Form {} was deleted from master", formName);
    return ResponseEntity.noContent().build();
  }
}
