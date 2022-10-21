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

import data.model.snapshot.model.DdmIndex;
import data.model.snapshot.model.DdmPrimaryKeyConstraint;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.model.DdmUniqueConstraint;
import data.model.snapshot.repository.impl.InMemDdmIndexRepository;
import data.model.snapshot.repository.impl.InMemDdmTableRepository;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InMemDdmIndexRepositoryTest {

  @Mock
  InMemDdmTableRepository tableRepository;
  @Mock
  DdmTable table;
  @Mock
  Map<String, DdmIndex> indexMap;
  @Mock
  Map<String, DdmUniqueConstraint> uniqueConstraints;

  @InjectMocks
  InMemDdmIndexRepository indexRepository;

  @Test
  void saveIndexTest() {
    final DdmIndex index = getIndex();
    Mockito.when(tableRepository.get(index.getTableName())).thenReturn(table);
    Mockito.when(table.getIndices()).thenReturn(indexMap);
    indexRepository.save(index);

    Mockito.verify(indexMap).put(index.getName(), index);
  }

  @Test
  void savePkTest() {
    final DdmPrimaryKeyConstraint pk = getPk();
    Mockito.when(tableRepository.get(pk.getTableName())).thenReturn(table);
    indexRepository.save(pk);

    Mockito.verify(table).setPrimaryKey(pk);
  }

  @Test
  void saveUniqueConstraintTest() {
    final DdmUniqueConstraint uk = getUk();
    Mockito.when(tableRepository.get(uk.getTableName())).thenReturn(table);
    Mockito.when(table.getUniqueConstraints()).thenReturn(uniqueConstraints);
    indexRepository.save(uk);

    Mockito.verify(uniqueConstraints).put(uk.getName(), uk);
  }

  @Test
  void getPkTest() {
    final DdmPrimaryKeyConstraint pk = getPk();
    Mockito.when(tableRepository.get(pk.getTableName())).thenReturn(table);
    Mockito.when(table.getPrimaryKey()).thenReturn(pk);
    final DdmIndex getPk = indexRepository.get(pk.getName(), pk.getTableName());
    Assertions.assertThat(getPk).isEqualTo(pk);
  }

  @Test
  void getUniqueTest() {
    final DdmUniqueConstraint uk = getUk();
    Mockito.when(tableRepository.get(uk.getTableName())).thenReturn(table);
    Mockito.when(table.getUniqueConstraints()).thenReturn(uniqueConstraints);
    Mockito.when(uniqueConstraints.containsKey(uk.getName())).thenReturn(true);
    Mockito.when(uniqueConstraints.get(uk.getName())).thenReturn(uk);
    final DdmIndex getUk = indexRepository.get(uk.getName(), uk.getTableName());

    Assertions.assertThat(getUk).isEqualTo(uk);
  }

  @Test
  void getIndexTest() {
    final DdmIndex index = getIndex();
    Mockito.when(tableRepository.get(index.getTableName())).thenReturn(table);

    Mockito.when(table.getIndices()).thenReturn(indexMap);
    Mockito.when(indexMap.get(index.getName())).thenReturn(index);
    final DdmIndex getIndex = indexRepository.get(index.getName(), index.getTableName());
    Assertions.assertThat(getIndex).isEqualTo(index);
  }

  @Test
  void deleteTest() {
    final DdmPrimaryKeyConstraint pk = getPk();
    Mockito.when(tableRepository.get(pk.getTableName())).thenReturn(table);
    Mockito.when(table.getPrimaryKey()).thenReturn(pk);
    Mockito.when(table.getIndices()).thenReturn(indexMap);
    Mockito.when(table.getUniqueConstraints()).thenReturn(uniqueConstraints);

    indexRepository.delete(pk.getName(), pk.getTableName());

    Mockito.verify(table).setPrimaryKey(null);
    Mockito.verify(uniqueConstraints).remove(pk.getName());
    Mockito.verify(indexMap).remove(pk.getName());
  }

  private DdmPrimaryKeyConstraint getPk() {
    DdmPrimaryKeyConstraint pk = new DdmPrimaryKeyConstraint();
    pk.setName("primaryKey");
    pk.setTableName("table");
    return pk;
  }

  private DdmUniqueConstraint getUk() {
    DdmUniqueConstraint uk = new DdmUniqueConstraint();
    uk.setName("uniqueConstraint");
    uk.setTableName("table");
    return uk;
  }

  private DdmIndex getIndex() {
    DdmIndex index = new DdmIndex();
    index.setName("index");
    index.setTableName("table");
    return index;
  }
}
