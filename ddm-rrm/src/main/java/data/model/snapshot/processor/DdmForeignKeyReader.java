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

import data.model.snapshot.model.DdmForeignKey;
import data.model.snapshot.repository.DdmForeignKeyRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmForeignKeyReader implements DdmNamedObjectReader<ForeignKey> {

  public static final String SUBJECT_TABLE = "subject";

  private final DdmForeignKeyRepository ddmForeignKeyRepository;

  @Override
  public void readNamedObject(ForeignKey foreignKey) {
    var columnReference = foreignKey.getColumnReferences().get(0);
    var targetTable = columnReference.getPrimaryKeyColumn().getParent().getName();
    var sourceTable = columnReference.getForeignKeyColumn().getParent().getName();
    if (SUBJECT_TABLE.equals(targetTable)) {
      return;
    }

    var ddmForeignKey = new DdmForeignKey();
    ddmForeignKey.setName(foreignKey.getName());
    ddmForeignKey.setSourceTable(sourceTable);
    ddmForeignKey.setTargetTable(targetTable);

    var columnPairs = foreignKey.getColumnReferences()
        .stream()
        .map(this::toColumnPair)
        .collect(Collectors.toList());
    ddmForeignKey.setColumnPairs(columnPairs);

    ddmForeignKeyRepository.save(ddmForeignKey);
  }

  private DdmForeignKey.ColumnPair toColumnPair(ForeignKeyColumnReference columnReference) {
    var columnPair = new DdmForeignKey.ColumnPair();
    columnPair.setTargetColumnName(columnReference.getPrimaryKeyColumn().getName());
    columnPair.setSourceColumnName(columnReference.getForeignKeyColumn().getName());
    return columnPair;
  }
}
