package com.catracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.catracker.config.CombatAchievementsConfig;
import com.catracker.ui.CombatAchievementsPanel;
import com.catracker.util.CombatAchievementsDataLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Combat Achievements Tracker",
	description = "Track and manage combat achievements with filtering, sorting, and progress tracking",
	tags = {"combat", "achievements", "tracker", "progress"}
)
public class CombatAchievementsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CombatAchievementsConfig config;

	@Getter
	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Getter
	private CombatAchievementsPanel panel;

	@Getter
	private CombatAchievementsDataLoader dataLoader;

	private NavigationButton navigationButton;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Combat Achievements Tracker starting up...");

		dataLoader = new CombatAchievementsDataLoader(client, clientThread);
		panel = new CombatAchievementsPanel(this);
		log.info("Panel created successfully");

		BufferedImage icon = null;
		try
		{
			icon = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "combat_achievements_icon.png");
		}
		catch (Exception e)
		{
			log.warn("Could not load icon, using text button instead", e);
		}

		if (icon != null)
		{
			navigationButton = NavigationButton.builder()
				.tooltip("Combat Achievements Tracker")
				.icon(icon)
				.priority(6)
				.panel(panel)
				.build();
		}
		else
		{
			navigationButton = NavigationButton.builder()
				.tooltip("Combat Achievements Tracker")
				.priority(6)
				.panel(panel)
				.build();
		}
		clientToolbar.addNavigation(navigationButton);
		log.info("Combat Achievements Tracker started successfully!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Combat Achievements Tracker stopping...");

		if (panel != null)
		{
			try
			{
				log.info("Saved tracked achievements before shutdown");
				Thread.sleep(100);
			}
			catch (Exception e)
			{
				log.error("Failed to save tracked achievements during shutdown", e);
			}
		}

		clientToolbar.removeNavigation(navigationButton);
		log.info("Combat Achievements Tracker stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		log.debug("Game state changed: {}", gameStateChanged.getGameState());

		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			dataLoader.requestDataLoad();
			log.info("Game logged in - will load Combat Achievements on next game tick");
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		dataLoader.handleGameTick(panel);
	}

	public CombatAchievementsConfig.TierGoal getTierGoal()
	{
		return config.tierGoal();
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE ||
			chatMessage.getType() == ChatMessageType.SPAM)
		{
			String message = chatMessage.getMessage();

			if (message.contains("Congratulations, you've completed") &&
				message.contains("combat task"))
			{
				handleCombatAchievementCompletion(message);
			}
		}
	}

	private void handleCombatAchievementCompletion(String message)
	{
		log.info("Combat achievement completed: {}", message);

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.onAchievementCompleted(message);
			}
		});
	}

	@Provides
	CombatAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatAchievementsConfig.class);
	}

	public void refreshCombatAchievements()
	{
		if (client != null && client.getGameState() == GameState.LOGGED_IN)
		{
			dataLoader.requestManualRefresh();
			log.info("Manual refresh requested - will load on next game tick");
		}
		else
		{
			log.warn("Cannot refresh - not logged in");
		}
	}
}