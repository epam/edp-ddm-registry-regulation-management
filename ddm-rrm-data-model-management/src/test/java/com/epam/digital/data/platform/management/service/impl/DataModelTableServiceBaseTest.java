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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.mapper.SchemaCrawlerMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
public abstract class DataModelTableServiceBaseTest {

  static final String VERSION_ID = "162";
  static final String SUBJECT_TABLE = "subject";
  static final String HEAD_BRANCH = "master";

  @Mock
  VersionContextComponentManager versionContextComponentManager;
  @Mock
  GerritPropertiesConfig gerritPropertiesConfig;
  @Spy
  private SchemaCrawlerMapper mapper = Mappers.getMapper(SchemaCrawlerMapper.class);
  @InjectMocks
  DataModelTableServiceImpl tableService;

  @BeforeEach
  @SneakyThrows
  void setup() {
    ReflectionTestUtils.setField(mapper, "subjectTable", SUBJECT_TABLE);
    Mockito.doReturn(HEAD_BRANCH).when(gerritPropertiesConfig).getHeadBranch();
  }
}
