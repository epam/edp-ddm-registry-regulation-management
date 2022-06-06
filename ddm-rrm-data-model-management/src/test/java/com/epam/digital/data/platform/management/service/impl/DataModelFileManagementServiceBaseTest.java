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

import static com.epam.digital.data.platform.management.constant.DataModelManagementConstants.DATA_MODEL_FOLDER;

import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.mapper.DataModelFileManagementMapper;
import com.epam.digital.data.platform.management.model.dto.DataModelFileType;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DataModelFileManagementServiceImpl.class
})
@ComponentScan(basePackageClasses = DataModelFileManagementMapper.class)
@EnableConfigurationProperties
abstract class DataModelFileManagementServiceBaseTest {

  public static final String TABLES_FILE_PATH = FilenameUtils.normalize(
      DATA_MODEL_FOLDER + "/" + DataModelFileType.TABLES_FILE.getFileName() + ".xml");

  @Autowired
  DataModelFileManagementService dataModelFileManagementService;

  @MockBean
  VersionContextComponentManager versionContextComponentManager;
  @Mock
  VersionedFileRepository versionedFileRepository;
  @MockBean
  CacheService cacheService;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(versionedFileRepository)
        .when(versionContextComponentManager)
        .getComponent(Mockito.anyString(), Mockito.eq(VersionedFileRepository.class));
  }
}
