/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.management.restapi.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TableInfo {

  @Schema(required = true, description = "Table name")
  private String name;
  @Schema(required = true, description = "Flag that indicates that the entity is an object in the subject data-model")
  private Boolean objectReference;
  @Schema(description = "Table description", nullable = true)
  private String description;

  @Schema(required = true, description = "Current table column map")
  private Map<String, ColumnShortInfo> columns;
  @Schema(description = "Current table foreign key map")
  private Map<String, ForeignKeyShortInfo> foreignKeys;
  @Schema(description = "Current table primary key index")
  private PrimaryKeyConstraintShortInfo primaryKey;
  @Schema(description = "Current table unique constraint index map (primary key excluded)")
  private Map<String, UniqueConstraintShortInfo> uniqueConstraints;
  @Schema(description = "Current table index map (unique constraints and primary key excluded)")
  private Map<String, IndexShortInfo> indices;

  @Getter
  @Setter
  @Builder
  @AllArgsConstructor
  public static class ColumnShortInfo {

    @Schema(required = true, description = "Table column name")
    private String name;
    @Schema(description = "Table column description")
    private String description;
    @Schema(required = true, description = "Table column data type")
    private String type; // enum with possible types
    @Schema(description = "Table column default value")
    private String defaultValue;
    @Schema(required = true, description = "Flag that indicates if table column can not be nullable")
    private Boolean notNullFlag;
  }

  @Getter
  @Builder
  public static class ForeignKeyShortInfo {

    @Schema(required = true, description = "Table foreign key name")
    private String name;
    @Schema(required = true, description = "Foreign key target table name")
    private String targetTable;
    @ArraySchema(
        schema = @Schema(description = "List of related column pairs"),
        minItems = 1)
    private List<ColumnPair> columnPairs;

    @Getter
    @Builder
    public static class ColumnPair {

      @Schema(required = true, description = "Name of the column from current table")
      private String sourceColumnName;
      @Schema(required = true, description = "Name of the column from target table")
      private String targetColumnName;
    }
  }

  @Getter
  @AllArgsConstructor
  public static class IndexShortInfo {

    @Schema(required = true, description = "Table index name")
    private String name;
    @ArraySchema(
        schema = @Schema(description = "Array of index columns"),
        minItems = 1)
    private List<Column> columns;

    @Getter
    @Builder
    public static class Column {

      @Schema(required = true, description = "Name of the column from current table")
      private String name;
      @Schema(required = true, description = "Column index sorting")
      private Sorting sorting;

      public enum Sorting {
        ASC, DESC, NONE
      }
    }
  }

  public static class UniqueConstraintShortInfo extends IndexShortInfo {

    public UniqueConstraintShortInfo(String name, List<Column> columns) {
      super(name, columns);
    }
  }

  public static class PrimaryKeyConstraintShortInfo extends UniqueConstraintShortInfo {

    public PrimaryKeyConstraintShortInfo(String name, List<Column> columns) {
      super(name, columns);
    }
  }
}
