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

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.RobotCommentInputDto;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GerritServiceRobotCommentTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void robotCommentTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
    var comment = RandomString.make();
    var requestDto = new RobotCommentInputDto();
    requestDto.setComment(comment);
    gerritService.robotComment(requestDto, changeId);
    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentNullCommentTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
    RobotCommentInputDto requestDto = new RobotCommentInputDto();
    gerritService.robotComment(requestDto, changeId);
    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentEmptyCommentTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
    RobotCommentInputDto requestDto = new RobotCommentInputDto();
    requestDto.setComment(Strings.EMPTY);
    gerritService.robotComment(requestDto, changeId);
    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentHttpStatusExceptionTest() {
    var changeId = RandomString.make();
    Mockito.doThrow(HttpStatusException.class).when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.robotComment(new RobotCommentInputDto(), changeId))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentNotFoundExceptionTest() {
    var changeId = RandomString.make();
    Mockito.doThrow(new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""))
        .when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.robotComment(new RobotCommentInputDto(), changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentRestApiExceptionTest() {
    var changeId = RandomString.make();
    Mockito.doThrow(RestApiException.class).when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.robotComment(new RobotCommentInputDto(), changeId))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(any());
  }
}
