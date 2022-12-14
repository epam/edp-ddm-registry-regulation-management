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
package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.exception.TableParseException;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import java.util.List;

/**
 * Provides methods to work with tables
 */
public interface DataModelService {

  /**
   * Get {@link List} of {@link TableShortInfoDto}
   * @return {@link List} of {@link TableShortInfoDto}
   * @throws TableParseException if any of the tables could not be parsed
   */
  List<TableShortInfoDto> list();

  /**
   * Get {@link TableInfoDto} by table name
   * @param name table name
   * @return {@link TableInfoDto}
   * @throws TableParseException if table could not be parsed
   * @throws TableNotFoundException if table doesn't exist
   */
  TableInfoDto get(String name);
}
