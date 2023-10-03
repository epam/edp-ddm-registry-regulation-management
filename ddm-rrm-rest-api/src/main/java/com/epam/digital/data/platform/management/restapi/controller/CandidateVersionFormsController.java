/*
 * Copyright 2023 EPAM Systems.
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
package com.epam.digital.data.platform.management.restapi.controller;

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
@Tag(description = "Registry regulations version-candidate Forms management Rest API",  name = "candidate-version-forms-api")
@RestController
@RequestMapping("/versions/candidates/{versionCandidateId}/forms")
@RequiredArgsConstructor
public class CandidateVersionFormsController {

  private final FormService formService;

  @Operation(
      summary = "Acquire list of forms with brief details for specific version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a list of JSON representations of user __forms__ from the __version-candidate__, containing only brief information about each _form_. If you need to retrieve full details of a single _form_ based on its __formName__, you can use the [GET](#candidate-version-forms-api/getForm) endpoint.",
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
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = FormDetailsShort.class)),
                  examples = {
                      @ExampleObject(value = "[\n" +
                          "  {\n" +
                          "    \"name\": \"john-does-form\",\n" +
                          "    \"title\": \"John Doe added new component\",\n" +
                          "    \"created\": \"2022-07-29T18:55:00.000Z\",\n" +
                          "    \"updated\": \"2022-07-29T18:56:00.000Z\"\n" +
                          "  }\n" +
                          "]")
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
  public ResponseEntity<List<FormDetailsShort>> getFormsByVersionId(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId) {
    log.info("Started getting forms for {} version candidate", versionCandidateId);
    var response = formService.getFormListByVersion(versionCandidateId).stream()
        .map(e -> FormDetailsShort.builder()
            .name(e.getName())
            .title(e.getTitle())
            .created(e.getCreated())
            .updated(e.getUpdated())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} forms from {} version candidate", response.size(), versionCandidateId);
    return ResponseEntity.ok().body(response);
  }

  @Operation(
      summary = "Create new form within specific version-candidate",
      description = "### Endpoint purpose: \n This endpoint is used for creating a JSON representation of a user __form__ in the __version-candidate__.\n### Form validation: \nBefore saving the new _form_ to the storage, the server validates the _form_. The _form_ must be a __json__ document and must have a non-empty __\"title\"__ field. Also the field __\"name\"__ must be present and equal to __\"path\"__ field, that must be present too. Also _both_ this values must be equal to __\"formName\"__ pathVariable. In other case the _form_ won't be working as expected. \n ### Missing form handling: \n If the specified _form_ does not already exist, the server will create a new _form_ with the provided data. Otherwise, the server will return a _409 Conflict_ error indicating that the _form_ already exists.\n ### Created and modified dates handling:\n If there any of __\"created\"__ or __\"modified\"__ fields present in the request body they will be ignored. The __\"created\"__ and __\"updated\"__ fields are automatically set to the current server time in UTC.",
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
      }
  )
  @PostMapping("/{formName}")
  public ResponseEntity<String> formCreate(@RequestBody String form,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Name of the new form to be created", required = true) String formName) {
    log.info("Started creating {} form for {} version candidate", formName, versionCandidateId);
    formService.createForm(formName, form, versionCandidateId);
    log.info("Form {} was created for {} version candidate. Retrieving this form", formName, versionCandidateId);
    var response = formService.getFormContent(formName, versionCandidateId);
    log.info("Finished getting {} form from {} version candidate", formName, versionCandidateId);
    return ResponseEntity.created(URI.create(
            String.format("/versions/candidates/%s/forms/%s", versionCandidateId, formName)))
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Get full details of the specific form within version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a JSON representation of a user __form__ from the __version-candidate__. This operation retrieves a single _form_ based on the specified __formName__. If you need to retrieve list of _forms_, you can use the [GET](#candidate-version-forms-api/getFormsByVersionId) endpoint.\n",
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
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @GetMapping("/{formName}")
  public ResponseEntity<String> getForm(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Form name", required = true) String formName) {
    log.info("Started getting {} form from {} version candidate", formName, versionCandidateId);
    var response = formService.getFormContent(formName, versionCandidateId);
    log.info("Finished getting {} form from {} version candidate", formName, versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Update existing form within version-candidate",
      description = "### Endpoint purpose: \n This endpoint is used for updating a json representation of a user __form__ in __version-candidate__.\n### Conflict resolving:\n In this endpoint [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, that can be previously obtained in [GET](#candidate-version-forms-api/getForm) request, as a value for __If-Match__ header so you can be sure that you're updating the last version of a _form_. But if your __If-Match__ value is differs from the servers you will receive a _409 Conflict_ instead of _412 Precondition Failed_. For _registry-regulation-management_ service this situation's considered as a conflict. If __If-Match__ is not present then conflict checking won't be performed.\n### Form validation: \nBefore saving the content to the storage, the __validation__ of a _form_ is executed. The _form_ must be a __json__ document and must have a non-empty __\"title\"__ field. Also the field __\"name\"__ must be present and equal to __\"path\"__ field, that must be present too. Also _both_ this values must be equal to __\"formName\"__ pathVariable. In other case the _form_ won't be working as expected. Changing __\"name\"__ or __\"path\"__ is not supported. If you need to change these fields then you need to copy the _form_ with new name and delete the previous _form_.\n### Missing form handling: \nIf the updated _form_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__) then the _form_ will be __created__ instead.\n### Created and modified dates handling:\nIf there any of __\"created\"__ or __\"modified\"__ fields present in the request body they will be ignored. Value for the __\"created\"__ field is automatically getting from the previous _form_ content (if present, in other case it's getting from the git log). And for the __\"updated\"__ value the current servers datetime in UTC is set.",
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
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PutMapping(value = "/{formName}")
  public ResponseEntity<String> updateForm(@RequestBody String form,
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Name of the form to be updated", required = true) String formName,
      @RequestHeader HttpHeaders headers) {
    var eTag = headers.getFirst("If-Match");
    log.info("Started updating {} form for {} version candidate", formName, versionCandidateId);
    formService.updateForm(String.valueOf(form), formName, versionCandidateId, eTag);
    log.info("Finished updating {} form for {} version candidate. Retrieving this form", formName, versionCandidateId);
    var response = formService.getFormContent(formName, versionCandidateId);
    log.info("Finished getting {} form from {} version candidate", formName, versionCandidateId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Delete existing form within version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for deleting a JSON representation of a user __form__ from the __version-candidate__.\n### Conflict resolving:\n In this endpoint, [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, which can be previously obtained in a [GET](#candidate-version-forms-api/getForm) request, as a value for the __If-Match__ header. This ensures that you're deleting the latest version of the _form_. However, if your __If-Match__ value differs from the server's value, you will receive _409 Conflict_ instead of _412 Precondition Failed_. For the _registry-regulation-management_ service, this situation is considered a conflict. If the __If-Match__ header is not present, conflict checking will not be performed.\n### Missing form handling:\nIf the specified _form_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__), the server will return a 404 Not Found error indicating that the specified _form_ does not exist.",
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
      }
  )
  @DeleteMapping("/{formName}")
  public ResponseEntity<String> deleteForm(@PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Name of the form to be deleted", required = true) String formName,
      @RequestHeader HttpHeaders headers) {
    log.info("Started deleting {} form for {} version candidate", formName, versionCandidateId);
    var eTag = headers.getFirst("If-Match");
    formService.deleteForm(formName, versionCandidateId, eTag);
    log.info("Form {} was deleted from {} version candidate", formName, versionCandidateId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Rollback existing form within version-candidate",
      description = "### Endpoint purpose:\n This endpoint is used for rolling back a user __form__ from the __version-candidate__. It is intended for situations where a __form__ needs to be reverted to a prior version, such as to mitigate data corruption or to restore a previous state.",
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
              description = "OK. Form successfully rolled back.",
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @PostMapping("/{formName}/rollback")
  public ResponseEntity<String> rollbackForm(
      @PathVariable @Parameter(description = "Version candidate identifier", required = true) String versionCandidateId,
      @PathVariable @Parameter(description = "Name of the form to be rolled back", required = true) String formName) {
    log.info("Started rollback {} form from {} version candidate", formName, versionCandidateId);
    formService.rollbackForm(formName, versionCandidateId);
    log.info("Finished rolling back form {} from the {} version candidate", formName,
        versionCandidateId);
    return ResponseEntity.ok().build();
  }
}
