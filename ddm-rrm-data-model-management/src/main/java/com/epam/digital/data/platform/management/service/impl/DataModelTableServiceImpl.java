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
package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.exception.VersionComponentCreationException;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.mapper.SchemaCrawlerMapper;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.service.DataModelTableService;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import schemacrawler.schema.Catalog;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataModelTableServiceImpl implements DataModelTableService {

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final VersionContextComponentManager versionContextComponentManager;
  private final SchemaCrawlerMapper mapper;

  @Override
  @NonNull
  public List<TableShortInfoDto> listTables(@NonNull String versionId) {
    log.debug("Trying to get list of tables in version '{}'", versionId);

    var catalog = getCatalog(versionId);
    if (Objects.isNull(catalog)) {
      return List.of();
    }

    var tablesDetails = mapper.toTableShortInfoDtos(catalog.getTables());
    tablesDetails.sort(Comparator.comparing(TableShortInfoDto::getName));

    log.debug("There were found {} tables for version '{}'", tablesDetails.size(), versionId);
    return tablesDetails;
  }

  @Override
  @NonNull
  public TableInfoDto getTable(@NonNull String versionId, @NonNull String tableName) {
    log.debug("Trying to get table with name '{}' in version '{}'", tableName, versionId);
    var catalog = getCatalog(versionId);
    if (Objects.isNull(catalog)) {
      throw tableNotFoundException(versionId, tableName);
    }

    var table = catalog.getTables().stream()
        .filter(t -> t.getName().equals(tableName))
        .reduce((t, t2) -> {
          throw new IllegalStateException("There cannot be several tables with same name");
        }).orElseThrow(() -> tableNotFoundException(versionId, tableName));

    log.debug("Table with name '{}' was found in version '{}'", tableName, versionId);
    return mapper.toTableInfoDto(table);
  }

  @Nullable
  private Catalog getCatalog(String versionId) {
    try {
      log.trace("trying getting schema catalog for version '{}'", versionId);
      return versionContextComponentManager.getComponent(versionId, Catalog.class);
    } catch (VersionComponentCreationException e) {
      if (gerritPropertiesConfig.getHeadBranch().equals(versionId)) {
        log.error("Couldn't connect to master version data-base: {}", e.getMessage());
        throw registryDataBaseConnectionException(e);
      } else {
        log.warn("Couldn't connect to version-candidate {} data-base: {}", versionId,
            e.getMessage());
        checkMainDataBaseConnection();
        return null;
      }
    }
  }

  private void checkMainDataBaseConnection() {
    var masterVersionId = gerritPropertiesConfig.getHeadBranch();
    log.trace("trying getting connection to database master version '{}'", masterVersionId);
    var datasource = versionContextComponentManager.getComponent(masterVersionId,
        RegistryDataSource.class);
    try (var ignoredConnection = datasource.getConnection()) {
    } catch (SQLException sqlException) {
      throw registryDataBaseConnectionException(sqlException);
    }
  }

  private TableNotFoundException tableNotFoundException(String versionId, String tableName) {
    return new TableNotFoundException(
        String.format("Table with name '%s' doesn't exist in version '%s'.", tableName, versionId));
  }

  private RegistryDataBaseConnectionException registryDataBaseConnectionException(Throwable cause) {
    return new RegistryDataBaseConnectionException(
        String.format("Couldn't connect to registry data-base: %s", cause.getMessage()), cause);
  }
}
