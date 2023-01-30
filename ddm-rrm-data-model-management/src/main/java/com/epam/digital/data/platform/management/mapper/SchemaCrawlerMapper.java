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

package com.epam.digital.data.platform.management.mapper;

import com.epam.digital.data.platform.liquibase.extension.DdmConstants;
import com.epam.digital.data.platform.management.model.dto.ColumnShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.ForeignKeyShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.IndexShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.PrimaryKeyConstraintShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.UniqueConstraintShortInfoDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.springframework.beans.factory.annotation.Value;
import schemacrawler.schema.BaseForeignKey;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnReference;
import schemacrawler.schema.ContainedObject;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Index;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.IndexColumnSortSequence;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Table;

/**
 * Mapper that is used to map {@link schemacrawler.schema.Catalog} object to collection of
 * {@link TableInfoDto} or {@link TableShortInfoDto}
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public abstract class SchemaCrawlerMapper {

  @Value("${registry-regulation-management.subject-table-name:subject}")
  private String subjectTable;

  public List<TableShortInfoDto> toTableShortInfoDtos(Collection<Table> tables) {
    return tables.stream()
        .filter(table -> !table.getName().endsWith(DdmConstants.SUFFIX_VIEW))
        .map(this::toTableShortInfoDto)
        .collect(Collectors.toList());
  }

  @Mapping(target = "description", source = "remarks")
  @Mapping(target = "objectReference", source = "importedForeignKeys")
  public abstract TableShortInfoDto toTableShortInfoDto(Table table);

  @Mapping(target = "description", source = "remarks")
  @Mapping(target = "objectReference", source = "importedForeignKeys")
  @Mapping(target = "columns", qualifiedByName = "toColumnMap")
  @Mapping(target = "foreignKeys", source = "importedForeignKeys", qualifiedByName = "toForeignKeyMap")
  public abstract TableInfoDto toTableInfoDto(Table table);

  /**
   * @return true if there is a foreign key to subject table in the foreign key list
   */
  public Boolean isObject(Collection<ForeignKey> importedForeignKeys) {
    return importedForeignKeys.stream()
        .map(BaseForeignKey::getColumnReferences)
        .flatMap(Collection::stream)
        .map(ColumnReference::getPrimaryKeyColumn)
        .map(ContainedObject::getParent)
        .map(NamedObject::getName)
        .anyMatch(subjectTable::equals);
  }

  @Named("toColumnMap")
  public Map<String, ColumnShortInfoDto> toColumnMap(Collection<Column> columns) {
    return mapColumns(toMap(columns));
  }

  public abstract Map<String, ColumnShortInfoDto> mapColumns(Map<String, Column> columns);


  @Mapping(target = "type", source = "type.name")
  @Mapping(target = "description", source = "remarks")
  @Mapping(target = "notNullFlag", expression = "java(!column.isNullable())")
  public abstract ColumnShortInfoDto toColumnShortInfoDto(Column column);

  @Named("toForeignKeyMap")
  public Map<String, ForeignKeyShortInfoDto> toForeignKeyMap(Collection<ForeignKey> foreignKeys) {
    return mapForeignKeys(toMap(foreignKeys));
  }

  public abstract Map<String, ForeignKeyShortInfoDto> mapForeignKeys(
      Map<String, ForeignKey> columns);

  public ForeignKeyShortInfoDto toForeignKeyShortInfoDto(ForeignKey foreignKey) {
    if (Objects.isNull(foreignKey)) {
      return null;
    }
    var foreignKeyShortInfoDto = new ForeignKeyShortInfoDto();
    foreignKeyShortInfoDto.setName(foreignKey.getName());
    foreignKeyShortInfoDto.setTargetTable(
        foreignKey.getColumnReferences().get(0).getPrimaryKeyColumn().getParent().getName());

    foreignKeyShortInfoDto.setColumnPairs(mapColumnPairs(foreignKey.getColumnReferences()));

    return foreignKeyShortInfoDto;
  }

  public abstract List<ForeignKeyShortInfoDto.ColumnPair> mapColumnPairs(
      Collection<ForeignKeyColumnReference> columnReferences);

  @Mapping(target = "targetColumnName", source = "primaryKeyColumn.name")
  @Mapping(target = "sourceColumnName", source = "foreignKeyColumn.name")
  public abstract ForeignKeyShortInfoDto.ColumnPair toColumnPair(
      ForeignKeyColumnReference columnReference);

  public <T extends NamedObject> Map<String, T> toMap(Collection<T> collection) {
    return collection.stream().collect(Collectors.toMap(NamedObject::getName, Function.identity()));
  }

  @AfterMapping
  public void mapIndices(@MappingTarget TableInfoDto tableInfoDto, Table table) {
    table.getIndexes().forEach(index -> mapIndex(tableInfoDto, index));
  }

  @AfterMapping
  public void mapIndex(@MappingTarget TableInfoDto tableInfoDto, Index index) {
    if (index.getName().endsWith(DdmConstants.SUFFIX_M2M)) {
      return;
    }

    if (index.getName().equals(index.getParent().getPrimaryKey().getName())) {
      tableInfoDto.setPrimaryKey(toPrimaryKeyConstraint(index));
    } else if (index.isUnique()) {
      var constraint = toUniqueConstraint(index);
      tableInfoDto.getUniqueConstraints().put(constraint.getName(), constraint);
    } else {
      var indexShortInfoDto = toIndexShortInfoDto(index);
      tableInfoDto.getIndices().put(indexShortInfoDto.getName(), indexShortInfoDto);
    }
  }

  public PrimaryKeyConstraintShortInfoDto toPrimaryKeyConstraint(Index index) {
    if (Objects.isNull(index)) {
      return null;
    }
    var primaryKey = new PrimaryKeyConstraintShortInfoDto();
    primaryKey.setName(index.getName());
    primaryKey.setColumns(toDdmIndexColumns(index.getColumns()));
    return primaryKey;
  }

  public UniqueConstraintShortInfoDto toUniqueConstraint(Index index) {
    if (Objects.isNull(index)) {
      return null;
    }
    var uniqueConstraint = new UniqueConstraintShortInfoDto();
    uniqueConstraint.setName(index.getName());
    uniqueConstraint.setColumns(toDdmIndexColumns(index.getColumns()));
    return uniqueConstraint;
  }

  public IndexShortInfoDto toIndexShortInfoDto(Index index) {
    if (Objects.isNull(index)) {
      return null;
    }
    var indexShortInfoDto = new IndexShortInfoDto();
    indexShortInfoDto.setName(index.getName());
    indexShortInfoDto.setColumns(toDdmIndexColumns(index.getColumns()));
    return indexShortInfoDto;
  }

  public abstract List<IndexShortInfoDto.Column> toDdmIndexColumns(List<IndexColumn> indexColumn);

  @Mapping(target = "sorting", source = "sortSequence")
  public abstract IndexShortInfoDto.Column toDdmIndexColumn(IndexColumn indexColumn);

  @ValueMapping(source = "ascending", target = "ASC")
  @ValueMapping(source = "descending", target = "DESC")
  @ValueMapping(source = "unknown", target = "NONE")
  public abstract IndexShortInfoDto.Column.Sorting mapSorting(
      IndexColumnSortSequence indexColumnSortSequence);
}
