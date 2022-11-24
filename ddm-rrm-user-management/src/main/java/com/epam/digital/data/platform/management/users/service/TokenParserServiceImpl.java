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

import com.epam.digital.data.platform.management.users.exception.JwtParsingException;
import com.epam.digital.data.platform.management.users.model.JwtClaims;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenParserServiceImpl implements TokenParserService {

  private final ObjectMapper objectMapper;

  @Override
  @NonNull
  public JwtClaims parseClaims(@NonNull String token) {
    try {
      var signedJWT = SignedJWT.parse(token);
      return objectMapper.readValue(signedJWT.getPayload().toString(), JwtClaims.class);
    } catch (ParseException | JsonProcessingException e) {
      throw new JwtParsingException("Error while JWT parsing", e);
    }
  }
}