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

import data.model.snapshot.model.DdmTable;
import data.model.snapshot.repository.DdmTableRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class InMemDdmTableRepository implements DdmTableRepository {

  private final Map<String, DdmTable> ddmTableMap = new ConcurrentHashMap<>();

  @Override
  public void save(DdmTable ddmTable) {
    ddmTableMap.put(ddmTable.getName(), ddmTable);
  }

  @Override
  public DdmTable get(String id) {
    return ddmTableMap.get(id);
  }

  @Override
  public void delete(String id) {
    ddmTableMap.remove(id);
  }

  @Override
  public Map<String, DdmTable> getAll() {
    return new HashMap<>(ddmTableMap);
  }
}
