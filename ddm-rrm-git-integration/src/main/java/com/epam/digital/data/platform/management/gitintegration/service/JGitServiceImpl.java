/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.management.gitintegration.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JGitServiceImpl implements JGitService {

  public static final String DATE_CACHE_NAME = "dates";

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final GitFileService gitFileService;
  private final JGitWrapper jGitWrapper;
  private final GitRetryable retryable;

  private final ConcurrentMap<String, Lock> lockMap = new ConcurrentHashMap<>();

  @Override
  public void cloneRepoIfNotExist(@NonNull String repositoryName) {
    log.debug("Trying to clone repository {}", repositoryName);
    var directory = getRepositoryDir(repositoryName);
    if (directory.exists()) {
//    this condition was written in order to avoid cloning repo several times
      return;
    }
    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var ignored = cloneRepo(directory)) {
      log.debug("Repository {} was successfully cloned.", repositoryName);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  public void resetHeadBranchToRemote() {
    var repositoryName = gerritPropertiesConfig.getHeadBranch();
    log.debug("Trying to reset repository {} to remote state", repositoryName);
    var repositoryDirectory = getExistedRepository(repositoryName);
    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryDirectory)) {
      log.trace("Fetching repo {}", repositoryName);
      fetchAll(git);

      log.trace("Hard reset {} on {}/{}", repositoryName, Constants.DEFAULT_REMOTE_NAME,
          repositoryName);
      hardResetOnOriginHeadBranch(git);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
    log.debug("Repository {} was successfully reset to remote state", repositoryName);
  }

  @Override
  public void fetch(@NonNull String repositoryName, @NonNull String refs) {
    log.debug("Trying to fetch and checkout repository {} to ref {}", repositoryName, refs);
    var repositoryDirectory = getExistedRepository(repositoryName);
    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryDirectory)) {
      log.trace("Fetching repo {} on ref specs {}", repositoryName, refs);
      fetch(git, refs);

      log.trace("Checkout repo {} on {}", repositoryName, Constants.FETCH_HEAD);
      checkoutFetchHead(git);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
    log.debug("Repository {} was successfully fetched and checkout to ref {}", repositoryName,
        refs);
  }

  @Override
  @NonNull
  public List<String> getFilesInPath(@NonNull String repositoryName, @NonNull String path) {
    log.debug("Retrieving file list in repository {} at path {}", repositoryName, path);
    var repositoryDirectory = getExistedRepository(repositoryName);
    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    log.trace("Opening repo {}", repositoryName);
    try (
        var git = openRepo(repositoryDirectory);
        var repository = git.getRepository();
        var treeWalk = jGitWrapper.getTreeWalk(repository, path)
    ) {
      log.trace("Retrieving files from {} at path {}", repositoryName, path);
      List<String> result = Objects.nonNull(treeWalk) ? getFiles(treeWalk) : List.of();
      log.debug("Found {} files in repository {} at path {}", result.size(), result, path);
      return result;
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Cacheable(DATE_CACHE_NAME)
  @Override
  @Nullable
  public FileDatesDto getDates(@NonNull String repositoryName, @NonNull String filePath) {
    log.debug("Retrieving git commit dates in repository {} for path {}", repositoryName, filePath);
    var repositoryDirectory = getExistedRepository(repositoryName);

    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryDirectory)) {
      log.trace("Retrieving commit stack for file {}", filePath);
      var revCommitList = getRevCommitList(filePath, git);

      if (revCommitList.isEmpty()) {
        log.debug("Git commit dates in repository {} for path {} wasn't found", repositoryName,
            filePath);
        return null;
      }

      log.trace("Retrieving updated date-time as first element in stack and created as last");
      var updatedTime = getCommitDateTime(revCommitList.get(0));
      var createdTime = getCommitDateTime(revCommitList.get(revCommitList.size() - 1));

      log.debug("Git commit dates in repository {} for path {} retrieved", repositoryName,
          filePath);
      return FileDatesDto.builder()
          .create(createdTime)
          .update(updatedTime)
          .build();
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  @Nullable
  public String getFileContent(@NonNull String repositoryName, @NonNull String filePath) {
    log.debug("Retrieving file content from repository {} at path {}", repositoryName, filePath);
    var repositoryDirectory = getExistedRepository(repositoryName);

    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (
        var git = openRepo(repositoryDirectory);
        var repository = git.getRepository();
        var treeWalk = jGitWrapper.getTreeWalk(repository, filePath)
    ) {
      log.trace("Reading file content from treeWalk");
      return Objects.isNull(treeWalk) ? null : getFileContent(repository, treeWalk);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  public void amend(@NonNull String repositoryName, @NonNull String filePath,
      @NonNull String fileContent) {
    log.debug("Trying to update file content in repository {} at path {}", repositoryName,
        filePath);
    var repositoryDirectory = getExistedRepository(repositoryName);

    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryDirectory)) {
      log.trace("Updating file at path {}", filePath);
      var file = gitFileService.writeFile(repositoryName, fileContent, filePath);
      log.trace("Commit file {} in repo {} with amend", filePath, repositoryName);
      doAmend(repositoryDirectory, file, git);
      log.debug("File {} updated in repo {}", filePath, repositoryName);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  public void delete(@NonNull String repositoryName, @NonNull String filePath) {
    log.debug("Trying to delete file from repository {} at path {}", repositoryName, filePath);
    var repositoryDirectory = getExistedRepository(repositoryName);

    log.trace("Synchronizing repo {}", repositoryName);
    var lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryDirectory)) {
      log.trace("Deleting file at path {}", filePath);
      var fileToDelete = new File(repositoryDirectory, FilenameUtils.normalize(filePath));
      if (fileToDelete.delete()) {
        log.trace("Commit file {} in repo {} with amend", filePath, repositoryName);
        doAmend(repositoryDirectory, fileToDelete, git);
        log.debug("File {} deleted from repo {}", filePath, repositoryName);
      }
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  public void deleteRepo(String repoName) {
    var repositoryFile = getRepositoryDir(repoName);
    try {
      FileUtils.delete(repositoryFile, FileUtils.RECURSIVE | FileUtils.SKIP_MISSING);
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during deleting repository %s: %s", repoName,
              e.getMessage()), e);
    }
  }

  @Override
  public boolean repoExists(String repositoryName) {
    return getRepositoryDir(repositoryName).exists();
  }

  @NonNull
  private Git cloneRepo(@NonNull File repositoryDirectory) {
    var cloneCommand = jGitWrapper.cloneRepository()
        .setURI(getRepositoryUrl())
        .setCredentialsProvider(getCredentialsProvider())
        .setCloneAllBranches(true)
        .setDirectory(repositoryDirectory);
    try {
      return Objects.requireNonNull(retryable.call(cloneCommand),
          "CloneCommand#call cannot be null");
    } catch (InvalidRemoteException e) {
      throw new IllegalStateException(
          String.format("Remote that is configured under \"gerrit\" prefix is invalid: %s",
              e.getMessage()), e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred during cloning repository %s: %s",
              repositoryDirectory.getName(), e.getMessage()), e);
    }
  }

  private Git openRepo(File repositoryDirectory) {
    try {
      return jGitWrapper.open(repositoryDirectory);
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during repository opening: %s", e.getMessage()), e);
    }
  }

  /**
   * Fetch method that needs opened {@link Git}
   */
  private void fetchAll(Git git) {
    fetch(git, null);
  }

  /**
   * Fetch specific refs with opened {@link Git}
   */
  private void fetch(@NonNull Git git, @Nullable String refs) {
    var fetchCommand = git.fetch()
        .setCredentialsProvider(getCredentialsProvider());
    if (Objects.nonNull(refs)) {
      fetchCommand.setRefSpecs(refs);
    }
    try {
      retryable.call(fetchCommand);
    } catch (InvalidRemoteException e) {
      throw new IllegalStateException("Default remote \"origin\" cannot be invalid", e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred while fetching: %s", e.getMessage()), e);
    }
  }

  private void checkoutFetchHead(@NonNull Git git) {
    var checkoutCommand = git.checkout()
        .setName(Constants.FETCH_HEAD);
    try {
      checkoutCommand.call();
    } catch (RefAlreadyExistsException | RefNotFoundException | InvalidRefNameException |
             CheckoutConflictException e) {
      // Checkout on FETCH_HEAD must not create any new refs, ref FETCH_HEAD must always exist
      // ref FETCH_HEAD must be always valid and there must not be any conflicts during such checkout
      throw new IllegalStateException(
          String.format("Checkout on FETCH_HEAD must not throw such exception: %s", e.getMessage()),
          e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred while checkout: %s", e.getMessage()), e);
    }
  }

  private void hardResetOnOriginHeadBranch(@NonNull Git git) {
    var resetCommand = git.reset()
        .setMode(ResetType.HARD)
        .setRef(Constants.DEFAULT_REMOTE_NAME + "/" + gerritPropertiesConfig.getHeadBranch());
    try {
      resetCommand.call();
    } catch (CheckoutConflictException e) {
      throw new IllegalStateException(
          String.format("Hard reset must not face any conflicts: %s", e.getMessage()), e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred during hard reset on origin %s: %s",
              gerritPropertiesConfig.getHeadBranch(), e.getMessage()), e);
    }
  }

  private static List<String> getFiles(TreeWalk treeWalk) {
    var files = new ArrayList<String>();
    try {
      treeWalk.enterSubtree();
      treeWalk.setRecursive(true);
      while (treeWalk.next()) {
        files.add(FilenameUtils.getName(treeWalk.getPathString()));
      }
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during reading files by path: %s", e.getMessage()),
          e);
    }
    Collections.sort(files);
    return files;
  }

  @NonNull
  private static LocalDateTime getCommitDateTime(@NonNull RevCommit commit) {
    return LocalDateTime.ofEpochSecond(commit.getCommitTime(), 0, ZoneOffset.UTC);
  }

  @NonNull
  private static String getFileContent(@NonNull Repository repository, @NonNull TreeWalk treeWalk) {
    try {
      var objectId = treeWalk.getObjectId(0);
      var loader = repository.open(objectId);
      return new String(loader.getBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during reading file content by path: %s",
              e.getMessage()), e);
    }
  }

  private void doAmend(File repoDirectory, File file, Git git) {
    addFileToGit(repoDirectory, file, git);
    var gitStatus = status(git);
    if (!gitStatus.isClean()) {
      commitAmend(git);
      pushChanges(git);
    }
  }

  private void commitAmend(Git git) {
    try {
      var lastCommit = git.log().call().iterator().next();
      git.commit()
          .setMessage(lastCommit.getFullMessage())
          .setAmend(true)
          .call();
    } catch (AbortedByHookException | ConcurrentRefUpdateException | NoHeadException |
             NoMessageException | ServiceUnavailableException | UnmergedPathsException |
             WrongRepositoryStateException e) {
      throw new IllegalStateException(
          String.format("Log/commit command doesn't expected to throw such exception: %s",
              e.getMessage()), e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred during amending commit: %s", e.getMessage()), e);
    }
  }

  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  private File getRepositoryDir(String repositoryName) {
    var repoDir = gerritPropertiesConfig.getRepositoryDirectory();
    return new File(
        FilenameUtils.normalizeNoEndSeparator(repoDir), FilenameUtils.getName(repositoryName));
  }

  private File getExistedRepository(String repositoryName) {
    var repo = getRepositoryDir(repositoryName);
    if (!repo.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
    return repo;
  }

  private Lock getLock(String repositoryName) {
    return lockMap.computeIfAbsent(repositoryName, s -> new ReentrantLock());
  }

  private List<RevCommit> getRevCommitList(String filePath, Git git) {
    var log = git.log();
    log.addPath(filePath);
    try {
      var revCommitIterable = log.call();
      return StreamSupport.stream(revCommitIterable.spliterator(), false)
          .collect(Collectors.toList());
    } catch (NoHeadException e) {
      // It's not expected of HEAD reference to disappear
      throw new IllegalStateException("HEAD reference doesn't exists", e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Could not execute log command: %s", e.getMessage()), e);
    }
  }

  private UsernamePasswordCredentialsProvider getCredentialsProvider() {
    return new UsernamePasswordCredentialsProvider(gerritPropertiesConfig.getUser(),
        gerritPropertiesConfig.getPassword());
  }

  private void addFileToGit(File repoDirectory, File file, Git git) {
    var filePattern = FilenameUtils.normalize(repoDirectory.toPath()
        .relativize(file.toPath()).toString(), true);
    try {
      if (file.exists()) {
        git.add().addFilepattern(filePattern).call();
      } else {
        git.rm().addFilepattern(filePattern).call();
      }
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Could not execute add/rm command: %s", e.getMessage()), e);
    }
  }

  private static Status status(Git git) {
    try {
      return git.status().call();
    } catch (NoWorkTreeException e) {
      throw new IllegalStateException(
          String.format("Work tree mustn't disappear: %s", e.getMessage()), e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Could not execute status command: %s", e.getMessage()), e);
    }
  }

  private void pushChanges(Git git) {
    var addCommand = git.remoteAdd()
        .setUri(getRepositoryURIish())
        .setName(Constants.DEFAULT_REMOTE_NAME);
    var push = git.push()
        .setCredentialsProvider(getCredentialsProvider())
        .setRemote(Constants.DEFAULT_REMOTE_NAME)
        .setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));

    try {
      addCommand.call();
      retryable.call(push);
    } catch (InvalidRemoteException e) {
      throw new IllegalStateException(
          String.format("Remote that is configured under \"gerrit\" prefix is invalid: %s",
              e.getMessage()), e);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Could not execute add-remote/push command: %s", e.getMessage()), e);
    }
  }

  private URIish getRepositoryURIish() {
    try {
      return new URIish(getRepositoryUrl())
          .setUser(gerritPropertiesConfig.getUser())
          .setPass(gerritPropertiesConfig.getPassword());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(
          "Repository url that is configured under \"gerrit\" prefix is invalid");
    }
  }

  private String getRepositoryUrl() {
    return gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository();
  }
}
