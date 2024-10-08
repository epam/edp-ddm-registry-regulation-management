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
package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.validation.TableName;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

/**
 * Provides methods to work with tables
 */
@Validated
public interface ReadDataBaseTablesService {

  /**
   * Get {@link List} of {@link TableShortInfoDto} in given version
   *
   * @param versionId id of version to search tables in
   * @param isSuccessfulBuild candidate/master version build status flag to control cache update
   * @return {@link List} of {@link TableShortInfoDto}
   *
   * @throws RegistryDataBaseConnectionException if it couldn't connect to master version database
   */
  @NonNull
  List<TableShortInfoDto> listTables(@NonNull String versionId, boolean isSuccessfulBuild);

  /**
   * Get {@link TableInfoDto} by table name in given version
   *
   * @param tableName table name
   * @param versionId id of version to search table in
   * @param isSuccessfulBuild candidate/master version build status flag to control cache update
   * @return {@link TableInfoDto}
   *
   * @throws TableNotFoundException              if table doesn't exist
   * @throws RegistryDataBaseConnectionException if it couldn't connect to master version database
   */
  @NonNull
  TableInfoDto getTable(@NonNull String versionId, @TableName @NonNull String tableName, boolean isSuccessfulBuild);
}
