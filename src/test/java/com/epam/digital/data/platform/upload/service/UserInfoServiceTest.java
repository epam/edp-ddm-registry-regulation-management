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

import com.epam.digital.data.platform.upload.model.JwtClaims;
import com.epam.digital.data.platform.upload.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceTest {

  @Mock
  TokenParserService tokenParserService;

  UserInfoService userInfoService;

  @Test
  void validCreateUsername() {
    userInfoService = new UserInfoService(new TokenParserService(new ObjectMapper()));
    var token = TestUtils.getContent("user-token");

    var username = userInfoService.createUsername(token);

    assertEquals("c0ab3771f5cd55655855c6b13d919e1bca19d08e6b345064025c05bbd574f802", username);
  }
}