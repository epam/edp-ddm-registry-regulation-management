/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.impl.JGitServiceImpl;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import com.epam.digital.data.platform.management.service.impl.RequestToFileConverter;
import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class JGitServiceTest {

  @TempDir
  private File tempDir;

  @InjectMocks
  private JGitServiceImpl jGitService;

  @Mock
  private JGitWrapper jGitWrapper;

  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;

  @Mock
  private Git git;
  @Mock
  private Repository repository;
  @Mock
  private CheckoutCommand checkoutCommand;
  @Mock
  private CloneCommand cloneCommand;
  @Mock
  private PullCommand pullCommand;
  @Mock
  private FetchCommand fetchCommand;

  @Mock
  private RequestToFileConverter requestToFileConverter;

  @Mock
  private RevTree revTree;

  @Mock
  private TreeWalk treeWalk;

  @Test
  @SneakyThrows
  void testCloneRepository() {
    File file = new File(tempDir, "version");
    file.createNewFile();
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(git.pull()).thenReturn(pullCommand);
    Mockito.when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    jGitService.cloneRepo("version");
    Mockito.verify(jGitWrapper, times(1)).open(file);
  }

  @Test
  @SneakyThrows
  void testCloneRepository1() {
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setURI(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCredentialsProvider(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setDirectory(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCloneAllBranches(true)).thenReturn(cloneCommand);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    jGitService.cloneRepo("version");
    Mockito.verify(jGitWrapper).cloneRepository();
  }

  @Test
  @SneakyThrows
  void testPull() {
    File file = new File(tempDir, "version");
    file.createNewFile();
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(git.pull()).thenReturn(pullCommand);
    Mockito.when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    jGitService.pull("version");
    Mockito.verify(jGitWrapper).open(file);
  }

  @Test
  @SneakyThrows
  void testPullDirectoryNotExist() {
    File file = new File("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);

    jGitService.pull("version");
    Mockito.verify(jGitWrapper, never()).open(file);
  }

  @Test
  @SneakyThrows
  void testFetch() {
    File file = new File(tempDir, "version");
    file.createNewFile();
    var changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs("refs for fetch");

    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(file)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs("refs for fetch")).thenReturn(fetchCommand);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("pass");
    Mockito.when(fetchCommand.setCredentialsProvider(
            refEq(new UsernamePasswordCredentialsProvider("user", "pass"))))
        .thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);

    jGitService.fetch("version", changeInfoDto);

    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @SneakyThrows
  void testFetchDirectoryNotExist() {
    var file = new File("/");

    jGitService.fetch("version", null);

    Mockito.verify(jGitWrapper, never()).open(file);
  }

  @Test
  @SneakyThrows
  void getFilesInPathTest() {
    File repo = new File(tempDir, "version");
    repo.createNewFile();
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository, "master")).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository, "/", revTree)).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    List<String> version = jGitService.getFilesInPath("version", "/");
    Assertions.assertNotNull(version);
    Assertions.assertNotEquals(0, version.size());
    Assertions.assertEquals("someFile", version.get(0));
  }

  @Test
  @SneakyThrows
  void getFilesInEmptyPathTest() {
    File repo = new File(tempDir, "version");
    repo.createNewFile();
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository, "master")).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    List<String> version = jGitService.getFilesInPath("version", "");
    Assertions.assertNotNull(version);
    Assertions.assertNotEquals(0, version.size());
    Assertions.assertEquals("someFile", version.get(0));
  }

  @Test
  @SneakyThrows
  void getFilesNoDirExists() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory())
        .thenReturn(tempDir.getPath() + "not_existed_path");
    List<String> version = jGitService.getFilesInPath("version", "");
    Assertions.assertNotNull(version);
    Assertions.assertEquals(0, version.size());
  }

  @Test
  @SneakyThrows
  void testAmendRepositoryNotExist() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory())
        .thenReturn(tempDir.getPath() + "not_existed_path");
    String amend = jGitService.amend(VersioningRequestDto.builder()
        .versionName("1")
        .build(), new ChangeInfoDto());
    Assertions.assertNull(amend);
  }

  @Test
  @SneakyThrows
  void testAmendEmptyInput() {
    File repo = new File(tempDir, "version");
    repo.createNewFile();
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs("refs")).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    var requestDto = VersioningRequestDto.builder().versionName("version").build();
    Mockito.when(requestToFileConverter.convert(requestDto)).thenReturn(null);

    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs("refs");
    String amend = jGitService.amend(requestDto, changeInfoDto);
    Assertions.assertNull(amend);
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void getFileContentRepoNotExistTest() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory())
        .thenReturn(tempDir.getPath() + "not_existed_path");
    String version = jGitService.getFileContent("version", "/");
    Assertions.assertEquals("Repository does not exist", version);
  }

  @Test
  @SneakyThrows
  void getFileContentPathEmptyTest() {
    File repo = new File(tempDir, "version");
    repo.createNewFile();
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    String version = jGitService.getFileContent("version", "");
    Assertions.assertEquals("Repository does not exist", version);

    version = jGitService.getFileContent("version", null);
    Assertions.assertEquals("Repository does not exist", version);

    Mockito.verify(jGitWrapper, times(2)).open(repo);
  }

  @Test
  @SneakyThrows
  void getFileContentEmptyTreeTest() {
    File repo = new File(tempDir, "version");
    repo.createNewFile();
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository, "master")).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(false);
    String version = jGitService.getFileContent("version", "/forms");
    Assertions.assertEquals("Repository does not exist", version);
    Mockito.verify(treeWalk).next();
  }

  @Test
  @SneakyThrows
  void deleteFalseTest() {
    File repo = new File(tempDir, "1");
    repo.createNewFile();

    ChangeInfoDto dto = new ChangeInfoDto();
    dto.setRefs("refs");
    dto.setNumber("1");
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs("refs")).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    String formNotDeleted = jGitService.delete(dto, "form");
    Assertions.assertNull(formNotDeleted);
    Mockito.verify(checkoutCommand).call();
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void deleteRepoNotExistTest() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory())
        .thenReturn(tempDir.getPath() + "not_existed_path");
    var changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setNumber("1");
    String formNotDeleted = jGitService.delete(changeInfoDto, "form");
    Assertions.assertNull(formNotDeleted);
  }

}
