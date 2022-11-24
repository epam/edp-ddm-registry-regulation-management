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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Table;

@ExtendWith(SpringExtension.class)
public class DdmCatalogReaderTest {

  @Mock
  DdmTableReader tableReader;

  @InjectMocks
  DdmCatalogReader catalogReader;

  @Test
  void readNamedObjectTest() {
    Table table = Mockito.mock(Table.class);
    List<Table> tables = new ArrayList<>();
    tables.add(table);
    final Catalog catalogMock = Mockito.mock(Catalog.class);
    Mockito.when(catalogMock.getTables()).thenReturn(tables);
    catalogReader.readNamedObject(catalogMock);

    Mockito.verify(tableReader).readNamedObject(table);
  }

}
