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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Data-model tables file in version candidates controller tests")
class CandidateVersionDataModelTablesControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/data-model/tables")
  class CandidateVersionDataModelTablesGetControllerIT {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void getDataModelTablesContent() {
      // add file to "remote" repo
      final var expectedContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/data-model/tables/GET/createTables.xml");
      context.addFileToRemoteHeadRepo("/data-model/createTables.xml", expectedContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .accept(MediaType.APPLICATION_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedContent.getBytes(StandardCharsets.UTF_8))
      );
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getDataModelTablesContent_versionCandidateCannotBeString() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", "master")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getDataModelTablesContent_versionCandidateDoesNotExist() {
      // mock gerrit change info doesn't exist
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is("getTablesFileContent.versionCandidateId: Version candidate does not exist."))
      );
    }

    @Test
    @DisplayName("should return 404 if tables file doesn't exist")
    @SneakyThrows
    void getDataModelTablesContent_tablesFileDoesNotExist() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("DATA_MODEL_FILE_NOT_FOUND")),
          jsonPath("$.details", is(String.format(
              "Data-model file data-model/createTables.xml is not found in version %s",
              versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/data-model/tables")
  class CandidateVersionDataModelTablesPutControllerIT {

    @Test
    @DisplayName("should return 200 with tables file content")
    @SneakyThrows
    void putDataModelTablesContent() {
      final var expectedContent = context.getResourceContent(
              "/versions/candidates/{versionCandidateId}/data-model/tables/PUT/createTables-valid.xml");

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .contentType(MediaType.APPLICATION_XML)
              .content(expectedContent.getBytes(StandardCharsets.UTF_8))
              .accept(MediaType.APPLICATION_XML)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_XML),
          content().bytes(expectedContent.getBytes(StandardCharsets.UTF_8))
      );

      Assertions.assertThat(
              context.getFileFromRemoteVersionCandidateRepo("/data-model/createTables.xml"))
          .isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void putDataModelTablesContent_versionCandidateCannotBeString() {
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", "master")
              .contentType(MediaType.APPLICATION_XML)
              .content("anyContent")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void putDataModelTablesContent_versionCandidateDoesNotExist() {
      // mock gerrit change info doesn't exist
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .contentType(MediaType.APPLICATION_XML)
              .content("anyContent")
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is("putTablesFileContent.versionCandidateId: Version candidate does not exist."))
      );
    }

    @Test
    @DisplayName("should return 422 if tables-file is not valid")
    @SneakyThrows
    void putDataModelTablesContent_tablesFileDoesNotExist() {
      final var expectedContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/data-model/tables/PUT/createTables-invalid.xml");

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/data-model/tables", versionCandidateId)
              .contentType(MediaType.APPLICATION_XML)
              .content(expectedContent)
              .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isUnprocessableEntity(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("INVALID_CHANGELOG")),
          jsonPath("$.details",
              is("putTablesFileContent.fileContent: cvc-elt.1.a: Cannot find the declaration of element 'databaseBrokenChangeLog'."))
      );
    }
  }
}
