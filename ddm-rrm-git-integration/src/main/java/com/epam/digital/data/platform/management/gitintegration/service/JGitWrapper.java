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

import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class JGitWrapper {

  @NonNull
  public Git open(@NonNull File repositoryDirectory) throws IOException {
    return Git.open(repositoryDirectory);
  }

  @NonNull
  public CloneCommand cloneRepository() {
    return Git.cloneRepository();
  }

  @NonNull
  protected RevTree getRevTree(@NonNull Repository repository) throws IOException {
    ObjectId lastCommitId = repository.resolve("HEAD");
    RevWalk revWalk = new RevWalk(repository);
    return revWalk.parseCommit(lastCommitId).getTree();
  }

  @Nullable
  public TreeWalk getTreeWalk(@NonNull Repository repository, @NonNull String path) {
    try {
      return getTreeWalk(repository, path, getRevTree(repository));
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during getting reference to commit tree: %s",
              e.getMessage()), e);
    }
  }

  @Nullable
  public TreeWalk getTreeWalk(@NonNull Repository repository, @NonNull String path,
      @NonNull RevTree revTree) {
    try {
      return TreeWalk.forPath(repository, path, revTree);
    } catch (IOException e) {
      throw new GitCommandException(
          String.format("Exception occurred during getting repository tree walk: %s",
              e.getMessage()), e);
    }
  }

  public String readFileContent(Path path) throws IOException {
    return Files.readString(path);
  }
}
