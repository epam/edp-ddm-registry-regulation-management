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

package com.epam.digital.data.platform.management.exception;

import lombok.Getter;

/**
 * Thrown in case if it couldn't find requested process
 */
@Getter
public class ProcessNotFoundException extends RuntimeException {
  private final String processName;
  public ProcessNotFoundException(String message, String processName) {
    super(message);
    this.processName = processName;
  }

  public ProcessNotFoundException(String message, Throwable cause, String processName) {
    super(message, cause);
    this.processName = processName;
  }
}
