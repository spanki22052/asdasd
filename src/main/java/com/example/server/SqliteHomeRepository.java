package com.example.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance SQLite-backed repository with connection pooling. Uses
 * HikariCP for optimal database performance.
 */
public class SqliteHomeRepository implements HomeRepository {

    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;

    public SqliteHomeRepository(JavaPlugin plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File dbFile = new File(plugin.getDataFolder(), "homes.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS homes (
                uuid TEXT PRIMARY KEY NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL,
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
            """;

        String createIndex = "CREATE INDEX IF NOT EXISTS idx_homes_updated ON homes(updated_at)";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute(createIndex);
            plugin.getLogger().info("SQLite database initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Home> loadAll() {
        Map<UUID, Home> homes = new HashMap<>();
        String query = "SELECT uuid, world, x, y, z, yaw, pitch FROM homes";

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    Home home = new Home(
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                    homes.put(uuid, home);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database: " + rs.getString("uuid"));
                }
            }

            plugin.getLogger().info("Loaded " + homes.size() + " homes from database");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load homes: " + e.getMessage());
        }

        return homes;
    }

    @Override
    public void saveAll(Map<UUID, Home> homes) {
        String upsert = """
            INSERT INTO homes (uuid, world, x, y, z, yaw, pitch, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(uuid) DO UPDATE SET
                world = excluded.world,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                yaw = excluded.yaw,
                pitch = excluded.pitch,
                updated_at = excluded.updated_at
            """;

        String delete = "DELETE FROM homes WHERE uuid NOT IN (SELECT uuid FROM (SELECT ? AS uuid))";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // Batch insert/update
            try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
                for (Map.Entry<UUID, Home> entry : homes.entrySet()) {
                    UUID uuid = entry.getKey();
                    Home home = entry.getValue();

                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, home.world());
                    stmt.setDouble(3, home.x());
                    stmt.setDouble(4, home.y());
                    stmt.setDouble(5, home.z());
                    stmt.setFloat(6, home.yaw());
                    stmt.setFloat(7, home.pitch());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Remove homes that no longer exist in cache
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM homes WHERE uuid NOT IN ("
                    + String.join(",", homes.keySet().stream().map(uuid -> "?").toList()) + ")")) {
                int idx = 1;
                for (UUID uuid : homes.keySet()) {
                    stmt.setString(idx++, uuid.toString());
                }
                if (!homes.isEmpty()) {
                    stmt.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save homes: " + e.getMessage());
        }
    }

    /**
     * Single home save operation for individual updates (more efficient than
     * saveAll)
     */
    public void saveHome(UUID uuid, Home home) {
        String upsert = """
            INSERT INTO homes (uuid, world, x, y, z, yaw, pitch, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(uuid) DO UPDATE SET
                world = excluded.world,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                yaw = excluded.yaw,
                pitch = excluded.pitch,
                updated_at = excluded.updated_at
            """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(upsert)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, home.world());
            stmt.setDouble(3, home.x());
            stmt.setDouble(4, home.y());
            stmt.setDouble(5, home.z());
            stmt.setFloat(6, home.yaw());
            stmt.setFloat(7, home.pitch());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save home for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Single home delete operation
     */
    public void deleteHome(UUID uuid) {
        String delete = "DELETE FROM homes WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(delete)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete home for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Close the connection pool
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
    }
}
