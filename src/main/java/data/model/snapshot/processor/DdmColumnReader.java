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

import data.model.snapshot.model.DdmColumn;
import data.model.snapshot.repository.DdmColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Column;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmColumnReader implements DdmNamedObjectReader<Column> {

  public static final String SUBJECT_TABLE_PRIMARY_KEY = "subject.subject_id";

  private final DdmColumnRepository ddmColumnRepository;

  @Override
  public void readNamedObject(Column column) {
    var tableName = column.getParent().getName();

    if (column.isPartOfForeignKey() && column.getReferencedColumn().getFullName()
        .endsWith(SUBJECT_TABLE_PRIMARY_KEY)) {
      return;
    }
    var ddmColumn = new DdmColumn();
    ddmColumn.setName(column.getName());
    ddmColumn.setTableName(tableName);
    ddmColumn.setType(column.getType().getName());
    ddmColumn.setDescription(column.getRemarks());
    ddmColumn.setDefaultValue(column.getDefaultValue());
    ddmColumn.setNotNullFlag(!column.isNullable());
    ddmColumnRepository.save(ddmColumn);
  }
}
