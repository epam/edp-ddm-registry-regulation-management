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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.epam.digital.data.platform.management.users.exception.JwtParsingException;
import com.epam.digital.data.platform.management.users.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenParserServiceTest {

  TokenParserService tokenParserService;

  @BeforeEach
  void init() {
    this.tokenParserService = new TokenParserServiceImpl(new ObjectMapper());
  }

  @Test
  void validParseClaims() {
    var token = TestUtils.getContent("user-token");

    var jwtClaims = tokenParserService.parseClaims(token);

    assertThat(jwtClaims.getSubject()).isEqualTo("49acde86-8ab9-43f4-a74b-a9c9bf8eee53");
    assertThat(jwtClaims.getEdrpou()).isEqualTo("11111111");
    assertThat(jwtClaims.getDrfo()).isEqualTo("0101010101");
    assertThat(jwtClaims.getFullName()).isEqualTo("Петров Петр Петровіч");
  }

  @Test
  void shouldThrowJwtParsingExceptionDueToParsingException() {
    assertThatCode(() -> tokenParserService.parseClaims("qwert"))
        .isInstanceOf(JwtParsingException.class)
        .hasMessage("Error while JWT parsing");
  }

}