package com.epam.digital.data.platform.management.mock;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.Changes.QueryRequest;
import com.google.gerrit.extensions.common.ChangeInfo;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;
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
  private GerritApi gerritApi;
  private Changes changes;

  @PostConstruct
  public void init() {
    gerritApi = Mockito.mock(GerritApi.class);
    changes = Mockito.mock(Changes.class);
    Mockito.when(gerritApi.changes()).thenReturn(changes);
  }

  @SneakyThrows
  public void mockGetLastMergedQuery(@Nullable ChangeInfo changeInfo) {
    var query = Mockito.mock(QueryRequest.class);
    var queryString = String.format("project:%s+status:merged",
        gerritPropertiesConfig.getRepository());
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
}
