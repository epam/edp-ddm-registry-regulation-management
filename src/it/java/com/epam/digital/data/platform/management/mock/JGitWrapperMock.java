package com.epam.digital.data.platform.management.mock;

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectLoader.SmallObject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.mockito.Mockito;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JGitWrapperMock {

  private static final String FORM_CONTENT = "{\n" +
      "  \"type\": \"form\",\n" +
      "  \"components\": [\n" +
      "   ],\n" +
      "  \"title\": \"title\",\n" +
      "  \"path\": \"add-fizfactors1\",\n" +
      "  \"name\": \"add-fizfactors1\",\n" +
      "  \"display\": \"form\",\n" +
      "  \"submissionAccess\": []\n" +
      "}";

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Getter
  private JGitWrapper jGitWrapper;
  @Getter
  private CloneCommand cloneCommand;

  @PostConstruct
  public void init() {
    jGitWrapper = Mockito.mock(JGitWrapper.class);
    mockCloneCommand();
    mockGetFileInPath();
  }

  @SneakyThrows
  private void mockCloneCommand() {
    cloneCommand = Mockito.mock(CloneCommand.class);
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);

    var repoURI = gerritPropertiesConfig.getUrl() + "/" + gerritPropertiesConfig.getRepository();
    Mockito.when(cloneCommand.setURI(repoURI)).thenReturn(cloneCommand);

    var credentialsProvider = new UsernamePasswordCredentialsProvider(
        gerritPropertiesConfig.getUser(), gerritPropertiesConfig.getPassword());
    Mockito.when(cloneCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(cloneCommand);

    var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(),
        gerritPropertiesConfig.getHeadBranch());
    Mockito.when(cloneCommand.setDirectory(directory)).thenReturn(cloneCommand);

    Mockito.when(cloneCommand.setCloneAllBranches(true)).thenReturn(cloneCommand);

    Mockito.doAnswer(invocationOnMock -> {
      log.info("Called clone command for {} version", "master");
      directory.mkdirs();
      return null;
    }).when(cloneCommand).call();
  }

  @SneakyThrows
  private void mockGetFileInPath() {
    var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(),
        gerritPropertiesConfig.getHeadBranch());
    var git = Mockito.mock(Git.class);
    Mockito.when(jGitWrapper.open(directory)).thenReturn(git);
    var repository = Mockito.mock(Repository.class);
    Mockito.when(git.getRepository()).thenReturn(repository);
    var revTree = Mockito.mock(RevTree.class);
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    var treeWalk = Mockito.mock(TreeWalk.class);
    Mockito.when(jGitWrapper.getTreeWalk(repository, "forms", revTree)).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false)
        .thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    var logCommand = Mockito.mock(LogCommand.class);
    Mockito.when(git.log()).thenReturn(logCommand);
    var revCommit = Mockito.mock(RevCommit.class);
    var revCommit2 = Mockito.mock(RevCommit.class);
    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
    SmallObject smallObject = new SmallObject(1, FORM_CONTENT.getBytes(StandardCharsets.UTF_8));
    Mockito.when(repository.open(any())).thenReturn(smallObject);
  }
}

