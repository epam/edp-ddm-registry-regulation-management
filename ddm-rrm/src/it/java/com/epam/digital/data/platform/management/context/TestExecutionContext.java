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

package com.epam.digital.data.platform.management.context;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.dto.TestFileDatesDto;
import com.epam.digital.data.platform.management.dto.TestVersionCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class TestExecutionContext {

  @Qualifier("test-directory")
  private final File testDirectory;
  private final GerritPropertiesConfig gerritProps;
  @Qualifier("gerritMockServer")
  private final WireMockServer gerritMockServer;

  private TestVersionCandidate versionCandidate;

  @PostConstruct
  public void init() {
    stubGerritCommon();
  }

  public File getHeadRepo() {
    return getRepo(gerritProps.getHeadBranch());
  }

  @SneakyThrows
  public String createVersionCandidate() {
    return createVersionCandidate(TestVersionCandidate.builder().build());
  }

  @SneakyThrows
  public String createVersionCandidate(TestVersionCandidate testVersionCandidate) {
    versionCandidate = testVersionCandidate;
    final var versionCandidateId = String.valueOf(versionCandidate.getNumber());

    try (final var git = Git.open(getHeadRepo())) {
      git.checkout()
          .setName(String.format("%s_ref", versionCandidateId))
          .setCreateBranch(true)
          .call();

      final var commit = git.commit()
          .setMessage(testVersionCandidate.getTopic())
          .setInsertChangeId(true)
          .setAllowEmpty(true)
          .call();
      final var message = commit.getFullMessage();
      final var changeId = message.split("Change-Id: ")[1].replace("\n", "");
      testVersionCandidate.setId(changeId);
      testVersionCandidate.setChangeId(changeId);

      git.checkout()
          .setName(gerritProps.getHeadBranch())
          .call();
    }

    stubGerritQueryChangesByNumber(versionCandidateId);
    stubGerritGetChangeById(testVersionCandidate.getChangeId());
    stubGerritGetChangeById(versionCandidateId);

    return versionCandidateId;
  }

  @SneakyThrows
  public String mockVersionCandidateDoesNotExist() {
    final var versionCandidate = new java.util.Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidate);

    gerritMockServer.addStubMapping(stubFor(
        get(String.format("/a/changes/?q=project:%s+%s", gerritProps.getRepository(),
            versionCandidateId)
        ).willReturn(
            aResponse()
                .withStatus(200)
                .withBody("[]")))
    );

    return versionCandidateId;
  }

  @SneakyThrows
  public void addFileToHeadRepo(String path, String content) {
    final var headRepo = getHeadRepo();
    final var fullPath = Path.of(headRepo.getPath(), path);
    Files.writeString(fullPath, content);

    try (final var git = Git.open(headRepo)) {
      git.add().addFilepattern(".").call();

      final var message = String.format("added file %s to head repo", path);
      git.commit().setMessage(message).call();
    }
  }

  @SneakyThrows
  public void addFileToVersionCandidateRemote(String path, String content) {
    final var headRepo = getHeadRepo();
    final var fullPath = Path.of(headRepo.getPath(), path);

    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      Files.writeString(fullPath, content);
      git.add().addFilepattern(".").call();

      final var message = git.log().call().iterator().next().getFullMessage();
      git.commit().setMessage(message).setAmend(true).call();
      git.checkout().setName(gerritProps.getHeadBranch()).call();
    }
  }

  @SneakyThrows
  public void deleteFileFromVersionCandidateRemote(String path) {
    final var headRepo = getHeadRepo();
    final var fullPath = Path.of(headRepo.getPath(), path);

    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      Files.deleteIfExists(fullPath);
      git.rm().addFilepattern(path).call();

      final var message = git.log().call().iterator().next().getFullMessage();
      git.commit().setMessage(message).setAmend(true).call();
      git.checkout().setName(gerritProps.getHeadBranch()).call();
    }
  }

  @SneakyThrows
  public String getFileFromRemoteVersionCandidateRepo(String path) {
    final var headRepo = getHeadRepo();
    final var fullFilePath = Path.of(headRepo.getPath(), path);
    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      final var fileContent = Files.readString(fullFilePath);
      git.checkout().setName(gerritProps.getHeadBranch()).call();
      return fileContent;
    }
  }

  @SneakyThrows
  public boolean isFileExistsInRemoteVersionCandidateRepo(String path) {
    final var headRepo = getHeadRepo();
    final var fullFilePath = Path.of(headRepo.getPath(), path);
    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      final var fileExists = Files.exists(fullFilePath);
      git.checkout().setName(gerritProps.getHeadBranch()).call();
      return fileExists;
    }
  }

  @SneakyThrows
  public String getResourceContent(String resourcePath) {
    final var resource = this.getClass().getResourceAsStream(resourcePath);
    if (Objects.isNull(resource)) {
      final var message = String.format("Resource with path %s doesn't exist", resourcePath);
      throw new IllegalArgumentException(message);
    }
    return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
  }

  @SneakyThrows
  public TestFileDatesDto getHeadRepoDatesByPath(String path) {
    try (final var git = Git.open(getHeadRepo())) {
      final var commitsIterable = git.log()
          .addPath(path)
          .call();
      final var commitsList = StreamSupport.stream(commitsIterable.spliterator(), false)
          .collect(Collectors.toList());

      final var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

      final var firstCommitIndex = commitsList.size() - 1;
      final var firstCommit = commitsList.get(firstCommitIndex);
      final var created = LocalDateTime.ofInstant(
              Instant.ofEpochSecond(firstCommit.getCommitTime()), ZoneId.of("UTC"))
          .format(dateTimeFormatter);

      final var lastCommitIndex = 0;
      final var lastCommit = commitsList.get(lastCommitIndex);
      final var updated = LocalDateTime.ofInstant(
              Instant.ofEpochSecond(lastCommit.getCommitTime()), ZoneId.of("UTC"))
          .format(dateTimeFormatter);

      return TestFileDatesDto.builder()
          .created(created)
          .updated(updated)
          .build();
    }
  }

  public File getRepo(String repo) {
    return new File(testDirectory, repo);
  }

  private void stubGerritCommon() {
    gerritMockServer.addStubMapping(stubFor(
        get("/login/").willReturn(aResponse().withStatus(200))));
    gerritMockServer.addStubMapping(stubFor(
        post("/login/").willReturn(aResponse().withStatus(200))));
    gerritMockServer.addStubMapping(stubFor(
        get("/a/config/server/version").willReturn(aResponse().withStatus(200).withBody("3.3.2"))));
  }

  private void stubGerritQueryChangesByNumber(String versionCandidateId) {
    gerritMockServer.addStubMapping(stubFor(get(
            String.format("/a/changes/?q=project:%s+%s", gerritProps.getRepository(),
                versionCandidateId)
        ).willReturn(aResponse()
            .withStatus(200)
            .withBody(String.format("[{\"id\":\"%s\",\"change_id\":\"%s\",\"_number\":%d}]",
                versionCandidate.getId(), versionCandidate.getChangeId(),
                versionCandidate.getNumber()))))
    );
  }

  @SneakyThrows
  private void stubGerritGetChangeById(String id) {
    final var gerritDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    final var response = Map.ofEntries(
        Map.entry("id", versionCandidate.getId()),
        Map.entry("change_id", versionCandidate.getChangeId()),
        Map.entry("_number", versionCandidate.getNumber()),
        Map.entry("subject", versionCandidate.getSubject()),
        Map.entry("topic", versionCandidate.getTopic()),
        Map.entry("created", versionCandidate.getCreated().format(gerritDateFormat)),
        Map.entry("updated", versionCandidate.getUpdated().format(gerritDateFormat)),
        Map.entry("owner", Map.of("username", gerritProps.getUser())),
        Map.entry("current_revision", versionCandidate.getCurrentRevision()),
        Map.entry("revisions", Map.of(versionCandidate.getCurrentRevision(),
            Map.of("ref", versionCandidate.getRef()))),
        Map.entry("labels", Map.of())
    );
    gerritMockServer.addStubMapping(stubFor(get(urlPathEqualTo(String.format("/a/changes/%s", id))
        ).willReturn(aResponse()
            .withStatus(200)
            .withBody(new ObjectMapper().writeValueAsString(response))))
    );
    gerritMockServer.addStubMapping(
        stubFor(get(urlPathEqualTo(String.format("/a/changes/%s/revisions/current/mergeable", id))
        ).willReturn(aResponse()
            .withStatus(200)
            .withBody(String.format("{\"mergeable\":%s}", versionCandidate.isMergeable()))))
    );
    gerritMockServer.addStubMapping(
        stubFor(get(urlPathEqualTo(String.format("/a/changes/%s/revisions/current/files", id))
        ).willReturn(aResponse()
            .withStatus(200)
            .withBody("{}")))
    );
  }
}
