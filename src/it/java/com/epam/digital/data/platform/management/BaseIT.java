package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.mock.GerritApiMock;
import com.epam.digital.data.platform.management.mock.JGitWrapperMock;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
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

  @TempDir
  private static File tempRepoDirectory;

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected JGitWrapperMock jGitWrapperMock;
  @Autowired
  protected GerritApiMock gerritApiMock;

  @BeforeAll
  static void setUpStatic() {
    System.setProperty("gerrit.repository-directory", tempRepoDirectory.getPath());
  }

  @BeforeEach
  @SneakyThrows
  void setUp() {
  }

  @AfterEach
  @SneakyThrows
  void tearDown() {
    FileUtils.deleteDirectory(tempRepoDirectory);
    Assertions.assertTrue(tempRepoDirectory.mkdirs());

  }
}
