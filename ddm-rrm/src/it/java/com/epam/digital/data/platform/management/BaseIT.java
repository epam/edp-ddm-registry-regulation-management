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

import com.epam.digital.data.platform.management.context.TestExecutionContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
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
import org.springframework.util.FileSystemUtils;

@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = UserImportApplication.class)
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

  @BeforeAll
  @SneakyThrows
  static void setUpStatic() {
    var path = System.getProperty("gerrit.repository-directory");
    if (Objects.isNull(path)) {
      final var tempRepoDirectory = Files.createTempDirectory("testDirectory").toFile();
      System.setProperty("gerrit.repository-directory", tempRepoDirectory.getPath());
    }
  }

  @BeforeEach
  @SneakyThrows
  void setUp() {
    if (!context.getTestDirectory().exists()) {
      Assertions.assertTrue(context.getTestDirectory().mkdirs());
    }

    final var headDir = context.getHeadRepo();
    try (var git = Git.init()
        .setInitialBranch(context.getGerritProps().getHeadBranch())
        .setDirectory(headDir)
        .call()) {
      // init head repo
      FileSystemUtils.copyRecursively(Path.of(ClassLoader.getSystemResource("baseRepo").toURI()),
          headDir.toPath());
      git.add().addFilepattern(".").call();
      git.commit().setMessage("added folder structure").call();
    }
  }

  @AfterEach
  @SneakyThrows
  void tearDown() {
    FileUtils.forceDelete(context.getTestDirectory());
  }
}
