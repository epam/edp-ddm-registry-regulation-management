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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JGitServiceImpl implements JGitService {

  private static final String ORIGIN = "origin";
  private static final String REPOSITORY_DOES_NOT_EXIST = "Repository does not exist";
  public static final String DATE_CACHE_NAME = "dates";

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final GitFileService gitFileService;
  private final JGitWrapper jGitWrapper;

  private final ConcurrentMap<String, Lock> lockMap = new ConcurrentHashMap<>();

  @Override
  public void cloneRepo(String repositoryName) {
    log.debug("Trying to clone repository {}", repositoryName);
    var directory = getRepositoryDir(repositoryName);
    if (directory.exists()) {
      // TODO throw exception if repository already exists
      return;
    }
    log.trace("Synchronizing repo {}", repositoryName);
    Lock lock = getLock(repositoryName);
    lock.lock();
    try (var call = jGitWrapper.cloneRepository()
        .setURI(getRepositoryUrl())
        .setCredentialsProvider(getCredentialsProvider())
        .setCloneAllBranches(true)
        .setDirectory(directory)
        .call()) {
      log.debug("Repository {} was successfully cloned.", repositoryName);
    } catch (GitAPIException e) {
      throw new GitCommandException(
          String.format("Exception occurred during cloning repository %s: %s",
              repositoryName, e.getMessage()), e);
    } finally {
      lock.unlock();
      log.trace("Repo {} lock released", repositoryName);
    }
  }

  @Override
  public void resetHeadBranchToRemote() {
    var repositoryName = gerritPropertiesConfig.getHeadBranch();
    log.debug("Trying to reset repository {} to remote state", repositoryName);
    var repositoryDirectory = getRepositoryDir(repositoryName);
    if (!repositoryDirectory.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
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
    var repositoryDirectory = getRepositoryDir(repositoryName);
    if (!repositoryDirectory.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
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
    log.debug("Repository {} was successfully fetched and checkout to ref {}", repositoryName, refs);
  }

  @Override
  public List<String> getFilesInPath(String repositoryName, String path) {
    File repositoryDirectory = getRepositoryDir(repositoryName);
    if (!repositoryDirectory.exists()) {
      return List.of();
    }
    List<String> items = new ArrayList<>();
    Lock lock = getLock(repositoryName);
    lock.lock();
    try (var repository = openRepo(repositoryDirectory).getRepository()) {
      RevTree tree = jGitWrapper.getRevTree(repository);
      if (path != null && !path.isEmpty()) {
        try (TreeWalk treeWalk = jGitWrapper.getTreeWalk(repository, path, tree)) {
          if (treeWalk != null) {
            try (TreeWalk dirWalk = jGitWrapper.getTreeWalk(repository)) {
              dirWalk.addTree(treeWalk.getObjectId(0));
              dirWalk.setRecursive(true);
              while (dirWalk.next()) {
                items.add(dirWalk.getPathString());
              }
            }
          }
        }
      } else {
        try (TreeWalk treeWalk = jGitWrapper.getTreeWalk(repository)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            items.add(treeWalk.getPathString());
          }
        }
      }
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during reading files by path: %s", e.getMessage()), e);
    } finally {
      lock.unlock();
    }
    Collections.sort(items);
    return items;
  }

  @Cacheable(DATE_CACHE_NAME)
  @Override
  public FileDatesDto getDates(String repositoryName, String filePath) {
    File repositoryDirectory = getRepositoryDir(repositoryName);
    if (!repositoryDirectory.exists()) {
      return null;
    }
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    try (var git = openRepo(repositoryDirectory)) {
      LogCommand log = git.log();
      log.addPath(filePath);
      Iterable<RevCommit> call = getRevCommits(log);
      Iterator<RevCommit> iterator = call.iterator();
      RevCommit revCommit = iterator.next();
      RevCommit last = iterator.next();
      while (iterator.hasNext()) {
        last = iterator.next();
      }
      fileDatesDto.setUpdate(
          LocalDateTime.ofEpochSecond(revCommit.getCommitTime(), 0, ZoneOffset.UTC));
      LocalDateTime createDate = LocalDateTime.ofEpochSecond(
          last != null ? last.getCommitTime() : revCommit.getCommitTime(), 0, ZoneOffset.UTC);
      fileDatesDto.setCreate(createDate);
    }
    return fileDatesDto;
  }

  @Override
  public String getFileContent(String repositoryName, String filePath) {
    File repositoryDirectory = getRepositoryDir(repositoryName);
    if (!repositoryDirectory.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
    Lock lock = getLock(repositoryName);
    lock.lock();
    try (var repository = openRepo(repositoryDirectory).getRepository()) {
      if (filePath != null && !filePath.isEmpty()) {
        RevTree tree = jGitWrapper.getRevTree(repository);
        try (TreeWalk treeWalk = jGitWrapper.getTreeWalk(repository)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          treeWalk.setFilter(PathFilter.create(filePath));
          if (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
          }
        }
      }
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during reading file content by path: %s",
              e.getMessage()), e);
    } finally {
      lock.unlock();
    }
    return null;
  }

  @Override
  public void amend(String repositoryName, String refs, String commitMessage, String changeId,
      String filePath, String fileContent) {
    File repositoryFile = getRepositoryDir(repositoryName);
    if (!repositoryFile.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
    Lock lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryFile)) {
      fetch(git, refs);
      checkoutFetchHead(git);
      File file = gitFileService.writeFile(repositoryName, fileContent, filePath);
      if (file != null) {
        doAmend(file, commitMessage, changeId, git);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  public void delete(String repositoryName, String filePath, String refs, String commitMessage,
      String changeId) {
    File repositoryFile = getRepositoryDir(repositoryName);
    if (!repositoryFile.exists()) {
      throw new RepositoryNotFoundException(
          String.format("Repository %s doesn't exists", repositoryName), repositoryName);
    }
    Lock lock = getLock(repositoryName);
    lock.lock();
    try (var git = openRepo(repositoryFile)) {
      fetch(git, refs);
      checkoutFetchHead(git);

      var repoDir = FilenameUtils.normalizeNoEndSeparator(
          gerritPropertiesConfig.getRepositoryDirectory());
      var fileDirectory =
          repoDir + File.separator + repositoryName + File.separator + filePath;
      File fileToDelete = new File(FilenameUtils.getFullPathNoEndSeparator(fileDirectory),
          FilenameUtils.getName(filePath));
      if (fileToDelete.delete()) {
        doAmend(fileToDelete, commitMessage, changeId, git);
      }
    } finally {
      lock.unlock();
    }
  }

  public void deleteRepo(String repoName) {
    File repositoryFile = getRepositoryDir(repoName);
    if (!repositoryFile.exists()) {
      return;
    }
    try {
      FileUtils.delete(repositoryFile, FileUtils.RECURSIVE);
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during deleting repository %s: %s", repoName,
              e.getMessage()), e);
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
      fetchCommand.call();
    } catch (InvalidRemoteException e) {
      throw new IllegalStateException("Default remote \"origin\" cannot be invalid", e);
    } catch (TransportException e) {
      // TODO add retry for TransportException
      throw new RuntimeException(
          String.format("Transport exception occurred while fetching: %s", e.getMessage()), e);
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

  private void doAmend(File file, String commitMessage, String changeId, Git git) {
    try {
      addFileToGit(file, git);
      Status gitStatus = git.status().call();

      if (!gitStatus.isClean()) {
        git.commit().setMessage(commitMessageWithChangeId(commitMessage, changeId))
            .setAmend(true).call();
        pushChanges(git);
      }
    } catch (URISyntaxException | GitAPIException e) {
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

  private Lock getLock(String repositoryName) {
    return lockMap.computeIfAbsent(repositoryName, s -> new ReentrantLock());
  }

  private Iterable<RevCommit> getRevCommits(LogCommand log) {
    try {
      return log.call();
    } catch (GitAPIException e) {
      throw new GitCommandException("Could not execute call command", e);
    }
  }

  private UsernamePasswordCredentialsProvider getCredentialsProvider() {
    return new UsernamePasswordCredentialsProvider(gerritPropertiesConfig.getUser(),
        gerritPropertiesConfig.getPassword());
  }

  private void addFileToGit(File file, Git git) throws GitAPIException {
    String filePattern =
        FilenameUtils.getName(file.getParent()) + "/" + FilenameUtils.getName(file.getName());
    if (file.exists()) {
      git.add().addFilepattern(filePattern).call();
    } else {
      git.rm().addFilepattern(filePattern).call();
    }
  }

  private String commitMessageWithChangeId(String commitMessage, String changeId) {
    return commitMessage + "\n\n" + "Change-Id: " + changeId;
  }

  private void pushChanges(Git git) throws URISyntaxException, GitAPIException {
    RemoteAddCommand addCommand = git.remoteAdd();
    addCommand.setUri(new URIish(getRepositoryUrl())
        .setUser(gerritPropertiesConfig.getUser())
        .setPass(gerritPropertiesConfig.getPassword()));
    addCommand.setName(ORIGIN);
    addCommand.call();
    PushCommand push = git.push();
    push.setCredentialsProvider(getCredentialsProvider());
    push.setRemote(ORIGIN);
    push.setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));
    push.call();
  }

  private String getRepositoryUrl() {
    return gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository();
  }
}
