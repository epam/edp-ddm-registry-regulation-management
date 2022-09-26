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
package data.model.snapshot.repository;

import static data.model.snapshot.DdmDataBaseSnapshoUtil.getTableColumns;

import data.model.snapshot.model.DdmColumn;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.repository.impl.InMemDdmColumnRepository;
import data.model.snapshot.repository.impl.InMemDdmTableRepository;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InMemDdmColumnRepositoryTest {

  @Mock
  Map<String, DdmColumn> columnMap;

  @Mock
  DdmTable table;

  @Mock
  InMemDdmTableRepository tableRepository;

  @InjectMocks
  InMemDdmColumnRepository columnRepository;

  @BeforeEach
  void setup(){
    final DdmColumn column = getTableColumns().get("column");
    Mockito.when(tableRepository.get(column.getTableName())).thenReturn(table);
    Mockito.when(table.getColumns()).thenReturn(columnMap);
  }

  @Test
  void saveTest() {
    final DdmColumn column = getTableColumns().get("column");
    columnRepository.save(column);
    Mockito.verify(columnMap).put(column.getName(), column);
  }

  @Test
  void getTest() {
    final DdmColumn column = getTableColumns().get("column");
    Mockito.when(columnMap.get(column.getName())).thenReturn(column);
    final DdmColumn getColumn = columnRepository.get(column.getName(), column.getTableName());
    Mockito.verify(columnMap).get(column.getName());
    Assertions.assertThat(getColumn).isEqualTo(column);
  }

  @Test
  void deleteTest() {
    final DdmColumn column = getTableColumns().get("column");
    Mockito.when(columnMap.remove(column.getName())).thenReturn(column);
    columnRepository.delete(column.getName(), column.getTableName());
    Mockito.verify(columnMap).remove(column.getName());
  }

}
