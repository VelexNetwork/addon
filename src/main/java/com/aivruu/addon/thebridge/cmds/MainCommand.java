package com.aivruu.addon.thebridge.cmds;

import com.aivruu.addon.thebridge.Constants;
import com.aivruu.addon.thebridge.model.ConfManagerModel;
import com.aivruu.addon.thebridge.model.ScoreboardManagerModel;
import com.google.common.base.Preconditions;
import com.iridium.iridiumcolorapi.IridiumColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MainCommand implements CommandExecutor {
	private final ConfManagerModel confManager;
	private final ScoreboardManagerModel scoreboardManager;
	
	public MainCommand(final @NotNull ConfManagerModel confManager, final @NotNull ScoreboardManagerModel scoreboardManager) {
		this.confManager = Preconditions.checkNotNull(confManager, "ConfManagerModel object cannot be null.");
		this.scoreboardManager = Preconditions.checkNotNull(scoreboardManager, "ScoreboardManagerModel object cannot be null.");
	}
	
	@Override
	public boolean onCommand(
		final @NotNull CommandSender sender,
		final @NotNull Command cmd,
		final @NotNull String label,
		final @NotNull String[] args
	) {
		if (!(sender instanceof Player)) return false;
		
		final Player player = (Player) sender;
		
		if (args.length == 0) {
			player.sendMessage(IridiumColorAPI.process(String.format(
				"&a&l| TheBridgeAddon &ais running on &a&lBukkit-%s",
				Bukkit.getBukkitVersion()
			)));
			player.sendMessage(IridiumColorAPI.process(String.format(
				"&a&l| &aAddon current version: &e%s",
				Constants.VERSION
			)));
			return false;
		}
		
		switch (args[0]) {
			default:
				player.sendMessage(IridiumColorAPI.process("&c&l| &cUnknown sub-command specified."));
				break;
			case "help":
				IridiumColorAPI.process(Arrays.asList(
					"&a&lTheBridgeAddon &8| &aAdmin Help Commands",
					"&7 - &b/tba help &fShows this message.",
					"&7 - &b/tba reload &fReloads the plugin."
				)).forEach(player::sendMessage);
				break;
			case "reload":
				player.sendMessage(IridiumColorAPI.process("&c&l| &cBy do this could happen an error during this process."));
				player.sendMessage(IridiumColorAPI.process("&c&l| &cWe recommend restart the server for this."));
				
				if (args.length == 1) {
					player.sendMessage(IridiumColorAPI.process("&c&l| &cIf you want to reload the plugin, type &e/sba reload confirm"));
					break;
				}
				
				if (args[1].equals("confirm")) {
					if (!confManager.wasLoaded()) {
						player.sendMessage(IridiumColorAPI.process("&c&l| &cConfiguration could not be reloaded correctly."));
						break;
					}
					
					scoreboardManager.reload();
					player.sendMessage(IridiumColorAPI.process("&a&l| &aPlugin reloaded correctly!"));
					break;
				}
				
				player.sendMessage(IridiumColorAPI.process("&c&l| &cReload confirmation cancelled."));
		}
		
		return false;
	}
}
