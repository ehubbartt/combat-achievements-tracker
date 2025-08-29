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
package com.catracker.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("combatachievements")
public interface CombatAchievementsConfig extends Config
{
	String CONFIG_GROUP_NAME = "combatachievements";

	@ConfigSection(
		name = "List Settings",
		description = "Configure how achievements are displayed",
		position = 0
	)
	String listSection = "list";

	@ConfigSection(
		name = "Notifications",
		description = "Configure completion notifications",
		position = 1
	)
	String notificationSection = "notifications";

	@ConfigItem(
		keyName = "tierGoal",
		name = "Set Your Current Goal",
		description = "Set a CA tier goal",
		section = listSection,
		position = 2
	)
	default TierGoal tierGoal()
	{
		return TierGoal.TIER_AUTO;
	}

	// Notifications
	@ConfigItem(
		keyName = "showProgressNotifications",
		name = "Show Progress Notifications",
		description = "Show notifications when completing achievements",
		section = notificationSection,
		position = 0
	)
	default boolean showProgressNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showChatMessages",
		name = "Show Chat Messages",
		description = "Show progress messages in chat",
		section = notificationSection,
		position = 1
	)
	default boolean showChatMessages()
	{
		return true;
	}

	enum TierGoal
	{
		TIER_AUTO("Auto"),
		TIER_EASY("Easy"),
		TIER_MEDIUM("Medium"),
		TIER_HARD("Hard"),
		TIER_ELITE("Elite"),
		TIER_MASTER("Master"),
		TIER_GRANDMASTER("Grandmaster");

		private final String displayName;

		TierGoal(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}
}