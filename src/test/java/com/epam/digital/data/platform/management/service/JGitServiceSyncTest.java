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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.impl.JGitServiceImpl;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import com.epam.digital.data.platform.management.service.impl.RequestToFileConverter;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
class JGitServiceSyncTest {

  @TempDir
  private File tempDir;

  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;

  @InjectMocks
  private JGitServiceImpl jGitService;

  @Mock
  private JGitWrapper jGitWrapper;

  @Mock
  private Git git;
  @Mock
  private Repository repository;

  @Mock
  private PullResult pullResult;

  @Mock
  private Ref checkoutResult;

  @Mock
  private RequestToFileConverter requestToFileConverter;

  @Mock
  private CheckoutCommand checkoutCommand;

  @Mock
  private PullCommand pullCommand;


  @BeforeAll
  @SneakyThrows
  static void setUpStatic() {

  }

  @Test
  @SneakyThrows
  void pullRepoSyncTest() {

    File file = new File(tempDir, "version");
    file.createNewFile();

    Counter counter = new Counter(2);
    when(git.pull()).thenReturn(pullCommand);
    when(git.checkout()).thenReturn(checkoutCommand);

    when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).then(new Answer<Ref>() {
      @Override
      public Ref answer(InvocationOnMock invocation) throws Throwable {
        synchronized (this) {
          wait(100);
          log.info("Called checkout command for {}", Thread.currentThread().getName());
          counter.check(0);
          return checkoutResult;
        }
      }
    });

    when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    when(pullCommand.call()).then(new Answer<PullResult>() {
      @Override
      public PullResult answer(InvocationOnMock invocation) throws Throwable {
        synchronized (this) {
          wait(100);
          log.info("Called pull command for {}", Thread.currentThread().getName());
          counter.check(1);
          return pullResult;
        }
      }
    });

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.pull("version");
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void cloneRepoSyncTest_alreadyCreatedRepo() {

    File file = new File(tempDir, "version");
    file.createNewFile();

    Counter counter = new Counter(2);
    when(git.pull()).thenReturn(pullCommand);
    when(git.checkout()).thenReturn(checkoutCommand);

    when(checkoutCommand.setName(any())).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).then(new Answer<Ref>() {
      @Override
      public Ref answer(InvocationOnMock invocation) throws Throwable {
        synchronized (this) {
          wait(100);
          log.info("Called checkout command for {}", Thread.currentThread().getName());
          counter.check(0);
          return checkoutResult;
        }
      }
    });

    when(pullCommand.setCredentialsProvider(any())).thenReturn(pullCommand);
    when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    when(pullCommand.call()).then(new Answer<PullResult>() {
      @Override
      public PullResult answer(InvocationOnMock invocation) throws Throwable {
        synchronized (this) {
          wait(100);
          log.info("Called pull command for {}", Thread.currentThread().getName());
          counter.check(1);
          return pullResult;
        }
      }
    });

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.cloneRepo("version");
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void getFilesInPathSyncTest() {

    String version = "version";

    File file = new File(tempDir, version);
    file.createNewFile();

    String path = "forms";

    Counter counter = new Counter(3);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");

    RevTree tree = mock(RevTree.class);
    when(jGitWrapper.getRevTree(repository, gerritPropertiesConfig.getHeadBranch()))
        .thenAnswer(new Answer<RevTree>() {
          @Override
          public synchronized RevTree answer(InvocationOnMock invocation) throws Throwable {
            wait(100);
            log.info("Called retrieving the rev tree for {}", Thread.currentThread().getName());
            counter.check(0);
            return tree;
          }
        });
    TreeWalk treeWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository, path, tree)).thenAnswer(new Answer<TreeWalk>() {
      @Override
      public synchronized TreeWalk answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called retrieving the tree walker by revision for {}",
            Thread.currentThread().getName());
        counter.check(1);
        return treeWalk;
      }
    });

    TreeWalk dirWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository)).thenReturn(dirWalk);

    ObjectId objectId = mock(ObjectId.class);
    when(treeWalk.getObjectId(0)).thenReturn(objectId);

    when(dirWalk.next()).thenReturn(true, false, true, false,
            true, false, true, false,
            true, false)
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            throw new Exception("Called too many times!");
          }
        });

    when(dirWalk.getPathString()).thenAnswer(new Answer<String>() {
      @Override
      public synchronized String answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called retrieving a ingle file from the tree for {}",
            Thread.currentThread().getName());
        counter.check(2);
        return "file";
      }
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

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void getFileContentSyncTest() {

    String version = "version";

    File file = new File(tempDir, version);
    file.createNewFile();

    String path = "forms";

    Counter counter = new Counter(3);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");

    RevTree tree = mock(RevTree.class);
    when(jGitWrapper.getRevTree(repository, gerritPropertiesConfig.getHeadBranch()))
        .thenAnswer(new Answer<RevTree>() {
          @Override
          public synchronized RevTree answer(InvocationOnMock invocation) throws Throwable {
            wait(100);
            log.info("Called retrieving the rev tree for {}", Thread.currentThread().getName());
            counter.check(0);
            return tree;
          }
        });
    TreeWalk treeWalk = mock(TreeWalk.class);
    when(jGitWrapper.getTreeWalk(repository)).thenAnswer(new Answer<TreeWalk>() {
      @Override
      public synchronized TreeWalk answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called retrieving the tree walker by revision for {}",
            Thread.currentThread().getName());
        counter.check(1);
        return treeWalk;
      }
    });

    when(treeWalk.next()).thenReturn(true);
    ObjectId objectId = mock(ObjectId.class);
    when(treeWalk.getObjectId(0)).thenReturn(objectId);

    ObjectLoader objectLoader = mock(ObjectLoader.class);

    when(objectLoader.getCachedBytes()).thenReturn("content".getBytes());
    when(objectLoader.getBytes()).thenReturn("content".getBytes());

    when(repository.open(objectId)).thenAnswer(new Answer<ObjectLoader>() {
      @Override
      public synchronized ObjectLoader answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called retrieving file content from the tree for {}",
            Thread.currentThread().getName());
        counter.check(2);
        return objectLoader;
      }
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

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void amendSyncTest() {

    String version = "version";

    File file = new File(tempDir, version);
    file.createNewFile();

    String path = "forms";

    Counter counter = new Counter(7);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    when(gerritPropertiesConfig.getUrl()).thenReturn("https://gerrit");
    when(gerritPropertiesConfig.getRepository()).thenReturn("repo");

    FetchCommand fetchCommand = mock(FetchCommand.class);

    when(git.fetch()).thenReturn(fetchCommand);
    when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);

    VersioningRequestDto requestDto = mock(VersioningRequestDto.class);
    when(requestDto.getVersionName()).thenReturn(version);
    ChangeInfoDto changeInfoDto = mock(ChangeInfoDto.class);

    when(changeInfoDto.getRefs()).thenReturn("refs");
    when(fetchCommand.setRefSpecs("refs")).thenReturn(fetchCommand);
    when(fetchCommand.call()).thenAnswer(new Answer<FetchResult>() {
      @Override
      public synchronized FetchResult answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called fetch command for {}", Thread.currentThread().getName());
        counter.check(0);
        return null;
      }
    });

    CheckoutCommand checkoutCommand = mock(CheckoutCommand.class);
    when(git.checkout()).thenReturn(checkoutCommand);
    when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).thenAnswer(new Answer<CheckoutResult>() {
      @Override
      public synchronized CheckoutResult answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called checkout command for {}", Thread.currentThread().getName());
        counter.check(1);
        return null;
      }
    });

    File amendedFile = mock(File.class);
    when(amendedFile.getParent()).thenReturn("parent");
    when(amendedFile.getName()).thenReturn("name");
    String filePattern = "parent/name";
    when(requestToFileConverter.convert(requestDto)).thenReturn(amendedFile);

    //add file to Git method
    when(amendedFile.exists()).thenReturn(true);
    AddCommand addCommand = mock(AddCommand.class);
    when(git.add()).thenReturn(addCommand);
    when(addCommand.addFilepattern(filePattern)).thenReturn(addCommand);
    when(addCommand.call()).thenAnswer(new Answer<DirCache>() {
      @Override
      public synchronized DirCache answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called add command for {}", Thread.currentThread().getName());
        counter.check(2);
        return null;
      }
    });

    // do amend method
    StatusCommand statusCommand = mock(StatusCommand.class);
    when(git.status()).thenReturn(statusCommand);
    Status addStatus = mock(Status.class);
    when(statusCommand.call()).thenAnswer(new Answer<Status>() {
      @Override
      public synchronized Status answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called status command for {}", Thread.currentThread().getName());
        counter.check(3);
        return addStatus;
      }
    });

    when(addStatus.isClean()).thenReturn(false);
    CommitCommand commitCommand = mock(CommitCommand.class);
    when(git.commit()).thenReturn(commitCommand);
    when(changeInfoDto.getSubject()).thenReturn("subject");
    when(changeInfoDto.getChangeId()).thenReturn("changeId");
    when(commitCommand.setMessage(any())).thenReturn(commitCommand);
    when(commitCommand.setAmend(true)).thenReturn(commitCommand);
    RevCommit revCommit = mock(RevCommit.class);
    when(commitCommand.call()).thenAnswer(new Answer<RevCommit>() {
      @Override
      public synchronized RevCommit answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called status command for {}", Thread.currentThread().getName());
        counter.check(4);
        return revCommit;
      }
    });

    //push changes method
    RemoteAddCommand remoteAddCommand = mock(RemoteAddCommand.class);
    when(git.remoteAdd()).thenReturn(remoteAddCommand);
    when(remoteAddCommand.call()).thenAnswer(new Answer<RemoteConfig>() {
      @Override
      public synchronized RemoteConfig answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called remote add command for {}", Thread.currentThread().getName());
        counter.check(5);
        return null;
      }
    });
    PushCommand pushCommand = mock(PushCommand.class);
    when(git.push()).thenReturn(pushCommand);
    when(pushCommand.call()).thenAnswer(new Answer<PushResult>() {
      @Override
      public synchronized PushResult answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called push command for {}", Thread.currentThread().getName());
        counter.check(6);
        return null;
      }
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.amend(requestDto, changeInfoDto);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }

  @Test
  @SneakyThrows
  void deleteSyncTest() {

    String version = "version";

    File file = new File(tempDir, version);
    file.createNewFile();

    String path = "forms";

    Counter counter = new Counter(2);

    when(jGitWrapper.open(file)).thenReturn(git);
    when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
    when(git.getRepository()).thenReturn(repository);
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    when(gerritPropertiesConfig.getUser()).thenReturn("user");
    when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    when(gerritPropertiesConfig.getUrl()).thenReturn("https://gerrit");
    when(gerritPropertiesConfig.getRepository()).thenReturn("repo");

    FetchCommand fetchCommand = mock(FetchCommand.class);

    when(git.fetch()).thenReturn(fetchCommand);
    when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);

    VersioningRequestDto requestDto = mock(VersioningRequestDto.class);
    when(requestDto.getVersionName()).thenReturn(version);
    ChangeInfoDto changeInfoDto = mock(ChangeInfoDto.class);

    when(changeInfoDto.getRefs()).thenReturn("refs");
    when(changeInfoDto.getNumber()).thenReturn(version);
    when(fetchCommand.setRefSpecs("refs")).thenReturn(fetchCommand);
    when(fetchCommand.call()).thenAnswer(new Answer<FetchResult>() {
      @Override
      public synchronized FetchResult answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called fetch command for {}", Thread.currentThread().getName());
        counter.check(0);
        return null;
      }
    });

    CheckoutCommand checkoutCommand = mock(CheckoutCommand.class);
    when(git.checkout()).thenReturn(checkoutCommand);
    when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    when(checkoutCommand.call()).thenAnswer(new Answer<CheckoutResult>() {
      @Override
      public synchronized CheckoutResult answer(InvocationOnMock invocation) throws Throwable {
        wait(100);
        log.info("Called checkout command for {}", Thread.currentThread().getName());
        counter.check(1);
        return null;
      }
    });

    int numberOfThreads = 5;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.execute(() -> {
        try {
          jGitService.delete(changeInfoDto, path);
        } catch (Exception e) {
          log.error("error", e);
          // Handle exception
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();

    Assertions.assertEquals(numberOfThreads, counter.getCompletedIterations());
    var errorList = counter.getErrorList();
    if (!errorList.isEmpty()) {
      throw errorList.get(0);
    }
  }


}
