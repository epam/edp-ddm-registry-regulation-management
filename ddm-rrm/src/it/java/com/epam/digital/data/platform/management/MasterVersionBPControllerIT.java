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

package com.epam.digital.data.platform.management;

import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;

@DisplayName("Business-process in master version controller tests")
class MasterVersionBPControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/master/business-processes/{businessProcessName}")
  class MasterVersionBPGetBpByNameControllerIT {

    @Test
    @DisplayName("should return 200 with business-process content")
    @SneakyThrows
    void getBusinessProcess() {
      // add file to "remote" repo and pull head repo
      final var expectedBpContent = context.getResourceContent(
          "/versions/master/business-processes/{businessProcessName}/GET/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", expectedBpContent);
      context.pullHeadRepo();

      // perform query
      mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "john-does-bp")
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isOk(),
          header().string(HttpHeaders.ETAG, ETagUtils.getETagFromContent(expectedBpContent)),
          content().contentType(MediaType.TEXT_XML),
          content().xml(expectedBpContent)
      );
    }

    @Test
    @DisplayName("should return 200 if business-process added to remote")
    @SneakyThrows
    void getBusinessProcess_businessProcessHasBeenPulledWhenReadFile() {
      // add file to "remote" repo and DO NOT pull the head repo
      final var expectedBpContent = context.getResourceContent(
          "/versions/master/business-processes/{businessProcessName}/GET/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", expectedBpContent);

      mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "john-does-bp")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          header().string(HttpHeaders.ETAG, ETagUtils.getETagFromContent(expectedBpContent)),
          content().contentType(MediaType.TEXT_XML),
          content().xml(expectedBpContent)
      );
    }

    @Test
    @DisplayName("should return 404 if business-process doesn't exist")
    @SneakyThrows
    void getBusinessProcess_businessProcessDoesNotExist() {
      mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "john-does-bp")
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
  @DisplayName("GET /versions/master/business-processes")
  class MasterVersionBPGetBpListControllerIT {

    @Test
    @DisplayName("should return 200 with all pulled business-processes")
    @SneakyThrows
    void getBusinessProcessesByVersionId() {
      // add 2 files to "remote" repo pull head branch repo and add 1 more file to "remote"
      final var johnDoesBpContent = context.getResourceContent(
          "/versions/master/business-processes/GET/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", johnDoesBpContent);
      final var mrSmithsBpContent = context.getResourceContent(
          "/versions/master/business-processes/GET/mr-smiths-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/mr-smiths-bp.bpmn", mrSmithsBpContent);
      context.pullHeadRepo();
      context.addFileToRemoteHeadRepo("/bpmn/mr-smiths-bp1.bpmn", mrSmithsBpContent);

      // define expected john-does-bp dates
      final var expectedJohnDoesBpDates = context.getHeadRepoDatesByPath(
          "bpmn/john-does-bp.bpmn");

      // perform query and expect only 2 of the processes that are pulled on head-branch repo
      mockMvc.perform(
          get("/versions/master/business-processes")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(2)),
          jsonPath("$[0].name", is("john-does-bp")),
          jsonPath("$[0].title", is("John Doe's BP")),
          jsonPath("$[0].created", is(expectedJohnDoesBpDates.getCreated())),
          jsonPath("$[0].updated", is(expectedJohnDoesBpDates.getUpdated())),
          jsonPath("$[0].etag", is(ETagUtils.getETagFromContent(johnDoesBpContent))),
          jsonPath("$[1].name", is("mr-smiths-bp")),
          jsonPath("$[1].title", is("Mr Smith's BP")),
          jsonPath("$[1].created", is("2022-10-28T06:16:26.123Z")),
          jsonPath("$[1].updated", is("2022-10-28T20:26:26.123Z")),
          jsonPath("$[1].etag", is(ETagUtils.getETagFromContent(mrSmithsBpContent)))
      );
    }

    @Test
    @DisplayName("should return 200 with empty array if there are no business-processes")
    @SneakyThrows
    void getBusinessProcessesByVersionId_NoBusinessProcesses() {
      // perform query
      mockMvc.perform(
          get("/versions/master/business-processes")
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$", hasSize(0))
      );
    }
  }

  @Nested
  @DisplayName("POST /versions/master/business-processes/{businessProcessName}")
  class CandidateVersionBPCreateBpByNameControllerIT {

    @Test
    @DisplayName("should return 201 and create business-process if there's no such process")
    @SneakyThrows
    void createBusinessProcess() {

      // define expected bp content to create
      final var expectedBpContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}/POST/valid-bp.bpmn");

      // perform query
      mockMvc.perform(
          post("/versions/master/business-processes/{businessProcessName}",
              "valid-bp")
              .contentType(MediaType.TEXT_XML)
              .content(expectedBpContent)
              .accept(MediaType.TEXT_XML)
      ).andExpectAll(
          status().isCreated(),
          header().string(HttpHeaders.ETAG, is(notNullValue())),
          content().contentType("text/xml"),
          xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("valid-bp"),
          xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string("Valid BP")
      );

      // assert that actual content and expected have no differences except for created and updated dates
      var actualBpContent = mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "valid-bp")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          header().string(HttpHeaders.ETAG, is(notNullValue())),
          content().contentType("text/xml"),
          xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("valid-bp"),
          xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string("Valid BP")
      ).andReturn().getResponse().getContentAsString();

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
          "/versions/master/business-processes/{businessProcessName}/DELETE/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", headBpContent);
      context.pullHeadRepo();

      // perform query
      mockMvc.perform(delete(
          "/versions/master/business-processes/{businessProcessName}",
          "john-does-bp")
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      mockMvc.perform(get(
          "/versions/master/business-processes/{businessProcessName}",
          "john-does-bp")
      ).andExpect(
          status().isNotFound()
      );
    }

    @Test
    @DisplayName("should return 204 if there's no such process")
    @SneakyThrows
    void deleteBusinessProcess_noBusinessProcessToDelete() {
      // perform query
      mockMvc.perform(
          delete(
              "/versions/master/business-processes/{businessProcessName}",
              "john-does-bp")
      ).andExpect(
          status().isNoContent()
      );
    }

    @Test
    @DisplayName("should return 204 and delete bp if there's already exists such bp")
    @SneakyThrows
    void deleteBP_validETag() {
      // add file to "remote" repo
      final var headBusinessContent = context.getResourceContent(
          "/versions/master/business-processes/{businessProcessName}/DELETE/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", headBusinessContent);
      context.pullHeadRepo();

      //perform get
      MockHttpServletResponse response = mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}",
              "john-does-bp")).andReturn().getResponse();

      //get eTag value from response
      String eTag = response.getHeader("ETag");

      // perform query
      mockMvc.perform(delete(
          "/versions/master/business-processes/{businessProcessName}",
          "john-does-bp")
          .header("If-Match", eTag)
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "john-does-bp")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound());
    }

    @Test
    @DisplayName("should return 409 with invalid ETag")
    @SneakyThrows
    void deleteBP_invalidETag() {
      // add file to "remote" repo
      final var headBusinessProcessContent = context.getResourceContent(
          "/versions/master/business-processes/{businessProcessName}/DELETE/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", headBusinessProcessContent);
      context.pullHeadRepo();

      // perform query
      mockMvc.perform(delete(
          "/versions/master/business-processes/{businessProcessName}",
          "john-does-bp")
          .header("If-Match", RandomString.make())
      ).andExpect(
          status().isConflict()
      );

      // assert that file was not deleted
      mockMvc.perform(
          get("/versions/master/business-processes/{businessProcessName}", "john-does-bp")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk());
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
