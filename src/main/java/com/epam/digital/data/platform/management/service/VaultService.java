package com.epam.digital.data.platform.management.service;

public interface VaultService {
    String decrypt(String encryptedContent);

    String encrypt(String content);
}
