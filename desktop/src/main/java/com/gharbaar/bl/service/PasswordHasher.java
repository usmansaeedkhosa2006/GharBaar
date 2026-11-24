package com.gharbaar.bl.service;

import org.mindrot.jbcrypt.BCrypt;

/**
 * =====================================================================
 * [BL - SERVICE] PasswordHasher  «pure fabrication»
 * UC-01 special requirement: "Password must be saved in a protected
 * (hashed) format". Uses BCrypt (same format as the web version).
 * =====================================================================
 */
public class PasswordHasher {

    public String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(10));
    }

    public boolean matches(String raw, String hash) {
        try {
            return hash != null && BCrypt.checkpw(raw, hash);
        } catch (IllegalArgumentException badHash) {
            return false;
        }
    }
}
