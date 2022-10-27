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
package data.model.snapshot.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import data.model.snapshot.model.DdmDataBaseSnapshot;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataBaseSnapshotWriter {

  private static final String SNAPSHOTS_PATH = "repositories/data-model-snapshot/";
  private static final String SNAPSHOTS_FILE_EXTENSION = ".json";

  private final ObjectMapper objectMapper;

  public void writeSnapshot(DdmDataBaseSnapshot snapshot) {
    var ddmTables = snapshot.getDdmTables();
    ddmTables.forEach((tableName, tableValue) -> {
      var snapshotString = serialize(tableValue);
      write(snapshotString, "tables", tableName + SNAPSHOTS_FILE_EXTENSION);
    });

    var ddmRolePermissions = snapshot.getDdmRolePermissions();
    ddmRolePermissions.forEach((roleName, roleValue) -> {
      var snapshotString = serialize(roleValue);
      write(snapshotString, "role-permissions", roleName + SNAPSHOTS_FILE_EXTENSION);
    });
  }

  private void write(String snapshotString, String entity, String filename) {
    var dataModelSnapshotDirectoryPath = SNAPSHOTS_PATH + entity;
    var file = new File(FilenameUtils.normalizeNoEndSeparator(dataModelSnapshotDirectoryPath), FilenameUtils.getName(filename));
    var directory = file.getParentFile();
    if (directory != null) {
      createDirectory(directory);
    }
    try (var writer = new FileWriter(file)) {
      writer.write(snapshotString);
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't print the snapshot", e);
    }
  }

  private void createDirectory(File directory) {
    try {
      Files.createDirectories(Paths.get(directory.getPath()));
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't create directory with path: " + directory.getPath(), e);
    }
  }

  private String serialize(Object snapshot) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Couldn't serialize the snapshot", e);
    }
  }
}
