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

import static data.model.snapshot.processor.DdmForeignKeyReader.SUBJECT_TABLE;
import static org.mockito.ArgumentMatchers.any;

import data.model.snapshot.repository.DdmForeignKeyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Table;

@ExtendWith(SpringExtension.class)
public class DdmForeignKeyReaderTest {

  @Mock
  DdmForeignKeyRepository foreignKeyRepository;

  @Mock
  List<ForeignKeyColumnReference> foreignKeyColumnReferences;
  @Mock
  ForeignKey foreignKey;
  @Mock
  ForeignKeyColumnReference foreignKeyColumnReference;
  @Mock
  Column column;
  @Mock
  Table table;

  @InjectMocks
  DdmForeignKeyReader foreignKeyReader;

  @BeforeEach
  void setup() {
    Mockito.when(foreignKey.getColumnReferences()).thenReturn(foreignKeyColumnReferences);
    Mockito.when(foreignKeyColumnReferences.get(0)).thenReturn(foreignKeyColumnReference);
    Mockito.when(foreignKeyColumnReference.getPrimaryKeyColumn()).thenReturn(column);
    Mockito.when(foreignKeyColumnReference.getForeignKeyColumn()).thenReturn(column);
    Mockito.when(column.getParent()).thenReturn(table);
  }

  @Test
  void readNamedObjectTest() {
    Mockito.when(table.getName()).thenReturn(SUBJECT_TABLE);
    foreignKeyReader.readNamedObject(foreignKey);

    Mockito.verify(foreignKeyRepository, Mockito.never()).save(any());
  }

  @Test
  void readNamedObjectSaveCalledTest() {
    foreignKeyReader.readNamedObject(foreignKey);

    Mockito.verify(foreignKeyRepository).save(any());
  }

}
