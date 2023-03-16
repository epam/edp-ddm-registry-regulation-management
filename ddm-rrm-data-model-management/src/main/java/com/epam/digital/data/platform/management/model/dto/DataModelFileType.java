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

package com.epam.digital.data.platform.management.model.dto;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public enum DataModelFileType {
  TABLES_FILE("createTables");

  final String fileName;

  @Nullable
  public static DataModelFileType getByFileName(@NonNull String fileName) {
    return Arrays.stream(values())
        .filter(fileType -> fileType.getFileName().equals(fileName))
        .reduce((fileType1, fileType2) -> {
          throw new IllegalStateException("File name must be unique in this enum");
        })
        .orElse(null);
  }
}
