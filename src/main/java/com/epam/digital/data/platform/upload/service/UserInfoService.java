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

package com.epam.digital.data.platform.upload.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class UserInfoService {

  private final TokenParserService tokenParserService;

  public UserInfoService(TokenParserService tokenParserService) {
    this.tokenParserService = tokenParserService;
  }

  public String createUsername(String token) {
    var jwtClaims = tokenParserService.parseClaims(token);

    var str = jwtClaims.getFullName().toLowerCase() + jwtClaims.getEdrpou() + jwtClaims.getDrfo();
    return DigestUtils.sha256Hex(str.getBytes(StandardCharsets.UTF_8));
  }
}