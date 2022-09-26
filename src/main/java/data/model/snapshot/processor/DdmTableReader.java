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

import com.epam.digital.data.platform.liquibase.extension.DdmConstants;
import data.model.snapshot.model.DdmTable;
import data.model.snapshot.repository.DdmTableRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Table;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmTableReader implements DdmNamedObjectReader<Table> {

  private final DdmTableRepository ddmTableRepository;
  private final DdmViewReader ddmViewReader;
  private final DdmColumnReader ddmColumnReader;
  private final DdmForeignKeyReader ddmForeignKeyReader;
  private final DdmIndexReader ddmIndexReader;

  @Override
  public void readNamedObject(Table table) {
    var tableName = table.getName();
    if (tableName.endsWith(DdmConstants.SUFFIX_VIEW)) {
      ddmViewReader.readNamedObject(table);
      return;
    }
    var ddmTable = getDdmTable(tableName);
    ddmTable.setDescription(table.getRemarks());
    ddmTable.setObjectReference(null); // metadata
    ddmTable.setHistoryFlag(null); // metadata
    ddmTableRepository.save(ddmTable);

    table.getColumns().forEach(ddmColumnReader::readNamedObject);
    table.getForeignKeys().forEach(ddmForeignKeyReader::readNamedObject);
    table.getIndexes().forEach(ddmIndexReader::readNamedObject);
  }

  private DdmTable getDdmTable(String tableName) {
    var ddmTable = ddmTableRepository.get(tableName);
    return Objects.isNull(ddmTable) ? new DdmTable(tableName) : ddmTable;
  }
}
