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

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceLastMergedTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getLastMergedMR() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(changeInfos);
    Mockito.when(changes.id(changeInfo.id)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
    var dto = new ChangeInfoDto();
    dto.setNumber("5");
    var result = gerritService.getLastMergedMR();

    assertThat(result).hasFieldOrPropertyWithValue("number", dto.getNumber());

    Mockito.verify(request).get();
    Mockito.verify(request).withLimit(1);
    Mockito.verify(changes).query("project:repo+status:merged+owner:user");
    Mockito.verify(gerritPropertiesConfig).getRepository();
  }

  @Test
  @SneakyThrows
  void getLastMergedMR_noMergedMRs() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(List.of());

    var result = gerritService.getLastMergedMR();

    Assertions.assertThat(result).isNull();

    Mockito.verify(request).get();
    Mockito.verify(request).withLimit(1);
    Mockito.verify(changes).query("project:repo+status:merged+owner:user");
    Mockito.verify(gerritPropertiesConfig).getRepository();
  }

  @Test
  @SneakyThrows
  void notFoundTest() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getLastMergedMR())
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void httpExceptionTest() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getLastMergedMR())
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getLastMergedMR())
        .isInstanceOf(GerritCommunicationException.class);
  }
}
