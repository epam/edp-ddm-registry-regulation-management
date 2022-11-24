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

import data.model.snapshot.model.DdmColumn;
import data.model.snapshot.repository.DdmColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class InMemDdmColumnRepository implements DdmColumnRepository {

  private final InMemDdmTableRepository inMemDdmTableRepository;

  @Override
  public void save(DdmColumn ddmColumn) {
    var table = inMemDdmTableRepository.get(ddmColumn.getTableName());
    table.getColumns().put(ddmColumn.getName(), ddmColumn);
  }

  @Override
  public DdmColumn get(String name, String tableName) {
    var table = inMemDdmTableRepository.get(tableName);
    return table.getColumns().get(name);
  }

  @Override
  public void delete(String name, String tableName) {
    var table = inMemDdmTableRepository.get(tableName);
    table.getColumns().remove(name);
  }
}
