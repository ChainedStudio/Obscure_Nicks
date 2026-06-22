package obscure.commands;

import obscure.main.ObscureNicks;
import obscure.managers.NicknameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NickCommand implements CommandExecutor {

    private final ObscureNicks plugin;

    public NickCommand(ObscureNicks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("errors.only-players"));
            return true;
        }

        Player player = (Player) sender;
        NicknameManager manager = plugin.getNicknameManager();
        String cmdName = cmd.getName().toLowerCase();

        // Enforce permission safety structures
        if (!player.hasPermission("lanick.command." + (cmdName.equals("fakerank") || cmdName.equals("nickrank") ? "rank" : cmdName))) {
            player.sendMessage(plugin.getMessage("errors.no-permission"));
            return true;
        }

        switch (cmdName) {
            case "togglenick":
                manager.toggleNick(player);
                return true;

            case "random":
                manager.setRandomIdentity(player);
                return true;

            case "nicked":
                boolean enabled = manager.isNickEnabled(player.getUniqueId());
                String storedNick = manager.getStoredNickname(player.getUniqueId());
                String storedSkin = manager.getStoredSkinValue(player.getUniqueId());
                String storedRank = manager.getStoredRank(player.getUniqueId());

                player.sendMessage(plugin.getMessage("status-summary.header"));
                player.sendMessage(plugin.getMessage("status-summary.state").replace("%state%", enabled ? "ENABLED" : "DISABLED"));
                player.sendMessage(plugin.getMessage("status-summary.nickname").replace("%name%", storedNick != null ? storedNick : "None"));
                player.sendMessage(plugin.getMessage("status-summary.skin").replace("%skin%", storedSkin != null && !storedSkin.isEmpty() ? "Texture Active" : "Default/None"));
                player.sendMessage(plugin.getMessage("status-summary.rank").replace("%rank%", storedRank != null ? storedRank : "default"));
                player.sendMessage(plugin.getMessage("status-summary.footer"));
                return true;

            case "nick":
                if (args.length < 1) {
                    player.sendMessage(plugin.getMessage("errors.usage-nick"));
                    return true;
                }
                manager.setNickAndSkin(player, args[0]);
                return true;

            case "nickname":
                if (args.length < 1) {
                    player.sendMessage(plugin.getMessage("errors.usage-nickname"));
                    return true;
                }
                manager.setNicknameInState(player, args[0]);
                return true;

            case "nickskin":
                if (args.length < 1) {
                    player.sendMessage(plugin.getMessage("errors.usage-skin"));
                    return true;
                }
                manager.setSkinInState(player, args[0]);
                return true;

            case "nickrank":
            case "fakerank":
                if (args.length < 1) {
                    player.sendMessage(plugin.getMessage("errors.usage-rank").replace("<command>", label));
                    return true;
                }
                manager.setFakeRankInState(player, args[0]);
                return true;

            default:
                return false;
        }
    }
}