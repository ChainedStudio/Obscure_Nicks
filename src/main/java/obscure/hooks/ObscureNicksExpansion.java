package obscure.hooks;

import obscure.main.ObscureNicks;
import obscure.managers.NicknameManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ObscureNicksExpansion extends PlaceholderExpansion {

    private final ObscureNicks plugin;

    public ObscureNicksExpansion(ObscureNicks plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Developer";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lanick";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        NicknameManager manager = plugin.getNicknameManager();
        UUID uuid = player.getUniqueId();
        boolean isNicked = manager.isNickEnabled(uuid);

        // 1. %lanick_name% -> Outputs fake name if nicked, normal name if not.
        if (params.equalsIgnoreCase("name")) {
            String nick = manager.getNickname(uuid);
            return (nick != null && !nick.isEmpty()) ? nick : player.getName();
        }

        // 2. %lanick_isnicked% -> Returns customized boolean representations from messages.yml
        if (params.equalsIgnoreCase("isnicked")) {
            String rawState = isNicked
                    ? plugin.getMessagesConfig().getString("boolean-values.yes", "&aYes")
                    : plugin.getMessagesConfig().getString("boolean-values.no", "&cNo");
            return ChatColor.translateAlternateColorCodes('&', rawState);
        }

        // 3. %lanick_rank% -> Outputs fake rank prefix if nicked, real LuckPerms prefix if not.
        if (params.equalsIgnoreCase("rank")) {
            if (isNicked) {
                String fakeRank = manager.getStoredRank(uuid);
                String displayRank = (fakeRank != null && !fakeRank.isEmpty()) ? fakeRank : plugin.getConfig().getString("defaults.rank", "default");
                return ChatColor.translateAlternateColorCodes('&', displayRank);
            }

            // Safe execution point: Isolated behind runtime check
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                return ChatColor.translateAlternateColorCodes('&', LuckPermsHelper.getPrefix(uuid));
            }
            return "";
        }

        // 4. %lanick_weight% -> Internal weight tracking layout used to re-sort NEZNAMY TAB
        if (params.equalsIgnoreCase("weight")) {
            String rankKey;
            if (isNicked) {
                rankKey = manager.getStoredRank(uuid);
            } else {
                rankKey = "default";
                if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                    rankKey = LuckPermsHelper.getPrimaryGroup(uuid);
                }
            }

            if (rankKey == null || rankKey.isEmpty()) {
                rankKey = "default";
            }

            int weight = plugin.getConfig().getInt("sorting-weights." + rankKey.toLowerCase(), 1);
            return String.valueOf(weight);
        }

        return null;
    }

    /**
     * Isolated helper class to enforce safe class-loading boundaries.
     * This class is only examined by the JVM if LuckPerms is actively verified as enabled.
     */
    private static class LuckPermsHelper {

        public static String getPrefix(UUID uuid) {
            try {
                net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = lp.getUserManager().getUser(uuid);
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        return prefix;
                    }
                }
            } catch (Exception ignored) {}
            return "";
        }

        public static String getPrimaryGroup(UUID uuid) {
            try {
                net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = lp.getUserManager().getUser(uuid);
                if (user != null && user.getPrimaryGroup() != null) {
                    return user.getPrimaryGroup();
                }
            } catch (Exception ignored) {}
            return "default";
        }
    }
}