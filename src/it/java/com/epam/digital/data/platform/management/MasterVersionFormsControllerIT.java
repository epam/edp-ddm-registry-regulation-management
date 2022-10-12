package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.dto.TestFormDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FormDetailsShort;
import com.epam.digital.data.platform.management.service.JGitService;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.epam.digital.data.platform.management.config.CacheCustomizer.DATE_CACHE_NAME;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MasterVersionFormsControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/master/forms";
  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private JGitService jGitService;

  @Test
  @SneakyThrows
  public void getForm() {
    String versionCandidateId = "head-branch";
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    var formDetails = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);

    jGitWrapperMock.mockCloneMasterCommand();
    jGitWrapperMock.mockGetForm(formDetails);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + "/{formName}", formName)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.name", is(formDetails.getName())),
        jsonPath("$.title", is(formDetails.getTitle()))
    );
  }

  @Test
  @SneakyThrows
  public void getForms() {
    String versionCandidateId = "head-branch";
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "id1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    var list = new ArrayList<TestFormDetailsShort>();
    list.add(initFormDetails("name", "title", "{\"name\":\"name\", \"title\":\"title\"}"));
    list.add(initFormDetails("name2", "title2", "{\"name\":\"name2\", \"title\":\"title2\"}"));

    jGitWrapperMock.mockCloneMasterCommand();
    jGitWrapperMock.mockGetFormsList(list);
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    jGitWrapperMock.mockLogCommand();
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$[0].name", is("name")),
        jsonPath("$[0].title", is("title")),
        jsonPath("$[1].name", is("name2")),
        jsonPath("$[1].title", is("title2"))
    );
  }

  @Test
  @SneakyThrows
  void testDatesInCache() {
    jGitWrapperMock.mockGetFileInPath();
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/master/forms")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.[0].name", is("someFile")),
            jsonPath("$.[0].title", is("title")),
            jsonPath("$.[0].created", is("1970-01-01T00:00:00.000Z")),
            jsonPath("$.[0].updated", is("1970-01-01T00:00:00.000Z")));
    Cache dates = cacheManager.getCache(DATE_CACHE_NAME);
    assertThat(dates).isNotNull();
    SimpleKey cacheKey = new SimpleKey("head-branch", "forms/someFile");
    Cache.ValueWrapper valueWrapper = dates.get(cacheKey);
    assertThat(valueWrapper).isNotNull();

    jGitService.formDatesCacheEvict();
    dates = cacheManager.getCache(DATE_CACHE_NAME);
    assertThat(dates.get(cacheKey)).isNull();
  }

}
