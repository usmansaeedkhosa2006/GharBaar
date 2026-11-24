package com.gharbaar.bl.domain;

/**
 * [BL - DOMAIN] Admin  extends User
 * Verifies listings (UC-06), tenants (UC-07) and resolves reports (UC-08).
 */
public class Admin extends User {

    public Admin(int userID, String username, String passwordHash, String name, String phone) {
        super(userID, username, passwordHash, name, phone);
    }

    @Override public String getRole() { return "ADMIN"; }
    @Override public String getHomeView() { return "AdminHome"; }
}
