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

import static com.epam.digital.data.platform.liquibase.extension.DdmConstants.SUFFIX_M2M;
import static org.mockito.ArgumentMatchers.any;

import data.model.snapshot.repository.DdmIndexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import schemacrawler.schema.Index;
import schemacrawler.schema.PrimaryKey;
import schemacrawler.schema.Table;

@ExtendWith(MockitoExtension.class)
public class DdmIndexReaderTest {

  @Mock
  DdmIndexRepository ddmIndexRepository;
  @Mock
  Index index;

  @Mock
  PrimaryKey primaryKey;
  @Mock
  Table table;
  @InjectMocks
  DdmIndexReader indexReader;

  @Test
  void readNamedObjectTest() {
    Mockito.when(index.getName()).thenReturn(SUFFIX_M2M);
    indexReader.readNamedObject(index);
    Mockito.verify(ddmIndexRepository, Mockito.never()).save(any());
  }

  @Test
  void readNamedObjectSaveCalledTest() {
    Mockito.when(index.getName()).thenReturn("index");
    Mockito.when(index.getParent()).thenReturn(table);
    Mockito.when(table.getPrimaryKey()).thenReturn(primaryKey);

    indexReader.readNamedObject(index);
    Mockito.verify(ddmIndexRepository).save(any());
  }

}
