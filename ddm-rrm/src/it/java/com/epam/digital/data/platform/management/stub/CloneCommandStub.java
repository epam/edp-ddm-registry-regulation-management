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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.CloneCommand;

@RequiredArgsConstructor
public class CloneCommandStub extends CloneCommand {

  private final TestExecutionContext context;

  @Override
  @SneakyThrows
  public CloneCommand setURI(String uri) {
    Assertions.assertThat(uri).isEqualTo(
        context.getGerritProps().getUrl() + "/" + context.getGerritProps().getRepository());

    return super.setURI(context.getHeadRepo().getAbsolutePath());
  }
}
