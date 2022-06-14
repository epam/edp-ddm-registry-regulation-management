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

import com.epam.digital.data.platform.upload.exception.JwtParsingException;
import com.epam.digital.data.platform.upload.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenParserServiceTest {

  TokenParserService tokenParserService;

  @BeforeEach
  void init() {
    this.tokenParserService = new TokenParserService(new ObjectMapper());
  }

  @Test
  void validParseClaims() {
    var token = TestUtils.getContent("user-token");

    var jwtClaims = tokenParserService.parseClaims(token);

    assertEquals("496fd2fd-3497-4391-9ead-41410522d06f", jwtClaims.getSubject());
    assertEquals("34554362", jwtClaims.getEdrpou());
    assertEquals("1010101014", jwtClaims.getDrfo());
    assertEquals("Сидоренко Василь Леонідович", jwtClaims.getFullName());
  }

  @Test
  void shouldThrowJwtParsingExceptionDueToParsingException() {
    var exception = assertThrows(JwtParsingException.class,
            () -> tokenParserService.parseClaims("qwert"));

    assertThat(exception.getMessage()).isEqualTo("Error while JWT parsing");
  }

}