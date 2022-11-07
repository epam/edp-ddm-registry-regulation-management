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
package data.model.snapshot.repository.impl;

import data.model.snapshot.model.DdmForeignKey;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.repository.DdmForeignKeyRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class InMemDdmForeignKeyRepository implements DdmForeignKeyRepository {

  private final InMemDdmTableRepository inMemDdmTableRepository;

  @Override
  public void save(DdmForeignKey ddmForeignKey) {
    var table = getTable(ddmForeignKey.getSourceTable());
    table.getForeignKeys().put(ddmForeignKey.getName(), ddmForeignKey);
  }

  @Override
  public DdmForeignKey get(String name, String tableName) {
    var table = getTable(tableName);
    return table.getForeignKeys().get(name);
  }

  @Override
  public void delete(String name, String tableName) {
    var table = getTable(tableName);
    table.getForeignKeys().remove(name);
  }

  private DdmTable getTable(String tableName) {
    var table = inMemDdmTableRepository.get(tableName);
    return Objects.isNull(table) ? new DdmTable(tableName) : table;
  }
}
