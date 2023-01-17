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

package com.epam.digital.data.platform.management.stub;

import com.epam.digital.data.platform.management.context.TestExecutionContext;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

public class RemoteAddCommandStub extends RemoteAddCommand {

  private final TestExecutionContext context;

  protected RemoteAddCommandStub(Repository repo, TestExecutionContext context) {
    super(repo);
    this.context = context;
  }

  @Override
  @SneakyThrows
  public RemoteAddCommand setUri(URIish uri) {
    final var gerritProps = context.getGerritProps();
    final var gerritUrl = new URIish(gerritProps.getUrl());
    Assertions.assertThat(uri)
        .hasFieldOrPropertyWithValue("host", gerritUrl.getHost())
        .hasFieldOrPropertyWithValue("port", gerritUrl.getPort())
        .hasFieldOrPropertyWithValue("scheme", gerritUrl.getScheme())
        .hasFieldOrPropertyWithValue("rawPath", "/" + gerritProps.getRepository())
        .hasFieldOrPropertyWithValue("user", gerritProps.getUser())
        .hasFieldOrPropertyWithValue("pass", gerritProps.getPassword());

    super.setUri(new URIish(context.getRemoteHeadRepo().getAbsolutePath()));
    return this;
  }
}
