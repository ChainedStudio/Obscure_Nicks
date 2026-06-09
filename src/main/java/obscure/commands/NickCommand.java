package obscure.commands;

import obscure.main.ObscureNicks;
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
            sender.sendMessage("§cOnly players can change their nicknames!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUsage: /nick <nickname/off>");
            return true;
        }

        String targetNick = args[0];

        // Intercept clear patterns
        if (targetNick.equalsIgnoreCase("off") || targetNick.equalsIgnoreCase("reset")) {
            plugin.getNicknameManager().resetNickname(player);
            return true;
        }

        player.sendMessage("§aProcessing nickname change to §e" + targetNick + "§a...");

        // Fire normal sync layout rules
        plugin.getNicknameManager().setNicknameAndSkin(player, targetNick);
        return true;
    }
}