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
package data.model.snapshot;

import data.model.snapshot.model.DdmDataBaseSnapshot;
import data.model.snapshot.model.DdmRolePermission;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.processor.DdmCatalogReader;
import data.model.snapshot.repository.DdmRolePermissionJpaRepository;
import data.model.snapshot.repository.DdmTableRepository;
import data.model.snapshot.writer.DataBaseSnapshotWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import schemacrawler.schema.Catalog;

@ExtendWith(SpringExtension.class)
public class DdmSchemaProcessorTest {

  @Mock
  private Catalog catalog;
  @Mock
  private DdmCatalogReader ddmCatalogReader;
  @Mock
  private DdmTableRepository ddmTableRepository;
  @Mock
  private DdmRolePermissionJpaRepository ddmRolePermissionJpaRepository;
  @Mock
  private DataBaseSnapshotWriter writer;

  @InjectMocks
  private DdmSchemaProcessor processor;

  @Test
  @SneakyThrows
  void runTest() {
    Map<String, DdmTable> map = new HashMap<>();
    Iterable<DdmRolePermission> rolePermissions = new ArrayList<>();
    var rolePermissionMap = StreamSupport.stream(rolePermissions.spliterator(), false)
        .collect(Collectors.toMap(DdmRolePermission::getPermissionId, Function.identity()));

    Mockito.doNothing().when(ddmCatalogReader).readNamedObject(catalog);
    Mockito.when(ddmTableRepository.getAll()).thenReturn(map);
    Mockito.when(ddmRolePermissionJpaRepository.findAll()).thenReturn(rolePermissions);
    processor.run();
    final DdmDataBaseSnapshot ddmDataBaseSnapshot = new DdmDataBaseSnapshot();
    ddmDataBaseSnapshot.setDdmTables(map);
    ddmDataBaseSnapshot.setDdmRolePermissions(rolePermissionMap);
    Mockito.verify(writer).writeSnapshot(ddmDataBaseSnapshot);
  }
}
