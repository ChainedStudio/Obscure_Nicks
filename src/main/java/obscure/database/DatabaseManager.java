package obscure.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        File dataFolder = new File(plugin.getDataFolder(), "data.db");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite database!", e);
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing the database connection!", e);
        }
    }

    private void createTables() {
        // Inside your DatabaseManager's createTables() or connection block, ensure this is the exact column layout:
        String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "nickname VARCHAR(16), " +
                "skin_value TEXT, " +
                "skin_signature TEXT, " +
                "rank VARCHAR(64)" +
                ");";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database tables!", e);
        }
    }

    public void savePlayerData(UUID uuid, String nickname, String skinValue, String skinSignature, String rank) {
        String sql = "INSERT INTO player_data(uuid, nickname, skin_value, skin_signature, rank) VALUES(?,?,?,?,?) " +
                "ON CONFLICT(uuid) DO UPDATE SET nickname=excluded.nickname, skin_value=excluded.skin_value, " +
                "skin_signature=excluded.skin_signature, rank=excluded.rank;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, nickname);
            stmt.setString(3, skinValue);
            stmt.setString(4, skinSignature);
            stmt.setString(5, rank);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for: " + uuid, e);
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        String sql = "SELECT * FROM player_data WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            rs.getString("nickname"),
                            rs.getString("skin_value"),
                            rs.getString("skin_signature"),
                            rs.getString("rank")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for: " + uuid, e);
        }
        return null;
    }

    public static class PlayerData {
        private final String nickname;
        private final String skinValue;
        private final String skinSignature;
        private final String rank;

        public PlayerData(String nickname, String skinValue, String skinSignature, String rank) {
            this.nickname = nickname;
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            this.rank = rank;
        }

        public String getNickname() { return nickname; }
        public String getSkinValue() { return skinValue; }
        public String getSkinSignature() { return skinSignature; }
        public String getRank() { return rank; }
    }
}