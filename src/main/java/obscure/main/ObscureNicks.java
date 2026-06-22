package obscure.main;

import obscure.database.DatabaseManager;
import obscure.hooks.ObscureNicksExpansion;
import obscure.commands.NickCommand;
import obscure.managers.NicknameManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class ObscureNicks extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private NicknameManager nicknameManager;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Override
    public void onEnable() {
        // Save default configuration maps
        saveDefaultConfig();
        createMessagesConfig();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.nicknameManager = new NicknameManager(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ObscureNicksExpansion(this).register();
        }

        NickCommand commandExecutor = new NickCommand(this);
        String[] commands = {"nick", "togglenick", "random", "nicked", "nickname", "nickskin", "nickrank", "fakerank"};
        for (String cmd : commands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(commandExecutor);
            }
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public String getMessage(String path) {
        String msg = messagesConfig.getString(path, "");
        String prefix = messagesConfig.getString("prefix", "");
        if (path.startsWith("prefix") || path.startsWith("status-summary")) {
            return ChatColor.translateAlternateColorCodes('&', msg);
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nicknameManager.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        nicknameManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Component nicknameComponent = event.getPlayer().displayName();
        event.renderer((source, sourceDisplayName, message, viewer) ->
                Component.text("<")
                        .append(nicknameComponent)
                        .append(Component.text("> "))
                        .append(message)
        );
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public NicknameManager getNicknameManager() { return nicknameManager; }
}