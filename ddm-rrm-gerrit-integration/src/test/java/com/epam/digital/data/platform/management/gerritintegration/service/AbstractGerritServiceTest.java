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

package com.epam.digital.data.platform.management.gerritintegration.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gerritintegration.mapper.GerritMapper;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangeApiRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangesRestClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class AbstractGerritServiceTest {

  @Mock
  GerritPropertiesConfig gerritPropertiesConfig;
  @Mock
  GerritApiImpl gerritApiImpl;
  @Spy
  GerritMapper mapper = Mappers.getMapper(GerritMapper.class);
  @InjectMocks
  GerritServiceImpl gerritService;
  @Mock
  ChangesRestClient changes;
  @Mock
  ChangeApiRestClient changeApiRestClient;
  @Mock
  Changes.QueryRequest request;
  @Mock
  RevisionApi revisionApi;
  @Mock
  GerritRestClient gerritRestClient;
  List<ChangeInfo> changeInfos = new ArrayList<>();
  ChangeInfo changeInfo = new ChangeInfo();

  @BeforeEach
  void initChanges() {
    changeInfo._number = 5;
    changeInfos.add(changeInfo);
    Mockito.lenient().when(gerritApiImpl.changes()).thenReturn(changes);
  }

}
