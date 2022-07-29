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
import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JGitServiceImpl implements JGitService {

    private static final String ORIGIN = "origin";
    private static final String REPOSITORY_DOES_NOT_EXIST = "Repository does not exist";

    @Autowired
    private GerritPropertiesConfig gerritPropertiesConfig;

    @Autowired
    private RequestToFileConverter requestToFileConverter;

    @Autowired
    private JGitWrapper jGitWrapper;

    @Override
    public void cloneRepo(String versionName) throws Exception {
        File directory = getRepositoryDir(versionName);
        if (directory.exists()) {
            pull(versionName);
        } else {
            jGitWrapper.cloneRepository().setURI(gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository()).setCredentialsProvider(getCredentialsProvider()).setDirectory(directory).setCloneAllBranches(true).call();
        }
    }

    @Override
    public void pull(String versionName) throws Exception {
        File repositoryDirectory = getRepositoryDir(versionName);
        if (!repositoryDirectory.exists()) {
            return;
        }
        try (Git git = jGitWrapper.open(repositoryDirectory)) {
            git.checkout().setName("refs/heads/" + gerritPropertiesConfig.getHeadBranch()).call();
            git.pull().setCredentialsProvider(getCredentialsProvider()).setRebase(true).call();
        }
    }

    @Override
    public List<String> getFilesInPath(String versionName, String path) throws Exception {
        File repositoryDirectory = getRepositoryDir(versionName);
        if (!repositoryDirectory.exists()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        try (Repository repository = jGitWrapper.open(repositoryDirectory).getRepository()) {
            RevTree tree = jGitWrapper.getRevTree(repository,
                gerritPropertiesConfig.getHeadBranch());
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
        }
        return items;
    }

    @Override
    public String getFileContent(String versionName, String filePath) throws Exception {
        File repositoryDirectory = getRepositoryDir(versionName);
        if (!repositoryDirectory.exists()) {
            return REPOSITORY_DOES_NOT_EXIST;
        }
        try (Repository repository = jGitWrapper.open(repositoryDirectory).getRepository()) {
            if (filePath != null && !filePath.isEmpty()) {
                RevTree tree = jGitWrapper.getRevTree(repository,
                    gerritPropertiesConfig.getHeadBranch());
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
        }
        return REPOSITORY_DOES_NOT_EXIST;
    }

    @Override
    public String amend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception {
        File repositoryFile = getRepositoryDir(requestDto.getVersionName());
        if (!repositoryFile.exists()) {
            return null;
        }
        try (Git git = jGitWrapper.open(repositoryFile)) {
            git.fetch().setCredentialsProvider(getCredentialsProvider())
                .setRefSpecs(changeInfoDto.getRefs()).call();
            git.checkout().setName("FETCH_HEAD").call();
            File file = requestToFileConverter.convert(requestDto);
            if (file != null) {
                return doAmend(file, changeInfoDto, git);
            }
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
        try (Git git = jGitWrapper.open(repositoryFile)) {
            git.fetch().setCredentialsProvider(getCredentialsProvider())
                .setRefSpecs(changeInfoDto.getRefs()).call();
            git.checkout().setName("FETCH_HEAD").call();

            var repoDir = FilenameUtils.normalizeNoEndSeparator(
                gerritPropertiesConfig.getRepositoryDirectory());
            var fileDirectory =
                repoDir + File.separator + changeInfoDto.getNumber() + File.separator + fileName;
            File fileToDelete = new File(FilenameUtils.getFullPathNoEndSeparator(fileDirectory),
                FilenameUtils.getName(fileName));
            if (fileToDelete.delete()) {
                return doAmend(fileToDelete, changeInfoDto, git);
            }
        }
        return null;
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
        addCommand.setUri(new URIish(
            gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository()).setUser(
            gerritPropertiesConfig.getUser()).setPass(gerritPropertiesConfig.getPassword()));
        addCommand.setName(ORIGIN);
        addCommand.call();
        PushCommand push = git.push();
        push.setCredentialsProvider(getCredentialsProvider());
        push.setRemote(ORIGIN);
        push.setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));
        push.call();
    }
}
