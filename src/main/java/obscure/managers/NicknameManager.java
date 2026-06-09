package obscure.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import obscure.main.ObscureNicks;
import obscure.database.DatabaseManager.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NicknameManager {

    private final ObscureNicks plugin;
    private final Map<UUID, String> activeNicknames = new HashMap<>();

    public NicknameManager(ObscureNicks plugin) {
        this.plugin = plugin;
    }

    public String getNickname(UUID uuid) {
        return activeNicknames.get(uuid);
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(uuid);
            if (data == null || data.getNickname() == null || data.getNickname().isEmpty()) return;

            activeNicknames.put(uuid, data.getNickname());

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.displayName(Component.text(data.getNickname()));
                player.playerListName(Component.text(data.getNickname()));

                if (data.getSkinValue() != null && !data.getSkinValue().isEmpty()) {
                    PlayerProfile profile = player.getPlayerProfile();
                    profile.removeProperties(profile.getProperties());
                    profile.setProperty(new ProfileProperty("textures", data.getSkinValue(), data.getSkinSignature()));
                    player.setPlayerProfile(profile);
                    refreshPlayerSkin(player);
                }
            });
        });
    }

    public void unloadPlayer(UUID uuid) {
        activeNicknames.remove(uuid);
    }

    /**
     * Resets a player back to their real name and original skin profiles.
     */
    public void resetNickname(Player player) {
        UUID uuid = player.getUniqueId();
        activeNicknames.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().savePlayerData(uuid, "", "", "", "default_rank");

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.displayName(Component.text(player.getName()));
                player.playerListName(Component.text(player.getName()));

                // Revert to pristine default Mojang settings
                PlayerProfile profile = player.getPlayerProfile();
                profile.removeProperties(profile.getProperties());
                player.setPlayerProfile(profile);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
                refreshPlayerSkin(player);

                player.sendMessage("§aYour nickname and skin have been reset back to default!");
            });
        });
    }

    public void setNicknameAndSkin(Player player, String nickname) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] skinData = fetchSkinFromMojang(nickname);

            if (skinData == null) {
                player.sendMessage("§cCould not find a valid Minecraft skin for '" + nickname + "'. Keeping your current skin!");
                plugin.getDatabaseManager().savePlayerData(uuid, nickname, "", "", "default_rank");
                activeNicknames.put(uuid, nickname);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.displayName(Component.text(nickname));
                    player.playerListName(Component.text(nickname));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                });
                return;
            }

            String textureValue = skinData[0];
            String textureSignature = skinData[1];

            plugin.getDatabaseManager().savePlayerData(uuid, nickname, textureValue, textureSignature, "default_rank");
            activeNicknames.put(uuid, nickname);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.displayName(Component.text(nickname));
                player.playerListName(Component.text(nickname));

                PlayerProfile profile = player.getPlayerProfile();
                profile.removeProperties(profile.getProperties());
                profile.setProperty(new ProfileProperty("textures", textureValue, textureSignature));
                player.setPlayerProfile(profile);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                // Force the network update logic
                refreshPlayerSkin(player);

                player.sendMessage("§aYour nickname has been changed to §e" + nickname + " §aand your skin has updated!");
            });
        });
    }

    /**
     * Bulletproof method forcing vanilla game client engines to clear cached skin properties.
     */
    private void refreshPlayerSkin(Player player) {
        if (!player.isOnline()) return;

        // 1. Un-track player from all online clients
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.hidePlayer(plugin, player);
            }
        }

        // 2. Schedule a 2-tick network buffer to cleanly update metadata profile layouts
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Re-track player for everyone else
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.showPlayer(plugin, player);
                }
            }

            // 3. Force update the local viewing client perspective without kicking them
            var loc = player.getLocation();
            var vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.removePassenger(player);
            }

            // Re-spawns player chunks internally to force local avatar reload
            player.teleportAsync(loc);
        }, 2L);
    }

    private String[] fetchSkinFromMojang(String name) {
        try {
            URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) uuidUrl.openConnection();
            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String uuid = json.get("id").getAsString();

            URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection conn2 = (HttpURLConnection) profileUrl.openConnection();
            if (conn2.getResponseCode() != 200) return null;

            BufferedReader reader2 = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
            JsonObject profileJson = JsonParser.parseReader(reader2).getAsJsonObject();
            JsonObject property = profileJson.getAsJsonArray("properties").get(0).getAsJsonObject();

            return new String[] { property.get("value").getAsString(), property.get("signature").getAsString() };
        } catch (Exception e) {
            return null;
        }
    }
}