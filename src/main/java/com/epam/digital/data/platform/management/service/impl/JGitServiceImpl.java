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

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.JGitService;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class JGitServiceImpl implements JGitService {

  private static final String ORIGIN = "origin";
  private static final String REPOSITORY_DOES_NOT_EXIST = "Repository does not exist";

  @Autowired
  private GerritPropertiesConfig gerritPropertiesConfig;

  @Autowired
  private RequestToFileConverter requestToFileConverter;

  @PostConstruct
  public void cloneRepo() throws Exception {
    File directory = getRepositoryFile();
    if (directory.exists()) {
      pull();
    } else {
      Git.cloneRepository()
              .setURI(gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository())
              .setCredentialsProvider(getCredentialsProvider())
              .setDirectory(directory)
              .setCloneAllBranches(true)
              .call();
    }
  }

  @Override
  public void pull() throws Exception {
    File repositoryDirectory = new File(gerritPropertiesConfig.getRepositoryDirectory());
    if(repositoryDirectory.exists()) {
      Git git = Git.open(repositoryDirectory);
      git.checkout().setName("refs/heads/" + gerritPropertiesConfig.getHeadBranch()).call();
      git.pull().setCredentialsProvider(getCredentialsProvider())
              .setRebase(true).call();
    }
  }

  @Override
  public List<String> getFilesInPath(String path) throws Exception {
    List<String> items = new ArrayList<>();
    File repositoryDirectory = getRepositoryFile();
    if(repositoryDirectory.exists()) {
      Repository repository = Git.open(repositoryDirectory).getRepository();
      if (path != null && !path.isEmpty()) {
        RevTree tree = getRevCommit(repository).getTree();
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree)) {
          try (TreeWalk dirWalk = new TreeWalk(repository)) {
            dirWalk.addTree(treeWalk.getObjectId(0));
            dirWalk.setRecursive(true);
            while (dirWalk.next()) {
              items.add(dirWalk.getPathString());
            }
          }
        }
      } else {
        RevTree tree = getRevCommit(repository).getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            items.add(treeWalk.getPathString());
          }
        }
      }
    }
    return items;
  }

  @Override
  public String getFileContent(String filePath) throws Exception {
    File repositoryDirectory = getRepositoryFile();
    if(repositoryDirectory.exists()) {
      Repository repository = Git.open(repositoryDirectory).getRepository();
      if (filePath != null && !filePath.isEmpty()) {
        RevTree tree = getRevCommit(repository).getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
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
    }
    return REPOSITORY_DOES_NOT_EXIST;
  }

  @Override
  public String convertAndAmend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception {
    File repositoryFile = getRepositoryFile();
    if(repositoryFile.exists()) {
      Git git = Git.open(repositoryFile);
      git.fetch()
              .setCredentialsProvider(getCredentialsProvider())
              .setRefSpecs(changeInfoDto.getRefs()).call();
      git.checkout().setName("FETCH_HEAD").call();
      File file = requestToFileConverter.convert(requestDto);
      if (file != null) {
        return amend(file, changeInfoDto, git);
      }
    }
    return null;
  }

  @Override
  public String amend(File file, ChangeInfoDto changeInfoDto, Git git) throws Exception {
    addFileToGit(file, git);
    Status gitStatus = git.status().call();

    if (!gitStatus.isClean()) {
      RevCommit commit = git.commit()
              .setMessage(commitMessageWithChangeId(changeInfoDto))
              .setAmend(true)
              .call();
      pushChanges(git);
      return commit.getId().toString();
    }
    return REPOSITORY_DOES_NOT_EXIST;
  }

  @Override
  public String delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception {
    File repositoryFile = getRepositoryFile();
    if(repositoryFile.exists()) {
      Git git = Git.open(repositoryFile);
      git.fetch()
              .setCredentialsProvider(getCredentialsProvider())
              .setRefSpecs(changeInfoDto.getRefs()).call();
      git.checkout().setName("FETCH_HEAD").call();

      File fileToDelete = new File(FilenameUtils.getFullPathNoEndSeparator(
              gerritPropertiesConfig.getRepositoryDirectory() + "forms/"), FilenameUtils.getName(fileName));
      if (fileToDelete.delete()) {
        return amend(fileToDelete, changeInfoDto, git);
      }
    }
    return null;
  }

  private File getRepositoryFile(){
    return new File(FilenameUtils.getFullPathNoEndSeparator(gerritPropertiesConfig.getRepositoryDirectory()));
  }

  private UsernamePasswordCredentialsProvider getCredentialsProvider() {
    return new UsernamePasswordCredentialsProvider(
            gerritPropertiesConfig.getUser(), gerritPropertiesConfig.getPassword());
  }

  private void addFileToGit(File file, Git git) throws Exception {
      String filePattern = FilenameUtils.getName(file.getParent()) + "/" + FilenameUtils.getName(file.getName());
      if (file.exists()) {
        git.add().addFilepattern(filePattern).call();
      } else {
        git.rm().addFilepattern(filePattern).call();
      }
  }

  private RevCommit getRevCommit(Repository repository) throws IOException {
    ObjectId lastCommitId = repository.resolve("refs/heads/" + gerritPropertiesConfig.getHeadBranch());
    RevWalk revWalk = new RevWalk(repository);
    return revWalk.parseCommit(lastCommitId);
  }

  private String commitMessageWithChangeId(ChangeInfoDto changeInfoDto){
    return changeInfoDto.getSubject() +
            "\n\n" +
            "Change-Id: " +
            changeInfoDto.getChangeId();
  }

  private void pushChanges(Git git) throws Exception {
    RemoteAddCommand addCommand = git.remoteAdd();
    addCommand.setUri(new URIish(gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository())
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
}
