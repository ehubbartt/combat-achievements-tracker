/*
 * Copyright (c) 2024, [Your Name] <[your-email@example.com]>
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

package com.catracker.util;

import com.catracker.config.CombatAchievementsConfig;
import com.catracker.model.CombatAchievement;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

import java.awt.Color;
import java.util.List;

/**
 * Utility class for sending chat messages related to combat achievements
 */
@Slf4j
public class ChatMessageUtil
{
	private final ChatMessageManager chatMessageManager;
	private final Client client;

	public ChatMessageUtil(ChatMessageManager chatMessageManager, Client client)
	{
		this.chatMessageManager = chatMessageManager;
		this.client = client;
	}

	/**
	 * Send a progress message when a combat achievement is completed
	 */
	public void sendProgressMessage(List<CombatAchievement> allAchievements,
									CombatAchievementsConfig.TierGoal tierGoal)
	{
		int totalCompletedPoints = allAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int pointGoal = getPointsFromGoal(tierGoal, totalCompletedPoints);
		String actualTierName = getActualTierName(tierGoal, totalCompletedPoints);

		ChatMessageBuilder message = new ChatMessageBuilder()
			.append(Color.CYAN, "[Combat Achievements] ");

		if (totalCompletedPoints >= pointGoal)
		{
			message.append(Color.GREEN, actualTierName + " tier completed! ")
				.append(Color.WHITE, "(" + totalCompletedPoints + " points)");
		}
		else
		{
			int remainingPoints = pointGoal - totalCompletedPoints;
			message.append(Color.WHITE, "Progress: " + totalCompletedPoints + "/" + pointGoal + " points ")
				.append(Color.YELLOW, "(" + remainingPoints + " points to " + actualTierName + ")");
		}

		sendChatMessage(message.build());
	}

	/**
	 * Send a custom chat message
	 */
	public void sendChatMessage(String message)
	{
		if (client == null)
		{
			return;
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private int getPointsFromGoal(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints)
	{
		if (tierGoal.toString().equalsIgnoreCase("AUTO"))
		{
			if (completedPoints < 38)
			{
				return 38;
			}
			else if (completedPoints < 148)
			{
				return 148;
			}
			else if (completedPoints < 394)
			{
				return 394;
			}
			else if (completedPoints < 1026)
			{
				return 1026;
			}
			else if (completedPoints < 1841)
			{
				return 1841;
			}
			else
			{
				return 2525;
			}
		}

		switch (tierGoal)
		{
			case TIER_EASY:
				return 38;
			case TIER_MEDIUM:
				return 148;
			case TIER_HARD:
				return 394;
			case TIER_ELITE:
				return 1026;
			case TIER_MASTER:
				return 1841;
			default:
				return 2525;
		}
	}

	private String getActualTierName(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints)
	{
		if (tierGoal.toString().equalsIgnoreCase("AUTO"))
		{
			if (completedPoints < 38)
			{
				return "Easy";
			}
			else if (completedPoints < 148)
			{
				return "Medium";
			}
			else if (completedPoints < 394)
			{
				return "Hard";
			}
			else if (completedPoints < 1026)
			{
				return "Elite";
			}
			else if (completedPoints < 1841)
			{
				return "Master";
			}
			else
			{
				return "Grandmaster";
			}
		}

		return tierGoal.toString().replace("TIER_", "");
	}
}