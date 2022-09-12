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

package com.epam.digital.data.platform.management.service.impl;

import static com.epam.digital.data.platform.management.config.CacheCustomizer.DATE_CACHE_NAME;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.GitCommandException;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.JGitService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JGitServiceImpl implements JGitService {

  private static final String ORIGIN = "origin";
  private static final String REPOSITORY_DOES_NOT_EXIST = "Repository does not exist";

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final RequestToFileConverter requestToFileConverter;
  private final JGitWrapper jGitWrapper;

  private final ConcurrentMap<String, Lock> lockMap = new ConcurrentHashMap<>();

  @Override
  public void cloneRepo(String versionName) throws Exception {
    File directory = getRepositoryDir(versionName);
    if (directory.exists()) {
      pull(versionName);
      return;
    }
    Lock lock = getLock(versionName);
    lock.lock();
    try (Git call = jGitWrapper.cloneRepository()
        .setURI(getRepositoryUrl())
        .setCredentialsProvider(getCredentialsProvider())
        .setDirectory(directory)
        .setCloneAllBranches(true)
        .call()) {
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void pull(String versionName) throws Exception {
    File repositoryDirectory = getRepositoryDir(versionName);
    if (!repositoryDirectory.exists()) {
      return;
    }
    Lock lock = getLock(versionName);
    lock.lock();
    try (Git git = jGitWrapper.open(repositoryDirectory)) {
      git.checkout().setName("refs/heads/" + gerritPropertiesConfig.getHeadBranch()).call();
      git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void fetch(String versionName, ChangeInfoDto changeInfoDto) throws Exception {
    var repositoryDirectory = getRepositoryDir(versionName);
    if (!repositoryDirectory.exists()) {
      return;
    }
    Lock lock = getLock(versionName);
    lock.lock();
    try (Git git = jGitWrapper.open(repositoryDirectory)) {
      fetch(git, changeInfoDto);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Fetch method that needs opened {@link Git}
   */
  private void fetch(Git git, ChangeInfoDto changeInfoDto) throws GitAPIException {
    git.fetch()
        .setCredentialsProvider(getCredentialsProvider())
        .setRefSpecs(changeInfoDto.getRefs())
        .call();
    git.checkout()
        .setName("FETCH_HEAD")
        .call();
  }

  @Override
  public List<String> getFilesInPath(String versionName, String path) throws Exception {
    File repositoryDirectory = getRepositoryDir(versionName);
    if (!repositoryDirectory.exists()) {
      return List.of();
    }
    List<String> items = new ArrayList<>();
    Lock lock = getLock(versionName);
    lock.lock();
    try (Repository repository = jGitWrapper.open(repositoryDirectory).getRepository()) {
      RevTree tree = jGitWrapper.getRevTree(repository);
      if (path != null && !path.isEmpty()) {
        try (TreeWalk treeWalk = jGitWrapper.getTreeWalk(repository, path, tree)) {
          try (TreeWalk dirWalk = jGitWrapper.getTreeWalk(repository)) {
            dirWalk.addTree(treeWalk.getObjectId(0));
            dirWalk.setRecursive(true);
            while (dirWalk.next()) {
              items.add(dirWalk.getPathString());
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
    } finally {
      lock.unlock();
    }
    Collections.sort(items);
    return items;
  }

  @Cacheable(DATE_CACHE_NAME)
  @Override
  public FileDatesDto getDates(String versionName, String filePath) {
    File repositoryDirectory = getRepositoryDir(versionName);
    if (!repositoryDirectory.exists()) {
      return null;
    }
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    try(Git git = getGit(repositoryDirectory)) {
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

  @CacheEvict(value = DATE_CACHE_NAME, allEntries = true)
  @Override
  public void formDatesCacheEvict() {
  }

  @Override
  public String getFileContent(String versionName, String filePath) throws Exception {
    File repositoryDirectory = getRepositoryDir(versionName);
    if (!repositoryDirectory.exists()) {
      return REPOSITORY_DOES_NOT_EXIST;
    }
    Lock lock = getLock(versionName);
    lock.lock();
    try (Repository repository = jGitWrapper.open(repositoryDirectory).getRepository()) {
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
    } finally {
      lock.unlock();
    }
    return REPOSITORY_DOES_NOT_EXIST;
  }

  @Override
  public String amend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto)
      throws Exception {
    File repositoryFile = getRepositoryDir(requestDto.getVersionName());
    if (!repositoryFile.exists()) {
      return null;
    }
    Lock lock = getLock(requestDto.getVersionName());
    lock.lock();
    try (Git git = jGitWrapper.open(repositoryFile)) {
      fetch(git, changeInfoDto);
      File file = requestToFileConverter.convert(requestDto);
      if (file != null) {
        return doAmend(file, changeInfoDto, git);
      }
    } finally {
      lock.unlock();
    }
    return null;
  }

  @Override
  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  public String delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception {
    File repositoryFile = getRepositoryDir(changeInfoDto.getNumber());
    if (!repositoryFile.exists()) {
      return null;
    }
    Lock lock = getLock(changeInfoDto.getNumber());
    lock.lock();
    try (Git git = jGitWrapper.open(repositoryFile)) {
      fetch(git, changeInfoDto);

      var repoDir = FilenameUtils.normalizeNoEndSeparator(
          gerritPropertiesConfig.getRepositoryDirectory());
      var fileDirectory =
          repoDir + File.separator + changeInfoDto.getNumber() + File.separator + fileName;
      File fileToDelete = new File(FilenameUtils.getFullPathNoEndSeparator(fileDirectory),
          FilenameUtils.getName(fileName));
      if (fileToDelete.delete()) {
        return doAmend(fileToDelete, changeInfoDto, git);
      }
    } finally {
      lock.unlock();
    }
    return null;
  }

  public void deleteRepo(String repoName) throws IOException {
    File repositoryFile = getRepositoryDir(repoName);
    if (!repositoryFile.exists()) {
      return;
    }

    FileUtils.delete(repositoryFile, FileUtils.RECURSIVE);
  }

  private String doAmend(File file, ChangeInfoDto changeInfoDto, Git git)
      throws GitAPIException, URISyntaxException {
    addFileToGit(file, git);
    Status gitStatus = git.status().call();

    if (!gitStatus.isClean()) {
      RevCommit commit = git.commit().setMessage(commitMessageWithChangeId(changeInfoDto))
          .setAmend(true).call();
      pushChanges(git);
      return commit.getId().toString();
    }
    return REPOSITORY_DOES_NOT_EXIST;
  }

  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  private File getRepositoryDir(String versionName) {
    var repoDir = gerritPropertiesConfig.getRepositoryDirectory();
    return new File(
        FilenameUtils.normalizeNoEndSeparator(repoDir), FilenameUtils.getName(versionName));
  }

  private Lock getLock(String versionName) {
    return lockMap.computeIfAbsent(versionName, s -> new ReentrantLock());
  }

  private Iterable<RevCommit> getRevCommits(LogCommand log) {
    try {
      return log.call();
    } catch (GitAPIException e) {
      throw new GitCommandException("Could not execute call command", e);
    }
  }

  private Git getGit(File repositoryDirectory) {
    try {
      return jGitWrapper.open(repositoryDirectory);
    } catch (IOException e) {
      throw new GitCommandException("Could not open repo", e);
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

  private String commitMessageWithChangeId(ChangeInfoDto changeInfoDto) {
    return changeInfoDto.getSubject() + "\n\n" + "Change-Id: " + changeInfoDto.getChangeId();
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
