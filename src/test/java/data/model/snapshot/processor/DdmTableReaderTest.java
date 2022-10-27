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
package data.model.snapshot.processor;

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.liquibase.extension.DdmConstants;
import data.model.snapshot.repository.DdmTableRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.Index;
import schemacrawler.schema.Table;

@ExtendWith(MockitoExtension.class)
public class DdmTableReaderTest {

  @Mock
  DdmTableRepository ddmTableRepository;
  @Mock
  DdmViewReader ddmViewReader;
  @Mock
  DdmColumnReader ddmColumnReader;
  @Mock
  DdmForeignKeyReader ddmForeignKeyReader;
  @Mock
  DdmIndexReader ddmIndexReader;

  @Mock
  Table table;
  @Mock
  Column column;
  @Mock
  ForeignKey foreignKey;
  @Mock
  Index index;


  @InjectMocks
  DdmTableReader tableReader;

  @Test
  void readNamedObjectTest() {
    final String tableName = DdmConstants.SUFFIX_VIEW;
    Mockito.when(table.getName()).thenReturn(tableName);

    tableReader.readNamedObject(table);

    Mockito.verify(ddmViewReader).readNamedObject(table);

    Mockito.verify(ddmTableRepository, Mockito.never()).save(any());
    Mockito.verify(ddmColumnReader, Mockito.never()).readNamedObject(column);
    Mockito.verify(ddmForeignKeyReader, Mockito.never()).readNamedObject(foreignKey);
    Mockito.verify(ddmIndexReader, Mockito.never()).readNamedObject(index);
    Mockito.verify(ddmTableRepository, Mockito.never()).get(tableName);
  }

  @Test
  void readNamedObjectSuccessTest() {
    final String tableName = "table";
    List<Column> columnList = List.of(column);
    List<ForeignKey> foreignKeyList = List.of(foreignKey);
    List<Index> indexList = List.of(index);

    Mockito.when(table.getName()).thenReturn(tableName);
    Mockito.when(table.getColumns()).thenReturn(columnList);
    Mockito.when(table.getForeignKeys()).thenReturn(foreignKeyList);
    Mockito.when(table.getIndexes()).thenReturn(indexList);

    tableReader.readNamedObject(table);

    Mockito.verify(ddmViewReader, Mockito.never()).readNamedObject(table);

    Mockito.verify(ddmTableRepository).save(any());
    Mockito.verify(ddmColumnReader).readNamedObject(column);
    Mockito.verify(ddmForeignKeyReader).readNamedObject(foreignKey);
    Mockito.verify(ddmIndexReader).readNamedObject(index);
    Mockito.verify(ddmTableRepository).get(tableName);
  }
}
