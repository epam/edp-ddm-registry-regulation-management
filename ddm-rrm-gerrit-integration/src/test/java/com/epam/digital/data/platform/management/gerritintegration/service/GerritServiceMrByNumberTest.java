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

import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.ArrayList;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceMrByNumberTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getMRByNumberNullableTest() {
    var versionNumber = RandomString.make();
    var testVersion = "project:+" + versionNumber;

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    Mockito.when(request.get()).thenReturn(new ArrayList<>());

    Assertions.assertThatCode(()-> gerritService.getMRByNumber(versionNumber))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void getMRByNumberTest() {
    var changeId = RandomString.make();
    var versionNumber = "10";
    var testVersion = "project:+" + versionNumber;
    var info = new ChangeInfo();
    info._number = 10;
    info.mergeable = true;
    changeInfo.changeId = changeId;

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    MergeableInfo mergeableInfo = Mockito.mock(MergeableInfo.class);
    mergeableInfo.mergeable = false;
    Mockito.when(changes.id(changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(info);
    Mockito.when(changeApi.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);
    Mockito.when(request.get()).thenReturn(changeInfos);

    Assertions.assertThat(info.mergeable).isTrue();
    ChangeInfoDto result = gerritService.getMRByNumber(versionNumber);

    Assertions.assertThat(result.getMergeable()).isFalse();
    Assertions.assertThat(result.getNumber()).isEqualTo(String.valueOf(info._number));
  }

  @Test
  @SneakyThrows
  void mrByNumberNotFoundTest() {
    var versionNumber = "10";
    var testVersion = "project:+" + versionNumber;
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getMRByNumber(versionNumber))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void mrByNumberHttpExceptionTest() {
    var versionNumber = "10";
    var testVersion = "project:+" + versionNumber;
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getMRByNumber(versionNumber))
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void mrByNumberRestApiExceptionTest() {
    var versionNumber = "10";
    var testVersion = "project:+" + versionNumber;
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getMRByNumber(versionNumber))
        .isInstanceOf(GerritCommunicationException.class);
  }
}
