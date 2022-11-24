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

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceListChangesTest  extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getMrChangesListTest() {
    var fileInfo = new FileInfo();
    fileInfo.status = 'A';
    fileInfo.size = 36;
    fileInfo.linesDeleted = 0;
    fileInfo.sizeDelta = 12;
    fileInfo.linesInserted = 12;
    fileInfo.oldPath = RandomString.make();
    var key = RandomString.make();
    Map<String, FileInfo> filesMap = new HashMap<>();
    filesMap.put(key, fileInfo);

    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.files()).thenReturn(filesMap);
    Map<String, FileInfoDto> files = gerritService.getListOfChangesInMR(changeId);
    Assertions.assertThat(files).isNotNull();

    var fileInfoDto = files.get(key);
    Assertions.assertThat(fileInfoDto).isNotNull();
    Assertions.assertThat(fileInfoDto.getStatus()).isEqualTo(fileInfo.status.toString());
    Assertions.assertThat(fileInfoDto.getSize()).isEqualTo(fileInfo.size);
    Assertions.assertThat(fileInfoDto.getLinesDeleted()).isEqualTo(fileInfo.linesDeleted);
    Assertions.assertThat(fileInfoDto.getLinesInserted()).isEqualTo(fileInfo.linesInserted);
    Assertions.assertThat(fileInfoDto.getSizeDelta()).isEqualTo(fileInfo.sizeDelta);
    Mockito.verify(revisionApi).files();
  }

  @Test
  @SneakyThrows
  void testNotFoundException() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getListOfChangesInMR(changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);

  }

  @Test
  @SneakyThrows
  void testHttpException() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getListOfChangesInMR(changeId))
        .doesNotThrowAnyException();

    Assertions.assertThatCode(() -> gerritService.getListOfChangesInMR(changeId))
        .isNull();
  }

  @Test
  @SneakyThrows
  void testRestApiException() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getListOfChangesInMR(changeId))
        .isInstanceOf(GerritCommunicationException.class);
  }
}
