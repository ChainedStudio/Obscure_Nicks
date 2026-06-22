package obscure.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import obscure.main.ObscureNicks;
import obscure.database.DatabaseManager.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NicknameManager {

    private final ObscureNicks plugin;
    private final Map<UUID, String> activeNicknames = new HashMap<>();
    private final Map<UUID, String> activeSkinValues = new HashMap<>();
    private final Map<UUID, String> activeSkinSignatures = new HashMap<>();
    private final Map<UUID, String> activeRanks = new HashMap<>();
    private final Map<UUID, Boolean> nickEnabled = new HashMap<>();

    public NicknameManager(ObscureNicks plugin) {
        this.plugin = plugin;
    }

    public String getNickname(UUID uuid) {
        if (!nickEnabled.getOrDefault(uuid, false)) return null;
        return activeNicknames.get(uuid);
    }

    public String getStoredNickname(UUID uuid) {
        return activeNicknames.get(uuid);
    }

    public String getStoredSkinValue(UUID uuid) {
        return activeSkinValues.get(uuid);
    }

    public String getStoredRank(UUID uuid) {
        return activeRanks.get(uuid);
    }

    public boolean isNickEnabled(UUID uuid) {
        return nickEnabled.getOrDefault(uuid, false);
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(uuid);
            if (data == null || data.getNickname() == null || data.getNickname().isEmpty()) {
                return;
            }

            activeNicknames.put(uuid, data.getNickname());
            activeSkinValues.put(uuid, data.getSkinValue());
            activeSkinSignatures.put(uuid, data.getSkinSignature());
            activeRanks.put(uuid, data.getRank());

            // To ensure network cross-server synchronization remains unified,
            // we default to enabling the state if data exists from a prior server session.
            nickEnabled.put(uuid, true);

            Bukkit.getScheduler().runTask(plugin, () -> applyNickState(player));
        });
    }

    public void unloadPlayer(UUID uuid) {
        activeNicknames.remove(uuid);
        activeSkinValues.remove(uuid);
        activeSkinSignatures.remove(uuid);
        activeRanks.remove(uuid);
        nickEnabled.remove(uuid);
    }

    public void toggleNick(Player player) {
        UUID uuid = player.getUniqueId();
        String nick = activeNicknames.get(uuid);
        if (nick == null || nick.isEmpty()) {
            player.sendMessage(plugin.getMessage("errors.no-profile"));
            return;
        }

        boolean nextState = !nickEnabled.getOrDefault(uuid, false);
        nickEnabled.put(uuid, nextState);

        if (nextState) {
            applyNickState(player);
            player.sendMessage(plugin.getMessage("success.nick-enabled"));
            playSoundEffect("audio.success", player);
        } else {
            removeNickState(player);
            player.sendMessage(plugin.getMessage("success.nick-disabled"));
            playSoundEffect("audio.disable", player);
        }
    }

    public void setNickAndSkin(Player player, String nickname) {
        UUID uuid = player.getUniqueId();
        boolean changeSkinConfig = plugin.getConfig().getBoolean("defaults.change-skin-with-name", true);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String textureValue = activeSkinValues.getOrDefault(uuid, "");
            String textureSignature = activeSkinSignatures.getOrDefault(uuid, "");

            if (changeSkinConfig) {
                String[] skinData = fetchSkinFromMojang(nickname);
                if (skinData != null) {
                    textureValue = skinData[0];
                    textureSignature = skinData[1];
                } else {
                    player.sendMessage(plugin.getMessage("errors.skin-fetch-failed").replace("%target%", nickname));
                }
            }

            String defaultRank = plugin.getConfig().getString("defaults.rank", "default");
            String currentRank = activeRanks.getOrDefault(uuid, defaultRank);

            activeNicknames.put(uuid, nickname);
            activeSkinValues.put(uuid, textureValue);
            activeSkinSignatures.put(uuid, textureSignature);
            nickEnabled.put(uuid, true);

            plugin.getDatabaseManager().savePlayerData(uuid, nickname, textureValue, textureSignature, currentRank);

            Bukkit.getScheduler().runTask(plugin, () -> {
                applyNickState(player);
                playSoundEffect("audio.success", player);
                player.sendMessage(plugin.getMessage("success.nick-and-skin-set").replace("%name%", nickname));
            });
        });
    }

    public void setNicknameInState(Player player, String nickname) {
        UUID uuid = player.getUniqueId();
        if (!nickEnabled.getOrDefault(uuid, false)) {
            player.sendMessage(plugin.getMessage("errors.no-profile"));
            return;
        }

        activeNicknames.put(uuid, nickname);
        plugin.getDatabaseManager().savePlayerData(uuid, nickname, activeSkinValues.getOrDefault(uuid, ""), activeSkinSignatures.getOrDefault(uuid, ""), activeRanks.getOrDefault(uuid, "default"));

        player.displayName(Component.text(nickname));
        player.playerListName(Component.text(nickname));
        refreshPlayerSkin(player);
        player.sendMessage(plugin.getMessage("success.nickname-changed").replace("%name%", nickname));
    }

    public void setSkinInState(Player player, String skinTargetName) {
        UUID uuid = player.getUniqueId();
        if (!nickEnabled.getOrDefault(uuid, false)) {
            player.sendMessage(plugin.getMessage("errors.no-profile"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] skinData = fetchSkinFromMojang(skinTargetName);
            if (skinData == null) {
                player.sendMessage(plugin.getMessage("errors.skin-fetch-failed").replace("%target%", skinTargetName));
                return;
            }

            activeSkinValues.put(uuid, skinData[0]);
            activeSkinSignatures.put(uuid, skinData[1]);
            plugin.getDatabaseManager().savePlayerData(uuid, activeNicknames.getOrDefault(uuid, player.getName()), skinData[0], skinData[1], activeRanks.getOrDefault(uuid, "default"));

            Bukkit.getScheduler().runTask(plugin, () -> {
                applyNickState(player);
                player.sendMessage(plugin.getMessage("success.skin-changed").replace("%name%", skinTargetName));
            });
        });
    }

    public void setFakeRankInState(Player player, String rank) {
        UUID uuid = player.getUniqueId();
        activeRanks.put(uuid, rank);
        plugin.getDatabaseManager().savePlayerData(uuid, activeNicknames.getOrDefault(uuid, player.getName()), activeSkinValues.getOrDefault(uuid, ""), activeSkinSignatures.getOrDefault(uuid, ""), rank);
        player.sendMessage(plugin.getMessage("success.rank-changed").replace("%rank%", rank));
    }

    public void setRandomIdentity(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> pool = plugin.getConfig().getStringList("random-pool");
        if (pool.isEmpty()) {
            pool = Arrays.asList("Notch", "Jeb_", "Grian");
        }
        String randomName = pool.get(new Random().nextInt(pool.size()));

        player.sendMessage(plugin.getMessage("success.randomizing").replace("%name%", randomName));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] skinData = fetchSkinFromMojang(randomName);
            String textureValue = (skinData != null) ? skinData[0] : "";
            String textureSignature = (skinData != null) ? skinData[1] : "";
            String defaultRank = plugin.getConfig().getString("defaults.rank", "default");

            activeNicknames.put(uuid, randomName);
            activeSkinValues.put(uuid, textureValue);
            activeSkinSignatures.put(uuid, textureSignature);
            nickEnabled.put(uuid, true);

            plugin.getDatabaseManager().savePlayerData(uuid, randomName, textureValue, textureSignature, activeRanks.getOrDefault(uuid, defaultRank));

            Bukkit.getScheduler().runTask(plugin, () -> {
                applyNickState(player);
                playSoundEffect("audio.randomize", player);
                player.sendMessage(plugin.getMessage("success.randomized").replace("%name%", randomName));
            });
        });
    }

    private void applyNickState(Player player) {
        UUID uuid = player.getUniqueId();
        String nick = activeNicknames.get(uuid);
        String skinVal = activeSkinValues.get(uuid);
        String skinSig = activeSkinSignatures.get(uuid);

        if (nick != null && !nick.isEmpty()) {
            player.displayName(Component.text(nick));
            player.playerListName(Component.text(nick));
        }
        if (skinVal != null && !skinVal.isEmpty()) {
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperties(profile.getProperties());
            profile.setProperty(new ProfileProperty("textures", skinVal, skinSig));
            player.setPlayerProfile(profile);
        }
        refreshPlayerSkin(player);
    }

    private void removeNickState(Player player) {
        player.displayName(Component.text(player.getName()));
        player.playerListName(Component.text(player.getName()));

        PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperties(profile.getProperties());
        player.setPlayerProfile(profile);
        refreshPlayerSkin(player);
    }

    private void refreshPlayerSkin(Player player) {
        if (!player.isOnline()) return;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.hidePlayer(plugin, player);
            }
        }

        long ticks = plugin.getConfig().getLong("skin-refresh.delay-ticks", 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.showPlayer(plugin, player);
                }
            }
            Entity vehicle = player.getVehicle();
            if (vehicle != null && plugin.getConfig().getBoolean("skin-refresh.remount-vehicles", true)) {
                vehicle.removePassenger(player);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> vehicle.addPassenger(player), 1L);
            }
            player.teleportAsync(player.getLocation());
        }, ticks);
    }

    private void playSoundEffect(String configPath, Player player) {
        if (!plugin.getConfig().getBoolean("audio.enabled", true)) return;
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString(configPath + ".sound"));
            float volume = (float) plugin.getConfig().getDouble(configPath + ".volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble(configPath + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {}
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