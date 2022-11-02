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

package com.epam.digital.data.platform.management.configuration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.epam.digital.data.platform.management.mock.VaultOperationsMock;
import com.epam.digital.data.platform.management.gitintegration.service.JGitWrapper;
import com.epam.digital.data.platform.management.stub.JGitWrapperStub;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.core.VaultOperations;

@Configuration
@RequiredArgsConstructor
public class TestConfiguration {

  private final VaultOperationsMock vaultOperationsMock;

  @Bean
  @Primary
  public JGitWrapper jGitWrapperStub() {
    return new JGitWrapperStub();
  }

  @Bean
  @SneakyThrows
  @Qualifier("test-directory")
  public File testDirectory(@Value("${gerrit.repository-directory}") String tempDirPath) {
    return new File(tempDirPath);
  }

  @Bean(destroyMethod = "stop")
  @SneakyThrows
  @Qualifier("gerritMockServer")
  public WireMockServer gerritMockServer(@Value("${gerrit.url}") String urlStr) {
    final var url = new URL(urlStr);
    final var wireMockServer = new WireMockRule(wireMockConfig().port(url.getPort()));
    WireMock.configureFor(url.getHost(), url.getPort());
    wireMockServer.start();
    return wireMockServer;
  }

  @Bean
  @Primary
  public VaultOperations vaultOperations() {
    return vaultOperationsMock.getVaultOperations();
  }
}
