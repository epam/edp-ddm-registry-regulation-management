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
package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.MasterVersionTableControllerIT;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.exception.TableParseException;
import com.epam.digital.data.platform.management.model.dto.TableDetailsShort;
import com.epam.digital.data.platform.management.service.impl.TableServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import data.model.snapshot.model.DdmTable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TableServiceTest {
  static final String DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON = "repositories/data-model-snapshot/tables/table_sample.json";
  static final String DATA_MODEL_SNAPSHOT_DIRECTORY = "repositories/data-model-snapshot/tables";

  @Mock
  ObjectMapper objectMapper;
  @InjectMocks
  TableServiceImpl tableService;

  @BeforeAll
  @SneakyThrows
  static void beforeAll() {
    Files.createDirectories(Paths.get(DATA_MODEL_SNAPSHOT_DIRECTORY));
  }

  @BeforeEach
  @SneakyThrows
  void setup() {
    var file = getFile("/table_sample.json");
    Files.deleteIfExists(Paths.get(DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON));
    Files.copy(file.toPath(),
        Paths.get(DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON));
  }

  @Test
  @SneakyThrows
  void listTest() {
    final DdmTable table = getTable();
    Mockito.when(objectMapper.readValue(anyString(), eq(DdmTable.class))).thenReturn(table);
    final List<TableDetailsShort> list = tableService.list();
    Assertions.assertThat(list).isNotEmpty();
    final TableDetailsShort tableDetailsShort = list.get(0);
    Assertions.assertThat(tableDetailsShort.getName()).isEqualTo(table.getName());
    Assertions.assertThat(tableDetailsShort.getDescription()).isEqualTo(table.getDescription());
    Assertions.assertThat(tableDetailsShort.getHistoryFlag()).isEqualTo(table.getHistoryFlag());
    Assertions.assertThat(tableDetailsShort.getObjectReference()).isEqualTo(table.getObjectReference());
  }

  @Test
  @SneakyThrows
  void getTest() {
    final String tableName = "table_sample";
    final DdmTable table = getTable();
    Mockito.when(objectMapper.readValue(anyString(), eq(DdmTable.class))).thenReturn(table);
    final DdmTable ddmTable = tableService.get(tableName);
    Assertions.assertThat(ddmTable).isNotNull();
    Assertions.assertThat(ddmTable.getName()).isEqualTo(table.getName());
  }

  @Test
  @SneakyThrows
  void getTableNotFoundTest() {
    final String tableName = "table_sample1";

    Assertions.assertThatThrownBy(() -> tableService.get(tableName))
        .isInstanceOf(TableNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void getTableParseExceptionTest() {
    final String tableName = "table_sample";
    Mockito.when(objectMapper.readValue(anyString(), eq(DdmTable.class))).thenThrow(
        JsonProcessingException.class);
    Assertions.assertThatThrownBy(() -> tableService.get(tableName))
        .isInstanceOf(TableParseException.class);
  }

  @AfterEach
  @SneakyThrows
  void deleteFile() {
    Files.deleteIfExists(Paths.get(DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON));
  }

  @SneakyThrows
  private static File getFile(String path) {
    return new File(
        Objects.requireNonNull(MasterVersionTableControllerIT.class.getResource(path)).toURI());
  }

  private DdmTable getTable() {
    DdmTable table = new DdmTable();
    table.setName("table_sample");
    table.setHistoryFlag(false);
    table.setDescription("John Doe's table");
    table.setObjectReference(true);
    return table;
  }
}
