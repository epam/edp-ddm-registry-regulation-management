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

package com.epam.digital.data.platform.management.forms.exception;

import lombok.Getter;
/**
 * Thrown in case if it couldn't find requested form
 */
@Getter
public class FormNotFoundException extends RuntimeException {
  private final String formName;
  public FormNotFoundException(String message, String formName) {
    super(message);
    this.formName = formName;
  }
}
