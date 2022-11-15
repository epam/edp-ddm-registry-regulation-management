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
package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.exception.TableParseException;
import com.epam.digital.data.platform.management.mapper.TableShortInfoMapper;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epam.digital.data.platform.management.service.DataModelService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataModelServiceImpl implements DataModelService {

  public static final String DIRECTORY_PATH = "repositories/data-model-snapshot/tables";
  private static final String JSON_FILE_EXTENSION = "json";
  private final TableShortInfoMapper mapper;
  private final ObjectMapper objectMapper;

  @Override
  public List<TableShortInfoDto> list() {
    log.debug("Trying to get list of tables");
    final File[] files = new File(DIRECTORY_PATH).listFiles();
    if (files == null) {
      log.debug("No one table found.");
      return new ArrayList<>();
    }
    List<TableShortInfoDto> tableDetails = new ArrayList<>();
    for (File file : files) {
      final String baseName = FilenameUtils.getBaseName(file.getName());
      log.trace("Getting table {}", baseName);
      tableDetails.add(mapper.toTableShortInfoDto(get(baseName)));
    }
    log.debug("There were found {} tables", tableDetails.size());
    return tableDetails;

  }

  @Override
  public TableInfoDto get(String name) {
    log.debug("Trying to get table with name '{}'", name);
    try (FileInputStream fis = new FileInputStream(getProcessPath(name))) {
      String data = IOUtils.toString(fis, StandardCharsets.UTF_8.name());
      log.debug("Table with name '{}' was found", name);
      return objectMapper.readValue(data, TableInfoDto.class);
    } catch (FileNotFoundException e) {
      throw new TableNotFoundException(String.format("Table with name '%s' doesn't exist.", name));
    } catch (IOException e) {
      throw new TableParseException(String.format("Cannot read the table with name '%s'.", name));
    }
  }

  private String getProcessPath(String processName) {
    return String.format("%s/%s.%s", DIRECTORY_PATH, FilenameUtils.getName(processName),
        JSON_FILE_EXTENSION);
  }
}
