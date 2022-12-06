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

package com.epam.digital.data.platform.management.restapi.exception;

import lombok.Getter;

@Getter
public class ETagFilteringException extends RuntimeException {

  private final String formName;
  private final String versionCandidate;

  public ETagFilteringException(String message, String formName, String versionCandidate) {
    super(message);
    this.formName = formName;
    this.versionCandidate = versionCandidate;
  }
}
