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

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Tables in version-candidate controller tests")
class CandidateVersionTableControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/tables")
  class CandidateVersionListTablesIT {

    @Test
    @DisplayName("should return 200 with all found tables")
    @SneakyThrows
    void getTablesTest() {
      final var versionCandidateId = context.createVersionCandidate();
      context.prepareRegistryDataSource(versionCandidateId, "liquibase/create-table_1.xml");
      context.prepareRegistryDataSource(versionCandidateId, "liquibase/create-table_2.xml");

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidateId)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(5)),
          jsonPath("$.[0].name", is("master_table")),
          jsonPath("$.[0].description", is("table in master version")),
          jsonPath("$.[0].objectReference", is(true)),
          jsonPath("$.[1].name", is("subject")),
          jsonPath("$.[1].description", is(nullValue())),
          jsonPath("$.[1].objectReference", is(false)),
          jsonPath("$.[2].name", is("subject_settings")),
          jsonPath("$.[2].description", is(nullValue())),
          jsonPath("$.[2].objectReference", is(true)),
          jsonPath("$.[3].name", is("table_1")),
          jsonPath("$.[3].description", is("table #1")),
          jsonPath("$.[3].objectReference", is(false)),
          jsonPath("$.[4].name", is("table_2")),
          jsonPath("$.[4].description", is("table #2")),
          jsonPath("$.[4].objectReference", is(false))
      );
    }

    @Test
    @DisplayName("should return different set of tables than master version")
    @SneakyThrows
    void getTablesTest_differentFromMaster() {
      final var versionCandidateId = context.createVersionCandidate();
      context.prepareRegistryDataSource(context.getGerritProps().getHeadBranch(),
          "liquibase/create-table_1.xml");
      context.prepareRegistryDataSource(versionCandidateId, "liquibase/create-table_2.xml");

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidateId)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(4)),
          jsonPath("$.[0].name", is("master_table")),
          jsonPath("$.[1].name", is("subject")),
          jsonPath("$.[2].name", is("subject_settings")),
          jsonPath("$.[3].name", is("table_2"))
      );

      mockMvc.perform(
          get("/versions/master/tables", versionCandidateId)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(4)),
          jsonPath("$.[0].name", is("master_table")),
          jsonPath("$.[1].name", is("subject")),
          jsonPath("$.[2].name", is("subject_settings")),
          jsonPath("$.[3].name", is("table_1"))
      );
    }

    @Test
    @DisplayName("should return 200 with empty list when version-candidate dataBase doesn't exist")
    @SneakyThrows
    void getTablesTest_noVersionCandidateDataBase() {
      final var versionCandidateId = context.createVersionCandidate();
      context.dropDataBase(versionCandidateId);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidateId)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getTablesTest_versionCandidateCannotBeString() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", "master")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getTablesTest_versionCandidateDoesNotExists() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidateId)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is("getTables.versionCandidateId: Version candidate does not exist."))
      );
    }

    @Test
    @DisplayName("should return 500 with empty list when version-candidate dataBase doesn't exist")
    @SneakyThrows
    void getTablesTest_noVersionCandidateDataBaseAndNoMasterDataBase() {
      final var versionCandidateId = context.createVersionCandidate();
      context.dropDataBase(versionCandidateId);
      context.dropDataBase(context.getGerritProps().getHeadBranch());

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables", versionCandidateId)
      ).andExpectAll(
          status().isInternalServerError(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details",
              is("Couldn't connect to registry data-base: FATAL: database \"registry_head_branch\" does not exist"))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/tables/{tableName}")
  class CandidateVersionGetTableIT {

    @Test
    @DisplayName("should return 200 with table info")
    @SneakyThrows
    void getTableTest() {
      final var versionCandidateId = context.createVersionCandidate();

      var name = "master_table";
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              name)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is(name)),
          jsonPath("$.description", is("table in master version")),
          jsonPath("$.objectReference", is(true)),
          jsonPath("$.columns.subject_id.name", is("subject_id")),
          jsonPath("$.columns.subject_id.description", is(nullValue())),
          jsonPath("$.columns.subject_id.type", is("uuid")),
          jsonPath("$.columns.subject_id.defaultValue", is(nullValue())),
          jsonPath("$.columns.subject_id.notNullFlag", is(true)),
          jsonPath("$.columns.id.name", is("id")),
          jsonPath("$.columns.id.description", is(nullValue())),
          jsonPath("$.columns.id.type", is("int4")),
          jsonPath("$.columns.id.defaultValue", is(nullValue())),
          jsonPath("$.columns.id.notNullFlag", is(true)),
          jsonPath("$.foreignKeys.fk_master_table_subject.name", is("fk_master_table_subject")),
          jsonPath("$.foreignKeys.fk_master_table_subject.targetTable", is("subject")),
          jsonPath("$.foreignKeys.fk_master_table_subject.sourceTable", is("master_table")),
          jsonPath("$.foreignKeys.fk_master_table_subject.columnPairs", hasSize(1)),
          jsonPath("$.foreignKeys.fk_master_table_subject.columnPairs[0].sourceColumnName",
              is("subject_id")),
          jsonPath("$.foreignKeys.fk_master_table_subject.columnPairs[0].targetColumnName",
              is("subject_id")),
          jsonPath("$.primaryKey.name", is("master_table_pkey")),
          jsonPath("$.primaryKey.columns[0].name", is("id")),
          jsonPath("$.primaryKey.columns[0].sorting", is("ASC")),
          jsonPath("$.uniqueConstraints", is(Map.of())),
          jsonPath("$.indices.ix_master_table_subject__subject_id.name",
              is("ix_master_table_subject__subject_id")),
          jsonPath("$.indices.ix_master_table_subject__subject_id.columns", hasSize(1)),
          jsonPath("$.indices.ix_master_table_subject__subject_id.columns[0].name",
              is("subject_id")),
          jsonPath("$.indices.ix_master_table_subject__subject_id.columns[0].sorting", is("ASC"))
      );
    }

    @Test
    @DisplayName("should return different table state than master version")
    @SneakyThrows
    void getTableTest_differentFromMaster() {
      final var versionCandidateId = context.createVersionCandidate();
      context.prepareRegistryDataSource(versionCandidateId, "liquibase/update-master_table.xml");

      var name = "master_table";
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              name)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is(name)),
          jsonPath("$.columns", hasKey("string_column")),
          jsonPath("$.columns.string_column.name", is("string_column"))
      );

      mockMvc.perform(
          get("/versions/master/tables/{tableName}", name)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is(name)),
          jsonPath("$.columns", not(hasKey("string_column")))
      );
    }

    @Test
    @DisplayName("should return 400 if try to put string as version-candidate")
    @SneakyThrows
    void getTableTest_versionCandidateCannotBeString() {
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", "master",
              "master_table")
      ).andExpectAll(
          status().isBadRequest(),
          content().string("")
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getTableTest_versionCandidateDoesNotExists() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              "master_table")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is("getTable.versionCandidateId: Version candidate does not exist."))
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate exists but version database doesn't")
    @SneakyThrows
    void getTableTest_versionCandidateExistsButDatabaseDoesNot() {
      final var versionCandidateId = context.createVersionCandidate();
      context.dropDataBase(versionCandidateId);

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              "master_table")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("TABLE_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format(
              "Table with name 'master_table' doesn't exist in version '%s'.", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 404 if table doesn't exist in version-candidate")
    @SneakyThrows
    void getTableTest_tableDoesNotExist() {
      final var versionCandidateId = context.createVersionCandidate();

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              "not_existing_table")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("TABLE_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format(
              "Table with name 'not_existing_table' doesn't exist in version '%s'.",
              versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 500 if version-candidate exists but all databases don't exist")
    @SneakyThrows
    void getTableTest_allDatabasesDoNotExist() {
      final var versionCandidateId = context.createVersionCandidate();
      context.dropDataBase(versionCandidateId);
      context.dropDataBase(context.getGerritProps().getHeadBranch());

      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/tables/{tableName}", versionCandidateId,
              "not_existing_table")
      ).andExpectAll(
          status().isInternalServerError(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details",
              is("Couldn't connect to registry data-base: FATAL: database \"registry_head_branch\" does not exist"))
      );
    }
  }
}
