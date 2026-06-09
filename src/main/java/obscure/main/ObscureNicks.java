package obscure.main;

import obscure.database.DatabaseManager;
import obscure.hooks.ObscureNicksExpansion;
import obscure.commands.NickCommand;
import obscure.managers.NicknameManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ObscureNicks extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private NicknameManager nicknameManager;

    @Override
    public void onEnable() {
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.nicknameManager = new NicknameManager(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ObscureNicksExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI with %lanick_name%!");
        } else {
            getLogger().warning("PlaceholderAPI not found! %lanick_name% placeholder will not work.");
        }

        NickCommand nickCommand = new NickCommand(this);
        if (getCommand("nick") != null) {
            getCommand("nick").setExecutor(nickCommand);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }
}