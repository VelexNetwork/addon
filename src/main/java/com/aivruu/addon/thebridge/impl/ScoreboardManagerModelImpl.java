package com.aivruu.addon.thebridge.impl;

import com.aivruu.addon.thebridge.ScoreboardAddonPlugin;
import com.aivruu.addon.thebridge.model.ScoreboardManagerModel;
import com.aivruu.addon.thebridge.model.config.ConfModel;
import com.aivruu.addon.thebridge.task.TitleUpdateTask;
import com.aivruu.addon.thebridge.utils.PlaceholderUtils;
import com.google.common.base.Preconditions;
import eu.mip.alandioda.bridge.spigot.TheBridge;
import eu.mip.alandioda.bridge.spigot.game.Game;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManagerModelImpl implements ScoreboardManagerModel {
	private final ScoreboardAddonPlugin plugin;
	private final ConfModel config;
	private final BukkitScheduler scheduler;
	private final Map<String, FastBoard> cache;
	private final Map<String, Integer> titleTasks;
	private final Map<String, Integer> contentTasks;
	
	public ScoreboardManagerModelImpl(
		final @NotNull ScoreboardAddonPlugin plugin,
		final @NotNull ConfModel config,
		final @NotNull BukkitScheduler scheduler
	) {
		this.plugin = Preconditions.checkNotNull(plugin, "ScoreboardAddonPlugin reference cannot be null.");
		this.config = Preconditions.checkNotNull(config, "ConfModel object cannot be null.");
		this.scheduler = Preconditions.checkNotNull(scheduler, "BukkitScheduler object cannot be null.");
		cache = new HashMap<>();
		titleTasks = new HashMap<>();
		contentTasks = new HashMap<>();
	}
	
	@Override
	public @Nullable FastBoard findOrNull(final @NotNull String id) {
		return cache.get(id);
	}
	
	@Override
	public void create(final @NotNull Player player, final @NotNull Game game) {
		final String id = player.getUniqueId().toString();
		final FastBoard board = new FastBoard(player);
		
		cache.put(id, board);
		
		if (config.enableAnimation()) {
			// Creates the title task for that player.
			titleTasks.put(id, new TitleUpdateTask(
				board,
				config.animationContent(),
				config.animationRate()
			).runTaskTimerAsynchronously(plugin, 0L, config.animationRate()).getTaskId());
			
			board.updateTitle(board.getTitle());
		} else {
			board.updateTitle(PlaceholderUtils.parse(player, config.scoreboardTitle()));
		}
		
		contentTasks.put(id, scheduler.runTaskTimerAsynchronously(
			plugin,
			() -> update(game), 20L,
			config.contentRate()).getTaskId()
		);
	}
	
	@Override
	public void remove(final @NotNull String id) {
		final int titleTask = titleTasks.remove(id);
		final int contentTask = contentTasks.remove(id);
		
		// If the animation is enabled, check if the task for the title is currently running.
		if (config.enableAnimation() && scheduler.isCurrentlyRunning(titleTask)) {
			scheduler.cancelTask(titleTask);
		}
		
		// Checks if the task for the scoreboard content is running yet.
		if (scheduler.isCurrentlyRunning(contentTask)) {
			scheduler.cancelTask(contentTask);
		}
		
		FastBoard board = cache.remove(id);
		// Checks if the FastBoard object for that id isn't in cache.
		if (board == null) return;
		
		// Checks if the board wasn't deleted yet.
		if (!board.isDeleted()) {
			board.delete();
		}

		board = null;
	}
	
	@Override
	public void update(final @NotNull Game game) {
		for (final FastBoard board : cache.values()) {
			Player player = board.getPlayer();
			
			switch (game.state) {
				case Waiting:
					board.updateLines(PlaceholderUtils.parse(player, config.scoreboardWaitingFormat()));
					break;
				case Starting:
					board.updateLines(PlaceholderUtils.parse(player, config.scoreboardStartingFormat()));
					break;
				case Playing:
					board.updateLines(PlaceholderUtils.parse(player, config.scoreboardPlayingFormat()));
					break;
				case Ending:
					for (final String playerName : game.teamPlayers.keySet()) {
						remove(Bukkit.getPlayer(playerName).getUniqueId().toString());
					}
			}
			
			player = null;
		}
	}
	
	@Override
	public void reload() {
		// Removes all the scoreboards to the players.
		for (Player player : Bukkit.getOnlinePlayers()) {
			remove(player.getUniqueId().toString());
			player = null;
		}
		
		TheBridge bridgePluginReference = JavaPlugin.getPlugin(TheBridge.class);
		
		// Creates again the scoreboard to the players.
		for (Player player : Bukkit.getOnlinePlayers()) {
			create(player, bridgePluginReference.GetGameByPlayer(player));
			player = null;
		}
		
		bridgePluginReference = null;
	}
	
	@Override
	public void clear() {
		for (final String id : cache.keySet()) {
			remove(id);
		}
		
		cache.clear();
	}
}
