package com.gharbaar.db;

import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.bl.domain.User;
import com.gharbaar.bl.domain.UserFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * =====================================================================
 * [DB LAYER] UserRepository  «pure fabrication»   -> table: users
 * =====================================================================
 */
public class UserRepository extends BaseRepository {

    static final String COLS = "u.user_id, u.username, u.password_hash, u.name, u.phone, u.role, u.cnic, " +
            "u.tenant_status, u.tenant_rejection_reason, u.is_active";

    /** Row -> correct User subclass (via UserFactory). Reused by other repositories. */
    static User mapUser(ResultSet rs) throws SQLException {
        return UserFactory.fromRecord(rs.getInt("user_id"), rs.getString("role"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("name"), rs.getString("phone"), rs.getString("cnic"),
                rs.getString("tenant_status"), rs.getString("tenant_rejection_reason"), rs.getBoolean("is_active"));
    }

    // ---- UC-01 Register ----
    public void save(User u) {
        String role = u.getRole().equals("TENANT") ? "CUSTOMER" : u.getRole();
        int id = insert("INSERT INTO users (username, password_hash, name, phone, role) VALUES (?,?,?,?,?)",
                u.getUsername(), u.getPasswordHash(), u.getName(), u.getPhone(), role);
        u.assignId(id);
    }

    /** Saves name/phone/active flag and tenant fields. */
    public void update(User u) {
        if (u instanceof Tenant t) {
            execute("UPDATE users SET name=?, phone=?, is_active=?, cnic=?, tenant_status=?, tenant_rejection_reason=? WHERE user_id=?",
                    u.getName(), u.getPhone(), u.isActive(), t.getCnic(), t.getVerificationStatus().name(),
                    t.getRejectionReason(), u.getUserID());
        } else {
            execute("UPDATE users SET name=?, phone=?, is_active=? WHERE user_id=?",
                    u.getName(), u.getPhone(), u.isActive(), u.getUserID());
        }
    }

    public boolean existsByUsername(String username) {
        return count("SELECT COUNT(*) FROM users WHERE LOWER(username)=LOWER(?)", username) > 0;
    }

    // ---- UC-02 Login ----
    public User findByUsername(String username) {
        return queryOne("SELECT " + COLS + " FROM users u WHERE LOWER(u.username)=LOWER(?)", UserRepository::mapUser, username);
    }

    public User findById(int id) {
        return queryOne("SELECT " + COLS + " FROM users u WHERE u.user_id=?", UserRepository::mapUser, id);
    }

    // ---- UC-07 Verify Tenant ----
    public Tenant findTenantById(int tenantId) {
        User u = findById(tenantId);
        return u instanceof Tenant t ? t : null;
    }

    public List<Tenant> findUnverifiedTenants() {
        return query("SELECT " + COLS + " FROM users u WHERE u.tenant_status='PENDING' ORDER BY u.user_id",
                rs -> (Tenant) mapUser(rs));
    }

    // ---- Admin: manage users ----
    public List<User> findAll() {
        return query("SELECT " + COLS + " FROM users u ORDER BY u.role, u.name", UserRepository::mapUser);
    }

    public int countByRole(String role) {
        return count("SELECT COUNT(*) FROM users WHERE role=?", role);
    }
}
