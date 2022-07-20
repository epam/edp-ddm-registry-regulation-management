package com.epam.digital.data.platform.upload.service;

public interface VaultService {
    String decrypt(String encryptedContent);

    String encrypt(String content);
}
