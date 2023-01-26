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
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * Provides methods to work with tables
 */
public interface DataModelService {

  /**
   * Get {@link List} of {@link TableShortInfoDto} in given version
   *
   * @param versionId id of version to search tables in
   * @return {@link List} of {@link TableShortInfoDto}
   *
   * @throws RegistryDataBaseConnectionException if it couldn't connect to master version database
   */
  @NonNull
  List<TableShortInfoDto> list(@NonNull String versionId);

  /**
   * Get {@link TableInfoDto} by table name in given version
   *
   * @param tableName table name
   * @param versionId id of version to search table in
   * @return {@link TableInfoDto}
   *
   * @throws TableNotFoundException              if table doesn't exist
   * @throws RegistryDataBaseConnectionException if it couldn't connect to master version database
   */
  @NonNull
  TableInfoDto get(@NonNull String versionId, @NonNull String tableName);
}
