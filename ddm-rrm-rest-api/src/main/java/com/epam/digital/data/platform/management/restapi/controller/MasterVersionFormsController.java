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
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.model.FormDetailsShort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(description = "Registry regulations Master version Forms management Rest API", name = "master-version-forms-api")
@RestController
@RequestMapping("/versions/master/forms")
@RequiredArgsConstructor
public class MasterVersionFormsController {

  private final FormService formService;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Operation(
      summary = "Get a list of forms with brief details for the master version",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a list of JSON representations of user __forms__ directly from the __master__ version, containing only brief information about each _form_. If you need to retrieve full details of a single _form_ based on its __formName__, you can use the [GET](#master-version-forms-api/getForm) endpoint.",
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
              description = "OK. Forms successfully retrieved.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = FormDetailsShort.class)),
                  examples = {
                      @ExampleObject(value = "[\n" +
                          "  {\n" +
                          "      \"name\": \"ExampleFormService\",\n" +
                          "      \"title\": \"Example Form\",\n" +
                          "      \"created\": \"2022-10-01T10:00:00\",\n" +
                          "      \"updated\": \"2022-11-15T13:30:00\"\n" +
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
  public ResponseEntity<List<FormDetailsShort>> getFormsFromMaster() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting forms from master");
    var response = formService.getFormListByVersion(masterVersionId).stream()
        .map(e -> FormDetailsShort.builder()
            .name(e.getName())
            .title(e.getTitle())
            .created(e.getCreated())
            .updated(e.getUpdated())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} forms in master", response.size());
    return ResponseEntity.ok().body(response);
  }

  @Operation(
      summary = "Get specific form full details",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representation of a user __form__ directly from the __master__ version. This operation retrieves a single _form_ based on the specified __formName__. If you need to retrieve list of _forms_, you can use the [GET](#master-version-forms-api/getFormsFromMaster) endpoint.\n",
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
              description = "Form successfully retrieved.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"display\": \"form\",\n"
                          + "  \"components\": [],\n"
                          + "  \"path\": \"my-awesome-form\",\n"
                          + "  \"name\": \"my-awesome-form\",\n"
                          + "  \"title\": \"Form human-readable title\",\n"
                          + "}")
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
  @GetMapping("/{formName}")
  public ResponseEntity<Object> getForm(
      @PathVariable
      @Parameter(description = "Form name", required = true) String formName) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Getting {} form from master", formName);
    var response = formService.getFormContent(formName, masterVersionId);
    log.info("Finished getting {} form form master", formName);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Create new form within master",
      description = "### Endpoint purpose: \n This endpoint is used for creating a JSON representation of a user __form__ directly in the __master__ version. It is intended for situations that require the creation of a new _form_. This operation creates a single _form_ and should be used when multiple _forms_ and/or _business-processes_ do not need to be created or modified simultaneously. If you need to create or modify several _forms_ and/or _business-processes_ at once, it is still recommended to use a _version-candidate_. \n ### Form validation: \nBefore saving the new _form_ to the storage, the server validates the _form_. The _form_ must be a __json__ document and must have a non-empty __\"title\"__ field. Also the field __\"name\"__ must be present and equal to __\"path\"__ field, that must be present too. Also _both_ this values must be equal to __\"formName\"__ pathVariable. In other case the _form_ won't be working as expected. \n ### Missing form handling: \n If the specified _form_ does not already exist, the server will create a new _form_ with the provided data. If the _form_ does exists, the server will return a _409 Conflict_ error indicating that the _form_ already exists.\n ### Created and modified dates handling:\n If there any of __\"created\"__ or __\"modified\"__ fields present in the request body they will be ignored. The __\"created\"__ and __\"updated\"__ fields are automatically set to the current server time in UTC.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"display\": \"form\",\n"
                      + "  \"components\": [],\n"
                      + "  \"path\": \"my-awesome-form\",\n"
                      + "  \"name\": \"my-awesome-form\",\n"
                      + "  \"title\": \"Form human-readable title\"\n"
                      + "}")
              }
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Form successfully created",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"display\": \"form\",\n"
                          + "  \"components\": [],\n"
                          + "  \"path\": \"my-awesome-form\",\n"
                          + "  \"name\": \"my-awesome-form\",\n"
                          + "  \"title\": \"Form human-readable title\",\n"
                          + "  \"created\":\"2023-03-28T09:18:41.941Z\",\n"
                          + "  \"modified\":\"2023-03-28T09:18:41.941Z\""
                          + "}")
                  }
              ),
              headers = @Header(name = HttpHeaders.ETAG, description = "New ETag value for conflict verification")
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
              responseCode = "409",
              description = "Conflict. It means that form already has been created.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. User form is not valid.",
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
  @PostMapping("/{formName}")
  public ResponseEntity<String> formCreate(
      @RequestBody String form,
      @PathVariable
      @Parameter(description = "Name of the new form to be created", required = true) String formName) {
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

  @Operation(
      summary = "Delete existing form within master",
      description = "### Endpoint purpose:\n This endpoint is used for deleting a JSON representation of a user __form__ directly from the __master__ version.\n### Conflict resolving:\n In this endpoint, [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, which can be previously obtained in a [GET](#master-version-forms-api/getForm) request, as a value for the __If-Match__ header. This ensures that you're deleting the latest version of the _form_. However, if your __If-Match__ value differs from the server's value, you will receive _409 Conflict_ instead of _412 Precondition Failed_. For the _registry-regulation-management_ service, this situation is considered a conflict. If the __If-Match__ header is not present, conflict checking will not be performed.\n ### Missing form handling:\n If the specified _form_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__), the server will return a 404 Not Found error indicating that the specified _form_ does not exist.",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "No Content. Form successfully deleted.",
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
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "409",
              description = "Conflict. __If-Match__ input value doesn't equal to servers value. It means that form already has been updated/deleted after user obtained __ETag__.",
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

  @Operation(
      summary = "Update existing form within master version.",
      description = "### Endpoint purpose: \n This endpoint is used for updating a json representation of a user __form__ directly in __master__ version. Just as if _version-candidate_ was created, the _form_ was updated in that _version-candidate_ and then the _version-candidate_ was submitted. It can be used if there is needed to update __a single form__. If you need to make some changes in several _forms_ and/or _business-processes_ at one time, it's still preferred to make this changes through a _version-candidate_. \n ### Conflict resolving:\n In this endpoint [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, that can be previously obtained in [GET](#master-version-forms-api/getForm) request, as a value for __If-Match__ header so you can be sure that you're updating the last version of a _form_. But if your __If-Match__ value is differs from the servers you will receive a _409 Conflict_ instead of _412 Precondition Failed_. For _registry-regulation-management_ service this situation's considered as a conflict. If __If-Match__ is not present then conflict checking won't be performed.\n### Form validation: \nBefore saving the content to the storage, the __validation__ of a _form_ is executed. The _form_ must be a __json__ document and must have a non-empty __\"title\"__ field. Also the field __\"name\"__ must be present and equal to __\"path\"__ field, that must be present too. Also _both_ this values must be equal to __\"formName\"__ pathVariable. In other case the _form_ won't be working as expected. Changing __\"name\"__ or __\"path\"__ is not supported. If you need to change these fields then you need to copy the _form_ with new name and delete the previous _form_. \n ### Missing form handling: \n If the updated _form_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__) then the _form_ will be __created__ instead.\n ### Created and modified dates handling:\n If there any of __\"created\"__ or __\"modified\"__ fields present in the request body they will be ignored. Value for the __\"created\"__ field is automatically getting from the previous _form_ content (if present, in other case it's getting from the git log). And for the __\"updated\"__ value the current servers datetime in UTC is set.",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              in = ParameterIn.HEADER,
              name = "If-Match",
              description = "ETag to verify whether user has latest data",
              schema = @Schema(type = "string")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Map.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"display\": \"form\",\n"
                      + "  \"components\": [],\n"
                      + "  \"path\": \"my-awesome-form\",\n"
                      + "  \"name\": \"my-awesome-form\",\n"
                      + "  \"title\": \"Form human-readable title\"\n"
                      + "}")})),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Form successfully updated.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"display\": \"form\",\n"
                          + "  \"components\": [],\n"
                          + "  \"path\": \"my-awesome-form\",\n"
                          + "  \"name\": \"my-awesome-form\",\n"
                          + "  \"title\": \"Form human-readable title\",\n"
                          + "  \"created\":\"2023-03-28T09:18:41.941Z\",\n"
                          + "  \"modified\":\"2023-03-29T09:58:44.100Z\""
                          + "}")
                  }
              ),
              headers = {
                  @Header(name = HttpHeaders.ETAG, description = "New ETag value for conflict verification")
              }
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Request body is not a valid json",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "409",
              description = "Conflict. __If-Match__ input value doesn't equal to servers value. It means that form already has been updated/deleted after user obtained __ETag__.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. User form is not valid.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      })
  @PutMapping(value = "/{formName}")
  public ResponseEntity<String> updateForm(
      @RequestBody String form,
      @PathVariable
      @Parameter(description = "Name of the form to be updated", required = true) String formName,
      @RequestHeader HttpHeaders headers) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");

    log.info("Started updating {} form for master", formName);
    formService.updateForm(form, formName, masterVersionId, eTag);
    log.info("Finished updating {} form for master. Retrieving this form", formName);
    var response = formService.getFormContent(formName, masterVersionId);
    log.info("Finished getting {} form from master", formName);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }
}
