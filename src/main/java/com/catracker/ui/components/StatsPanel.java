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
package com.catracker.ui.components;

import com.catracker.CombatAchievementsPlugin;
import com.catracker.config.CombatAchievementsConfig;
import com.catracker.model.BossStats;
import com.catracker.model.CombatAchievement;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Panel that displays statistics about combat achievements progress
 */
public class StatsPanel extends JPanel
{

	private final CombatAchievementsPlugin plugin;

	private final JLabel totalPointsLabel = new JLabel("Total Points: 0");
	private final JLabel trackedPointsLabel = new JLabel("Tracked: 0");
	private final JLabel goalLabel = new JLabel("Progress: 0/50");

	public StatsPanel(CombatAchievementsPlugin plugin)
	{
		this.plugin = plugin;
		initializePanel();
	}

	private void initializePanel()
	{
		setLayout(new GridLayout(3, 1, 0, 5));
		setBorder(new EmptyBorder(8, 10, 8, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		setupStatsLabels();

		JPanel totalCard = createStatCard("Total Progress", totalPointsLabel, ColorScheme.BRAND_ORANGE);
		JPanel trackedCard = createStatCard("Tracked Tasks", trackedPointsLabel, new Color(50, 150, 150));
		JPanel goalCard = createStatCard("Goal Progress", goalLabel, new Color(120, 160, 80));

		add(totalCard);
		add(trackedCard);
		add(goalCard);
	}

	private void setupStatsLabels()
	{
		totalPointsLabel.setFont(FontManager.getRunescapeSmallFont());
		totalPointsLabel.setForeground(Color.WHITE);

		trackedPointsLabel.setFont(FontManager.getRunescapeSmallFont());
		trackedPointsLabel.setForeground(Color.CYAN);

		goalLabel.setFont(FontManager.getRunescapeSmallFont());
		goalLabel.setForeground(Color.GREEN);
	}

	private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(6, 8, 6, 8)
		));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeSmallFont());
		titleLabel.setForeground(Color.LIGHT_GRAY);
		titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

		valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
		valueLabel.setFont(FontManager.getRunescapeSmallFont());

		JPanel accentLine = new JPanel();
		accentLine.setBackground(accentColor);
		accentLine.setPreferredSize(new Dimension(3, 0));

		JPanel content = new JPanel(new GridLayout(2, 1, 0, 2));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.setBorder(new EmptyBorder(0, 8, 0, 0));
		content.add(titleLabel);
		content.add(valueLabel);

		card.add(accentLine, BorderLayout.WEST);
		card.add(content, BorderLayout.CENTER);

		return card;
	}

	/**
	 * Update stats for regular view (all tasks or tracked tasks)
	 */
	public void updateStats(List<CombatAchievement> allAchievements,
							List<CombatAchievement> trackedAchievements,
							List<CombatAchievement> visibleAchievements,
							String viewContext)
	{

		int totalCompletedPoints = allAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int totalPoints = visibleAchievements.stream()
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int completedPoints = visibleAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int totalTrackedPoints = trackedAchievements.stream()
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int completedTrackedPoints = trackedAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		CombatAchievementsConfig.TierGoal tierGoal = plugin.getTierGoal();
		int pointGoal = getPointsFromGoal(tierGoal, totalCompletedPoints);
		String actualTierName = getActualTierName(tierGoal, totalCompletedPoints);

		totalPointsLabel.setText("Total: " + completedPoints + "/" + totalPoints + " pts" +
			" (" + visibleAchievements.size() + " " + viewContext + ")");

		trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
			totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");

		if (totalCompletedPoints >= pointGoal)
		{
			goalLabel.setText("Goal: " + actualTierName + " Completed! (" + totalCompletedPoints + " pts)");
		}
		else
		{
			int remainingPoints = pointGoal - totalCompletedPoints;
			goalLabel.setText("Goal: " + remainingPoints + " pts to " + actualTierName);
		}
	}

	/**
	 * Update stats for boss view
	 */
	public void updateBossStats(Map<String, BossStats> bossStatsMap,
								List<CombatAchievement> allAchievements,
								List<CombatAchievement> trackedAchievements)
	{

		int totalBosses = bossStatsMap.size();
		int completedBosses = (int) bossStatsMap.values().stream()
			.filter(stats -> stats.completed == stats.total && stats.total > 0)
			.count();

		totalPointsLabel.setText("Bosses: " + completedBosses + "/" + totalBosses + " complete");

		int totalTrackedPoints = trackedAchievements.stream()
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int completedTrackedPoints = trackedAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
			totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");

		int totalCompletedPoints = allAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		CombatAchievementsConfig.TierGoal tierGoal = plugin.getTierGoal();
		int pointGoal = getPointsFromGoal(tierGoal, totalCompletedPoints);
		String actualTierName = getActualTierName(tierGoal, totalCompletedPoints);

		if (totalCompletedPoints >= pointGoal)
		{
			goalLabel.setText("Goal: " + actualTierName + " Completed! (" + totalCompletedPoints + " pts)");
		}
		else
		{
			int remainingPoints = pointGoal - totalCompletedPoints;
			goalLabel.setText("Goal: " + remainingPoints + " pts to " + actualTierName);
		}
	}

	public int getPointsFromGoal(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints)
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

		return tierGoal.toString();
	}
}