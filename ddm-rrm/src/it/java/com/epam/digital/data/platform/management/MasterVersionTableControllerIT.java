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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Tables in master version controller tests")
public class MasterVersionTableControllerIT extends BaseIT {

  static final String BASE_URL = "/versions/master/tables";
  static final String DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON = "repositories/data-model-snapshot/tables/table_sample.json";
  static final String DATA_MODEL_SNAPSHOT_DIRECTORY = "repositories/data-model-snapshot/tables";

  @BeforeAll
  @SneakyThrows
  static void beforeAll() {
    Files.createDirectories(Paths.get(DATA_MODEL_SNAPSHOT_DIRECTORY));
  }

  @BeforeEach
  @SneakyThrows
  void setup() {
    var file = getFile("/table_sample.json");

    Files.copy(file.toPath(),
        Paths.get(DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON));
  }

  @SneakyThrows
  private static File getFile(String path) {
    return new File(
        Objects.requireNonNull(MasterVersionTableControllerIT.class.getResource(path)).toURI());
  }

  @Test
  @DisplayName("GET /versions/master/tables should return 200 with all tables")
  @SneakyThrows
  void getTablesTest() {
    mockMvc.perform(
        get(BASE_URL)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.[0].name", is("table_sample")),
        jsonPath("$.[0].description", is("John Doe's table")),
        jsonPath("$.[0].objectReference", is(true)),
        jsonPath("$.[0].historyFlag", is(false))
    );
  }

  @Test
  @DisplayName("GET /versions/master/tables/{tableName} should return 200 with table info")
  @SneakyThrows
  void getTableTest() {
    var name = "table_sample";
    mockMvc.perform(
        get(String.format("%s/%s", BASE_URL, name))
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.name", is(name)),
        jsonPath("$.description", is("John Doe's table")),
        jsonPath("$.objectReference", is(true)),
        jsonPath("$.historyFlag", is(false))
    );
  }

  @AfterEach
  @SneakyThrows
  void deleteFile() {
    Files.deleteIfExists(Paths.get(DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON));
  }

}
