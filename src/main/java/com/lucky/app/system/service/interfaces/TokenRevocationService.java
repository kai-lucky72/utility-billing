package com.lucky.app.system.service.interfaces;

/** Contract for the JWT blacklist: revoke a token, check revocation, and purge expired entries. */
public interface TokenRevocationService {
    void revoke(String token);
    boolean isRevoked(String token);
    void deleteExpiredTokens();
}
