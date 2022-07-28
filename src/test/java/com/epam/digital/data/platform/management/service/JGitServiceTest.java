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

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.impl.JGitServiceImpl;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import com.epam.digital.data.platform.management.service.impl.RequestToFileConverter;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
public class JGitServiceTest {

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
    File file = new File("/");
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(git.pull()).thenReturn(pullCommand);
    Mockito.when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(eq(true))).thenReturn(pullCommand);
    jGitService.cloneRepo("version");
    Mockito.verify(jGitWrapper, times(1)).open(file);
  }

  @Test
  @SneakyThrows
  void testCloneRepositor1y() {
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setURI(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCredentialsProvider(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setDirectory(any())).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCloneAllBranches(eq(true))).thenReturn(cloneCommand);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    jGitService.cloneRepo("version");
    Mockito.verify(jGitWrapper, times(1)).cloneRepository();
  }

  @Test
  @SneakyThrows
  void testPull() {
    File file = new File("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(git.pull()).thenReturn(pullCommand);
    Mockito.when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(eq(true))).thenReturn(pullCommand);
    jGitService.pull("version");
    Mockito.verify(jGitWrapper, times(1)).open(file);
  }

  @Test
  @SneakyThrows
  void testPullDirectoryNotExist() {
    File file = new File("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);

    jGitService.pull("version");
    Mockito.verify(jGitWrapper, times(0)).open(file);
  }

  @Test
  @SneakyThrows
  void getFilesInPathTest(){
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(jGitWrapper.getRevTree(any(),any())).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(any(),any(), any())).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(any())).thenReturn(treeWalk);
    Mockito.when(treeWalk.getPathString()).thenReturn("version");
    List<String> version = jGitService.getFilesInPath("version", "/");
    Assertions.assertNotNull(version);

  }

  @Test
  @SneakyThrows
  void getFilesInEmptyPathTest(){
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(jGitWrapper.getRevTree(any(),any())).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(any())).thenReturn(treeWalk);
    List<String> version = jGitService.getFilesInPath("version", "");
    Assertions.assertNotNull(version);
  }

  @Test
  @SneakyThrows
  void testAmendRepositoryNotExist() {
    String amend = jGitService.amend(VersioningRequestDto.builder().build(), new ChangeInfoDto());
    Assertions.assertNull(amend);
  }

  @Test
  @SneakyThrows
  void testAmendEmptyInput() {
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(anyString())).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(requestToFileConverter.convert(any())).thenReturn(null);

    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs("refs");
    String amend = jGitService.amend(VersioningRequestDto.builder().build(), changeInfoDto);
    Assertions.assertNull(amend);
  }

  @Test
  @SneakyThrows
  void getFileContentRepoNotExistTest() {
    String version = jGitService.getFileContent("version", "/");
    Assertions.assertEquals("Repository does not exist", version);
  }

  @Test
  @SneakyThrows
  void getFileContentPathEmptyTest() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    String version = jGitService.getFileContent("version", "");
    Assertions.assertEquals("Repository does not exist", version);

    version = jGitService.getFileContent("version", null);
    Assertions.assertEquals("Repository does not exist", version);
  }

  @Test
  @SneakyThrows
  void getFileContentEmptyTreeTest() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(jGitWrapper.getRevTree(any(),any())).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(any())).thenReturn(treeWalk);
    String version = jGitService.getFileContent("version", "/forms");
    Assertions.assertEquals("Repository does not exist", version);
  }

  @Test
  @SneakyThrows
  void deleteFalseTest(){
    ChangeInfoDto dto = new ChangeInfoDto();
    dto.setRefs("refs");
    dto.setNumber("1");
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(anyString())).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn("/");
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    String formNotDeleted = jGitService.delete(dto, "form");
    Assertions.assertNull(formNotDeleted);
  }

  @Test
  @SneakyThrows
  void deleteRepoNotExistTest(){
    String formNotDeleted = jGitService.delete(new ChangeInfoDto(), "form");
    Assertions.assertNull(formNotDeleted);
  }

}
