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

package com.epam.digital.data.platform.management.filemanagement.service;

import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Used for working with files in specific version. It's needed to create an instance of
 * {@link VersionedFileRepository} for every single version.
 *
 * @see HeadFileRepositoryImpl implementation for head (readable only) version
 * @see VersionedFileRepositoryImpl implementation for writable version
 * @see VersionedFileRepositoryFactory factory of the VersionedFileRepository
 */
public interface VersionedFileRepository {

  /**
   * Get list of files in specific path
   *
   * @param path version relative path to look file into
   * @return {@link VersionedFileInfoDto} representation of file info
   */
  @NonNull
  List<VersionedFileInfoDto> getFileList(@NonNull String path);

  /**
   * Creates or updates file at specific path with specific content
   *
   * @param path    version relative path of file to write new content
   * @param content new content to write
   * @throws UnsupportedOperationException if updating isn't allowed in version
   */
  void writeFile(@NonNull String path, @NonNull String content);

  /**
   * Reads file content at specific path in the version
   *
   * @param path version relative path of file to read the content
   * @return file content or null if file doesn't exist in version
   */
  @Nullable
  String readFile(@NonNull String path);

  /**
   * Checks if file exists at specific path in the version
   *
   * @param path version relative path of file to check if file exists
   * @return true if file exists and false otherwise
   */
  boolean isFileExists(@NonNull String path);

  /**
   * Deletes file at specific path in the version
   *
   * @param path version relative path of file to delete
   * @throws UnsupportedOperationException if updating isn't allowed in version
   */
  void deleteFile(@NonNull String path);

  /**
   * Gets an id of the version of this repository
   *
   * @return id of the version
   */
  @NonNull
  String getVersionId();

  /**
   * Updates a version with remote version changes
   */
  void updateRepository();
}
