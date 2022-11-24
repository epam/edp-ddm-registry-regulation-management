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
import java.util.List;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;

public class FetchCommandStub extends FetchCommand {

  private final TestExecutionContext context;

  protected FetchCommandStub(Repository repo, TestExecutionContext context) {
    super(repo);
    this.context = context;
  }

  @Override
  public FetchCommand setRefSpecs(List<RefSpec> specs) {
    final var expectedSpec = context.getVersionCandidate().getRef();
    Assertions.assertThat(specs)
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("source", expectedSpec);

    super.setRefSpecs(List.of(new RefSpec(
        String.format("refs/heads/%s_ref", context.getVersionCandidate().getNumber()))));
    return this;
  }
}
