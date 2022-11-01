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
import data.model.snapshot.processor.DdmCatalogReader;
import data.model.snapshot.repository.DdmRolePermissionJpaRepository;
import data.model.snapshot.repository.DdmTableRepository;
import data.model.snapshot.writer.DataBaseSnapshotWriter;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Catalog;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmSchemaProcessor {

  @Autowired(required = false)
  @Lazy
  private Catalog catalog;
  private final DdmCatalogReader ddmCatalogReader;

  private final DdmTableRepository ddmTableRepository;
  private final DdmRolePermissionJpaRepository ddmRolePermissionJpaRepository;

  private final DataBaseSnapshotWriter writer;

  public void run() {
    ddmCatalogReader.readNamedObject(catalog);

    var snapshot = new DdmDataBaseSnapshot();
    var rolePermissions = ddmRolePermissionJpaRepository.findAll();
    var rolePermissionMap = StreamSupport.stream(rolePermissions.spliterator(), false)
        .collect(Collectors.toMap(DdmRolePermission::getPermissionId, Function.identity()));

    snapshot.setDdmTables(ddmTableRepository.getAll());
    snapshot.setDdmRolePermissions(rolePermissionMap);

    writer.writeSnapshot(snapshot);
  }
}
