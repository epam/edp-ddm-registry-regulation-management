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

import data.model.snapshot.model.DdmForeignKey;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.repository.impl.InMemDdmForeignKeyRepository;
import data.model.snapshot.repository.impl.InMemDdmTableRepository;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class InMemDdmForeignKeyRepositoryTest {

  @Mock
  InMemDdmTableRepository tableRepository;
  @Mock
  DdmTable table;
  @Mock
  Map<String, DdmForeignKey> foreignKeyMap;

  @InjectMocks
  InMemDdmForeignKeyRepository ddmForeignKeyRepository;

  @BeforeEach
  void setup() {
    final DdmForeignKey foreignKey = getForeignKey();
    Mockito.when(tableRepository.get(foreignKey.getSourceTable())).thenReturn(table);
    Mockito.when(table.getForeignKeys()).thenReturn(foreignKeyMap);
  }

  @Test
  void saveTest() {
    final DdmForeignKey foreignKey = getForeignKey();
    ddmForeignKeyRepository.save(foreignKey);
    Mockito.verify(foreignKeyMap).put(foreignKey.getName(), foreignKey);
  }

  @Test
  void getTest() {
    final DdmForeignKey foreignKey = getForeignKey();
    Mockito.when(foreignKeyMap.get(foreignKey.getName())).thenReturn(foreignKey);
    final DdmForeignKey ddmForeignKey = ddmForeignKeyRepository.get(foreignKey.getName(),
        foreignKey.getSourceTable());
    Mockito.verify(foreignKeyMap).get(foreignKey.getName());
    Assertions.assertThat(ddmForeignKey).isEqualTo(foreignKey);
  }

  @Test
  void deleteTest() {
    final DdmForeignKey foreignKey = getForeignKey();
    ddmForeignKeyRepository.delete(foreignKey.getName(), foreignKey.getSourceTable());
    Mockito.verify(foreignKeyMap).remove(foreignKey.getName());
  }

  private DdmForeignKey getForeignKey() {
    DdmForeignKey fk = new DdmForeignKey();
    fk.setName("foreignKey");
    fk.setTargetTable("targetTable");
    fk.setSourceTable("sourceTable");
    return fk;
  }
}
