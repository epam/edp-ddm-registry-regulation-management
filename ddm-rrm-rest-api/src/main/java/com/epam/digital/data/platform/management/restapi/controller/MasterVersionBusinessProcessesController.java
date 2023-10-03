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
import com.epam.digital.data.platform.management.groups.service.GroupService;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.validation.businessProcess.BusinessProcess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(description = "Registry regulations master Business processes management Rest API",  name = "master-version-business-processes-api")
@RestController
@Validated
@RequestMapping("/versions/master/business-processes")
@RequiredArgsConstructor
public class MasterVersionBusinessProcessesController {

  private final BusinessProcessService businessProcessService;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final GroupService groupService;

  @Operation(
      summary = "Get a list of business processes with brief details for the master version",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a list of JSON representations of user __business processes__ directly from the __master__ version, containing only brief information about each _business process_. If you need to retrieve full details of a single _business process_ based on its __businessProcessName__, you can use the [GET](#master-version-business-processes-api/getBusinessProcess) endpoint.",
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
              description = "OK. Business processes successfully retrieved.",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(schema = @Schema(implementation = BusinessProcessDetailsShort.class)))
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
  public ResponseEntity<List<BusinessProcessDetailsShort>> getBusinessProcessesFromMaster() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting business processes from master");
    var response = businessProcessService.getProcessesByVersion(masterVersionId).stream()
        .map(e -> BusinessProcessDetailsShort.builder()
            .name(e.getName())
            .title(e.getTitle())
            .created(e.getCreated())
            .updated(e.getUpdated())
            .build())
        .collect(Collectors.toList());
    log.info("Found {} business processes in master", response.size());
    return ResponseEntity.ok().body(response);
  }

  @Operation(
      summary = "Get specific business process full details",
      description = "### Endpoint purpose:\n This endpoint is used for retrieving a XML representation of a user __business-process__ directly from the __master__ version. This operation retrieves a single _business-process_ based on the specified __businessProcessName__ with full details in _XML_ format. If you need to retrieve list of _business-processes_ with brief information and in _json_ format, you can use the [GET](#master-version-business-processes-api/getBusinessProcessesFromMaster).\n",
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
              description = "OK. Business process successfully retrieved.",
              content = @Content(
                  mediaType = MediaType.TEXT_PLAIN_VALUE,
                  examples = {
                      @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                          + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                          + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                          + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                          + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\"\n"
                          + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\"\n"
                          + "  modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\">\n"
                          + "  <bpmn:process id=\"john-does-bp\" name=\"John Doe's BP\" isExecutable=\"true\">\n"
                          + "    <bpmn:startEvent id=\"StartEvent_1\"/>\n"
                          + "  </bpmn:process>\n"
                          + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                          + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"name\">\n"
                          + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n"
                          + "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\"/>\n"
                          + "      </bpmndi:BPMNShape>\n"
                          + "    </bpmndi:BPMNPlane>\n"
                          + "  </bpmndi:BPMNDiagram>\n"
                          + "</bpmn:definitions>")
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
  @GetMapping("/{businessProcessName}")
  public ResponseEntity<String> getBusinessProcess(
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.info("Started getting {} business process from master", businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName, masterVersionId);
    log.info("Finished getting {} business process from master", businessProcessName);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Update business process within master version.",
      description = "### Endpoint purpose: \n This endpoint is used for updating a xml representation of a user __business process__ directly in __master__ version. Just as if _version-candidate_ was created, the _business process_ was updated in that _version-candidate_ and then the _version-candidate_ was submitted. It can be used if there is needed to update __a single business process__. If you need to make some changes in several _business processes_ at one time, it's still preferred to make this changes through a _version-candidate_. \n ### Conflict resolving:\n In this endpoint [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, that can be previously obtained in [GET](#master-version-business-processes-api/getBusinessProcess) request, as a value for __If-Match__ header so you can be sure that you're updating the last version of a _business-process_. But if your __If-Match__ value is differs from the servers you will receive a _409 Conflict_ instead of _412 Precondition Failed_. For _registry-regulation-management_ service this situation's considered as a conflict. If __If-Match__ is not present then conflict checking won't be performed.\n### Business process validation: \nBefore saving the content to the storage, the __validation__ of a _business-process_ is executed. The _business-process_ must be a __xml__ document, must conform to the BPMN20.xsd schema (available at https://github.com/bpmn-io/bpmn-moddle/blob/master/resources/bpmn/xsd/BPMN20.xsd) and must have a non-empty __\"name\"__ field (attribute as part of tCallableElement). Also _name_ values must be equal to __\"businessProcessName\"__ pathVariable. In other case the _business-process_ won't be working as expected. Changing __\"name\"__ is not supported. If you need to change this field then you need to copy the _business-process_ with new name and delete the previous _business-process_. \n ### Missing business process handling: \n If the updated _business-process_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__) then the _business-process_ will be __created__ instead.",
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
          content = @Content(
              mediaType = MediaType.TEXT_PLAIN_VALUE,
              schema = @Schema(type = "string"),
              examples = {
                  @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                      + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                      + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                      + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                      + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\"\n"
                      + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\"\n"
                      + "  modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\">\n"
                      + "  <bpmn:process id=\"john-does-bp\" name=\"John Doe's BP\" isExecutable=\"true\">\n"
                      + "    <bpmn:startEvent id=\"StartEvent_1\"/>\n"
                      + "  </bpmn:process>\n"
                      + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                      + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"name\">\n"
                      + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n"
                      + "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\"/>\n"
                      + "      </bpmndi:BPMNShape>\n"
                      + "    </bpmndi:BPMNPlane>\n"
                      + "  </bpmndi:BPMNDiagram>\n"
                      + "</bpmn:definitions>")
              }
          )),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Business process successfully updated.",
              content = @Content(
                  mediaType = MediaType.TEXT_PLAIN_VALUE,
                  examples = {
                      @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                          + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                          + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                          + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                          + "  xmlns:rrm=\"http://registry-regulation-management\"\n"
                          + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\"\n"
                          + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\"\n"
                          + "  modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\"\n"
                          + "  rrm:created=\"2022-10-28T06:16:26.123Z\" rrm:modified=\"2022-10-28T20:26:26.123Z\">\n\n"
                          + "  <bpmn:process id=\"john-does-bp\" name=\"John Doe's BP\" isExecutable=\"true\">\n"
                          + "    <bpmn:startEvent id=\"StartEvent_1\"/>\n"
                          + "  </bpmn:process>\n"
                          + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                          + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"name\">\n"
                          + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n"
                          + "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\"/>\n"
                          + "      </bpmndi:BPMNShape>\n"
                          + "    </bpmndi:BPMNPlane>\n"
                          + "  </bpmndi:BPMNDiagram>\n"
                          + "</bpmn:definitions>")
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
              responseCode = "409",
              description = "Conflict. __If-Match__ input value doesn't equal to servers value. It means that business process already has been updated/deleted after user obtained __ETag__.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. User business process is not valid.",
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
  @PutMapping("/{businessProcessName}")
  public ResponseEntity<String> updateBusinessProcess(
      @RequestBody @BusinessProcess String businessProcess,
      @PathVariable @Parameter(description = "Process name", required = true)
      String businessProcessName,
      @RequestHeader HttpHeaders headers) {

    log.info("Started updating business process {} for master", businessProcessName);

    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");

    businessProcessService.updateProcess(businessProcess, businessProcessName, masterVersionId,
        eTag);
    log.info(
        "Finished updating business process {} for master. Retrieving this process",
        businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName, masterVersionId);
    log.info("Finished getting business process {} from {} version candidate", businessProcessName,
        masterVersionId);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Create new business process",
      description = "### Endpoint purpose: \n This endpoint is used for creating a xml representation of a user __business process__ directly in __master__ version. Just as if _version-candidate_ was created, the _business process_ was created in that _version-candidate_ and then the _version-candidate_ was submitted. It can be used if there is needed to create __a single business process__. If you need to create several _business processes_ at one time, it's still preferred to make this changes through a _version-candidate_. \n ### Business process validation: \nBefore saving the content to the storage, the __validation__ of a _business-process_ is executed. The _business-process_ must be a __xml__ document, must conform to the BPMN20.xsd schema (available at https://github.com/bpmn-io/bpmn-moddle/blob/master/resources/bpmn/xsd/BPMN20.xsd) and must have a non-empty __\"name\"__ field (attribute as part of tCallableElement). Also _name_ values must be equal to __\"businessProcessName\"__ pathVariable. In other case the _business-process_ won't be working as expected. \n### Missing business process handling: \n If the specified _business-process_ does not already exist, the server will create a new _business-process_ with the provided data. Otherwise, the server will return a _409 Conflict_ error indicating that the _business-process_ already exists.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(
              mediaType = MediaType.TEXT_PLAIN_VALUE,
              schema = @Schema(type = "string"),
              examples = {
                  @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                      + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                      + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                      + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                      + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\"\n"
                      + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\"\n"
                      + "  modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\">\n"
                      + "  <bpmn:process id=\"john-does-bp\" name=\"John Doe's BP\" isExecutable=\"true\">\n"
                      + "    <bpmn:startEvent id=\"StartEvent_1\"/>\n"
                      + "  </bpmn:process>\n"
                      + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                      + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"name\">\n"
                      + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n"
                      + "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\"/>\n"
                      + "      </bpmndi:BPMNShape>\n"
                      + "    </bpmndi:BPMNPlane>\n"
                      + "  </bpmndi:BPMNDiagram>\n"
                      + "</bpmn:definitions>")
              }
          )),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Business process successfully created.",
              content = @Content(
                  mediaType = MediaType.TEXT_PLAIN_VALUE,
                  examples = {
                      @ExampleObject(value = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                          + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
                          + "  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"
                          + "  xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"
                          + "  xmlns:rrm=\"http://registry-regulation-management\"\n"
                          + "  xmlns:modeler=\"http://camunda.org/schema/modeler/1.0\" id=\"Definitions_1poh5q3\"\n"
                          + "  targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.1.0\"\n"
                          + "  modeler:executionPlatform=\"Camunda Cloud\" modeler:executionPlatformVersion=\"8.0.0\"\n"
                          + "  rrm:created=\"2022-10-28T06:16:26.123Z\" rrm:modified=\"2022-10-28T20:26:26.123Z\">\n\n"
                          + "  <bpmn:process id=\"john-does-bp\" name=\"John Doe's BP\" isExecutable=\"true\">\n"
                          + "    <bpmn:startEvent id=\"StartEvent_1\"/>\n"
                          + "  </bpmn:process>\n"
                          + "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n"
                          + "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"name\">\n"
                          + "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n"
                          + "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\"/>\n"
                          + "      </bpmndi:BPMNShape>\n"
                          + "    </bpmndi:BPMNPlane>\n"
                          + "  </bpmndi:BPMNDiagram>\n"
                          + "</bpmn:definitions>")
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
              responseCode = "409",
              description = "Conflict. It means that business process already has been created.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Unprocessable Entity. User business process is not valid.",
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
  @PostMapping("/{businessProcessName}")
  public ResponseEntity<String> createBusinessProcess(
      @RequestBody @BusinessProcess String businessProcess,
      @PathVariable @Parameter(description = "Name of the new process to be created", required = true) String businessProcessName) {
    log.info("Started creating business process {} for master version", businessProcessName);
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();

    businessProcessService.createProcess(businessProcessName, businessProcess, masterVersionId);
    log.info("Finished creating business process {} for master version. Retrieving process",
        businessProcessName);
    var response = businessProcessService.getProcessContent(businessProcessName,
        masterVersionId);
    log.info("Finished getting business process {} from master version", businessProcessName);
    return ResponseEntity.created(URI.create(
            String.format("/versions/master/business-processes/%s",
                businessProcessName)))
        .contentType(MediaType.TEXT_XML)
        .eTag(ETagUtils.getETagFromContent(response))
        .body(response);
  }

  @Operation(
      summary = "Delete existing business process",
      description = "### Endpoint purpose:\n This endpoint is used for deleting a user __business-process__ directly from the __master__ version.\n### Conflict resolving:\n In this endpoint, [Conditional requests](https://datatracker.ietf.org/doc/html/rfc9110#name-conditional-requests) are supported. You can use an __ETag__ header value, which can be previously obtained in a [GET](#master-version-business-processes-api/getBusinessProcess) request, as a value for the __If-Match__ header. This ensures that you're deleting the latest version of the _business process_. However, if your __If-Match__ value differs from the server's value, you will receive _409 Conflict_ instead of _412 Precondition Failed_. For the _registry-regulation-management_ service, this situation is considered a conflict. If the __If-Match__ header is not present, conflict checking will not be performed.\n ### Missing business process handling:\n If the specified _business process_ is missing and the _If-Match_ header is not present (or equal to __\"*\"__), the server will return a 404 Not Found error indicating that the specified _business process_ does not exist.",
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
              description = "No Content. Business process successfully deleted.",
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
              description = "Conflict. __If-Match__ input value doesn't equal to servers value. It means that business process already has been updated/deleted after user obtained __ETag__.",
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
  @DeleteMapping("/{businessProcessName}")
  public ResponseEntity<String> deleteBusinessProcess(
      @PathVariable @Parameter(description = "Process name", required = true) String businessProcessName,
      @RequestHeader HttpHeaders headers) {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    var eTag = headers.getFirst("If-Match");

    log.info("Started deleting business process {} from master", businessProcessName);
    businessProcessService.deleteProcess(businessProcessName, masterVersionId, eTag);
    groupService.deleteProcessDefinition(businessProcessName, masterVersionId);
    log.info("Finished deleting business process {} from master", businessProcessName);
    return ResponseEntity.noContent().build();
  }
}
