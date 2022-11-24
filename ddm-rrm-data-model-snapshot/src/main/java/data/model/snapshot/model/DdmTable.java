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
package data.model.snapshot.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DdmTable {

  private String name;
  private Boolean historyFlag;
  private Boolean objectReference;
  private String description;

  private Map<String, DdmColumn> columns = new HashMap<>();
  private Map<String, DdmForeignKey> foreignKeys = new HashMap<>();
  private DdmPrimaryKeyConstraint primaryKey;
  private Map<String, DdmUniqueConstraint> uniqueConstraints = new HashMap<>();
  private Map<String, DdmIndex> indices = new HashMap<>();

  public DdmTable(String name) {
    this.name = name;
  }
}
