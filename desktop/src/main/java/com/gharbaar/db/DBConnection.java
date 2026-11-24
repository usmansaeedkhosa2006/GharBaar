package com.gharbaar.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * =====================================================================
 * [DB LAYER] DBConnection  -  «pure fabrication, singleton»  (GoF: Singleton)
 * One shared JDBC connection for the whole desktop app.
 * Reads connection settings from db.properties, searched in:
 *   1) ./db.properties   2) ~/.gharbaar/db.properties   3) classpath
 * =====================================================================
 */
public final class DBConnection {

    // ---- Singleton instance ----
    private static DBConnection instance;

    private Connection connection;
    private final String url;
    private final String user;
    private final String password;

    private DBConnection() {
        Properties p = loadProperties();
        this.url = p.getProperty("db.url");
        this.user = p.getProperty("db.user");
        this.password = p.getProperty("db.password");
        if (url == null || url.isBlank())
            throw new DataAccessException("db.url missing in db.properties", null);
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    /** Returns a live connection, reconnecting if the old one timed out. */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                connection = DriverManager.getConnection(url, user, password);
            }
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database. Check your internet connection.", e);
        }
    }

    // ---- [helper] locate db.properties ----
    private static Properties loadProperties() {
        Properties p = new Properties();
        Path[] candidates = {
                Path.of("db.properties"),
                Path.of(System.getProperty("user.home"), ".gharbaar", "db.properties")
        };
        try {
            for (Path c : candidates) {
                if (Files.exists(c)) {
                    try (InputStream in = new FileInputStream(c.toFile())) { p.load(in); return p; }
                }
            }
            try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
                if (in != null) { p.load(in); return p; }
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not read db.properties", e);
        }
        throw new DataAccessException("db.properties not found. Copy db.properties.example to db.properties.", null);
    }
}
