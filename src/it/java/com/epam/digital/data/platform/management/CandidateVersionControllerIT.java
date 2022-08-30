package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gerrit.extensions.common.ChangeInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;

import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfo;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CandidateVersionControllerIT extends BaseIT {
  @Test
  @SneakyThrows
  public void getVersionsList() {
    var changesInfoList = new ArrayList<ChangeInfo>();
    changesInfoList.add(initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    changesInfoList.add(initChangeInfo(2, "user", "user@epam.com", "user"));
    gerritApiMock.mockGetVersionsList(changesInfoList);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/candidates")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$", hasSize(2)),
            jsonPath("$[0].id", is("1"))
        );
  }

  @Test
  @SneakyThrows
  public void getVersionsList_noVersions() {
    var changesInfoList = new ArrayList<ChangeInfo>();
    gerritApiMock.mockGetVersionsList(changesInfoList);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/candidates")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            jsonPath("$", hasSize(0))
        );
  }

  @Test
  @SneakyThrows
  public void getVersionDetails() {
    String candidateId = "candidateId";
    gerritApiMock.mockGetMRByNumber(candidateId, initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/candidates/" + candidateId)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.id", is("1")),
            jsonPath("$.author", is("admin")),
            jsonPath("$.description", is("this is description for version candidate 1")),
            jsonPath("$.name", is("commit message")),
            jsonPath("$.validations", nullValue())
        );
  }

  @Test
  @SneakyThrows
  public void createNewVersion() {
    ObjectMapper mapper = new ObjectMapper();
    CreateVersionRequest subject = new CreateVersionRequest();
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    subject.setDescription("request description");
    subject.setName("request name");
    gerritApiMock.mockGetMRByNumber(String.valueOf(changeInfo._number), changeInfo);
    gerritApiMock.mockCreateChanges(changeInfo, subject);
    mockMvc.perform(MockMvcRequestBuilders.post("/versions/candidates").contentType(MediaType.APPLICATION_JSON_VALUE).content(mapper.writeValueAsString(subject))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isCreated(),
            content().contentType("application/json"),
            jsonPath("$.id", is("1")),
            jsonPath("$.author", is("admin")),
            jsonPath("$.description", is("this is description for version candidate 1")),
            jsonPath("$.name", is("commit message")),
            jsonPath("$.validations", nullValue())
        );
  }

  @Test
  @SneakyThrows
  public void getVersionDetails_noSuchVersion() {
    String candidateId = "notExistingCandidate";
    gerritApiMock.mockGetMRByNumber(candidateId, null);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/candidates/" + candidateId)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isInternalServerError());
  }

}
