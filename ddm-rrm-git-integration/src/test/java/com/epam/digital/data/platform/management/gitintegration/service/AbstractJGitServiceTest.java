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

package com.epam.digital.data.platform.management.gitintegration.service;

import com.epam.digital.data.platform.management.core.config.CacheConfig;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.config.RetryConfig;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RetryConfig.class,
    CacheConfig.class,
    CacheAutoConfiguration.class,
    GitRetryable.class,
    JGitServiceImpl.class})
abstract class AbstractJGitServiceTest {

  @TempDir
  File tempDir;

  @Autowired
  JGitService jGitService;

  @MockBean
  JGitWrapper jGitWrapper;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  GitFileService gitFileService;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(tempDir.getPath()).when(gerritPropertiesConfig).getRepositoryDirectory();
  }
}
