package obscure.hooks;

import obscure.main.ObscureNicks;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

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
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("name")) {
            String nickname = plugin.getNicknameManager().getNickname(player.getUniqueId());
            return (nickname != null && !nickname.isEmpty()) ? nickname : player.getName();
        }

        return null;
    }
}