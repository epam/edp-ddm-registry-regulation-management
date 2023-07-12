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

package com.epam.digital.data.platform.management.stub;

import com.epam.digital.data.platform.management.context.TestExecutionContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;

public class PushCommandStub extends PushCommand {

  private final TestExecutionContext context;

  protected PushCommandStub(Repository repo, TestExecutionContext context) {
    super(repo);
    this.context = context;
  }

  @Override
  public PushCommand setCredentialsProvider(CredentialsProvider credentialsProvider) {
    if (credentialsProvider != CredentialsProvider.getDefault()) {
      Assertions.assertThat(credentialsProvider)
          .hasFieldOrPropertyWithValue("username", context.getGerritProps().getUser())
          .hasFieldOrPropertyWithValue("password",
              context.getGerritProps().getPassword().toCharArray());
    }

    return this;
  }

  @Override
  public PushCommand setRefSpecs(RefSpec... specs) {
    return this.setRefSpecs(Arrays.asList(specs));
  }

  @Override
  public PushCommand setRefSpecs(List<RefSpec> specs) {
    Assertions.assertThat(specs)
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("source", "HEAD")
        .satisfiesAnyOf(
            spec -> Assertions.assertThat(spec)
                .hasFieldOrPropertyWithValue("destination",
                    "refs/for/" + context.getGerritProps().getHeadBranch() + "%private,submit"),
            spec -> Assertions.assertThat(spec)
                .hasFieldOrPropertyWithValue("destination",
                    "refs/for/" + context.getGerritProps().getHeadBranch()));

    if (Objects.nonNull(context.getVersionCandidate())) {
      super.setRefSpecs(List.of(new RefSpec(
          String.format("HEAD:refs/heads/%s_ref", context.getVersionCandidate().getNumber()))));
    }
    super.setForce(true);
    return this;
  }
}
