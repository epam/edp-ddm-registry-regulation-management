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

package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.context.TestExecutionContext;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Abstract integration test that is used for setuping test execution context for all integration
 * tests
 * <p>
 * <ul><lh>Sets up the {@link TestExecutionContext test execution context} that is used for stubbing gerrit
 * responces and git interaction:</lh>
 * <li>For gerrit integration sets up the WireMock server</li>
 * <li>For git integration uses
 * {@link com.epam.digital.data.platform.management.stub.JGitWrapperStub jgit wrapper stub} that
 * used {@link TestExecutionContext#getRemoteHeadRepo()} as remote repository for master version and
 * version candidates</li></ul>
 * <p>
 * The {@link TestExecutionContext#getRemoteHeadRepo() "remote" repository} and
 * {@link TestExecutionContext#getGerritMockServer() gerrit server} are cleaned before each testcase. So
 * that each test case is isolated.
 * <p>
 * Used spring profiles {@code local} for plain logging and {@code test} for seting up test
 * configuration such as disabling async and retry for strict expectations and setting repositories
 * folder in temporary space so there's won't be no leftovers on filesystem.
 *
 * @see TestExecutionContext
 * @see com.epam.digital.data.platform.management.stub
 */
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = RegistryRegulationManagementApplication.class)
public abstract class BaseIT {

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected TestExecutionContext context;
  protected final static Map<String, String> BPMN_NAMESPACES = Map.of(
      "bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL",
      "bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI",
      "dc", "http://www.omg.org/spec/DD/20100524/DC",
      "modeler", "http://camunda.org/schema/modeler/1.0");

  @BeforeEach
  @SneakyThrows
  void setUp() {
    if (!context.getTestDirectory().exists()) {
      Assertions.assertThat(context.getTestDirectory().mkdirs()).isTrue();
    }

    context.stubGerritCommon();
    context.resetRemoteRepo();
    context.resetHeadBranchDataBase();
    final var remoteHeadRepo = context.getRemoteHeadRepo();
    var headRepo = context.getHeadRepo();
    if (headRepo.exists()) {
      FileUtils.forceDelete(headRepo);
    }
    try (var ignored = Git.cloneRepository()
        .setDirectory(context.getHeadRepo())
        .setURI(remoteHeadRepo.getAbsolutePath())
        .call()) {
      //close ignored Git object
    }
  }

  @AfterEach
  @SneakyThrows
  void tearDown() {
    FileUtils.forceDelete(context.getTestDirectory());
    context.getGerritMockServer().resetAll();
    if (Objects.nonNull(context.getVersionCandidate())) {
      context.dropDataBase(String.valueOf(context.getVersionCandidate().getNumber()));
    }
  }
}
