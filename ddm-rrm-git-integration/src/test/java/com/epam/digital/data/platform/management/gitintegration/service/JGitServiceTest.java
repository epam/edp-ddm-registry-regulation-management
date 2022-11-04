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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.epam.digital.data.platform.management.core.config.CacheConfig;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.config.RetryConfig;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RetryConfig.class,
    CacheConfig.class,
    CacheAutoConfiguration.class,
    GitRetryable.class,
    JGitServiceImpl.class})
@DisplayName("JGit service unit test")
class JGitServiceTest {

  @TempDir
  File tempDir;

  @Autowired
  JGitService jGitService;

  @MockBean
  JGitWrapper jGitWrapper;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  GitFileService gitFileService;

  @Mock
  private Git git;
  @Mock
  private Repository repository;
  @Mock
  private CheckoutCommand checkoutCommand;
  @Mock
  private FetchCommand fetchCommand;

  @Mock
  private RevTree revTree;

  @Mock
  private TreeWalk treeWalk;

  @Mock
  private LogCommand logCommand;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(tempDir.getPath()).when(gerritPropertiesConfig).getRepositoryDirectory();
  }

  @Nested
  @DisplayName("JGitService#clone")
  class JGitServiceCloneTest {

    @Test
    @DisplayName("should not clone if repo already exists")
    @SneakyThrows
    void testCloneRepository_repoAlreadyExists() {
      var repoName = RandomString.make();
      var file = new File(tempDir, repoName);
      Assertions.assertThat(file.createNewFile()).isTrue();

      jGitService.cloneRepoIfNotExist(repoName);

      Mockito.verify(jGitWrapper, Mockito.never()).cloneRepository();
    }

    @Test
    @DisplayName("should clone repo doesn't exist")
    @SneakyThrows
    void testCloneRepository() {
      final var context = new Context();

      final var git = Mockito.mock(Git.class);
      Mockito.doReturn(git).when(context.cloneCommand).call();

      jGitService.cloneRepoIfNotExist(context.repoName);

      context.verifyMockInvocations();
      Mockito.verify(git).close();
    }

    @Test
    @DisplayName("should throw IllegalStateException if there is invalid remote")
    @SneakyThrows
    void testCloneRepository_invalidRemote() {
      final var context = new Context();

      Mockito.doThrow(InvalidRemoteException.class).when(context.cloneCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(context.repoName))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Remote that is configured under \"gerrit\" prefix is invalid: ")
          .hasCauseInstanceOf(InvalidRemoteException.class);

      context.verifyMockInvocations();
    }

    @Test
    @DisplayName("should retry and throw GitCommandException if there is transport exception")
    @SneakyThrows
    void testCloneRepository_transportException() {
      final var context = new Context();

      Mockito.doThrow(TransportException.class).when(context.cloneCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(context.repoName))
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred during cloning repository %s: ",
              context.repoName)
          .hasCauseInstanceOf(GitAPIException.class);

      Mockito.verify(context.cloneCommand, Mockito.times(3)).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if there is unknown git exception")
    @SneakyThrows
    void testCloneRepository_gitApiException() {
      final var context = new Context();

      Mockito.doThrow(new GitAPIException("message") {
      }).when(context.cloneCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(context.repoName))
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred during cloning repository %s: ",
              context.repoName)
          .hasCauseInstanceOf(GitAPIException.class);

      context.verifyMockInvocations();
    }

    @Test
    @DisplayName("should throw NPE if clone command returned null instead of Git (not possible in real)")
    @SneakyThrows
    void testCloneRepository_cloneReturnNull() {
      final var context = new Context();

      Mockito.doReturn(null).when(context.cloneCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.cloneRepoIfNotExist(context.repoName))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("CloneCommand#call cannot be null");

      context.verifyMockInvocations();
    }

    class Context {

      final String repoName = RandomString.make();
      final String repoUrl = RandomString.make();
      final String user = RandomString.make();
      final String password = RandomString.make();

      final CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

      Context() {
        final var directory = new File(tempDir, repoName);

        Mockito.doReturn(cloneCommand).when(jGitWrapper).cloneRepository();
        Mockito.doReturn(user).when(gerritPropertiesConfig).getUser();
        Mockito.doReturn(password).when(gerritPropertiesConfig).getPassword();
        Mockito.doReturn(repoUrl).when(gerritPropertiesConfig).getUrl();
        Mockito.doReturn(repoName).when(gerritPropertiesConfig).getRepository();

        Mockito.doReturn(cloneCommand).when(cloneCommand).setURI(repoUrl + "/" + repoName);
        Mockito.doReturn(cloneCommand).when(cloneCommand).setDirectory(directory);
        Mockito.doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));
        Mockito.doReturn(cloneCommand).when(cloneCommand).setCloneAllBranches(true);
      }

      @SneakyThrows
      void verifyMockInvocations() {
        final var directory = new File(tempDir, repoName);

        Mockito.verify(jGitWrapper).cloneRepository();
        Mockito.verify(cloneCommand).setURI(repoUrl + "/" + repoName);
        Mockito.verify(cloneCommand).setDirectory(directory);
        Mockito.verify(cloneCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));
        Mockito.verify(cloneCommand).setCloneAllBranches(true);
        Mockito.verify(cloneCommand).call();
      }
    }
  }

  @Nested
  @DisplayName("JGitService#resetHeadBranchToRemote")
  class JGitServiceResetHeadBranchToRemoteTest {

    @Test
    @DisplayName("should call fetch all and reset commands")
    @SneakyThrows
    void testResetHeadBranchToRemote() {
      final var context = new Context();

      jGitService.resetHeadBranchToRemote();

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.resetCommand).call();
    }

    @Test
    @DisplayName("should throw IllegalStateException if there is invalid remote")
    @SneakyThrows
    void testResetHeadBranchToRemote_invalidRemote() {
      final var context = new Context();

      var invalidRemote = new InvalidRemoteException("invalid remote");
      Mockito.when(context.fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Default remote \"origin\" cannot be invalid")
          .hasCauseInstanceOf(InvalidRemoteException.class);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.resetCommand, Mockito.never()).call();
    }

    @Test
    @DisplayName("should throw IllegalStateException if there is checkout conflict")
    @SneakyThrows
    void testResetHeadBranchToRemote_checkoutConflict() {
      final var context = new Context();

      Mockito.doThrow(CheckoutConflictException.class)
          .when(context.resetCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Hard reset must not face any conflicts: ")
          .hasCauseInstanceOf(CheckoutConflictException.class);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.resetCommand).call();
    }

    @Test
    @DisplayName("should retry and throw GitCommandException if there is transport exception")
    @SneakyThrows
    void testResetHeadBranchToRemote_transportException() {
      final var context = new Context();

      Mockito.doThrow(TransportException.class)
          .when(context.fetchCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred while fetching: ")
          .hasCauseInstanceOf(TransportException.class);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand, Mockito.times(3)).call();
      Mockito.verify(context.resetCommand, Mockito.never()).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if couldn't open the repo due to any IOException")
    @SneakyThrows
    void testResetHeadBranchToRemote_couldNotOpenRepo() {
      var repoName = RandomString.make();
      var file = new File(tempDir, repoName);
      Assertions.assertThat(file.createNewFile()).isTrue();

      Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);

      Mockito.doThrow(IOException.class).when(jGitWrapper).open(file);
      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred during repository opening: ")
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("should throw GitCommandException if there is unknown git exception during fetch")
    @SneakyThrows
    void testResetHeadBranchToRemote_fetchGitCommandException() {
      final var context = new Context();

      var gitAPIException = new GitAPIException("some git API exception") {
      };
      Mockito.doThrow(gitAPIException)
          .when(context.fetchCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(GitCommandException.class)
          .hasMessage("Exception occurred while fetching: some git API exception")
          .hasCause(gitAPIException);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.resetCommand, Mockito.never()).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if there is unknown git exception during reset")
    @SneakyThrows
    void testResetHeadBranchToRemote_resetGitCommandException() {
      final var context = new Context();

      var gitAPIException = new GitAPIException("some git API exception") {
      };
      Mockito.doThrow(gitAPIException)
          .when(context.resetCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(GitCommandException.class)
          .hasMessage("Exception occurred during hard reset on origin %s: some git API exception",
              context.repoName)
          .hasCause(gitAPIException);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.resetCommand).call();
    }

    @Test
    @DisplayName("should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
    @SneakyThrows
    void testResetHeadBranchToRemoteDirectoryNotExist() {
      var headBranch = RandomString.make();
      var repo = new File(tempDir, headBranch);
      Assertions.assertThat(repo.exists()).isFalse();

      Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(headBranch);

      Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
          .isInstanceOf(RepositoryNotFoundException.class)
          .hasNoCause()
          .hasMessage("Repository " + headBranch + " doesn't exists");

      Mockito.verify(jGitWrapper, never()).open(any());
    }

    class Context {

      final String repoName = RandomString.make();
      final String user = RandomString.make();
      final String password = RandomString.make();

      final File directory = new File(tempDir, repoName);
      final Git git = Mockito.mock(Git.class);

      final FetchCommand fetchCommand = Mockito.mock(FetchCommand.class);
      final ResetCommand resetCommand = Mockito.mock(ResetCommand.class);

      @SneakyThrows
      Context() {
        Assertions.assertThat(directory.mkdirs()).isTrue();
        Mockito.doReturn(git).when(jGitWrapper).open(directory);
        Mockito.doReturn(repoName).when(gerritPropertiesConfig).getHeadBranch();
        Mockito.doReturn(user).when(gerritPropertiesConfig).getUser();
        Mockito.doReturn(password).when(gerritPropertiesConfig).getPassword();

        Mockito.doReturn(fetchCommand).when(git).fetch();
        Mockito.doReturn(fetchCommand)
            .when(fetchCommand).setCredentialsProvider(
                Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));

        Mockito.doReturn(resetCommand).when(git).reset();
        Mockito.doReturn(resetCommand).when(resetCommand).setMode(ResetType.HARD);
        Mockito.doReturn(resetCommand)
            .when(resetCommand).setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName);
      }

      @SneakyThrows
      void verifyMockInvocations() {
        Mockito.verify(jGitWrapper).open(directory);

        Mockito.verify(git).fetch();
        Mockito.verify(fetchCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));

        Mockito.verify(git, Mockito.atMostOnce()).reset();
        Mockito.verify(resetCommand, Mockito.atMostOnce()).setMode(ResetType.HARD);
        Mockito.verify(resetCommand, Mockito.atMostOnce())
            .setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName);

        Mockito.verify(git).close();
      }
    }
  }

  @Nested
  @DisplayName("JGitService#fetch")
  class JGitServiceFetchSpecificRefTest {

    @Test
    @DisplayName("should call fetch refs and checkout commands")
    @SneakyThrows
    void testFetch() {
      final var context = new Context();

      jGitService.fetch(context.repoName, context.refs);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.checkoutCommand).call();
    }

    @Test
    @DisplayName("should throw IllegalStateException if there is invalid remote")
    @SneakyThrows
    void testFetch_invalidRemote() {
      final var context = new Context();

      var invalidRemote = new InvalidRemoteException("invalid remote");
      Mockito.when(context.fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
      Assertions.assertThatThrownBy(() -> jGitService.fetch(context.repoName, context.refs))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Default remote \"origin\" cannot be invalid")
          .hasCauseInstanceOf(InvalidRemoteException.class);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.checkoutCommand, Mockito.never()).call();
    }

    @DisplayName("should throw IllegalStateException if there is checkout conflict")
    @ParameterizedTest
    @ValueSource(classes = {
        CheckoutConflictException.class,
        RefAlreadyExistsException.class,
        RefNotFoundException.class,
        InvalidRefNameException.class
    })
    @SneakyThrows
    void testResetHeadBranchToRemote_checkoutConflict(
        Class<? extends GitAPIException> checkoutConflictType) {
      final var context = new Context();

      Mockito.doThrow(checkoutConflictType)
          .when(context.checkoutCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.fetch(context.repoName, context.refs))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Checkout on FETCH_HEAD must not throw such exception: ")
          .hasCauseInstanceOf(checkoutConflictType);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.checkoutCommand).call();
    }

    @Test
    @DisplayName("should retry and throw GitCommandException if there is transport exception")
    @SneakyThrows
    void testResetHeadBranchToRemote_transportException() {
      final var context = new Context();

      Mockito.doThrow(TransportException.class)
          .when(context.fetchCommand).call();

      Assertions.assertThatThrownBy(() -> jGitService.fetch(context.repoName, context.refs))
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred while fetching: ")
          .hasCauseInstanceOf(TransportException.class);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand, Mockito.times(3)).call();
      Mockito.verify(context.checkoutCommand, Mockito.never()).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if there is unknown git exception during fetch")
    @SneakyThrows
    void testFetch_fetchGitCommandException() {
      final var context = new Context();

      var gitAPIException = new GitAPIException("some git API exception") {
      };
      Mockito.doThrow(gitAPIException)
          .when(context.fetchCommand).call();
      Assertions.assertThatThrownBy(() -> jGitService.fetch(context.repoName, context.refs))
          .isInstanceOf(GitCommandException.class)
          .hasMessage("Exception occurred while fetching: some git API exception")
          .hasCause(gitAPIException);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.checkoutCommand, Mockito.never()).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if there is unknown git exception during checkout")
    @SneakyThrows
    void testFetch_checkoutGitCommandException() {
      final var context = new Context();

      var gitAPIException = new GitAPIException("some git API exception") {
      };

      Mockito.doThrow(gitAPIException)
          .when(context.checkoutCommand).call();
      Assertions.assertThatThrownBy(() -> jGitService.fetch(context.repoName, context.refs))
          .isInstanceOf(GitCommandException.class)
          .hasMessage("Exception occurred while checkout: some git API exception")
          .hasCause(gitAPIException);

      context.verifyMockInvocations();
      Mockito.verify(context.fetchCommand).call();
      Mockito.verify(context.checkoutCommand).call();
    }

    @Test
    @DisplayName("should throw GitCommandException if couldn't open the repo due to any IOException")
    @SneakyThrows
    void testFetch_couldNotOpenRepo() {
      var repoName = RandomString.make();
      var ref = RandomString.make();
      var file = new File(tempDir, repoName);
      Assertions.assertThat(file.createNewFile()).isTrue();

      Mockito.doThrow(IOException.class).when(jGitWrapper).open(file);
      Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, ref))
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred during repository opening: ")
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
    @SneakyThrows
    void testFetchDirectoryNotExist() {
      var repoName = RandomString.make();
      final String refs = RandomString.make();
      Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
          .isInstanceOf(RepositoryNotFoundException.class)
          .hasMessage("Repository " + repoName + " doesn't exists")
          .hasNoCause();

      Mockito.verify(jGitWrapper, never()).open(Mockito.any());
    }

    class Context {

      final String repoName = RandomString.make();
      final String user = RandomString.make();
      final String password = RandomString.make();
      final String refs = RandomString.make();

      final File directory = new File(tempDir, repoName);
      final Git git = Mockito.mock(Git.class);

      final FetchCommand fetchCommand = Mockito.mock(FetchCommand.class);
      final CheckoutCommand checkoutCommand = Mockito.mock(CheckoutCommand.class);

      @SneakyThrows
      Context() {
        Assertions.assertThat(directory.mkdirs()).isTrue();
        Mockito.doReturn(git).when(jGitWrapper).open(directory);
        Mockito.doReturn(user).when(gerritPropertiesConfig).getUser();
        Mockito.doReturn(password).when(gerritPropertiesConfig).getPassword();

        Mockito.doReturn(fetchCommand).when(git).fetch();
        Mockito.doReturn(fetchCommand).when(fetchCommand).setRefSpecs(refs);
        Mockito.doReturn(fetchCommand)
            .when(fetchCommand).setCredentialsProvider(
                Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));

        Mockito.doReturn(checkoutCommand).when(git).checkout();
        Mockito.doReturn(checkoutCommand).when(checkoutCommand).setName(Constants.FETCH_HEAD);
      }

      @SneakyThrows
      void verifyMockInvocations() {
        Mockito.verify(jGitWrapper).open(directory);

        Mockito.verify(git).fetch();
        Mockito.verify(fetchCommand).setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));
        Mockito.verify(fetchCommand).setRefSpecs(refs);

        Mockito.verify(git, Mockito.atMostOnce()).reset();
        Mockito.verify(checkoutCommand, Mockito.atMostOnce()).setName(Constants.FETCH_HEAD);

        Mockito.verify(git).close();
      }
    }
  }

  @Nested
  @DisplayName("JGitService#getFilesInPath")
  class JGitServiceGetFilesInPathTest {

    @Test
    @DisplayName("should return list of all found files")
    @SneakyThrows
    void getFilesInPathTest() {
      var treeWalk = Mockito.mock(TreeWalk.class);
      Mockito.doReturn(true, true, false).when(treeWalk).next();
      Mockito.doReturn("bpmn/filename2.bpmn", "bpmn/filename1.bpmn")
          .when(treeWalk).getPathString();

      var context = new Context(treeWalk);

      var actualFiles = jGitService.getFilesInPath(context.repoName, context.filePath);
      Assertions.assertThat(actualFiles)
          .isNotNull()
          .hasSize(2)
          .containsExactly("filename1.bpmn", "filename2.bpmn");

      context.verifyMockInvocations();
      Mockito.verify(treeWalk).enterSubtree();
      Mockito.verify(treeWalk, Mockito.times(3)).next();
      Mockito.verify(treeWalk, Mockito.times(2)).getPathString();
    }

    @Test
    @DisplayName("should return empty list if path isn't found in repository")
    @SneakyThrows
    void getFilesInPathTest_treeWalkNull() {
      var context = new Context(null);

      var actualFiles = jGitService.getFilesInPath(context.repoName, context.filePath);
      Assertions.assertThat(actualFiles)
          .isNotNull()
          .hasSize(0);

      context.verifyMockInvocations();
    }

    @Test
    @DisplayName("should throw GitCommandException if IOException occurred")
    @SneakyThrows
    void getFilesInPathTest_ioException() {
      var treeWalk = Mockito.mock(TreeWalk.class);
      Mockito.doThrow(IOException.class).when(treeWalk).enterSubtree();

      var context = new Context(treeWalk);

      Assertions.assertThatThrownBy(
              () -> jGitService.getFilesInPath(context.repoName, context.filePath))
          .isInstanceOf(GitCommandException.class)
          .hasMessageContaining("Exception occurred during reading files by path: ")
          .hasCauseInstanceOf(IOException.class);

      context.verifyMockInvocations();
      Mockito.verify(treeWalk).enterSubtree();
      Mockito.verify(treeWalk, Mockito.never()).next();
      Mockito.verify(treeWalk, Mockito.never()).getPathString();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if path is empty")
    @SneakyThrows
    void getFilesInEmptyPathTest() {
      var context = new Context(null);

      Mockito.doCallRealMethod().when(jGitWrapper).getTreeWalk(context.repository, "");

      Assertions.assertThatThrownBy(() -> jGitService.getFilesInPath(context.repoName, ""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Empty path not permitted.");
    }

    @Test
    @DisplayName("Should throw RepositoryNotFoundException if couldn't open the repo due to non existence")
    @SneakyThrows
    void testFetchDirectoryNotExist() {
      final var repoName = RandomString.make();
      final var path = RandomString.make();

      Assertions.assertThatThrownBy(() -> jGitService.getFilesInPath(repoName, path))
          .isInstanceOf(RepositoryNotFoundException.class)
          .hasMessage("Repository %s doesn't exists", repoName)
          .hasNoCause();

      Mockito.verify(jGitWrapper, never()).open(Mockito.any());
    }

    class Context {

      final String repoName = RandomString.make();
      final String filePath = RandomString.make();

      final File directory = new File(tempDir, repoName);
      final Git git = Mockito.mock(Git.class);
      final Repository repository = Mockito.mock(Repository.class);

      @SneakyThrows
      Context(TreeWalk treeWalkMock) {
        Assertions.assertThat(directory.mkdirs()).isTrue();
        Mockito.doReturn(git).when(jGitWrapper).open(directory);
        Mockito.doReturn(repository).when(git).getRepository();
        Mockito.doReturn(treeWalkMock).when(jGitWrapper).getTreeWalk(repository, filePath);
      }

      @SneakyThrows
      void verifyMockInvocations() {
        Mockito.verify(jGitWrapper).open(directory);
        Mockito.verify(git).getRepository();
        Mockito.verify(jGitWrapper).getTreeWalk(repository, filePath);
      }
    }
  }

  @Test
  @SneakyThrows
  void getFormDatesTest() {
    var repo = new File(tempDir, "version");
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(git.log()).thenReturn(logCommand);

    RevCommit revCommit = mock(RevCommit.class);
    RevCommit revCommit2 = mock(RevCommit.class);

    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
    FileDatesDto version = jGitService.getDates("version", "forms");
    Assertions.assertThat(version).isNotNull();
    Assertions.assertThat(version.getCreate()).isNotNull();
    Assertions.assertThat(version.getUpdate()).isNotNull();
  }

  @Test
  @SneakyThrows
  void testAmendRepositoryNotExist() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    final var filecontent = RandomString.make();
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class)
        .isThrownBy(() -> jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath,
            filecontent));
  }

  @Test
  @SneakyThrows
  void testAmendEmptyInput() {

    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    final var filecontent = RandomString.make();
    var repo = new File(tempDir, repositoryName);
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(gitFileService.writeFile(repositoryName, filecontent, filepath)).thenReturn(null);

    jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath, filecontent);
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void getFileContentRepoNotExistTest() {
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class)
        .isThrownBy(() -> jGitService.getFileContent("version", "/"));
  }

  @Test
  @SneakyThrows
  void getFileContentPathEmptyTest() {
    var repo = new File(tempDir, "version");
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Assertions.assertThat(jGitService.getFileContent("version", "")).isNull();
    Assertions.assertThat(jGitService.getFileContent("version", null)).isNull();
    Mockito.verify(jGitWrapper, times(2)).open(repo);
  }

  @Test
  @SneakyThrows
  void getFileContentEmptyTreeTest() {
    var repo = new File(tempDir, "version");
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(false);
    Assertions.assertThat(jGitService.getFileContent("version", "/forms")).isNull();
    Mockito.verify(treeWalk).next();
  }

  @Test
  @SneakyThrows
  void deleteFalseTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    var repo = new File(tempDir, repositoryName);
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn("password");
    jGitService.delete(repositoryName, filepath, refs, commitMessage, changeId);
    Mockito.verify(checkoutCommand).call();
    Mockito.verify(git, never()).add();
    Mockito.verify(git, never()).rm();
  }

  @Test
  @SneakyThrows
  void deleteRepoNotExistTest() {
    final var repositoryName = RandomString.make();
    final var refs = RandomString.make();
    final var commitMessage = RandomString.make();
    final var changeId = RandomString.make();
    final var filepath = RandomString.make();
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class).isThrownBy(
        () -> jGitService.delete(repositoryName, filepath, refs, commitMessage, changeId));
  }
}
