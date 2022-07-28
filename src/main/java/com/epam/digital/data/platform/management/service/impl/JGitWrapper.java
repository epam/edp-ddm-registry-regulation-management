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

    public RevTree getRevTree(Repository repository, String headBranch) throws IOException {
        ObjectId lastCommitId = repository.resolve("refs/heads/" + headBranch);
        RevWalk revWalk = new RevWalk(repository);
        return revWalk.parseCommit(lastCommitId).getTree();
    }

    public TreeWalk getTreeWalk(Repository repository, String path, RevTree tree) throws IOException {
        return TreeWalk.forPath(repository, path, tree);
    }
    public TreeWalk getTreeWalk(Repository r){
        return new TreeWalk(r);
    }
}
