/*
 * Copyright 2023 EPAM Systems.
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

import com.epam.digital.data.platform.management.config.DataSourceConfigurationProperties;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import com.epam.digital.data.platform.management.dto.TestFileDatesDto;
import com.epam.digital.data.platform.management.dto.TestVersionCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gerrit.extensions.common.LabelInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
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
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/**
 * Test execution context that is accessible from every test case.
 * <p>
 * Used for setting up the gerrit stubs and setting up the remote repository state for test case
 * <p>
 * After creating the bean it's stubs common gerrit stuff such as authentication and getting gerrit
 * version. Also sets up the remote repository for cloning it by
 * {@link com.epam.digital.data.platform.management.core.event.ApplicationStartedEventListener} on
 * application starting
 */
@Getter
@Component
@RequiredArgsConstructor
public class TestExecutionContext {

  /**
   * Directory that is used as remote repository for all clones in application. Can be recreated by
   * {@link TestExecutionContext#resetRemoteRepo()}
   */
  private File remoteHeadRepo;

  /**
   * Gerrit's properties config for accessing in each test case. Can be modified, but the test case
   * that is modifying it has to return the original state after test case completion.
   */
  private final GerritPropertiesConfig gerritProps;

  /**
   * Datasource's properties config for accessing in each test case. Can be modified, but the test
   * case that is modifying it has to return the original state after test case completion.
   */
  private final DataSourceConfigurationProperties dataSourceProps;

  /**
   * WireMock server for stubbing gerrit responses. Can be used for direct stubbing if there isn't
   * enough the stubbing methods.
   *
   * @see TestExecutionContext#createVersionCandidate(TestVersionCandidate)
   * @see TestExecutionContext#stubGerritCommon()
   * @see TestExecutionContext#stubGerritGetChangeById(String)
   * @see TestExecutionContext#stubGerritQueryChangesByNumber(String)
   */
  @Qualifier("gerritMockServer")
  private final WireMockServer gerritMockServer;

  /**
   * Used for storing the version-candidate stub info for test case. There can be only one
   * version-candidate per test case. Must be recreated in every test-case that is testing the
   * version-candidate scope
   */
  private TestVersionCandidate versionCandidate;

  /**
   * Instance of current running embedded postgres. Used for creating and accessing databases.
   *
   * @see TestExecutionContext#createDataBase(String)
   * @see TestExecutionContext#dropDataBase(String)
   * @see TestExecutionContext#prepareRegistryDataSource(String, String)
   */
  private EmbeddedPostgres embeddedPostgres;

  /**
   * Modifiable version of {@link VersionContextComponentManager}
   */
  private final TestVersionContextComponentManager versionContext;

  @PostConstruct
  @SneakyThrows
  public void init() {
    resetRemoteRepo();
    this.embeddedPostgres = EmbeddedPostgres.builder().start();
    resetHeadBranchDataBase();
  }

  /**
   * Used for recreating "remote repository"
   */
  @SneakyThrows
  public void resetRemoteRepo() {
    if (!Objects.isNull(remoteHeadRepo) && remoteHeadRepo.exists()) {
      FileUtils.forceDelete(remoteHeadRepo);
    }
    remoteHeadRepo = new File(gerritProps.getRepositoryDirectory(), "remote-repo");
    try (var git = Git.init()
        .setInitialBranch(gerritProps.getHeadBranch())
        .setDirectory(remoteHeadRepo)
        .call()) {
      // init head repo
      FileSystemUtils.copyRecursively(Path.of(ClassLoader.getSystemResource("baseRepo").toURI()),
          remoteHeadRepo.toPath());
      git.add().addFilepattern(".").call();
      git.commit().setMessage("added folder structure").call();
    }
  }

  /**
   * Drops and recreates database for master version
   */
  public void resetHeadBranchDataBase() {
    dropDataBase(gerritProps.getHeadBranch());
    createDataBase(gerritProps.getHeadBranch());
    prepareRegistryDataSource(gerritProps.getHeadBranch(), "liquibase/master-liquibase.xml");
  }

  /**
   * @return Head-branch repo that is representing a master version repository
   */
  public File getHeadRepo() {
    return getRepo(gerritProps.getHeadBranch());
  }

  /**
   * Used for creating version-candidate with random info
   *
   * @return version-candidate number
   *
   * @see TestExecutionContext#createVersionCandidate(TestVersionCandidate)
   * @see TestExecutionContext#mockVersionCandidateDoesNotExist()
   */
  @SneakyThrows
  public String createVersionCandidate() {
    return createVersionCandidate(TestVersionCandidate.builder().build());
  }

  /**
   * Used for creating version-candidate with predefined info. Creates a branch in the remote repo
   * that is used as ref for that version-candidate and stubs gerrit responses for it. Also creates
   * database for this version-candidate
   *
   * @return version-candidate number
   *
   * @see TestExecutionContext#createVersionCandidate()
   * @see TestExecutionContext#mockVersionCandidateDoesNotExist()
   */
  @SneakyThrows
  public String createVersionCandidate(TestVersionCandidate testVersionCandidate) {
    versionCandidate = testVersionCandidate;
    final var versionCandidateId = String.valueOf(versionCandidate.getNumber());

    try (final var git = Git.open(getRemoteHeadRepo())) {
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

    createDataBase(versionCandidateId);
    prepareRegistryDataSource(versionCandidateId, "liquibase/master-liquibase.xml");

    return versionCandidateId;
  }

  /**
   * Stubs gerrit responses for non existed version-candidate
   *
   * @return random version candidate number
   *
   * @see TestExecutionContext#createVersionCandidate()
   * @see TestExecutionContext#createVersionCandidate(TestVersionCandidate)
   */
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

  /**
   * Adds file to the head-branch to the remote repository. For
   * {@link TestExecutionContext#getHeadRepo() head-branch repository} it's needed to
   * {@link TestExecutionContext#pullHeadRepo() pull} first to see this changes in master version.
   *
   * @param path    new file path
   * @param content new file content
   */
  @SneakyThrows
  public void addFileToRemoteHeadRepo(String path, String content) {
    final var headRepo = getRemoteHeadRepo();
    final var fullPath = Path.of(headRepo.getPath(), path);
    Files.writeString(fullPath, content);

    try (final var git = Git.open(headRepo)) {
      git.add().addFilepattern(".").call();

      final var message = String.format("added file %s to head repo", path);
      git.commit().setMessage(message).call();
    }
  }

  /**
   * Adds file to the version-candidate branch to the remote repository. It's needed to
   * {@link TestExecutionContext#createVersionCandidate(TestVersionCandidate) create version
   * candidate first}.
   *
   * @param path    new file path
   * @param content new file content
   */
  @SneakyThrows
  public void addFileToVersionCandidateRemote(String path, String content) {
    final var headRepo = getRemoteHeadRepo();
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

  /**
   * Deletes file from the version-candidate branch from the remote repository. It's needed to
   * {@link TestExecutionContext#createVersionCandidate(TestVersionCandidate) create version
   * candidate first}.
   *
   * @param path file path to delete
   */
  @SneakyThrows
  public void deleteFileFromVersionCandidateRemote(String path) {
    final var headRepo = getRemoteHeadRepo();
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

  /**
   * Reads file from the version-candidate branch from the remote repository. It's needed to
   * {@link TestExecutionContext#createVersionCandidate(TestVersionCandidate) create version
   * candidate first}.
   *
   * @param path file path to read
   * @return file content
   */
  @SneakyThrows
  public String getFileFromRemoteVersionCandidateRepo(String path) {
    final var headRepo = getRemoteHeadRepo();
    final var fullFilePath = Path.of(headRepo.getPath(), path);
    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      final var fileContent = Files.readString(fullFilePath);
      git.checkout().setName(gerritProps.getHeadBranch()).call();
      return fileContent;
    }
  }

  /**
   * Checks if file exists in the version-candidate branch in the remote repository. It's needed to
   * {@link TestExecutionContext#createVersionCandidate(TestVersionCandidate) create version
   * candidate first}.
   *
   * @param path file path to read
   * @return true if file exists and false otherwise
   */
  @SneakyThrows
  public boolean isFileExistsInRemoteVersionCandidateRepo(String path) {
    final var headRepo = getRemoteHeadRepo();
    final var fullFilePath = Path.of(headRepo.getPath(), path);
    try (final var git = Git.open(headRepo)) {
      git.checkout().setName(String.format("%s_ref", versionCandidate.getNumber())).call();
      final var fileExists = Files.exists(fullFilePath);
      git.checkout().setName(gerritProps.getHeadBranch()).call();
      return fileExists;
    }
  }

  /**
   * Used for reading resource folder content
   *
   * @param resourcePath path of the resource to read
   * @return file content
   */
  @SneakyThrows
  public String getResourceContent(String resourcePath) {
    final var resource = this.getClass().getResourceAsStream(resourcePath);
    if (Objects.isNull(resource)) {
      final var message = String.format("Resource with path %s doesn't exist", resourcePath);
      throw new IllegalArgumentException(message);
    }
    return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
  }

  /**
   * Reads git dates (creation and last updating) from head branch from remote repository for a
   * specific path.
   *
   * @param path to find dates from git log
   * @return {@link TestFileDatesDto} representation of dates
   */
  @SneakyThrows
  public TestFileDatesDto getHeadRepoDatesByPath(String path) {
    try (final var git = Git.open(getRemoteHeadRepo())) {
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

  /**
   * Used for pull remote changes to the master version repository after changing the remote repo
   */
  @SneakyThrows
  public void pullHeadRepo() {
    try (var git = Git.open(getHeadRepo())) {
      git.pull().call();
    }
  }

  public File getRepo(String repo) {
    return new File(gerritProps.getRepositoryDirectory(), repo);
  }

  public File getTestDirectory() {
    return new File(gerritProps.getRepositoryDirectory());
  }

  /**
   * Stubs common gerrit requests such as login and get version
   */
  public void stubGerritCommon() {
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
        Map.entry("labels", Map.of("Verified", new LabelInfo()))
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

  /**
   * Creates database for specified version
   *
   * @param versionId id of version to create database for
   */
  @SneakyThrows
  public void createDataBase(String versionId) {
    var registryDs = getRegistryDataSource(versionId);
    try (var conn = embeddedPostgres.getPostgresDatabase().getConnection();
        var stmt = conn.createStatement()) {
      stmt.executeUpdate(String.format("CREATE DATABASE %s;", getRegistryDbName(versionId)));
    }
    versionContext.setComponent(versionId, RegistryDataSource.class, registryDs);
  }

  /**
   * Drops database for specified version
   *
   * @param versionId id of version to drop database for
   */
  @SneakyThrows
  public void dropDataBase(String versionId) {
    try (var conn = embeddedPostgres.getPostgresDatabase().getConnection();
        var stmt = conn.createStatement()) {
      stmt.executeUpdate(
          String.format("DROP DATABASE IF EXISTS %s WITH (FORCE);", getRegistryDbName(versionId)));
    }
  }

  /**
   * Prepares versions database with specified liquibase script
   *
   * @param versionId               id of version to prepare database for
   * @param liquibaseScriptLocation the location of the liquibase script
   */
  @SneakyThrows
  public void prepareRegistryDataSource(String versionId, String liquibaseScriptLocation) {
    var registryDs = getRegistryDataSource(versionId);

    var preparer = LiquibasePreparer.forClasspathLocation(liquibaseScriptLocation);
    preparer.prepare(registryDs);
  }

  private RegistryDataSource getRegistryDataSource(String versionId) {
    var registryDs = embeddedPostgres.getDatabase(dataSourceProps.getUsername(),
        getRegistryDbName(versionId));
    return DataSourceBuilder.derivedFrom(registryDs)
        .type(RegistryDataSource.class)
        .build();
  }

  private String getRegistryDbName(String versionId) {
    return String.format("%s_%s", dataSourceProps.getRegistryDataBase(),
        versionId.replace("-", "_"));
  }
}
