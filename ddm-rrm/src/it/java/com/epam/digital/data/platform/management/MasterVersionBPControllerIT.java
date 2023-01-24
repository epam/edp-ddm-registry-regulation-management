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

package com.epam.digital.data.platform.management;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
          content().contentType(MediaType.TEXT_XML),
          content().xml(expectedBpContent)
      );
    }

    @Test
    @DisplayName("should return 404 if business-process hasn't been pulled from remote")
    @SneakyThrows
    void getBusinessProcess_businessProcessHasNotBeenPulled() {
      // add file to "remote" repo and DO NOT pull the head repo
      final var expectedBpContent = context.getResourceContent(
          "/versions/master/business-processes/{businessProcessName}/GET/john-does-bp.bpmn");
      context.addFileToRemoteHeadRepo("/bpmn/john-does-bp.bpmn", expectedBpContent);

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
}
