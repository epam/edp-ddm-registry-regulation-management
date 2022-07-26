package com.epam.digital.data.platform.management.service.impl;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
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
}
