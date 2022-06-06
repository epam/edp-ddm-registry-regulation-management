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

package com.epam.digital.data.platform.management.exception;

import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * Exception that is thrown in case if there doesn't exist requested data-model file in requested
 * version
 */
@Getter
public class DataModelFileNotFoundInVersionException extends RuntimeException {

  private final String filePath;
  private final String versionId;

  public DataModelFileNotFoundInVersionException(@NonNull String filePath,
      @NonNull String versionId) {
    this(filePath, versionId,
        String.format("Data-model file %s is not found in version %s", filePath, versionId));
  }

  public DataModelFileNotFoundInVersionException(@NonNull String filePath,
      @NonNull String versionId, @NonNull String message) {
    super(message);
    this.filePath = filePath;
    this.versionId = versionId;
  }
}
