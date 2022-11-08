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

package com.epam.digital.data.platform.management.osintegration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;

@Service
@RequiredArgsConstructor
public class VaultServiceImpl implements VaultService {

  @Value("${vault.key}")
  private final String keyName;
  private final VaultOperations operations;

  @Override
  public String decrypt(String encryptedContent) {
    return operations.opsForTransit().decrypt(keyName, encryptedContent);
  }

  @Override
  public String encrypt(String content) {
    return operations.opsForTransit().encrypt(keyName, content);
  }
}
