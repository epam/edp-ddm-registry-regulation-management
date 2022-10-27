/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.management.service.impl;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class JGitWrapper {

  public Git open(File repositoryDirectory) throws IOException {
    return Git.open(repositoryDirectory);
  }

  public CloneCommand cloneRepository() {
    return Git.cloneRepository();
  }

  public RevTree getRevTree(Repository repository) throws IOException {
    ObjectId lastCommitId = repository.resolve("HEAD");
    RevWalk revWalk = new RevWalk(repository);
    return revWalk.parseCommit(lastCommitId).getTree();
  }

  public TreeWalk getTreeWalk(Repository repository, String path, RevTree tree) throws IOException {
    return TreeWalk.forPath(repository, path, tree);
  }

  public TreeWalk getTreeWalk(Repository r) {
    return new TreeWalk(r);
  }
}
