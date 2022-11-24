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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.TestUtils;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.exception.TableParseException;
import com.epam.digital.data.platform.management.mapper.TableShortInfoMapper;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DataModelServiceTest {
  static final String DATA_MODEL_SNAPSHOT_TABLES_TABLE_SAMPLE_JSON = "repositories/data-model-snapshot/tables/table_sample.json";
  static final String DATA_MODEL_SNAPSHOT_DIRECTORY = "repositories/data-model-snapshot/tables";

  @Mock
  ObjectMapper objectMapper;
  @Spy
  private TableShortInfoMapper mapper = Mappers.getMapper(TableShortInfoMapper.class);
  @InjectMocks
  DataModelServiceImpl tableService;

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
    final TableInfoDto table = getTable();
    Mockito.when(objectMapper.readValue(TestUtils.getContent("table_sample.json"), TableInfoDto.class)).thenReturn(table);
    final List<TableShortInfoDto> list = tableService.list();
    Assertions.assertThat(list).isNotEmpty();
    final TableShortInfoDto tableDetailsShort = list.get(0);
    Assertions.assertThat(tableDetailsShort.getName()).isEqualTo(table.getName());
    Assertions.assertThat(tableDetailsShort.getDescription()).isEqualTo(table.getDescription());
    Assertions.assertThat(tableDetailsShort.getHistoryFlag()).isEqualTo(table.getHistoryFlag());
    Assertions.assertThat(tableDetailsShort.getObjectReference()).isEqualTo(table.getObjectReference());
  }

  @Test
  @SneakyThrows
  void getTest() {
    final String tableName = "table_sample";
    final TableInfoDto table = getTable();
    Mockito.when(objectMapper.readValue(TestUtils.getContent("table_sample.json"), TableInfoDto.class)).thenReturn(table);
    final TableInfoDto tableInfoDto = tableService.get(tableName);
    Assertions.assertThat(tableInfoDto).isNotNull();
    Assertions.assertThat(tableInfoDto.getName()).isEqualTo(table.getName());
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
    Mockito.when(objectMapper.readValue(TestUtils.getContent("table_sample.json"), TableInfoDto.class)).thenThrow(
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
        Objects.requireNonNull(DataModelServiceTest.class.getResource(path)).toURI());
  }

  private TableInfoDto getTable() {
    TableInfoDto table = new TableInfoDto();
    table.setName("table_sample");
    table.setHistoryFlag(false);
    table.setDescription("John Doe's table");
    table.setObjectReference(true);
    return table;
  }
}
