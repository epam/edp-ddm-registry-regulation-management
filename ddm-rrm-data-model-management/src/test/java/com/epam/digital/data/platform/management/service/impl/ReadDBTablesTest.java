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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.core.exception.VersionComponentCreationException;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.model.dto.IndexShortInfoDto.Column.Sorting;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnDataType;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Index;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.IndexColumnSortSequence;
import schemacrawler.schema.PrimaryKey;
import schemacrawler.schema.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataModelServiceImpl#get(String, String)")
class ReadDBTablesTest extends ReadDataBaseTablesServiceBaseTest {

  public static final String TABLE_SAMPLE = "table_sample";

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table from db if catalog is created for both version-candidate or master version. Cache is not updated when isSuccessfulBuild flag false")
  @SneakyThrows
  void getTest_fromDbWithoutUpdateCache(String versionId) {
    configureMocks(versionId);

    final var resultTableInfoDto = tableService.getTable(versionId, TABLE_SAMPLE, false);
    assertions(resultTableInfoDto);
    verify(cacheService).getCatalogCache(versionId);
    verify(versionContextComponentManager).getComponent(versionId, Catalog.class);
    verify(cacheService, never()).updateCatalogCache(eq(versionId), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table from db if catalog is created for both version-candidate or master version. Cache is updated when isSuccessfulBuild flag true")
  @SneakyThrows
  void getTest_fromDbWithUpdateCache(String versionId) {
    configureMocks(versionId);

    final var resultTableInfoDto = tableService.getTable(versionId, TABLE_SAMPLE, true);
    assertions(resultTableInfoDto);
    verify(cacheService).getCatalogCache(versionId);
    verify(versionContextComponentManager).getComponent(versionId, Catalog.class);
    verify(cacheService).updateCatalogCache(eq(versionId), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table from cache for both version-candidate or master version. Cache is not updated")
  @SneakyThrows
  void getTest_fromCacheWithoutUpdate(String versionId) {
    Catalog catalog = configureMocks(versionId);
    when(cacheService.getCatalogCache(versionId)).thenReturn(catalog);

    final var resultTableInfoDto = tableService.getTable(versionId, TABLE_SAMPLE, true);
    assertions(resultTableInfoDto);
    verify(cacheService).getCatalogCache(versionId);
    verify(versionContextComponentManager, never()).getComponent(versionId, Catalog.class);
    verify(cacheService, never()).updateCatalogCache(eq(versionId), any());
  }

  private void assertions(TableInfoDto resultTableInfoDto) {
    assertThat(resultTableInfoDto)
        .hasFieldOrPropertyWithValue("name", TABLE_SAMPLE)
        .hasFieldOrPropertyWithValue("description", "John Doe's table")
        .hasFieldOrPropertyWithValue("objectReference", false);
    assertThat(resultTableInfoDto.getColumns())
        .hasSize(3);
    assertThat(resultTableInfoDto.getColumns().get("id"))
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("description", "John Doe's table id")
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", "generate_uuid()")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    assertThat(resultTableInfoDto.getColumns().get("varchar_col"))
        .hasFieldOrPropertyWithValue("name", "varchar_col")
        .hasFieldOrPropertyWithValue("description", "John Doe's table varchar column")
        .hasFieldOrPropertyWithValue("type", "VARCHAR")
        .hasFieldOrPropertyWithValue("defaultValue", "varchar constant")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    assertThat(resultTableInfoDto.getColumns().get("integer_col"))
        .hasFieldOrPropertyWithValue("name", "integer_col")
        .hasFieldOrPropertyWithValue("description", "John Doe's table integer column")
        .hasFieldOrPropertyWithValue("type", "INTEGER")
        .hasFieldOrPropertyWithValue("defaultValue", null)
        .hasFieldOrPropertyWithValue("notNullFlag", false);
    assertThat(resultTableInfoDto.getForeignKeys())
        .hasSize(1)
        .extracting("another_table_fk")
        .hasFieldOrPropertyWithValue("name", "another_table_fk")
        .hasFieldOrPropertyWithValue("targetTable", "another_table");
    assertThat(
            resultTableInfoDto.getForeignKeys().get("another_table_fk").getColumnPairs())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("sourceColumnName", "integer_col")
        .hasFieldOrPropertyWithValue("targetColumnName", "another_table_col");
    assertThat(resultTableInfoDto.getPrimaryKey())
        .hasFieldOrPropertyWithValue("name", "table_sample_pk");
    assertThat(resultTableInfoDto.getPrimaryKey().getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("sorting", Sorting.ASC);
    assertThat(resultTableInfoDto.getUniqueConstraints())
        .hasSize(1)
        .extracting("varchar_col_unique")
        .hasFieldOrPropertyWithValue("name", "varchar_col_unique");
    assertThat(
            resultTableInfoDto.getUniqueConstraints().get("varchar_col_unique").getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "varchar_col")
        .hasFieldOrPropertyWithValue("sorting", Sorting.DESC);
    assertThat(resultTableInfoDto.getIndices())
        .hasSize(1)
        .extracting("integer_col_idx")
        .hasFieldOrPropertyWithValue("name", "integer_col_idx");
    assertThat(
            resultTableInfoDto.getIndices().get("integer_col_idx").getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "integer_col")
        .hasFieldOrPropertyWithValue("sorting", Sorting.NONE);
  }

  private Catalog configureMocks(String versionId) {
    // mock catalog
    var catalog = mock(Catalog.class);
    doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // mock table 'table_sample' data
    var table = mock(Table.class);
    doReturn(TABLE_SAMPLE).when(table).getName();
    doReturn(true).when(table).hasRemarks();
    doReturn("John Doe's table").when(table).getRemarks();

    // mock column data types
    var uuidType = mock(ColumnDataType.class);
    doReturn("UUID").when(uuidType).getName();
    var varcharType = mock(ColumnDataType.class);
    doReturn("VARCHAR").when(varcharType).getName();
    var integerType = mock(ColumnDataType.class);
    doReturn("INTEGER").when(integerType).getName();

    // mock column 'id' for table 'table_sample'
    var idColumn = mock(Column.class);
    doReturn("id").when(idColumn).getName();
    doReturn(true).when(idColumn).hasRemarks();
    doReturn("John Doe's table id").when(idColumn).getRemarks();
    doReturn(uuidType).when(idColumn).getType();
    doReturn(true).when(idColumn).hasDefaultValue();
    doReturn("generate_uuid()").when(idColumn).getDefaultValue();
    doReturn(false).when(idColumn).isNullable();
    doReturn(table).when(idColumn).getParent();

    // mock column 'varchar_col' for table 'table_sample'
    var varcharColumn = mock(Column.class);
    doReturn("varchar_col").when(varcharColumn).getName();
    doReturn(true).when(varcharColumn).hasRemarks();
    doReturn("John Doe's table varchar column").when(varcharColumn).getRemarks();
    doReturn(varcharType).when(varcharColumn).getType();
    doReturn(true).when(varcharColumn).hasDefaultValue();
    doReturn("varchar constant").when(varcharColumn).getDefaultValue();
    doReturn(false).when(varcharColumn).isNullable();
    doReturn(table).when(varcharColumn).getParent();

    // mock column 'integer_col' for table 'table_sample'
    var integerColumn = mock(Column.class);
    doReturn("integer_col").when(integerColumn).getName();
    doReturn(true).when(integerColumn).hasRemarks();
    doReturn("John Doe's table integer column").when(integerColumn).getRemarks();
    doReturn(integerType).when(integerColumn).getType();
    doReturn(false).when(integerColumn).hasDefaultValue();
    doReturn(true).when(integerColumn).isNullable();
    doReturn(table).when(integerColumn).getParent();

    // mock column 'another_table_col' for the another table named 'another_table'
    var anotherTableColumn = mock(Column.class);
    var anotherTable = mock(Table.class);
    doReturn(anotherTable).when(anotherTableColumn).getParent();
    doReturn("another_table").when(anotherTable).getName();
    doReturn("another_table_col").when(anotherTableColumn).getName();

    // mock primary key index for table 'table_sample'
    var primaryKeyIndexColumn = mock(IndexColumn.class);
    doReturn("id").when(primaryKeyIndexColumn).getName();
    doReturn(IndexColumnSortSequence.ascending).when(primaryKeyIndexColumn)
        .getSortSequence();

    var primaryKeyIndex = mock(Index.class);
    doReturn("table_sample_pk").when(primaryKeyIndex).getName();
    doReturn(List.of(primaryKeyIndexColumn)).when(primaryKeyIndex).getColumns();
    doReturn(true).when(primaryKeyIndex).isUnique();
    doReturn(table).when(primaryKeyIndex).getParent();

    // mock primary key for table 'table_sample'
    var primaryKey = mock(PrimaryKey.class);
    doReturn("table_sample_pk").when(primaryKey).getName();

    // mock unique index for table 'table_sample'
    var uniqueIndexColumn = mock(IndexColumn.class);
    doReturn("varchar_col").when(uniqueIndexColumn).getName();
    doReturn(IndexColumnSortSequence.descending).when(uniqueIndexColumn).getSortSequence();

    var uniqueIndex = mock(Index.class);
    doReturn("varchar_col_unique").when(uniqueIndex).getName();
    doReturn(List.of(uniqueIndexColumn)).when(uniqueIndex).getColumns();
    doReturn(true).when(uniqueIndex).isUnique();
    doReturn(table).when(uniqueIndex).getParent();

    // mock simple index for table 'table_sample'
    var indexColumn = mock(IndexColumn.class);
    doReturn("integer_col").when(indexColumn).getName();
    doReturn(IndexColumnSortSequence.unknown).when(indexColumn).getSortSequence();

    var index = mock(Index.class);
    doReturn("integer_col_idx").when(index).getName();
    doReturn(List.of(indexColumn)).when(index).getColumns();
    doReturn(false).when(index).isUnique();
    doReturn(table).when(index).getParent();

    // mock foreign key from table 'table_sample' to table 'another_table'
    var foreignKeyColumn = mock(ForeignKeyColumnReference.class);
    doReturn(integerColumn).when(foreignKeyColumn).getForeignKeyColumn();
    doReturn(anotherTableColumn).when(foreignKeyColumn).getPrimaryKeyColumn();

    var foreignKey = mock(ForeignKey.class);
    doReturn("another_table_fk").when(foreignKey).getName();
    doReturn(List.of(foreignKeyColumn)).when(foreignKey).getColumnReferences();

    doReturn(List.of(idColumn, varcharColumn, integerColumn)).when(table).getColumns();
    doReturn(List.of(primaryKeyIndex, uniqueIndex, index)).when(table).getIndexes();
    doReturn(primaryKey).when(table).getPrimaryKey();
    doReturn(List.of(foreignKey)).when(table).getImportedForeignKeys();

    doReturn(List.of(table)).when(catalog).getTables();
    return catalog;
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table with objectReference=true if table has foreign key to table subject for both version-candidate or master version")
  @SneakyThrows
  void getTest_objectReference(String versionId) {
    // mock catalog
    var catalog = mock(Catalog.class);
    doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // mock table 'table_with_object_reference' data
    var table = mock(Table.class);
    var tableName = "table_with_object_reference";
    doReturn(tableName).when(table).getName();
    doReturn(true).when(table).hasRemarks();
    doReturn("Table with object reference").when(table).getRemarks();

    // mock table 'subject' data
    var tableSubject = mock(Table.class);
    doReturn(SUBJECT_TABLE).when(tableSubject).getName();

    // mock column data type
    var uuidType = mock(ColumnDataType.class);
    doReturn("UUID").when(uuidType).getName();

    // mock column 'id' for table 'table_with_object_reference'
    var idColumn = mock(Column.class);
    doReturn("id").when(idColumn).getName();
    doReturn(true).when(idColumn).hasRemarks();
    doReturn("Table with object reference id").when(idColumn).getRemarks();
    doReturn(uuidType).when(idColumn).getType();
    doReturn(true).when(idColumn).hasDefaultValue();
    doReturn("generate_uuid()").when(idColumn).getDefaultValue();
    doReturn(false).when(idColumn).isNullable();
    doReturn(table).when(idColumn).getParent();

    // mock column 'subjectId' for table 'table_with_object_reference'
    var subjectId = mock(Column.class);
    doReturn("subject_id").when(subjectId).getName();
    doReturn(false).when(subjectId).hasRemarks();
    doReturn(uuidType).when(subjectId).getType();
    doReturn(false).when(subjectId).hasDefaultValue();
    doReturn(false).when(subjectId).isNullable();
    doReturn(table).when(subjectId).getParent();

    // mock column 'subjectId' for table 'subject'
    var subjectSubjectId = mock(Column.class);
    doReturn(tableSubject).when(subjectSubjectId).getParent();
    doReturn("subject_id").when(subjectSubjectId).getName();

    // mock primary key index for table 'table_with_object_reference'
    var primaryKeyIndexColumn = mock(IndexColumn.class);
    doReturn("id").when(primaryKeyIndexColumn).getName();
    doReturn(IndexColumnSortSequence.ascending).when(primaryKeyIndexColumn)
        .getSortSequence();

    var primaryKeyIndex = mock(Index.class);
    doReturn("table_with_object_reference_pk").when(primaryKeyIndex).getName();
    doReturn(List.of(primaryKeyIndexColumn)).when(primaryKeyIndex).getColumns();
    doReturn(true).when(primaryKeyIndex).isUnique();
    doReturn(table).when(primaryKeyIndex).getParent();

    // mock primary key for table 'table_with_object_reference'
    var primaryKey = mock(PrimaryKey.class);
    doReturn("table_with_object_reference_pk").when(primaryKey).getName();

    // mock foreign key from table 'table_with_object_reference' to table 'subject'
    var foreignKeyColumn = mock(ForeignKeyColumnReference.class);
    doReturn(subjectId).when(foreignKeyColumn).getForeignKeyColumn();
    doReturn(subjectSubjectId).when(foreignKeyColumn).getPrimaryKeyColumn();

    var foreignKey = mock(ForeignKey.class);
    doReturn("subject__subject_id__fk").when(foreignKey).getName();
    doReturn(List.of(foreignKeyColumn)).when(foreignKey).getColumnReferences();

    doReturn(List.of(idColumn, subjectId)).when(table).getColumns();
    doReturn(List.of(primaryKeyIndex)).when(table).getIndexes();
    doReturn(primaryKey).when(table).getPrimaryKey();
    doReturn(List.of(foreignKey)).when(table).getImportedForeignKeys();

    doReturn(List.of(table)).when(catalog).getTables();

    final var resultTableInfoDto = tableService.getTable(versionId, tableName, false);
    assertThat(resultTableInfoDto)
        .hasFieldOrPropertyWithValue("name", "table_with_object_reference")
        .hasFieldOrPropertyWithValue("description", "Table with object reference")
        .hasFieldOrPropertyWithValue("objectReference", true);
    assertThat(resultTableInfoDto.getColumns())
        .hasSize(2);
    assertThat(resultTableInfoDto.getColumns().get("id"))
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("description", "Table with object reference id")
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", "generate_uuid()")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    assertThat(resultTableInfoDto.getColumns().get("subject_id"))
        .hasFieldOrPropertyWithValue("name", "subject_id")
        .hasFieldOrPropertyWithValue("description", null)
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", null)
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    assertThat(resultTableInfoDto.getForeignKeys())
        .hasSize(1)
        .extracting("subject__subject_id__fk")
        .hasFieldOrPropertyWithValue("name", "subject__subject_id__fk")
        .hasFieldOrPropertyWithValue("targetTable", SUBJECT_TABLE);
    assertThat(
            resultTableInfoDto.getForeignKeys().get("subject__subject_id__fk").getColumnPairs())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("sourceColumnName", "subject_id")
        .hasFieldOrPropertyWithValue("targetColumnName", "subject_id");
    assertThat(resultTableInfoDto.getPrimaryKey())
        .hasFieldOrPropertyWithValue("name", "table_with_object_reference_pk");
    assertThat(resultTableInfoDto.getPrimaryKey().getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("sorting", Sorting.ASC);
    assertThat(resultTableInfoDto.getUniqueConstraints()).isEmpty();
    assertThat(resultTableInfoDto.getIndices()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should throw TableNotFoundException if there no tables found with given name for both version-candidate and master version")
  @SneakyThrows
  void getTableNotFoundTest(String versionId) {
    var catalog = mock(Catalog.class);
    doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    doReturn(List.of()).when(catalog).getTables();

    final var tableName = "table_sample1";

    assertThatThrownBy(() -> tableService.getTable(versionId, tableName, false))
        .isInstanceOf(TableNotFoundException.class)
        .hasMessage("Table with name 'table_sample1' doesn't exist in version '%s'.", versionId);
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should throw IllegalStateException if there are more than 1 table found with given name for both version-candidate and master version")
  @SneakyThrows
  void getTable_moreThanOneTableFound(String versionId) {
    var catalog = mock(Catalog.class);
    doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    final var tableName = "table_sample1";

    var table1 = mock(Table.class);
    var table2 = mock(Table.class);
    doReturn(tableName).when(table1).getName();
    doReturn(tableName).when(table2).getName();

    doReturn(List.of(table1, table2)).when(catalog).getTables();

    assertThatThrownBy(() -> tableService.getTable(versionId, tableName, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("There cannot be several tables with same name");
  }

  @Test
  @DisplayName("should throw TableNotFoundException if catalog couldn't be created for version-candidate but it's possible to create a connection to master version database")
  @SneakyThrows
  void listTest_couldNotConnectToVersionCandidateDataBaseButCanToMasterVersionDataBase() {
    doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(VERSION_ID, Catalog.class);

    var registryDataSource = mock(RegistryDataSource.class);
    var connection = mock(Connection.class);
    doReturn(registryDataSource)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, RegistryDataSource.class);
    doReturn(connection).when(registryDataSource).getConnection();

    assertThatThrownBy(() -> tableService.getTable(VERSION_ID, "some_table", false))
        .isInstanceOf(TableNotFoundException.class)
        .hasMessage("Table with name 'some_table' doesn't exist in version '162'.");
    verify(connection).close();
  }

  @Test
  @DisplayName("should throw RegistryDataBaseConnectionException if catalog couldn't be created for version-candidate and it's not possible to create connection to master version database")
  @SneakyThrows
  void listTest_couldNotConnectToVersionCandidateDataBaseAndToMasterVersionDataBase() {
    doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(VERSION_ID, Catalog.class);

    var registryDataSource = mock(RegistryDataSource.class);
    doReturn(registryDataSource)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, RegistryDataSource.class);
    doThrow(SQLException.class).when(registryDataSource).getConnection();

    assertThatThrownBy(() -> tableService.getTable(VERSION_ID, "some_table", false))
        .isInstanceOf(RegistryDataBaseConnectionException.class)
        .hasMessageContaining("Couldn't connect to registry data-base: ");
  }
}
