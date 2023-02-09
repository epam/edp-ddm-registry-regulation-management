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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
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

@DisplayName("DataModelServiceImpl#get(String, String)")
class DataModelTableServiceGetTableTest extends DataModelTableServiceBaseTest {

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table if catalog is created for both version-candidate or master version")
  @SneakyThrows
  void getTest(String versionId) {
    // mock catalog
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // mock table 'table_sample' data
    var table = Mockito.mock(Table.class);
    var tableName = "table_sample";
    Mockito.doReturn(tableName).when(table).getName();
    Mockito.doReturn(true).when(table).hasRemarks();
    Mockito.doReturn("John Doe's table").when(table).getRemarks();

    // mock column data types
    var uuidType = Mockito.mock(ColumnDataType.class);
    Mockito.doReturn("UUID").when(uuidType).getName();
    var varcharType = Mockito.mock(ColumnDataType.class);
    Mockito.doReturn("VARCHAR").when(varcharType).getName();
    var integerType = Mockito.mock(ColumnDataType.class);
    Mockito.doReturn("INTEGER").when(integerType).getName();

    // mock column 'id' for table 'table_sample'
    var idColumn = Mockito.mock(Column.class);
    Mockito.doReturn("id").when(idColumn).getName();
    Mockito.doReturn(true).when(idColumn).hasRemarks();
    Mockito.doReturn("John Doe's table id").when(idColumn).getRemarks();
    Mockito.doReturn(uuidType).when(idColumn).getType();
    Mockito.doReturn(true).when(idColumn).hasDefaultValue();
    Mockito.doReturn("generate_uuid()").when(idColumn).getDefaultValue();
    Mockito.doReturn(false).when(idColumn).isNullable();
    Mockito.doReturn(table).when(idColumn).getParent();

    // mock column 'varchar_col' for table 'table_sample'
    var varcharColumn = Mockito.mock(Column.class);
    Mockito.doReturn("varchar_col").when(varcharColumn).getName();
    Mockito.doReturn(true).when(varcharColumn).hasRemarks();
    Mockito.doReturn("John Doe's table varchar column").when(varcharColumn).getRemarks();
    Mockito.doReturn(varcharType).when(varcharColumn).getType();
    Mockito.doReturn(true).when(varcharColumn).hasDefaultValue();
    Mockito.doReturn("varchar constant").when(varcharColumn).getDefaultValue();
    Mockito.doReturn(false).when(varcharColumn).isNullable();
    Mockito.doReturn(table).when(varcharColumn).getParent();

    // mock column 'integer_col' for table 'table_sample'
    var integerColumn = Mockito.mock(Column.class);
    Mockito.doReturn("integer_col").when(integerColumn).getName();
    Mockito.doReturn(true).when(integerColumn).hasRemarks();
    Mockito.doReturn("John Doe's table integer column").when(integerColumn).getRemarks();
    Mockito.doReturn(integerType).when(integerColumn).getType();
    Mockito.doReturn(false).when(integerColumn).hasDefaultValue();
    Mockito.doReturn(true).when(integerColumn).isNullable();
    Mockito.doReturn(table).when(integerColumn).getParent();

    // mock column 'another_table_col' for the another table named 'another_table'
    var anotherTableColumn = Mockito.mock(Column.class);
    var anotherTable = Mockito.mock(Table.class);
    Mockito.doReturn(anotherTable).when(anotherTableColumn).getParent();
    Mockito.doReturn("another_table").when(anotherTable).getName();
    Mockito.doReturn("another_table_col").when(anotherTableColumn).getName();

    // mock primary key index for table 'table_sample'
    var primaryKeyIndexColumn = Mockito.mock(IndexColumn.class);
    Mockito.doReturn("id").when(primaryKeyIndexColumn).getName();
    Mockito.doReturn(IndexColumnSortSequence.ascending).when(primaryKeyIndexColumn)
        .getSortSequence();

    var primaryKeyIndex = Mockito.mock(Index.class);
    Mockito.doReturn("table_sample_pk").when(primaryKeyIndex).getName();
    Mockito.doReturn(List.of(primaryKeyIndexColumn)).when(primaryKeyIndex).getColumns();
    Mockito.doReturn(true).when(primaryKeyIndex).isUnique();
    Mockito.doReturn(table).when(primaryKeyIndex).getParent();

    // mock primary key for table 'table_sample'
    var primaryKey = Mockito.mock(PrimaryKey.class);
    Mockito.doReturn("table_sample_pk").when(primaryKey).getName();

    // mock unique index for table 'table_sample'
    var uniqueIndexColumn = Mockito.mock(IndexColumn.class);
    Mockito.doReturn("varchar_col").when(uniqueIndexColumn).getName();
    Mockito.doReturn(IndexColumnSortSequence.descending).when(uniqueIndexColumn).getSortSequence();

    var uniqueIndex = Mockito.mock(Index.class);
    Mockito.doReturn("varchar_col_unique").when(uniqueIndex).getName();
    Mockito.doReturn(List.of(uniqueIndexColumn)).when(uniqueIndex).getColumns();
    Mockito.doReturn(true).when(uniqueIndex).isUnique();
    Mockito.doReturn(table).when(uniqueIndex).getParent();

    // mock simple index for table 'table_sample'
    var indexColumn = Mockito.mock(IndexColumn.class);
    Mockito.doReturn("integer_col").when(indexColumn).getName();
    Mockito.doReturn(IndexColumnSortSequence.unknown).when(indexColumn).getSortSequence();

    var index = Mockito.mock(Index.class);
    Mockito.doReturn("integer_col_idx").when(index).getName();
    Mockito.doReturn(List.of(indexColumn)).when(index).getColumns();
    Mockito.doReturn(false).when(index).isUnique();
    Mockito.doReturn(table).when(index).getParent();

    // mock foreign key from table 'table_sample' to table 'another_table'
    var foreignKeyColumn = Mockito.mock(ForeignKeyColumnReference.class);
    Mockito.doReturn(integerColumn).when(foreignKeyColumn).getForeignKeyColumn();
    Mockito.doReturn(anotherTableColumn).when(foreignKeyColumn).getPrimaryKeyColumn();

    var foreignKey = Mockito.mock(ForeignKey.class);
    Mockito.doReturn("another_table_fk").when(foreignKey).getName();
    Mockito.doReturn(List.of(foreignKeyColumn)).when(foreignKey).getColumnReferences();

    Mockito.doReturn(List.of(idColumn, varcharColumn, integerColumn)).when(table).getColumns();
    Mockito.doReturn(List.of(primaryKeyIndex, uniqueIndex, index)).when(table).getIndexes();
    Mockito.doReturn(primaryKey).when(table).getPrimaryKey();
    Mockito.doReturn(List.of(foreignKey)).when(table).getImportedForeignKeys();

    Mockito.doReturn(List.of(table)).when(catalog).getTables();

    final var resultTableInfoDto = tableService.getTable(versionId, tableName);
    Assertions.assertThat(resultTableInfoDto)
        .hasFieldOrPropertyWithValue("name", "table_sample")
        .hasFieldOrPropertyWithValue("description", "John Doe's table")
        .hasFieldOrPropertyWithValue("objectReference", false);
    Assertions.assertThat(resultTableInfoDto.getColumns())
        .hasSize(3);
    Assertions.assertThat(resultTableInfoDto.getColumns().get("id"))
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("description", "John Doe's table id")
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", "generate_uuid()")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    Assertions.assertThat(resultTableInfoDto.getColumns().get("varchar_col"))
        .hasFieldOrPropertyWithValue("name", "varchar_col")
        .hasFieldOrPropertyWithValue("description", "John Doe's table varchar column")
        .hasFieldOrPropertyWithValue("type", "VARCHAR")
        .hasFieldOrPropertyWithValue("defaultValue", "varchar constant")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    Assertions.assertThat(resultTableInfoDto.getColumns().get("integer_col"))
        .hasFieldOrPropertyWithValue("name", "integer_col")
        .hasFieldOrPropertyWithValue("description", "John Doe's table integer column")
        .hasFieldOrPropertyWithValue("type", "INTEGER")
        .hasFieldOrPropertyWithValue("defaultValue", null)
        .hasFieldOrPropertyWithValue("notNullFlag", false);
    Assertions.assertThat(resultTableInfoDto.getForeignKeys())
        .hasSize(1)
        .extracting("another_table_fk")
        .hasFieldOrPropertyWithValue("name", "another_table_fk")
        .hasFieldOrPropertyWithValue("targetTable", "another_table");
    Assertions.assertThat(
            resultTableInfoDto.getForeignKeys().get("another_table_fk").getColumnPairs())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("sourceColumnName", "integer_col")
        .hasFieldOrPropertyWithValue("targetColumnName", "another_table_col");
    Assertions.assertThat(resultTableInfoDto.getPrimaryKey())
        .hasFieldOrPropertyWithValue("name", "table_sample_pk");
    Assertions.assertThat(resultTableInfoDto.getPrimaryKey().getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("sorting", Sorting.ASC);
    Assertions.assertThat(resultTableInfoDto.getUniqueConstraints())
        .hasSize(1)
        .extracting("varchar_col_unique")
        .hasFieldOrPropertyWithValue("name", "varchar_col_unique");
    Assertions.assertThat(
            resultTableInfoDto.getUniqueConstraints().get("varchar_col_unique").getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "varchar_col")
        .hasFieldOrPropertyWithValue("sorting", Sorting.DESC);
    Assertions.assertThat(resultTableInfoDto.getIndices())
        .hasSize(1)
        .extracting("integer_col_idx")
        .hasFieldOrPropertyWithValue("name", "integer_col_idx");
    Assertions.assertThat(
            resultTableInfoDto.getIndices().get("integer_col_idx").getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "integer_col")
        .hasFieldOrPropertyWithValue("sorting", Sorting.NONE);
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return table with objectReference=true if table has foreign key to table subject for both version-candidate or master version")
  @SneakyThrows
  void getTest_objectReference(String versionId) {
    // mock catalog
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // mock table 'table_with_object_reference' data
    var table = Mockito.mock(Table.class);
    var tableName = "table_with_object_reference";
    Mockito.doReturn(tableName).when(table).getName();
    Mockito.doReturn(true).when(table).hasRemarks();
    Mockito.doReturn("Table with object reference").when(table).getRemarks();

    // mock table 'subject' data
    var tableSubject = Mockito.mock(Table.class);
    Mockito.doReturn(SUBJECT_TABLE).when(tableSubject).getName();

    // mock column data type
    var uuidType = Mockito.mock(ColumnDataType.class);
    Mockito.doReturn("UUID").when(uuidType).getName();

    // mock column 'id' for table 'table_with_object_reference'
    var idColumn = Mockito.mock(Column.class);
    Mockito.doReturn("id").when(idColumn).getName();
    Mockito.doReturn(true).when(idColumn).hasRemarks();
    Mockito.doReturn("Table with object reference id").when(idColumn).getRemarks();
    Mockito.doReturn(uuidType).when(idColumn).getType();
    Mockito.doReturn(true).when(idColumn).hasDefaultValue();
    Mockito.doReturn("generate_uuid()").when(idColumn).getDefaultValue();
    Mockito.doReturn(false).when(idColumn).isNullable();
    Mockito.doReturn(table).when(idColumn).getParent();

    // mock column 'subjectId' for table 'table_with_object_reference'
    var subjectId = Mockito.mock(Column.class);
    Mockito.doReturn("subject_id").when(subjectId).getName();
    Mockito.doReturn(false).when(subjectId).hasRemarks();
    Mockito.doReturn(uuidType).when(subjectId).getType();
    Mockito.doReturn(false).when(subjectId).hasDefaultValue();
    Mockito.doReturn(false).when(subjectId).isNullable();
    Mockito.doReturn(table).when(subjectId).getParent();

    // mock column 'subjectId' for table 'subject'
    var subjectSubjectId = Mockito.mock(Column.class);
    Mockito.doReturn(tableSubject).when(subjectSubjectId).getParent();
    Mockito.doReturn("subject_id").when(subjectSubjectId).getName();

    // mock primary key index for table 'table_with_object_reference'
    var primaryKeyIndexColumn = Mockito.mock(IndexColumn.class);
    Mockito.doReturn("id").when(primaryKeyIndexColumn).getName();
    Mockito.doReturn(IndexColumnSortSequence.ascending).when(primaryKeyIndexColumn)
        .getSortSequence();

    var primaryKeyIndex = Mockito.mock(Index.class);
    Mockito.doReturn("table_with_object_reference_pk").when(primaryKeyIndex).getName();
    Mockito.doReturn(List.of(primaryKeyIndexColumn)).when(primaryKeyIndex).getColumns();
    Mockito.doReturn(true).when(primaryKeyIndex).isUnique();
    Mockito.doReturn(table).when(primaryKeyIndex).getParent();

    // mock primary key for table 'table_with_object_reference'
    var primaryKey = Mockito.mock(PrimaryKey.class);
    Mockito.doReturn("table_with_object_reference_pk").when(primaryKey).getName();

    // mock foreign key from table 'table_with_object_reference' to table 'subject'
    var foreignKeyColumn = Mockito.mock(ForeignKeyColumnReference.class);
    Mockito.doReturn(subjectId).when(foreignKeyColumn).getForeignKeyColumn();
    Mockito.doReturn(subjectSubjectId).when(foreignKeyColumn).getPrimaryKeyColumn();

    var foreignKey = Mockito.mock(ForeignKey.class);
    Mockito.doReturn("subject__subject_id__fk").when(foreignKey).getName();
    Mockito.doReturn(List.of(foreignKeyColumn)).when(foreignKey).getColumnReferences();

    Mockito.doReturn(List.of(idColumn, subjectId)).when(table).getColumns();
    Mockito.doReturn(List.of(primaryKeyIndex)).when(table).getIndexes();
    Mockito.doReturn(primaryKey).when(table).getPrimaryKey();
    Mockito.doReturn(List.of(foreignKey)).when(table).getImportedForeignKeys();

    Mockito.doReturn(List.of(table)).when(catalog).getTables();

    final var resultTableInfoDto = tableService.getTable(versionId, tableName);
    Assertions.assertThat(resultTableInfoDto)
        .hasFieldOrPropertyWithValue("name", "table_with_object_reference")
        .hasFieldOrPropertyWithValue("description", "Table with object reference")
        .hasFieldOrPropertyWithValue("objectReference", true);
    Assertions.assertThat(resultTableInfoDto.getColumns())
        .hasSize(2);
    Assertions.assertThat(resultTableInfoDto.getColumns().get("id"))
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("description", "Table with object reference id")
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", "generate_uuid()")
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    Assertions.assertThat(resultTableInfoDto.getColumns().get("subject_id"))
        .hasFieldOrPropertyWithValue("name", "subject_id")
        .hasFieldOrPropertyWithValue("description", null)
        .hasFieldOrPropertyWithValue("type", "UUID")
        .hasFieldOrPropertyWithValue("defaultValue", null)
        .hasFieldOrPropertyWithValue("notNullFlag", true);
    Assertions.assertThat(resultTableInfoDto.getForeignKeys())
        .hasSize(1)
        .extracting("subject__subject_id__fk")
        .hasFieldOrPropertyWithValue("name", "subject__subject_id__fk")
        .hasFieldOrPropertyWithValue("targetTable", SUBJECT_TABLE);
    Assertions.assertThat(
            resultTableInfoDto.getForeignKeys().get("subject__subject_id__fk").getColumnPairs())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("sourceColumnName", "subject_id")
        .hasFieldOrPropertyWithValue("targetColumnName", "subject_id");
    Assertions.assertThat(resultTableInfoDto.getPrimaryKey())
        .hasFieldOrPropertyWithValue("name", "table_with_object_reference_pk");
    Assertions.assertThat(resultTableInfoDto.getPrimaryKey().getColumns())
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("name", "id")
        .hasFieldOrPropertyWithValue("sorting", Sorting.ASC);
    Assertions.assertThat(resultTableInfoDto.getUniqueConstraints()).isEmpty();
    Assertions.assertThat(resultTableInfoDto.getIndices()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should throw TableNotFoundException if there no tables found with given name for both version-candidate and master version")
  @SneakyThrows
  void getTableNotFoundTest(String versionId) {
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    Mockito.doReturn(List.of()).when(catalog).getTables();

    final var tableName = "table_sample1";

    Assertions.assertThatThrownBy(() -> tableService.getTable(versionId, tableName))
        .isInstanceOf(TableNotFoundException.class)
        .hasMessage("Table with name 'table_sample1' doesn't exist in version '%s'.", versionId);
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should throw IllegalStateException if there are more than 1 table found with given name for both version-candidate and master version")
  @SneakyThrows
  void getTable_moreThanOneTableFound(String versionId) {
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    final var tableName = "table_sample1";

    var table1 = Mockito.mock(Table.class);
    var table2 = Mockito.mock(Table.class);
    Mockito.doReturn(tableName).when(table1).getName();
    Mockito.doReturn(tableName).when(table2).getName();

    Mockito.doReturn(List.of(table1, table2)).when(catalog).getTables();

    Assertions.assertThatThrownBy(() -> tableService.getTable(versionId, tableName))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("There cannot be several tables with same name");
  }

  @Test
  @DisplayName("should throw TableNotFoundException if catalog couldn't be created for version-candidate but it's possible to create a connection to master version database")
  @SneakyThrows
  void listTest_couldNotConnectToVersionCandidateDataBaseButCanToMasterVersionDataBase() {
    Mockito.doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(VERSION_ID, Catalog.class);

    var registryDataSource = Mockito.mock(RegistryDataSource.class);
    var connection = Mockito.mock(Connection.class);
    Mockito.doReturn(registryDataSource)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.doReturn(connection).when(registryDataSource).getConnection();

    Assertions.assertThatThrownBy(() -> tableService.getTable(VERSION_ID, "some_table"))
        .isInstanceOf(TableNotFoundException.class)
        .hasMessage("Table with name 'some_table' doesn't exist in version '162'.");
    Mockito.verify(connection).close();
  }

  @Test
  @DisplayName("should throw RegistryDataBaseConnectionException if catalog couldn't be created for version-candidate and it's not possible to create connection to master version database")
  @SneakyThrows
  void listTest_couldNotConnectToVersionCandidateDataBaseAndToMasterVersionDataBase() {
    Mockito.doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(VERSION_ID, Catalog.class);

    var registryDataSource = Mockito.mock(RegistryDataSource.class);
    Mockito.doReturn(registryDataSource)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.doThrow(SQLException.class).when(registryDataSource).getConnection();

    Assertions.assertThatThrownBy(() -> tableService.getTable(VERSION_ID, "some_table"))
        .isInstanceOf(RegistryDataBaseConnectionException.class)
        .hasMessageContaining("Couldn't connect to registry data-base: ");
  }
}
