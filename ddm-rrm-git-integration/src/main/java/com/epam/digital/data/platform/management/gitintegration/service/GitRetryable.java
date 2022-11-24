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

package com.epam.digital.data.platform.management.gitintegration.service;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Class that is used for retryable git command calling
 */
@Component
public class GitRetryable {

  /**
   * Call retryable git command
   *
   * @param gitCommand command to call
   * @param <T>        return type of {@link GitCommand<T>}
   * @return {@link GitCommand<T>#call()} result
   *
   * @throws GitAPIException if {@link GitCommand<T>#call()} throw such
   */
  @Retryable(TransportException.class)
  @Nullable
  public <T> T call(@NonNull GitCommand<T> gitCommand) throws GitAPIException {
    return gitCommand.call();
  }
}
