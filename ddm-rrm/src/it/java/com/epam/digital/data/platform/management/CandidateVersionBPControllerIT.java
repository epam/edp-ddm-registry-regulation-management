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

package com.epam.digital.data.platform.management;

import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;

@DisplayName("Business-process in version candidates controller tests")
class CandidateVersionBPControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}")
  class CandidateVersionBPGetBpByNameControllerIT {

    @Test
    @DisplayName("should return 200 with business-process content")
    @SneakyThrows
    void getBusinessProcess() {
      // add file to "remote" repo
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/GET/john-does-bp.bpmn");
      context.addFileToHeadRepo("/bpmn/john-does-bp.bpmn", expectedBpContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "john-does-bp")
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.TEXT_XML),
          content().xml(expectedBpContent)
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getBusinessProcess_versionCandidateDoesNotExist() {
      // mock gerrit change info doesn't exist
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "john-does-bp")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("REPOSITORY_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format("Version %s not found", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 404 if business-process doesn't exist")
    @SneakyThrows
    void getBusinessProcess_businessProcessDoesNotExist() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "john-does-bp")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("PROCESS_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is("Process john-does-bp not found"))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/business-processes")
  class CandidateVersionBPGetBpListControllerIT {

    @Test
    @DisplayName("should return 200 with all found business-processes")
    @SneakyThrows
    void getBusinessProcessesByVersionId() {
      // add files to "remote" repo
      final var johnDoesBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/GET/john-does-bp.bpmn");
      context.addFileToHeadRepo("/bpmn/john-does-bp.bpmn", johnDoesBpContent);
      final var mrSmithsBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/GET/mr-smiths-bp.bpmn");
      context.addFileToHeadRepo("/bpmn/mr-smiths-bp.bpmn", mrSmithsBpContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected john-does-bp dates
      final var expectedJohnDoesBpDates = context.getHeadRepoDatesByPath(
          "bpmn/john-does-bp.bpmn");

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$[0].name", is("john-does-bp")),
          jsonPath("$[0].title", is("John Doe's BP")),
          jsonPath("$[0].created", is(expectedJohnDoesBpDates.getCreated())),
          jsonPath("$[0].updated", is(expectedJohnDoesBpDates.getUpdated())),
          jsonPath("$[1].name", is("mr-smiths-bp")),
          jsonPath("$[1].title", is("Mr Smith's BP")),
          jsonPath("$[1].created", is("2022-10-28T06:16:26.123Z")),
          jsonPath("$[1].updated", is("2022-10-28T20:26:26.123Z"))
      );
    }

    @Test
    @DisplayName("should return 200 with empty array if there are no business-processes")
    @SneakyThrows
    void getBusinessProcessesByVersionId_NoBusinessProcesses() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$", hasSize(0))
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getBusinessProcessesByVersionId_versionCandidateDoesNotExist() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/business-processes", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType("application/json"),
          jsonPath("$.code", is("REPOSITORY_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format("Version %s not found", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("POST /versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}")
  class CandidateVersionBPCreateBpByNameControllerIT {

    @Test
    @DisplayName("should return 200 and create business-process if there's no such process")
    @SneakyThrows
    void createBusinessProcess() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected bp content to create
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST/valid-bp.bpmn");

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isCreated(),
          content().contentType("text/xml"),
          xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("valid-bp"),
          xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string("Valid BP")
      );

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualBpContent = context.getFileFromRemoteVersionCandidateRepo(
          "/bpmn/valid-bp.bpmn");

      assertNoDifferences(expectedBpContent, actualBpContent);

      // assert that business process dates are close to current date
      final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new InputSource(new StringReader(actualBpContent)));
      final var createdXpath = XPathFactory.newInstance().newXPath();
      final var created = createdXpath.compile("/definitions/@created").evaluate(document);
      final var updated = createdXpath.compile("/definitions/@modified").evaluate(document);
      Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void createBusinessProcess_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // define expected bp content to create
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST/valid-bp.bpmn");

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("REPOSITORY_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format("Version %s not found", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 409 if there's already exists such process")
    @SneakyThrows
    void createBusinessProcess_processAlreadyExists() {
      // add file to "remote" repo
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST/valid-bp.bpmn");
      context.addFileToHeadRepo("/bpmn/valid-bp.bpmn", expectedBpContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isConflict(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION")),
          jsonPath("$.details", is("Process with path 'bpmn/valid-bp.bpmn' already exists"))
      );
    }

    @Test
    @DisplayName("should return 422 if trying create not valid business-process")
    @SneakyThrows
    void createBusinessProcess_notValidBusinessProcess() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected bp content to create
      final var notValidBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST/not-valid-bp.bpmn");

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(notValidBpContent)
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isUnprocessableEntity(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("BUSINESS_PROCESS_CONTENT_EXCEPTION")),
          jsonPath("$.details",
              is("createBusinessProcess.businessProcess: cvc-datatype-valid.1.2.1: '' is not a valid value for 'dateTime'."))
      );
    }
  }

  @Nested
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}")
  class CandidateVersionBPUpdateBpByNameControllerIT {

    @Test
    @DisplayName("should return 200 and update business-process if there's already exists such process")
    @SneakyThrows
    void updateBusinessProcess() {
      // add file to "remote" repo
      final var headBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT/valid-bp-head.bpmn");
      context.addFileToHeadRepo("/bpmn/valid-bp.bpmn", headBpContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected bp content to update
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT/valid-bp-version-candidate.bpmn");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML).content(expectedBpContent)
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType("text/xml"),
          xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("valid-bp"),
          xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(
              "Valid BP Version Candidate")
      );

      // define expected created date for process
      final var expectedCreated = context.getHeadRepoDatesByPath("bpmn/valid-bp.bpmn")
          .getCreated();

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualBpContent = context.getFileFromRemoteVersionCandidateRepo(
          "/bpmn/valid-bp.bpmn");

      assertNoDifferences(expectedBpContent, actualBpContent);

      // assert that business process dates are close to current date
      final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new InputSource(new StringReader(actualBpContent)));
      final var createdXpath = XPathFactory.newInstance().newXPath();
      final var created = createdXpath.compile("/definitions/@created").evaluate(document);
      final var updated = createdXpath.compile("/definitions/@modified").evaluate(document);
      Assertions.assertThat(created)
          .isEqualTo(expectedCreated);
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 200 and create business-process if there's no such process")
    @SneakyThrows
    void updateBusinessProcess_noBusinessProcessToUpdate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected bp content to create
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT/valid-bp-version-candidate.bpmn");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType("text/xml"),
          xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("valid-bp"),
          xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(
              "Valid BP Version Candidate")
      );

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualBpContent = context.getFileFromRemoteVersionCandidateRepo(
          "/bpmn/valid-bp.bpmn");

      assertNoDifferences(expectedBpContent, actualBpContent);

      // assert that business process dates are close to current date
      final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new InputSource(new StringReader(actualBpContent)));
      final var createdXpath = XPathFactory.newInstance().newXPath();
      final var created = createdXpath.compile("/definitions/@created").evaluate(document);
      final var updated = createdXpath.compile("/definitions/@modified").evaluate(document);
      Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void updateBusinessProcess_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // define expected bp content to update
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT/valid-bp-version-candidate.bpmn");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("REPOSITORY_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format("Version %s not found", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 422 if trying update business-process with not valid data")
    @SneakyThrows
    void updateBusinessProcess_notValidBusinessProcess() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected bp content to update
      final var notValidBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/PUT/not-valid-bp.bpmn");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(notValidBpContent)
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isUnprocessableEntity(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("BUSINESS_PROCESS_CONTENT_EXCEPTION")),
          jsonPath("$.details",
              is("updateBusinessProcess.businessProcess: cvc-datatype-valid.1.2.1: '' is not a valid value for 'dateTime'."))
      );
    }
  }

  @Nested
  @DisplayName("DELETE /versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}")
  class CandidateVersionBPDeleteBpByNameControllerIT {

    @Test
    @DisplayName("should return 204 and delete business-process if there's already exists such process")
    @SneakyThrows
    void deleteBusinessProcess() {
      // add file to "remote" repo
      final var headBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/DELETE/john-does-bp.bpmn");
      context.addFileToHeadRepo("/bpmn/john-does-bp.bpmn", headBpContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(delete(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
          versionCandidateId, "john-does-bp")
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      final var isFileExists = context.isFileExistsInRemoteVersionCandidateRepo(
          "/bpmn/john-does-bp.bpmn");
      Assertions.assertThat(isFileExists).isFalse();
    }

    @Test
    @DisplayName("should return 204 if there's no such process")
    @SneakyThrows
    void deleteBusinessProcess_noBusinessProcessToDelete() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          delete(
              "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "john-does-bp")
      ).andExpect(
          status().isNoContent()
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void deleteBusinessProcess_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          delete(
              "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
              versionCandidateId, "valid-bp")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("REPOSITORY_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format("Version %s not found", versionCandidateId)))
      );
    }
  }

  private void assertNoDifferences(String bpFileContent, String actualContent) {
    final var documentDiff = DiffBuilder
        .compare(actualContent)
        .withTest(bpFileContent)
        .withAttributeFilter(
            attr -> !attr.getName().equals("rrm:modified") && !attr.getName()
                .equals("rrm:created"))
        .build();
    Assertions.assertThat(documentDiff.hasDifferences()).isFalse();
  }
}
