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
import data.model.snapshot.model.DdmIndex;
import data.model.snapshot.model.DdmIndex.Column;
import data.model.snapshot.model.DdmIndex.Column.Sorting;
import data.model.snapshot.model.DdmPrimaryKeyConstraint;
import data.model.snapshot.model.DdmUniqueConstraint;
import data.model.snapshot.repository.DdmIndexRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Index;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.IndexColumnSortSequence;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DdmIndexReader implements DdmNamedObjectReader<Index> {

  private final DdmIndexRepository ddmIndexRepository;

  @Override
  public void readNamedObject(Index index) {
    if (index.getName().endsWith(DdmConstants.SUFFIX_M2M)) {
      return;
    }

    var table = index.getParent();
    DdmIndex ddmIndex;
    if (index.getName().equals(table.getPrimaryKey().getName())) {
      ddmIndex = new DdmPrimaryKeyConstraint();
    } else if (index.isUnique()) {
      ddmIndex = new DdmUniqueConstraint();
    } else {
      ddmIndex = new DdmIndex();
    }

    ddmIndex.setName(index.getName());
    ddmIndex.setColumns(getDdmIndexColumns(index));
    ddmIndex.setTableName(table.getName());
    ddmIndexRepository.save(ddmIndex);
  }

  private List<Column> getDdmIndexColumns(Index index) {
    return index.getColumns()
        .stream()
        .map(this::toDdmIndexColumn)
        .collect(Collectors.toList());
  }

  private Column toDdmIndexColumn(IndexColumn indexColumn) {
    var column = new Column();
    column.setName(indexColumn.getName());
    column.setSorting(mapSorting(indexColumn.getSortSequence()));
    return column;
  }

  private Sorting mapSorting(IndexColumnSortSequence indexColumnSortSequence) {
    switch (indexColumnSortSequence) {
      case ascending:
        return Sorting.ASC;
      case descending:
        return Sorting.DESC;
      default:
        return Sorting.NONE;
    }
  }
}
