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

package com.epam.digital.data.platform.management.users.service;

import com.epam.digital.data.platform.management.security.model.SecurityContext;
import com.epam.digital.data.platform.management.users.model.CephFileDto;
import com.epam.digital.data.platform.management.users.model.CephFileInfoDto;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service that is used to manage users file in ceph
 */
public interface UserImportService {

  /**
   * Store file in ceph
   *
   * @param file            file to store
   * @param securityContext current security-context with JWT
   * @return info about stored file
   */
  @NonNull
  CephFileInfoDto storeFile(@NonNull MultipartFile file, @NonNull SecurityContext securityContext);

  /**
   * Get stored user file info from ceph
   *
   * @param securityContext current security-context with JWT
   * @return info about stored file
   */
  @NonNull
  CephFileInfoDto getFileInfo(@NonNull SecurityContext securityContext);

  /**
   * Delete file from ceph by key
   *
   * @param cephKey key of file to delete
   */
  void delete(@NonNull String cephKey);

  /**
   * Download stored file from ceph by key
   *
   * @param cephKey key of file to download
   */
  @NonNull
  CephFileDto downloadFile(@NonNull String cephKey);
}
