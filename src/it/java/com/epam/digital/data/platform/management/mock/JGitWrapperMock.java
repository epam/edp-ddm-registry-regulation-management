package com.epam.digital.data.platform.management.mock;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import java.io.File;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.mockito.Mockito;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JGitWrapperMock {

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Getter
  private JGitWrapper jGitWrapper;
  @Getter
  private CloneCommand cloneCommand;

  @PostConstruct
  public void init() {
    jGitWrapper = Mockito.mock(JGitWrapper.class);
    mockCloneCommand();
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
      return null;
    }).when(cloneCommand).call();
  }
}

