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
