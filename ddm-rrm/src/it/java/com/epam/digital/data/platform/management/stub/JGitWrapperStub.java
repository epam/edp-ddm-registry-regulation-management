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
import com.epam.digital.data.platform.management.gitintegration.service.JGitWrapper;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;

public class JGitWrapperStub extends JGitWrapper {

  @Autowired
  private TestExecutionContext context;

  @Override
  public CloneCommandStub cloneRepository() {
    return new CloneCommandStub(context);
  }

  @Override
  public Git open(File repositoryDirectory) throws IOException {
    final var git = super.open(repositoryDirectory);
    final var repo = git.getRepository();
    return new GitStub(repo, context);
  }
}
