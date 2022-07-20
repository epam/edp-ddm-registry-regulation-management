package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.JwtClaims;

public interface TokenParserService {
    JwtClaims parseClaims(String token);
}
