package com.gharbaar.bl.domain;

import com.gharbaar.bl.service.PasswordHasher;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [BL - DOMAIN] User  (abstract)
 * Base class of Customer, Agent, Admin (and Tenant via Customer).
 * OOP: abstraction (getRole / getHomeView are abstract) +
 *      polymorphism (each subclass decides its own home screen).
 * GRASP: Information Expert for password checking,
 *        Creator of Report (User files a Report).
 * =====================================================================
 */
public abstract class User {

    // ---- attributes (protected, as in the DCD) ----
    protected int userID;
    protected String username;
    protected String passwordHash;
    protected String name;
    protected String phone;
    protected boolean active = true;

    protected User(int userID, String username, String passwordHash, String name, String phone) {
        this.userID = userID;
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
    }

    // ---------------------------------------------------------------
    // [Expert] password verification (UC-02 Login)
    // ---------------------------------------------------------------
    public boolean verifyPassword(String raw, PasswordHasher h) {
        return raw != null && h.matches(raw, passwordHash);
    }

    // ---------------------------------------------------------------
    // [Creator] a User creates the Report he files (UC-05)
    // ---------------------------------------------------------------
    public Report fileReport(String type, String description) {
        return new Report(this, type, description);
    }

    // ---- abstract (polymorphic) operations ----
    public abstract String getRole();
    public abstract String getHomeView();

    // ---------------------------------------------------------------
    // [Validation] registration input (UC-01 Register)
    // ---------------------------------------------------------------
    public static List<String> validateRegistration(String username, String password, String name, String phone) {
        List<String> errors = new ArrayList<>();
        if (username == null || !username.matches("[A-Za-z0-9_]{3,30}"))
            errors.add("Username must be 3-30 letters, digits or underscores.");
        if (password == null || password.length() < 8)
            errors.add("Password must be at least 8 characters.");
        else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            errors.add("Password must contain letters and numbers.");
        if (name == null || name.trim().length() < 3 || name.trim().length() > 80)
            errors.add("Full name must be 3-80 characters.");
        if (phone == null || !phone.matches("03\\d{9}"))
            errors.add("Phone must be a Pakistani mobile number like 03001234567.");
        return errors;
    }

    // ---- getters / encapsulated setters ----
    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Only the repository assigns the generated id once, after INSERT. */
    public void assignId(int id) {
        if (this.userID != 0) throw new IllegalStateException("ID already assigned");
        this.userID = id;
    }

    @Override public String toString() { return name + " (" + getRole() + ")"; }
}
