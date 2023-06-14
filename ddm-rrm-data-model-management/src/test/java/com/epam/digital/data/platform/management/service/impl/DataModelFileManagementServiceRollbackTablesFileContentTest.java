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

import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("DataModelFileService#rollbackTables(String)")
class DataModelFileManagementServiceRollbackTablesFileContentTest extends
    DataModelFileManagementServiceBaseTest {


  @Test
  @DisplayName("should call rollback tables file on repo")
  void rollbackTableFileContentTest() {
    final var versionId = RandomString.make();

    dataModelFileManagementService.rollbackTables(versionId);

    Mockito.verify(versionContextComponentManager)
        .getComponent(versionId, VersionedFileRepository.class);
    Mockito.verify(versionedFileRepository).rollbackFile(TABLES_FILE_PATH);
  }
}
