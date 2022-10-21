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

package com.epam.digital.data.platform.management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.management.service.impl.VaultServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

  static final String VAULT_KEY = "key";

  @Mock
  VaultTransitOperations vaultTransitOperations;

  @Mock
  VaultOperations operations;

  VaultService vaultService;

  @BeforeEach
  void init() {
    when(operations.opsForTransit()).thenReturn(vaultTransitOperations);
    this.vaultService = new VaultServiceImpl(VAULT_KEY, operations);
  }

  @Test
  void validEncryptData() {
    final var content = "content";
    when(vaultTransitOperations.encrypt(VAULT_KEY, content)).thenReturn(content);

    var encryptedContent = vaultService.encrypt(content);

    assertEquals(content, encryptedContent);
  }

  @Test
  void validDecryptData() {
    final var content = "content";
    when(vaultTransitOperations.decrypt(VAULT_KEY, content)).thenReturn(content);

    var decryptedContent = vaultService.decrypt(content);

    assertEquals(content, decryptedContent);
  }
}