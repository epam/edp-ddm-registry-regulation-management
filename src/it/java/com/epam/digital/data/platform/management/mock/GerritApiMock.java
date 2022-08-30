package com.epam.digital.data.platform.management.mock;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.accounts.AccountInput;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.Changes.QueryRequest;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.MergeableInfo;
import java.util.List;
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
  private final GerritApi gerritApi = Mockito.mock(GerritApi.class);
  private final Changes changes = Mockito.mock(Changes.class);

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
      RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
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
  public void mockGetMRByNumber(String number, @Nullable ChangeInfo changeInfo) {
    var query = Mockito.mock(QueryRequest.class);
    var queryString = String.format("project:%s+%s", gerritPropertiesConfig.getRepository(), number);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
    Mockito.when(changes.query(queryString)).thenReturn(query);
    if (Objects.isNull(changeInfo)) {
      Mockito.when(query.get()).thenReturn(List.of());
      return;
    }
    if (Objects.isNull(changeInfo.changeId)) {
      changeInfo.changeId = UUID.randomUUID().toString();
    }
    Mockito.when(query.get()).thenReturn(List.of(changeInfo));
    var mergeableInfo = new MergeableInfo();
    mergeableInfo.mergeable = true;
    Mockito.when(changes.id(changeInfo.changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(changeInfo);
    Mockito.when(changeApi.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);
  }

  @SneakyThrows
  public void mockCreateChanges(ChangeInfo info, CreateVersionRequest subject) {
    ChangeInput changeInput = initChangeInput(subject);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.create(Mockito.refEq(changeInput, "author"))).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(info);
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