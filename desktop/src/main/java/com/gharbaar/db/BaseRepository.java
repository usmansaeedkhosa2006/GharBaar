package com.gharbaar.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [DB LAYER] BaseRepository  (abstract)
 * Common JDBC helpers inherited by every repository:
 *   - query()   : SELECT -> List<T>
 *   - insert()  : INSERT ... RETURNING id
 *   - execute() : UPDATE / DELETE
 *   - inTransaction() : run several statements atomically
 * All SQLExceptions are converted to DataAccessException.
 * =====================================================================
 */
public abstract class BaseRepository {

    /** Maps one ResultSet row to an object. */
    @FunctionalInterface
    protected interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    /** Work done inside a transaction. */
    @FunctionalInterface
    protected interface TxWork<T> { T run(Connection c) throws SQLException; }

    protected Connection conn() { return DBConnection.getInstance().getConnection(); }

    // ---- SELECT ----
    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database read failed: " + e.getMessage(), e);
        }
    }

    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    protected int count(String sql, Object... params) {
        Integer n = queryOne(sql, rs -> rs.getInt(1), params);
        return n == null ? 0 : n;
    }

    // ---- INSERT ... RETURNING ----
    protected int insert(String sql, Object... params) {
        return insert(conn(), sql, params);
    }

    protected int insert(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database write failed: " + e.getMessage(), e);
        }
    }

    // ---- UPDATE / DELETE ----
    protected int execute(String sql, Object... params) {
        return execute(conn(), sql, params);
    }

    protected int execute(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Database update failed: " + e.getMessage(), e);
        }
    }

    // ---- TRANSACTION ----
    protected <T> T inTransaction(TxWork<T> work) {
        Connection c = conn();
        try {
            c.setAutoCommit(false);
            T result = work.run(c);
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try { c.rollback(); } catch (SQLException ignored) { }
            if (e instanceof DataAccessException dae) throw dae;
            throw new DataAccessException("Database transaction failed: " + e.getMessage(), e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    // ---- helpers ----
    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDateTime t) ps.setTimestamp(i + 1, Timestamp.valueOf(t));
            else if (p instanceof java.time.LocalDate d) ps.setDate(i + 1, java.sql.Date.valueOf(d));
            else ps.setObject(i + 1, p);
        }
    }

    protected static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    /** builds "?,?,?" for IN clauses */
    protected static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }
}
