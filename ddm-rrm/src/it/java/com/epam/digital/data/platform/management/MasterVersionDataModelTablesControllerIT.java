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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Data-model tables file in master version controller tests")
class MasterVersionDataModelTablesControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/master/data-model/tables")
  class MasterVersionDataModelTablesGetControllerIT {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void getTablesFileContent() {
      // add file to "remote" repo and pull head repo
      final var expectedContent = context.getResourceContent(
          "/versions/master/data-model/tables/GET/createTables.xml");
      context.addFileToRemoteHeadRepo("/data-model/createTables.xml", expectedContent);
      context.pullHeadRepo();

      // perform query
      mockMvc.perform(
          get("/versions/master/data-model/tables")
              .accept(MediaType.APPLICATION_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedContent.getBytes(StandardCharsets.UTF_8))
      );
    }

    @Test
    @DisplayName("should return 404 if tables file hasn't been pulled from remote")
    @SneakyThrows
    void getTablesFileContent_tablesFileHasNotBeenPulled() {
      // add file to "remote" repo and DO NOT pull the head repo
      final var expectedContent = context.getResourceContent(
          "/versions/master/data-model/tables/GET/createTables.xml");
      context.addFileToRemoteHeadRepo("/data-model/createTables.xml", expectedContent);

      mockMvc.perform(
          get("/versions/master/data-model/tables")
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("DATA_MODEL_FILE_NOT_FOUND")),
          jsonPath("$.details",
              is("Data-model file data-model/createTables.xml is not found in version head-branch"))
      );
    }

    @Test
    @DisplayName("should return 404 if tables file doesn't exist")
    @SneakyThrows
    void getTablesFileContent_tablesFileDoesNotExist() {
      mockMvc.perform(
          get("/versions/master/data-model/tables")
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("DATA_MODEL_FILE_NOT_FOUND")),
          jsonPath("$.details",
              is("Data-model file data-model/createTables.xml is not found in version head-branch"))
      );
    }
  }
}
