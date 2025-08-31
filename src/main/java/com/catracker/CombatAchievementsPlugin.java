/*
 * Copyright (c) 2025, Ethan Hubbartt <ehubbartt@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.catracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.catracker.config.CombatAchievementsConfig;
import com.catracker.ui.CombatAchievementsPanel;
import com.catracker.util.CombatAchievementsDataLoader;
import com.catracker.util.ChatMessageUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
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

	@Getter
	@Inject
	private CombatAchievementsConfig config;

	@Getter
	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Getter
	private CombatAchievementsPanel panel;

	@Getter
	private CombatAchievementsDataLoader dataLoader;

	@Getter
	private ChatMessageUtil chatMessageUtil;

	private NavigationButton navigationButton;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Combat Achievements Tracker starting up...");

		dataLoader = new CombatAchievementsDataLoader(client, clientThread);
		chatMessageUtil = new ChatMessageUtil(chatMessageManager, client);
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

				// Send progress chat message if enabled - wait for data refresh to complete
				if (config.showGoalProgress())
				{
					dataLoader.requestManualRefresh(achievements ->
					{
						SwingUtilities.invokeLater(() ->
						{
							chatMessageUtil.sendProgressMessage(achievements, getTierGoal());
						});
					});
				}
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