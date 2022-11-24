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

import static data.model.snapshot.processor.DdmColumnReader.SUBJECT_TABLE_PRIMARY_KEY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;

import data.model.snapshot.model.DdmColumn;
import data.model.snapshot.repository.DdmColumnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnDataType;
import schemacrawler.schema.Table;

@ExtendWith(SpringExtension.class)
public class DdmColumnReaderTest {

  @Mock
  DdmColumnRepository columnRepository;

  @InjectMocks
  DdmColumnReader columnReader;

  @Test
  void readNamedObjectTest() {
    Column column = Mockito.mock(Column.class);
    Table table = Mockito.mock(Table.class);
    Mockito.when(column.getParent()).thenReturn(table);
    Mockito.when(column.isPartOfForeignKey()).thenReturn(true);
    Mockito.when(column.getReferencedColumn()).thenReturn(column);
    Mockito.when(column.getFullName()).thenReturn(SUBJECT_TABLE_PRIMARY_KEY);

    columnReader.readNamedObject(column);

    Mockito.verify(columnRepository, Mockito.never()).save(refEq(new DdmColumn()));
  }

  @Test
  void readNamedObjectSuccessTest() {
    Column column = Mockito.mock(Column.class);
    Table table = Mockito.mock(Table.class);
    final ColumnDataType dataType = Mockito.mock(ColumnDataType.class);
    Mockito.when(column.getParent()).thenReturn(table);
    Mockito.when(column.isPartOfForeignKey()).thenReturn(false);
    Mockito.when(column.getType()).thenReturn(dataType);
    var ddmColumn = new DdmColumn();
    ddmColumn.setNotNullFlag(true);
    columnReader.readNamedObject(column);
    Mockito.verify(columnRepository).save(eq(ddmColumn));
  }

}
