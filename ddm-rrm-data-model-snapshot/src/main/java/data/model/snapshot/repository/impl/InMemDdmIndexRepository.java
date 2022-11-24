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

import data.model.snapshot.model.DdmIndex;
import data.model.snapshot.model.DdmPrimaryKeyConstraint;
import data.model.snapshot.model.DdmUniqueConstraint;
import data.model.snapshot.repository.DdmIndexRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class InMemDdmIndexRepository implements DdmIndexRepository {

  private final InMemDdmTableRepository inMemDdmTableRepository;

  @Override
  public void save(DdmIndex ddmIndex) {
    var table = inMemDdmTableRepository.get(ddmIndex.getTableName());
    if (ddmIndex instanceof DdmPrimaryKeyConstraint) {
      table.setPrimaryKey((DdmPrimaryKeyConstraint) ddmIndex);
    } else if (ddmIndex instanceof DdmUniqueConstraint) {
      table.getUniqueConstraints().put(ddmIndex.getName(), (DdmUniqueConstraint) ddmIndex);
    } else {
      table.getIndices().put(ddmIndex.getName(), ddmIndex);
    }
  }

  @Override
  public DdmIndex get(String name, String tableName) {
    var table = inMemDdmTableRepository.get(tableName);
    if (Objects.nonNull(table.getPrimaryKey()) && name.equals(table.getPrimaryKey().getName())) {
      return table.getPrimaryKey();
    }
    if (table.getUniqueConstraints().containsKey(name)) {
      return table.getUniqueConstraints().get(name);
    }
    return table.getIndices().get(name);
  }

  @Override
  public void delete(String name, String tableName) {
    var table = inMemDdmTableRepository.get(tableName);
    if (Objects.nonNull(table.getPrimaryKey()) && name.equals(table.getPrimaryKey().getName())) {
      table.setPrimaryKey(null);
    }
    table.getUniqueConstraints().remove(name);
    table.getIndices().remove(name);
  }
}
