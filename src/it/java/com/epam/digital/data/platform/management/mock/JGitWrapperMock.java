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
package com.epam.digital.data.platform.management.mock;

import static com.epam.digital.data.platform.management.util.InitialisationUtils.createTempRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FormDetailsShort;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import com.google.gson.Gson;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectLoader.SmallObject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JGitWrapperMock {

  private static final String FORM_CONTENT = "{\n" +
      "  \"type\": \"form\",\n" +
      "  \"components\": [\n" +
      "   ],\n" +
      "  \"title\": \"title\",\n" +
      "  \"path\": \"add-fizfactors1\",\n" +
      "  \"name\": \"add-fizfactors1\",\n" +
      "  \"display\": \"form\",\n" +
      "  \"submissionAccess\": []\n" +
      "}";

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Getter
  private final JGitWrapper jGitWrapper = Mockito.mock(JGitWrapper.class);
  @Getter
  private CloneCommand cloneCommand;
  @Getter
  private Git git;
  @Getter
  private Repository repository;
  private CheckoutCommand checkoutCommand;
  private RevTree revTree;
  private TreeWalk treeWalk;
  private TreeWalk dirWalk;
  private ObjectLoader loader;
  private final Gson gson = new Gson();

  public void init() {
    mockCloneCommand(gerritPropertiesConfig.getHeadBranch());
  }

  @SneakyThrows
  public void mockCloneMasterCommand() {
    mockCloneCommand(gerritPropertiesConfig.getHeadBranch());
  }

  @SneakyThrows
  public void mockCloneCommand(String versionName) {
    createTempRepo(versionName);
    ObjectId objectId = Mockito.mock(ObjectId.class);
    cloneCommand = Mockito.mock(CloneCommand.class);
    git = Mockito.mock(Git.class);
    repository = Mockito.mock(Repository.class);
    revTree = Mockito.mock(RevTree.class);
    treeWalk = Mockito.mock(TreeWalk.class);
    loader = Mockito.mock(ObjectLoader.class);
    dirWalk = Mockito.mock(TreeWalk.class);

    var repoURI = gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository();
    var credentialsProvider = new UsernamePasswordCredentialsProvider(
        gerritPropertiesConfig.getUser(), gerritPropertiesConfig.getPassword());
    var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(), versionName);

    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setURI(repoURI)).thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(cloneCommand);
    Mockito.when(cloneCommand.setCloneAllBranches(true)).thenReturn(cloneCommand);
    Mockito.doAnswer(invocationOnMock -> {
      log.info("Called clone command for {} version", versionName);
      directory.mkdirs();
      return null;
    }).when(cloneCommand).call();
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(jGitWrapper.open(directory)).thenReturn(git);
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    Mockito.when(loader.getCachedBytes())
        .thenReturn("This is our response".getBytes(StandardCharsets.UTF_8));
    Mockito.when(treeWalk.getObjectId(0)).thenReturn(objectId);
    Mockito.when(repository.open(any())).thenReturn(loader);
    Mockito.when(cloneCommand.setDirectory(any())).thenReturn(cloneCommand);
  }

  @SneakyThrows
  public void mockCommitCommand() {
    CommitCommand commitCommand = Mockito.mock(CommitCommand.class);
    RevCommit revCommit = Mockito.mock(RevCommit.class);
    Mockito.when(git.commit()).thenReturn(commitCommand);
    Mockito.when(commitCommand.setMessage(anyString())).thenReturn(commitCommand);
    Mockito.when(commitCommand.setAmend(true)).thenReturn(commitCommand);
    Mockito.when(commitCommand.call()).thenReturn(revCommit);
  }

  @SneakyThrows
  public void mockPullCommand() {
    PullCommand pullCommand = Mockito.mock(PullCommand.class);
    var credentialsProvider = new UsernamePasswordCredentialsProvider(
        gerritPropertiesConfig.getUser(), gerritPropertiesConfig.getPassword());
    Mockito.when(pullCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    Mockito.when(git.pull()).thenReturn(pullCommand);
  }

  @SneakyThrows
  public void mockFetchCommand(ChangeInfoDto changeInfoDto) {
    FetchCommand fetchCommand = Mockito.mock(FetchCommand.class);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(Mockito.refEq(changeInfoDto.getRefs())))
        .thenReturn(fetchCommand);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
  }

  @SneakyThrows
  public void mockLogCommand() {
    LogCommand logCommand = Mockito.mock(LogCommand.class);
    Mockito.when(git.log()).thenReturn(logCommand);
    var revCommit = Mockito.mock(RevCommit.class);
    var revCommit2 = Mockito.mock(RevCommit.class);
    Mockito.when(logCommand.addPath(anyString())).thenReturn(logCommand);
    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
  }

  @SneakyThrows
  public void mockAddCommand() {
    AddCommand addCommand = Mockito.mock(AddCommand.class);
    Mockito.when(git.add()).thenReturn(addCommand);
    Mockito.when(addCommand.addFilepattern(anyString())).thenReturn(addCommand);
  }

  @SneakyThrows
  public void mockRemoteAddCommand() {
    RemoteAddCommand remoteAddCommand = Mockito.mock(RemoteAddCommand.class);
    Mockito.when(git.remoteAdd()).thenReturn(remoteAddCommand);
    Mockito.when(remoteAddCommand.setUri(any(URIish.class))).thenReturn(remoteAddCommand);
  }

  @SneakyThrows
  public void mockStatusCommand() {
    StatusCommand statusCommand = Mockito.mock(StatusCommand.class);
    Status status = Mockito.mock(Status.class);
    Mockito.when(git.status()).thenReturn(statusCommand);
    Mockito.when(statusCommand.call()).thenReturn(status);
  }

  public void mockPushCommand() {
    PushCommand pushCommand = Mockito.mock(PushCommand.class);
    Mockito.when(git.push()).thenReturn(pushCommand);
  }

  @SneakyThrows
  public void mockCheckoutCommand() {
    checkoutCommand = Mockito.mock(CheckoutCommand.class);
    Mockito.when(checkoutCommand.setName("refs/heads/" + gerritPropertiesConfig.getHeadBranch()))
        .thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
  }

  @SneakyThrows
  public void mockGetForm(FormDetailsShort formDetails) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository))).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true);
    Mockito.when(loader.getBytes())
        .thenReturn((gson.toJson(formDetails).getBytes(StandardCharsets.UTF_8)));
  }

  @SneakyThrows
  public void mockGetFormsList(List<FormDetailsShort> list) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository), anyString(), eq(revTree)))
        .thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(dirWalk);
    Mockito.when(dirWalk.next()).thenAnswer(new Answer<Boolean>() {
      int count = 0;

      @Override
      public Boolean answer(InvocationOnMock invocation) {
        if (count++ < list.size()) {
          return true;
        }
        count = 0;
        return false;
      }
    });
    Mockito.when(dirWalk.getPathString()).thenAnswer(new Answer<String>() {
      int count = 0;

      @Override
      public String answer(InvocationOnMock invocation) {
        return "forms/" + list.get(count++).getName();
      }
    });

    Mockito.when(loader.getBytes()).thenAnswer(new Answer<byte[]>() {
      int count = 0;

      @Override
      public byte[] answer(InvocationOnMock invocation) {
        if (count < list.size()) {
          return gson.toJson(list.get(count++)).getBytes(StandardCharsets.UTF_8);
        }
        return null;
      }
    });
  }

  @SneakyThrows
  public void mockGetFileInPath() {
    var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(),
        gerritPropertiesConfig.getHeadBranch());
    var git = Mockito.mock(Git.class);
    Mockito.when(jGitWrapper.open(directory)).thenReturn(git);
    var repository = Mockito.mock(Repository.class);
    Mockito.when(git.getRepository()).thenReturn(repository);
    var revTree = Mockito.mock(RevTree.class);
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    var treeWalk = Mockito.mock(TreeWalk.class);
    Mockito.when(jGitWrapper.getTreeWalk(repository, "forms", revTree)).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false)
        .thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    var logCommand = Mockito.mock(LogCommand.class);
    Mockito.when(git.log()).thenReturn(logCommand);
    var revCommit = Mockito.mock(RevCommit.class);
    var revCommit2 = Mockito.mock(RevCommit.class);
    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
    SmallObject smallObject = new SmallObject(1, FORM_CONTENT.getBytes(StandardCharsets.UTF_8));
    Mockito.when(repository.open(any())).thenReturn(smallObject);
  }

  public void resetAll() {
    Stream.of(jGitWrapper, cloneCommand, git, repository, checkoutCommand, revTree, treeWalk,
            dirWalk, loader)
        .filter(Objects::nonNull)
        .forEach(Mockito::reset);
  }
}

