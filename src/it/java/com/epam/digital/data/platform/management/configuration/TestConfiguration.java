package com.epam.digital.data.platform.management.configuration;

import com.epam.digital.data.platform.management.mock.GerritApiMock;
import com.epam.digital.data.platform.management.mock.JGitWrapperMock;
import com.epam.digital.data.platform.management.mock.VaultOperationsMock;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.core.VaultOperations;

@Configuration
@RequiredArgsConstructor
public class TestConfiguration {

  private final GerritApiMock gerritApiMock;
  private final JGitWrapperMock jGitWrapperMock;
  private final VaultOperationsMock vaultOperationsMock;

  @Bean
  @Primary
  public GerritApiImpl gerritApi() {
    return gerritApiMock.getGerritApi();
  }

  @Bean
  @Primary
  public JGitWrapper gitWrapper() {
    return jGitWrapperMock.getJGitWrapper();
  }

  @Bean
  @Primary
  public VaultOperations vaultOperations() {
    return vaultOperationsMock.getVaultOperations();
  }
}
