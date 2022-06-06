/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.gerritintegration.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritConflictException;
import com.epam.digital.data.platform.management.gerritintegration.mapper.GerritMapper;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoShortDto;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.RobotCommentInputDto;
import com.google.gerrit.extensions.api.accounts.AccountInput;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.RebaseInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GerritServiceImpl implements GerritService {

  public static final String CODE_REVIEW_LABEL = "Code-Review";
  public static final short CODE_REVIEW_VALUE = 2;
  public static final String VERIFIED_LABEL = "Verified";
  public static final short VERIFIED_VALUE = 1;

  @Autowired
  private GerritPropertiesConfig gerritPropertiesConfig;

  @Autowired
  private GerritMapper gerritMapper;

  @Autowired
  private GerritApiImpl gerritApi;

  @Override
  public List<ChangeInfoShortDto> getMRList() {
    var query = String.format("project:%s+status:open+owner:%s",
        gerritPropertiesConfig.getRepository(), gerritPropertiesConfig.getUser());
    try {
      return gerritApi.changes().query(query).get().stream()
          .map(gerritMapper::toChangeInfoShortDto)
          .collect(Collectors.toList());
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidates", ex);
      } else {
        throw new GerritCommunicationException("Something went wrong wile getting candidates", ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException("Something went wrong wile getting candidates", ex);
    }
  }

  @Nullable
  @Override
  public ChangeInfoDto getLastMergedMR() {
    var query = String.format("project:%s+status:merged+owner:%s",
        gerritPropertiesConfig.getRepository(), gerritPropertiesConfig.getUser());
    var changes = gerritApi.changes();
    try {
      var changeInfoList = changes.query(query).withLimit(10).get();
      if (changeInfoList.isEmpty()) {
        return null;
      }
      var changeId = changeInfoList.stream()
          .max(Comparator.comparing(changeInfo -> changeInfo.submitted))
          .get().changeId;
      return gerritMapper.toChangeInfoDto(changes.id(changeId).get());
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found last merged change", ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile getting last merged change", ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException("Something went wrong wile getting last merged change",
          ex);
    }
  }

  @Override
  public List<String> getClosedMrIds() {
    String query = String.format("project:%s+status:closed+owner:%s",
        gerritPropertiesConfig.getRepository(), gerritPropertiesConfig.getUser());
    try {
      return gerritApi.changes().query(query).get().stream()
          .map(change -> String.valueOf(change._number)).collect(Collectors.toList());
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found any closed candidate", ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile getting closed candidates", ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile getting closed candidates", ex);
    }
  }

  @Override
  public ChangeInfoDto getMRByNumber(String number) {
    String query = String.format("project:%s+%s", gerritPropertiesConfig.getRepository(), number);
    Changes changes = gerritApi.changes();

    try {
      var changeInfos = changes.query(query).get();
      if (changeInfos.isEmpty()) {
        throw new GerritChangeNotFoundException("Could not get change info for " + number + " MR");
      }
      ChangeApi changeApi = changes.id(changeInfos.get(0).changeId);
      ChangeInfo changeInfo = changeApi.get();
      ChangeInfoDto changeInfoDto = gerritMapper.toChangeInfoDto(changeInfo);
      changeInfoDto.setMergeable(changeApi.current().mergeable().mergeable);
      String currentRevision = changeInfo.currentRevision;
      RevisionInfo revisionInfo = changeInfo.revisions.get(currentRevision);
      changeInfoDto.setRefs(revisionInfo.ref);
      return changeInfoDto;
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with number " + number,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile getting candidate with number " + number, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile getting candidate with number " + number, ex);
    }
  }

  @Override
  public ChangeInfoDto getChangeInfo(String changeId) {
    try {
      ChangeInfo changeInfo = gerritApi.changes().id(changeId).get();
      ChangeInfoDto changeInfoDto = gerritMapper.toChangeInfoDto(changeInfo);
      String currentRevision = changeInfo.currentRevision;
      RevisionInfo revisionInfo = changeInfo.revisions.get(currentRevision);
      changeInfoDto.setRefs(revisionInfo.ref);
      return changeInfoDto;
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile getting candidate with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile getting candidate with id " + changeId, ex);
    }
  }

  @Override
  public Map<String, FileInfoDto> getListOfChangesInMR(String changeId) {
    try {
      return gerritMapper.toFileDto(gerritApi.changes().id(changeId).current().files());
    } catch (HttpStatusException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId, e);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile getting changes in candidate with id " + changeId, ex);
    }
    return null;
  }

  @Override
  public String getFileContent(String changeId, String filePath) {
    try {
      if (changeId != null) {
        String currentRevision = gerritApi.changes().id(changeId).get().currentRevision;
        String request = String.format("/changes/%s/revisions/%s/files/%s/content", changeId,
            currentRevision, filePath.replace("/", "%2F"));
        JsonElement response = gerritApi.restClient().getRequest(request);
        return response.getAsString();
      }
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile getting file content from candidate with id " + changeId,
            ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile getting file content from candidate with id " + changeId, ex);
    }
    return null;
  }

  @Override
  public void submitChanges(String changeId) {
    try {
      gerritApi.changes().id(changeId).current().submit();
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      }
      if (ex.getStatusCode() == HttpStatus.CONFLICT.value()) {
        throw new GerritConflictException(
            "Failed to submit 1 change due to the following problems:\nChange" + changeId +
                ". Project policy requires all submissions to be a fast-forward. Please rebase the change locally and upload again for review");
      }
      throw new GerritCommunicationException(
          "Something went wrong wile submitting candidate with id " + changeId, ex);
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile submitting candidate with id " + changeId, ex);
    }
  }

  @Override
  public void deleteChanges(String changeId) {
    try {
      gerritApi.changes().id(changeId).delete();
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile deleting candidate with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile deleting candidate with id " + changeId, ex);
    }
  }

  @Override
  public String createChanges(CreateChangeInputDto createDto) {
    ChangeInput changeInput = new ChangeInput();
    changeInput.subject = createDto.getName();
    changeInput.status = ChangeStatus.NEW;
    changeInput.topic = createDto.getDescription();
    changeInput.project = gerritPropertiesConfig.getRepository();
    changeInput.branch = gerritPropertiesConfig.getHeadBranch();
    AccountInput accountInput = new AccountInput();
    accountInput.username = gerritPropertiesConfig.getUser();
    accountInput.name = gerritPropertiesConfig.getUser();
    accountInput.email = gerritPropertiesConfig.getUser();
    accountInput.httpPassword = gerritPropertiesConfig.getPassword();
    changeInput.author = accountInput;

    try {
      ChangeInfo changeInfo = gerritApi.changes().create(changeInput).get();
      return String.valueOf(changeInfo._number);
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException(
            "Could not create change with name " + createDto.getName(), ex);
      }
      throw new GerritCommunicationException(
          "Something went wrong while creating change with name " + createDto.getName(), ex);
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong while creating change with name " + createDto.getName(), ex);
    }

  }

  @Override
  public Boolean review(String changeId) {
    ReviewInput reviewInput = new ReviewInput();
    reviewInput.reviewer(gerritPropertiesConfig.getUser(), ReviewerState.REVIEWER, true);
    reviewInput.label(CODE_REVIEW_LABEL, CODE_REVIEW_VALUE);
    reviewInput.label(VERIFIED_LABEL, VERIFIED_VALUE);
    reviewInput.ready = true;
    try {
      ReviewResult review = gerritApi.changes().id(changeId).current().review(reviewInput);
      return review.ready;
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile reviewing candidate with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile reviewing candidate with id " + changeId, ex);
    }
  }

  @Override
  public void declineChange(String changeId) {
    try {
      gerritApi.changes().id(changeId).abandon();
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong wile declining candidate with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong wile declining candidate with id " + changeId, ex);
    }
  }

  @Override
  public void rebase(String changeId) {
    if (changeId != null) {
      String request = String.format("/changes/%s/rebase", changeId);
      Gson gson = new Gson();
      try {
        gerritApi.restClient()
            .postRequest(request, gson.toJson(new RebaseInput(), RebaseInput.class));
      } catch (HttpStatusException ex) {
        if (ex.getStatusCode() != HttpStatus.CONFLICT.value()) {
          throw new GerritCommunicationException(
              "Something went wrong while rebasing change on candidate with id " + changeId, ex);
        }
        log.info(ex.getMessage());
      } catch (RestApiException ex) {
        throw new GerritCommunicationException(
            "Something went wrong while rebasing change on candidate with id " + changeId, ex);
      }
    }
  }

  @Override
  public void robotComment(RobotCommentInputDto requestDto, String changeId) {
    ReviewInput reviewInput = new ReviewInput();
    if (requestDto.getComment() != null && !requestDto.getComment().isEmpty()) {
      Map<String, List<ReviewInput.RobotCommentInput>> robotComments = new HashMap<>();
      ReviewInput.RobotCommentInput robotCommentInput = new ReviewInput.RobotCommentInput();
      robotCommentInput.path = requestDto.getFilePath();
      robotCommentInput.robotId = requestDto.getRobotId();
      robotCommentInput.robotRunId = requestDto.getRobotRunId();
      robotCommentInput.message = requestDto.getComment();
      robotComments.put(robotCommentInput.path, Collections.singletonList(robotCommentInput));
      reviewInput.robotComments = robotComments;
    }
    reviewInput.message(requestDto.getMessage());

    try {
      gerritApi.changes().id(changeId).current().review(reviewInput);
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong while creating robot comment on candidate with id " + changeId,
            ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong while creating robot comment on candidate with id " + changeId, ex);
    }
  }

  @Override
  public String getTopic(String changeId) {
    try {
      return gerritApi.changes().id(changeId).topic();
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong while getting topic from candidate with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong while getting topic from candidate with id " + changeId, ex);
    }
  }

  @Override
  public void setTopic(String text, String changeId) {
    try {
      gerritApi.changes().id(changeId).topic(text);
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new GerritChangeNotFoundException("Could not found candidate with id " + changeId,
            ex);
      } else {
        throw new GerritCommunicationException(
            "Something went wrong while setting topic to change with id " + changeId, ex);
      }
    } catch (RestApiException ex) {
      throw new GerritCommunicationException(
          "Something went wrong while setting topic to change with id " + changeId, ex);
    }
  }
}
