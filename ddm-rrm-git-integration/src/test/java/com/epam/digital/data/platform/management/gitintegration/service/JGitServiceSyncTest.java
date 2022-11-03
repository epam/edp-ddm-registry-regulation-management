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
package com.epam.digital.data.platform.management.gitintegration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
class JGitServiceSyncTest {

  @TempDir
  private File tempDir;

  @InjectMocks
  private JGitServiceImpl jGitService;

  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;
  @Mock
  private JGitWrapper jGitWrapper;
  @Mock
  private GitRetryable gitRetryable;
  @Mock
  private Git git;
  @Mock
  private Repository repository;
  @Mock
  private GitFileService gitFileService;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    Mockito.doCallRealMethod().when(gitRetryable).call(any());
  }

  @Test
  @SneakyThrows
  void resetHeadBranchToRemoteSyncTest() {
    var repoName = RandomString.make();
    var username = RandomString.make();
    var password = RandomString.make();

    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(username);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var fetchCountDownLatch = new CountDownLatch(2);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(username, password))))
        .thenReturn(fetchCommand);

    var resetCommand = Mockito.mock(ResetCommand.class);
    var resetCountDownLatch = new CountDownLatch(2);
    Mockito.when(git.reset()).thenReturn(resetCommand);
    Mockito.when(resetCommand.setMode(ResetType.HARD)).thenReturn(resetCommand);
    Mockito.when(resetCommand.setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName))
        .thenReturn(resetCommand);

    Mockito.when(fetchCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(50);
      var fetchCount = fetchCountDownLatch.getCount();
      var resetCount = resetCountDownLatch.getCount();
      Assertions.assertThat(fetchCount).isEqualTo(resetCount);
      fetchCountDownLatch.countDown();
      return null;
    });

    Mockito.when(resetCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(50);
      var fetchCount = fetchCountDownLatch.getCount();
      var resetCount = resetCountDownLatch.getCount();
      Assertions.assertThat(fetchCount).isEqualTo(resetCount - 1);
      resetCountDownLatch.countDown();
      return null;
    });

    var service = Executors.newFixedThreadPool(2);

    var firstReset = service.submit(() -> jGitService.resetHeadBranchToRemote());
    var secondReset = service.submit(() -> jGitService.resetHeadBranchToRemote());

    firstReset.get();
    Assertions.assertThat(firstReset.isDone()).isTrue();
    secondReset.get();
    Assertions.assertThat(secondReset.isDone()).isTrue();

    Assertions.assertThat(fetchCountDownLatch.getCount()).isEqualTo(0);
    Assertions.assertThat(resetCountDownLatch.getCount()).isEqualTo(0);

    Mockito.verify(fetchCommand, times(2)).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(username, password)));
    Mockito.verify(fetchCommand, times(2)).call();

    Mockito.verify(resetCommand, times(2)).setMode(ResetType.HARD);
    Mockito.verify(resetCommand, times(2)).setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName);
    Mockito.verify(resetCommand, times(2)).call();

    Mockito.verify(git, times(2)).close();
  }

  @Test
  @SneakyThrows
  void getFilesInPathSyncTest() {
    String version = "version";

    var file = new File(tempDir, version);
    Assertions.assertThat(file.createNewFile()).isTrue();

    String path = "forms";

    Counter counter = new Counter(3);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");

    RevTree tree = mock(RevTree.class);
    when(jGitWrapper.getRevTree(repository)).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving the rev tree for {}", Thread.currentThread().getName());
      counter.check(0);
      return tree;
    });
    TreeWalk treeWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository, path, tree)).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving the tree walker by revision for {}",
          Thread.currentThread().getName());
      counter.check(1);
      return treeWalk;
    });

    TreeWalk dirWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository)).thenReturn(dirWalk);

    ObjectId objectId = mock(ObjectId.class);
    when(treeWalk.getObjectId(0)).thenReturn(objectId);

    when(dirWalk.next()).thenReturn(true, false, true, false,
            true, false, true, false,
            true, false)
        .thenThrow(new RuntimeException("Called too many times!"));

    when(dirWalk.getPathString()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving a ingle file from the tree for {}",
          Thread.currentThread().getName());
      counter.check(2);
      return "file";
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.getFilesInPath(version, path);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertThat(counter.getCompletedIterations()).isEqualTo(numberOfThreads);
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void getFileContentSyncTest() {
    String version = "version";

    var file = new File(tempDir, version);
    Assertions.assertThat(file.createNewFile()).isTrue();

    String path = "forms";

    Counter counter = new Counter(3);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");

    RevTree tree = mock(RevTree.class);
    when(jGitWrapper.getRevTree(repository)).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving the rev tree for {}", Thread.currentThread().getName());
      counter.check(0);
      return tree;
    });
    TreeWalk treeWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository)).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving the tree walker by revision for {}",
          Thread.currentThread().getName());
      counter.check(1);
      return treeWalk;
    });

    when(treeWalk.next()).thenReturn(true);
    ObjectId objectId = mock(ObjectId.class);
    when(treeWalk.getObjectId(0)).thenReturn(objectId);

    ObjectLoader objectLoader = mock(ObjectLoader.class);

    when(objectLoader.getCachedBytes()).thenReturn("content".getBytes());
    when(objectLoader.getBytes()).thenReturn("content".getBytes());

    when(repository.open(objectId)).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called retrieving file content from the tree for {}",
          Thread.currentThread().getName());
      counter.check(2);
      return objectLoader;
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.getFileContent(version, path);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertThat(counter.getCompletedIterations()).isEqualTo(numberOfThreads);
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void amendSyncTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    final var filecontent = RandomString.make();

    var file = new File(tempDir, repositoryName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    Counter counter = new Counter(7);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    when(gerritPropertiesConfig.getUrl()).thenReturn("https://gerrit");
    when(gerritPropertiesConfig.getRepository()).thenReturn("repo");

    FetchCommand fetchCommand = mock(FetchCommand.class);

    when(git.fetch()).thenReturn(fetchCommand);
    when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);

    when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    when(fetchCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called fetch command for {}", Thread.currentThread().getName());
      counter.check(0);
      return null;
    });

    CheckoutCommand checkoutCommand = mock(CheckoutCommand.class);
    when(git.checkout()).thenReturn(checkoutCommand);
    when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called checkout command for {}", Thread.currentThread().getName());
      counter.check(1);
      return null;
    });

    File amendedFile = mock(File.class);
    when(amendedFile.getParent()).thenReturn("parent");
    when(amendedFile.getName()).thenReturn("name");
    String filePattern = "parent/name";
    when(gitFileService.writeFile(repositoryName, filecontent, filepath)).thenReturn(amendedFile);

    //add file to Git method
    when(amendedFile.exists()).thenReturn(true);
    AddCommand addCommand = mock(AddCommand.class);
    when(git.add()).thenReturn(addCommand);
    when(addCommand.addFilepattern(filePattern)).thenReturn(addCommand);
    when(addCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called add command for {}", Thread.currentThread().getName());
      counter.check(2);
      return null;
    });

    // do amend method
    StatusCommand statusCommand = mock(StatusCommand.class);
    when(git.status()).thenReturn(statusCommand);
    Status addStatus = mock(Status.class);
    when(statusCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called status command for {}", Thread.currentThread().getName());
      counter.check(3);
      return addStatus;
    });

    when(addStatus.isClean()).thenReturn(false);
    CommitCommand commitCommand = mock(CommitCommand.class);
    when(git.commit()).thenReturn(commitCommand);
    when(commitCommand.setMessage(any())).thenReturn(commitCommand);
    when(commitCommand.setAmend(true)).thenReturn(commitCommand);
    when(commitCommand.setAmend(true)).thenReturn(commitCommand);
    RevCommit revCommit = mock(RevCommit.class);
    when(commitCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called status command for {}", Thread.currentThread().getName());
      counter.check(4);
      return revCommit;
    });

    //push changes method
    RemoteAddCommand remoteAddCommand = mock(RemoteAddCommand.class);
    when(git.remoteAdd()).thenReturn(remoteAddCommand);
    when(remoteAddCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called remote add command for {}", Thread.currentThread().getName());
      counter.check(5);
      return null;
    });
    PushCommand pushCommand = mock(PushCommand.class);
    when(git.push()).thenReturn(pushCommand);
    when(pushCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called push command for {}", Thread.currentThread().getName());
      counter.check(6);
      return null;
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath, filecontent);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertThat(counter.getCompletedIterations()).isEqualTo(numberOfThreads);
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void deleteSyncTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();

    var file = new File(tempDir, repositoryName);
    Assertions.assertThat(file.createNewFile()).isTrue();


    Counter counter = new Counter(2);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    when(gerritPropertiesConfig.getUrl()).thenReturn("https://gerrit");
    when(gerritPropertiesConfig.getRepository()).thenReturn("repo");

    FetchCommand fetchCommand = mock(FetchCommand.class);

    when(git.fetch()).thenReturn(fetchCommand);
    when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);

    when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    when(fetchCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called fetch command for {}", Thread.currentThread().getName());
      counter.check(0);
      return null;
    });

    CheckoutCommand checkoutCommand = mock(CheckoutCommand.class);
    when(git.checkout()).thenReturn(checkoutCommand);
    when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).thenAnswer(invocation -> {
      Thread.sleep(100);
      log.info("Called checkout command for {}", Thread.currentThread().getName());
      counter.check(1);
      return null;
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.delete(repositoryName, filepath, refs, commitMessage, changeId);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertThat(counter.getCompletedIterations()).isEqualTo(numberOfThreads);
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }
}
