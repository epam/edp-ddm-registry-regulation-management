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

package com.epam.digital.data.platform.management.users.service;

import com.epam.digital.data.platform.management.users.model.JwtClaims;
import org.springframework.lang.NonNull;

/**
 * Service that is used to parse x-access-token claims
 */
public interface TokenParserService {

  /**
   * Parses JWT claims
   *
   * @param token JWT to parse
   * @return {@link JwtClaims} claims representation
   */
  @NonNull
  JwtClaims parseClaims(@NonNull String token);
}
