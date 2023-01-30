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

@DisplayName("Tables in master version controller tests")
public class MasterVersionTableControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/master/tables")
  class MasterVersionGetTablesIT {

    @Test
    @DisplayName("should return 200 with all tables")
    @SneakyThrows
    void getTablesTest() {
      mockMvc.perform(
          get("/versions/master/tables")
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(3)),
          jsonPath("$.[0].name", is("master_table")),
          jsonPath("$.[0].description", is("table in master version")),
          jsonPath("$.[0].objectReference", is(true)),
          jsonPath("$.[1].name", is("subject")),
          jsonPath("$.[1].description", is(nullValue())),
          jsonPath("$.[1].objectReference", is(false)),
          jsonPath("$.[2].name", is("subject_settings")),
          jsonPath("$.[2].description", is(nullValue())),
          jsonPath("$.[2].objectReference", is(true))
      );
    }

    @Test
    @DisplayName("should return 500 if couldn't connect to registry database")
    @SneakyThrows
    void getTablesTest_noRegistryDataBase() {
      context.dropDataBase(context.getGerritProps().getHeadBranch());
      mockMvc.perform(
          get("/versions/master/tables")
      ).andExpectAll(
          status().isInternalServerError(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details",
              is("Couldn't connect to registry data-base: Schema crawler catalog couldn't be created: FATAL: database \"registry_head_branch\" does not exist"))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/master/tables/{tableName}")
  class MasterVersionGetTableIT {

    @Test
    @DisplayName("should return 200 with table info")
    @SneakyThrows
    void getTableTest() {
      context.prepareRegistryDataSource(context.getGerritProps().getHeadBranch(),
          "liquibase/update-master_table.xml");
      var name = "master_table";
      mockMvc.perform(
          get("/versions/master/tables/{tableName}", name)
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
          jsonPath("$.columns.string_column.name", is("string_column")),
          jsonPath("$.columns.string_column.description", is("string column in master_table")),
          jsonPath("$.columns.string_column.type", is("varchar")),
          jsonPath("$.columns.string_column.defaultValue", is(nullValue())),
          jsonPath("$.columns.string_column.notNullFlag", is(false)),
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
    @DisplayName("should return 400 if table name is not valid")
    @SneakyThrows
    void getTableTest_tableNameNotValid() {
      mockMvc.perform(
          get("/versions/master/tables/{tableName}", "table_with_suffix_v")
      ).andExpectAll(
          status().isBadRequest(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("Bad Request")),
          jsonPath("$.details", is("getTable.tableName: Table cannot be ended with suffix '_v'."))
      );
    }

    @Test
    @DisplayName("should return 404 if table doesn't exist")
    @SneakyThrows
    void getTableTest_tableDoesNotExist() {
      mockMvc.perform(
          get("/versions/master/tables/{tableName}", "not_existing_table")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("TABLE_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is(String.format(
              "Table with name 'not_existing_table' doesn't exist in version '%s'.",
              context.getGerritProps().getHeadBranch())))
      );
    }

    @Test
    @DisplayName("should return 500 if couldn't connect to registry database")
    @SneakyThrows
    void getTableTest_noRegistryDataBase() {
      context.dropDataBase(context.getGerritProps().getHeadBranch());
      mockMvc.perform(
          get("/versions/master/tables/{tableName}", "master_table")
      ).andExpectAll(
          status().isInternalServerError(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.traceId", is(notNullValue())),
          jsonPath("$.code", is("REGISTRY_DATA_BASE_CONNECTION_ERROR")),
          jsonPath("$.details",
              is("Couldn't connect to registry data-base: Schema crawler catalog couldn't be created: FATAL: database \"registry_head_branch\" does not exist"))
      );
    }
  }
}
