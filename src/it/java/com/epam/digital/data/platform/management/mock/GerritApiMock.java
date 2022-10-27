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
package com.epam.digital.data.platform.management.mock;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.google.gerrit.extensions.api.accounts.AccountInput;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.Changes.QueryRequest;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GerritApiMock {

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Getter
  private final GerritApiImpl gerritApi = Mockito.mock(GerritApiImpl.class);
  private final Changes changes = Mockito.mock(Changes.class);
  private static final String CODE_REVIEW_LABEL = "Code-Review";
  private static final short CODE_REVIEW_VALUE = 2;
  private static final String VERIFIED_LABEL = "Verified";
  private static final short VERIFIED_VALUE = 1;

  public void init() {
    Mockito.when(gerritApi.changes()).thenReturn(changes);
  }

  @SneakyThrows
  public void mockGetLastMergedQuery(@Nullable ChangeInfo changeInfo) {
    var query = Mockito.mock(QueryRequest.class);
    var queryString = String.format("project:%s+status:merged+owner:%s",
        gerritPropertiesConfig.getRepository(), gerritPropertiesConfig.getUser());
    Mockito.when(changes.query(queryString)).thenReturn(query);
    Mockito.when(query.withLimit(1)).thenReturn(query);
    if (Objects.isNull(changeInfo)) {
      Mockito.when(query.get()).thenReturn(List.of());
      return;
    }
    if (Objects.isNull(changeInfo.changeId)) {
      changeInfo.changeId = UUID.randomUUID().toString();
    }
    Mockito.when(query.get()).thenReturn(List.of(changeInfo));

    var changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(changeInfo.changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(changeInfo);
  }

  @SneakyThrows
  public void mockGetVersionsList(@Nullable List<ChangeInfo> changeInfoList) {
    var query = Mockito.mock(QueryRequest.class);
    var queryString = String.format("project:%s+status:open+owner:%s",
        gerritPropertiesConfig.getRepository(), gerritPropertiesConfig.getUser());
    Mockito.when(changes.query(queryString)).thenReturn(query);
    if (Objects.isNull(changeInfoList)) {
      Mockito.when(query.get()).thenReturn(List.of());
      return;
    }
    var mergeableInfo = new MergeableInfo();
    mergeableInfo.mergeable = true;

    for (ChangeInfo changeInfo : changeInfoList) {
      ChangeApi changeApi = Mockito.mock(ChangeApi.class);
      final var revisionApi = Mockito.mock(RevisionApi.class);
      Mockito.when(changes.id(changeInfo.changeId)).thenReturn(changeApi);
      Mockito.when(changeApi.get()).thenReturn(changeInfo);
      Mockito.when(changeApi.current()).thenReturn(revisionApi);
      Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);

      if (Objects.isNull(changeInfo.changeId)) {
        changeInfo.changeId = UUID.randomUUID().toString();
      }
    }
    Mockito.when(query.get()).thenReturn(changeInfoList);
  }

  @SneakyThrows
  public void mockGetChangeInfo(final String number, @Nullable final ChangeInfo changeInfo) {
    final var repo = gerritPropertiesConfig.getRepository();
    final var queryString = String.format("project:%s+%s", repo, number);

    final var query = Mockito.mock(QueryRequest.class);
    Mockito.when(changes.query(queryString)).thenReturn(query);
    if (Objects.isNull(changeInfo)) {
      Mockito.when(query.get()).thenReturn(List.of());
      return;
    }
    if (Objects.isNull(changeInfo.changeId)) {
      changeInfo.changeId = UUID.randomUUID().toString();
    }
    Mockito.when(query.get()).thenReturn(List.of(changeInfo));

    final var changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(changeInfo.changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(changeInfo);

    final var revisionApi = Mockito.mock(RevisionApi.class);
    Mockito.when(changeApi.current()).thenReturn(revisionApi);

    final var mergeableInfo = new MergeableInfo();
    mergeableInfo.mergeable = changeInfo.mergeable;
    Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);
  }

  @SneakyThrows
  public void mockSubmit(final String versionName) {
    final var changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(versionName)).thenReturn(changeApi);

    final var revisionApi = Mockito.mock(RevisionApi.class);
    Mockito.when(changeApi.current()).thenReturn(revisionApi);

    final var reviewInput = new ReviewInput()
        .reviewer(gerritPropertiesConfig.getUser(), ReviewerState.REVIEWER, true)
        .label(CODE_REVIEW_LABEL, CODE_REVIEW_VALUE)
        .label(VERIFIED_LABEL, VERIFIED_VALUE)
        .setReady(true);
    final var reviewResult = new ReviewResult();
    reviewResult.ready = true;
    Mockito.when(revisionApi.review(Mockito.refEq(reviewInput, "reviewers")))
        .thenReturn(reviewResult);
  }

  @SneakyThrows
  public void mockCreateChanges(ChangeInfo info, CreateVersionRequest subject) {
    ChangeInput changeInput = initChangeInput(subject);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.create(Mockito.refEq(changeInput, "author"))).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(info);
  }

  @SneakyThrows
  public void mockGetChangesInMr(String changeId, Map<String, FileInfo> changedFiles) {
    final var revisionApi = changes.id(changeId).current();
    Mockito.when(revisionApi.files()).thenReturn(changedFiles);
  }

  @SneakyThrows
  public void mockNotFound(String changeVersion) {
    Mockito.when(gerritApi.changes()).thenReturn(changes);
    Mockito.when(changes.id(changeVersion)).thenThrow(new GerritChangeNotFoundException("Message"));
  }

  private ChangeInput initChangeInput(CreateVersionRequest subject) {
    subject.setName(subject.getName());
    subject.setDescription(subject.getDescription());
    ChangeInput changeInput = new ChangeInput();
    changeInput.subject = subject.getName();
    changeInput.status = ChangeStatus.NEW;
    changeInput.topic = subject.getDescription();
    changeInput.project = gerritPropertiesConfig.getRepository();
    changeInput.branch = gerritPropertiesConfig.getHeadBranch();
    AccountInput accountInput = new AccountInput();
    accountInput.username = gerritPropertiesConfig.getUser();
    accountInput.name = gerritPropertiesConfig.getUser();
    accountInput.email = gerritPropertiesConfig.getUser();
    accountInput.httpPassword = gerritPropertiesConfig.getPassword();
    changeInput.author = accountInput;
    return changeInput;
  }

  public void resetAll() {
    Stream.of(gerritApi, changes)
        .filter(Objects::nonNull)
        .forEach(Mockito::reset);
  }
}