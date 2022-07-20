package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.upload.model.JwtClaims;

public interface TokenParserService {
    JwtClaims parseClaims(String token);
}
