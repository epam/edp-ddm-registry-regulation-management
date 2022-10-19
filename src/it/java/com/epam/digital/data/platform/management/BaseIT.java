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

package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.mock.GerritApiMock;
import com.epam.digital.data.platform.management.mock.JGitWrapperMock;
import com.epam.digital.data.platform.management.util.InitialisationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = UserImportApplication.class)
public abstract class BaseIT {

  protected static File tempRepoDirectory;

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected JGitWrapperMock jGitWrapperMock;
  @Autowired
  protected GerritApiMock gerritApiMock;
  @Autowired
  protected GerritPropertiesConfig gerritPropertiesConfig;
  @Autowired
  protected ObjectMapper objectMapper;

  protected final String testProcessFormat = getResource("test-process-format.bpmn");
  protected final String businessProcess = getResource("test-process.bpmn");
  protected final static Map<String, String> BPMN_NAMESPACES = Map.of(
      "bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL",
      "bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI",
      "dc", "http://www.omg.org/spec/DD/20100524/DC",
      "modeler", "http://camunda.org/schema/modeler/1.0");

  @BeforeAll
  static void setUpStatic() throws IOException {
    var path = System.getProperty("gerrit.repository-directory");
    if (Objects.nonNull(path)) {
      tempRepoDirectory = new File(path);
    } else {
      tempRepoDirectory = Files.createTempDirectory("testDirectory").toFile();
      System.setProperty("gerrit.repository-directory", tempRepoDirectory.getPath());
    }
  }

  @BeforeEach
  @SneakyThrows
  void setUp() {
    jGitWrapperMock.init();
    gerritApiMock.init();
    InitialisationUtils.createTempRepo(gerritPropertiesConfig.getHeadBranch());
  }

  @SneakyThrows
  private String getResource(String resourcePath) {
    return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourcePath),
        StandardCharsets.UTF_8);
  }

  @AfterEach
  @SneakyThrows
  void tearDown() {
    FileUtils.deleteDirectory(tempRepoDirectory);
    Assertions.assertTrue(tempRepoDirectory.mkdirs());
    jGitWrapperMock.resetAll();
    gerritApiMock.resetAll();
  }
}
