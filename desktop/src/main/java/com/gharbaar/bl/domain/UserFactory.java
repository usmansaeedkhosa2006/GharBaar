package com.gharbaar.bl.domain;

/**
 * =====================================================================
 * [BL - DOMAIN] UserFactory   (GoF: Factory)
 * Decides WHICH User subclass to instantiate, so no other class has to
 * switch on the role string (Protected Variations: adding a new role
 * only changes this class).
 * =====================================================================
 */
public final class UserFactory {

    private UserFactory() { }

    /** Used by UC-01 Register: only Customer and Agent can self-register. */
    public static User createNew(String role, String username, String passwordHash, String name, String phone) {
        return switch (role) {
            case "CUSTOMER" -> new Customer(0, username, passwordHash, name, phone);
            case "AGENT" -> new Agent(0, username, passwordHash, name, phone);
            default -> throw new IllegalArgumentException("You can only register as a Customer or an Agent.");
        };
    }

    /** Used by UserRepository to rebuild a user from a database row. */
    public static User fromRecord(int id, String role, String username, String hash, String name, String phone,
                                  String cnic, String tenantStatus, String tenantRejection, boolean active) {
        User u = switch (role) {
            case "ADMIN" -> new Admin(id, username, hash, name, phone);
            case "AGENT" -> new Agent(id, username, hash, name, phone);
            default -> tenantStatus == null
                    ? new Customer(id, username, hash, name, phone)
                    : new Tenant(id, username, hash, name, phone, cnic,
                                 VerificationStatus.valueOf(tenantStatus), tenantRejection);
        };
        u.setActive(active);
        return u;
    }
}
