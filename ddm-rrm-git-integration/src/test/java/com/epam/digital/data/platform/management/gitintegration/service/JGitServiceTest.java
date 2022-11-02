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
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
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
  private FetchCommand fetchCommand;

  @Mock
  private GitFileService gitFileService;

  @Mock
  private RevTree revTree;

  @Mock
  private TreeWalk treeWalk;

  @Mock
  private LogCommand logCommand;

  @BeforeEach
  void setUp() {
    Mockito.when(gerritPropertiesConfig.getRepositoryDirectory()).thenReturn(tempDir.getPath());
  }

  @Test
  @SneakyThrows
  void testCloneRepository() {
    var repoName = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    jGitService.cloneRepo(repoName);
    Mockito.verify(jGitWrapper, Mockito.never()).open(file);
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
  void testResetHeadBranchToRemote() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();

    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var resetCommand = Mockito.mock(ResetCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.reset()).thenReturn(resetCommand);

    Mockito.when(fetchCommand.setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(resetCommand.setMode(ResetType.HARD)).thenReturn(resetCommand);
    Mockito.when(resetCommand.setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName))
        .thenReturn(resetCommand);

    jGitService.resetHeadBranchToRemote();

    Mockito.verify(jGitWrapper).open(file);

    Mockito.verify(git).fetch();
    Mockito.verify(git).reset();
    Mockito.verify(git).close();

    Mockito.verify(fetchCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));
    Mockito.verify(fetchCommand).call();

    Mockito.verify(resetCommand).setMode(ResetType.HARD);
    Mockito.verify(resetCommand).setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName);
    Mockito.verify(resetCommand).call();
  }

  @Test
  @SneakyThrows
  void testResetHeadBranchToRemote_illegalState() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var resetCommand = Mockito.mock(ResetCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.reset()).thenReturn(resetCommand);

    Mockito.when(fetchCommand.setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(resetCommand.setMode(ResetType.HARD)).thenReturn(resetCommand);
    Mockito.when(resetCommand.setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName))
        .thenReturn(resetCommand);

    var invalidRemote = new InvalidRemoteException("invalid remote");
    Mockito.when(fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default remote \"origin\" cannot be invalid")
        .hasCause(invalidRemote);

    var checkoutConflict = new CheckoutConflictException(List.of(),
        new org.eclipse.jgit.errors.CheckoutConflictException("form.json"));
    Mockito.when(resetCommand.call()).thenThrow(checkoutConflict).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Hard reset must not face any conflicts: " + checkoutConflict.getMessage())
        .hasCause(checkoutConflict);
  }

  @Test
  @SneakyThrows
  void testResetHeadBranchToRemote_runtimeException() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);

    Mockito.when(fetchCommand.setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    var transportException = new TransportException("transport exception");
    Mockito.when(fetchCommand.call()).thenThrow(transportException);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Transport exception occurred while fetching: transport exception")
        .hasCause(transportException);
  }

  @Test
  @SneakyThrows
  void testResetHeadBranchToRemote_couldNotOpenRepo() {
    var repoName = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);

    var ioException = new IOException("io exception");
    Mockito.when(jGitWrapper.open(file)).thenThrow(ioException);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred during repository opening: io exception")
        .hasCause(ioException);
  }

  @Test
  @SneakyThrows
  void testResetHeadBranchToRemote_gitCommandException() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var resetCommand = Mockito.mock(ResetCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.reset()).thenReturn(resetCommand);

    Mockito.when(fetchCommand.setCredentialsProvider(
            Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(resetCommand.setMode(ResetType.HARD)).thenReturn(resetCommand);
    Mockito.when(resetCommand.setRef(Constants.DEFAULT_REMOTE_NAME + "/" + repoName))
        .thenReturn(resetCommand);

    var gitAPIException = new GitAPIException("some git API exception") {
    };
    Mockito.when(fetchCommand.call()).thenThrow(gitAPIException).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while fetching: some git API exception")
        .hasCause(gitAPIException);

    Mockito.when(resetCommand.call()).thenThrow(gitAPIException).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.resetHeadBranchToRemote())
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred during hard reset on origin " + repoName
            + ": some git API exception")
        .hasCause(gitAPIException);
  }

  @Test
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

  @Test
  @SneakyThrows
  void testFetch() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var refs = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var checkoutCommand = Mockito.mock(CheckoutCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);

    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(
            refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(checkoutCommand.setName(Constants.FETCH_HEAD)).thenReturn(checkoutCommand);

    jGitService.fetch(repoName, refs);

    Mockito.verify(jGitWrapper).open(file);

    Mockito.verify(git).fetch();
    Mockito.verify(git).checkout();
    Mockito.verify(git).close();

    Mockito.verify(fetchCommand).setCredentialsProvider(
        Mockito.refEq(new UsernamePasswordCredentialsProvider(user, password)));
    Mockito.verify(fetchCommand).call();

    Mockito.verify(checkoutCommand).setName(Constants.FETCH_HEAD);
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @SneakyThrows
  void testFetch_illegalStateException() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var refs = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var checkoutCommand = Mockito.mock(CheckoutCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);

    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(
            refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(checkoutCommand.setName(Constants.FETCH_HEAD)).thenReturn(checkoutCommand);

    var invalidRemote = new InvalidRemoteException("invalid remote");
    Mockito.when(fetchCommand.call()).thenThrow(invalidRemote).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default remote \"origin\" cannot be invalid")
        .hasCause(invalidRemote);

    var checkoutConflict = new CheckoutConflictException(List.of(),
        new org.eclipse.jgit.errors.CheckoutConflictException("form.json"));
    Mockito.when(checkoutCommand.call()).thenThrow(checkoutConflict).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Checkout on FETCH_HEAD must not throw such exception: "
            + checkoutConflict.getMessage())
        .hasCause(checkoutConflict);

    var refAlreadyExists = new RefAlreadyExistsException("ref already exists");
    Mockito.when(checkoutCommand.call()).thenThrow(refAlreadyExists).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Checkout on FETCH_HEAD must not throw such exception: "
            + refAlreadyExists.getMessage())
        .hasCause(refAlreadyExists);

    var refNotFound = new RefNotFoundException("ref not found");
    Mockito.when(checkoutCommand.call()).thenThrow(refNotFound).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Checkout on FETCH_HEAD must not throw such exception: " + refNotFound.getMessage())
        .hasCause(refNotFound);

    var invalidRefName = new InvalidRefNameException("invalid ref name");
    Mockito.when(checkoutCommand.call()).thenThrow(invalidRefName).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Checkout on FETCH_HEAD must not throw such exception: " + invalidRefName.getMessage())
        .hasCause(invalidRefName);
  }

  @Test
  @SneakyThrows
  void testFetch_gitCommandException() {
    var repoName = RandomString.make();
    var user = RandomString.make();
    var password = RandomString.make();
    var refs = RandomString.make();
    var file = new File(tempDir, repoName);
    Assertions.assertThat(file.createNewFile()).isTrue();

    var fetchCommand = Mockito.mock(FetchCommand.class);
    var checkoutCommand = Mockito.mock(CheckoutCommand.class);

    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(gerritPropertiesConfig.getPassword()).thenReturn(password);

    Mockito.when(jGitWrapper.open(file)).thenReturn(git);

    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);

    Mockito.when(fetchCommand.setRefSpecs(refs)).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setCredentialsProvider(
            refEq(new UsernamePasswordCredentialsProvider(user, password))))
        .thenReturn(fetchCommand);

    Mockito.when(checkoutCommand.setName(Constants.FETCH_HEAD)).thenReturn(checkoutCommand);

    var gitAPIException = new GitAPIException("some git API exception") {
    };
    Mockito.when(fetchCommand.call()).thenThrow(gitAPIException).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while fetching: some git API exception")
        .hasCause(gitAPIException);

    Mockito.when(checkoutCommand.call()).thenThrow(gitAPIException).thenReturn(null);
    Assertions.assertThatThrownBy(() -> jGitService.fetch(repoName, refs))
        .isInstanceOf(GitCommandException.class)
        .hasMessage("Exception occurred while checkout: some git API exception")
        .hasCause(gitAPIException);
  }

  @Test
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

  @Test
  @SneakyThrows
  void getFilesInPathTest() {
    var repo = new File(tempDir, "version");
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository, "/", revTree)).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    List<String> version = jGitService.getFilesInPath("version", "/");
    Assertions.assertThat(version)
        .isNotNull()
        .hasSize(1)
        .element(0).isEqualTo("someFile");
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
  void getFilesInEmptyPathTest() {
    var repo = new File(tempDir, "version");
    Assertions.assertThat(repo.createNewFile()).isTrue();

    Mockito.when(jGitWrapper.open(repo)).thenReturn(git);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    List<String> version = jGitService.getFilesInPath("version", "");
    Assertions.assertThat(version)
        .isNotNull()
        .hasSize(1)
        .element(0).isEqualTo("someFile");
  }

  @Test
  @SneakyThrows
  void getFilesNoDirExists() {
    List<String> version = jGitService.getFilesInPath("version", "");
    Assertions.assertThat(version)
        .isNotNull()
        .isEmpty();
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
        .isThrownBy(() -> jGitService.amend(repositoryName, refs, commitMessage, changeId, filepath, filecontent));
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
    Assertions.assertThatExceptionOfType(RepositoryNotFoundException.class).isThrownBy(() -> jGitService.getFileContent("version", "/"));
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
