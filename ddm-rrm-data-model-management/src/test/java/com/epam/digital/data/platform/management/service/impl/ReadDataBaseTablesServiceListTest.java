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
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Table;

@DisplayName("DataModelServiceImpl#list(String)")
class ReadDataBaseTablesServiceListTest extends ReadDataBaseTablesServiceBaseTest {

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should return list of tables if catalog is created for both version-candidate or master version")
  @SneakyThrows
  void listTest(String versionId) {
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // Mock subject table
    var subjectTable = Mockito.mock(Table.class);
    var subjectTableColumn = Mockito.mock(Column.class);
    Mockito.doReturn(SUBJECT_TABLE).when(subjectTable).getName();
    Mockito.doReturn(false).when(subjectTable).hasRemarks();
    Mockito.doReturn(List.of(subjectTableColumn)).when(subjectTable).getColumns();
    Mockito.doReturn(subjectTable).when(subjectTableColumn).getParent();

    // Mock table that doesn't have object reference (foreign key to table 'subject')
    var tableWithoutObjectReference = Mockito.mock(Table.class);
    Mockito.doReturn("table_without_object_reference")
        .when(tableWithoutObjectReference).getName();
    Mockito.doReturn(true).when(tableWithoutObjectReference).hasRemarks();
    Mockito.doReturn("Table that doesn't have foreign key to subject table")
        .when(tableWithoutObjectReference).getRemarks();

    // Mock table that has object reference (foreign key to table 'subject')
    var tableWithObjectReference = Mockito.mock(Table.class);
    var subjectForeignKey = Mockito.mock(ForeignKey.class);
    var subjectForeignKeyColumnReference = Mockito.mock(ForeignKeyColumnReference.class);

    Mockito.doReturn("table_with_object_reference")
        .when(tableWithObjectReference).getName();
    Mockito.doReturn(true).when(tableWithObjectReference).hasRemarks();
    Mockito.doReturn("Table that has foreign key to subject table")
        .when(tableWithObjectReference).getRemarks();
    Mockito.doReturn(List.of(subjectForeignKey))
        .when(tableWithObjectReference).getImportedForeignKeys();
    Mockito.doReturn(List.of(subjectForeignKeyColumnReference))
        .when(subjectForeignKey).getColumnReferences();
    Mockito.doReturn(subjectTableColumn)
        .when(subjectForeignKeyColumnReference).getPrimaryKeyColumn();

    Mockito.doReturn(List.of(tableWithoutObjectReference, tableWithObjectReference, subjectTable))
        .when(catalog).getTables();

    final var resultList = tableService.listTables(versionId);
    Assertions.assertThat(resultList).hasSize(3);
    Assertions.assertThat(resultList)
        .element(0)
        .hasFieldOrPropertyWithValue("name", SUBJECT_TABLE)
        .hasFieldOrPropertyWithValue("description", null)
        .hasFieldOrPropertyWithValue("objectReference", false);
    Assertions.assertThat(resultList)
        .element(1)
        .hasFieldOrPropertyWithValue("name", "table_with_object_reference")
        .hasFieldOrPropertyWithValue("description", "Table that has foreign key to subject table")
        .hasFieldOrPropertyWithValue("objectReference", true);
    Assertions.assertThat(resultList)
        .element(2)
        .hasFieldOrPropertyWithValue("name", "table_without_object_reference")
        .hasFieldOrPropertyWithValue("description",
            "Table that doesn't have foreign key to subject table")
        .hasFieldOrPropertyWithValue("objectReference", false);
  }

  @Test
  @DisplayName("should throw RegistryDataBaseConnectionException if catalog couldn't be created for master version")
  void listTest_couldNotConnectToMasterDataBase() {
    Mockito.doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, Catalog.class);

    Assertions.assertThatThrownBy(() -> tableService.listTables(HEAD_BRANCH))
        .isInstanceOf(RegistryDataBaseConnectionException.class)
        .hasMessageContaining("Couldn't connect to registry data-base: ");
  }

  @ParameterizedTest
  @ValueSource(strings = {VERSION_ID, HEAD_BRANCH})
  @DisplayName("should not contain table with '_v' suffix")
  void listTest_shouldNotContainTableWithViewSuffix(String versionId) {
    var catalog = Mockito.mock(Catalog.class);
    Mockito.doReturn(catalog)
        .when(versionContextComponentManager).getComponent(versionId, Catalog.class);

    // Mock view
    var view = Mockito.mock(Table.class);
    Mockito.doReturn("view_v").when(view).getName();

    Assertions.assertThat(tableService.listTables(versionId)).isEmpty();
  }

  @Test
  @DisplayName("should return empty list if catalog couldn't be created for version-candidate but it's possible to create a connection to master version database")
  @SneakyThrows
  void listTest_couldNotConnectToVersionCandidateDataBaseButCanToMasterVersionDataBase() {
    Mockito.doThrow(VersionComponentCreationException.class)
        .when(versionContextComponentManager).getComponent(VERSION_ID, Catalog.class);

    var registryDataSource = Mockito.mock(RegistryDataSource.class);
    var connection = Mockito.mock(Connection.class);
    Mockito.doReturn(registryDataSource)
        .when(versionContextComponentManager).getComponent(HEAD_BRANCH, RegistryDataSource.class);
    Mockito.doReturn(connection).when(registryDataSource).getConnection();

    final var resultList = tableService.listTables(VERSION_ID);
    Assertions.assertThat(resultList).isEmpty();
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

    Assertions.assertThatThrownBy(() -> tableService.listTables(VERSION_ID))
        .isInstanceOf(RegistryDataBaseConnectionException.class)
        .hasMessageContaining("Couldn't connect to registry data-base: ");
  }
}
