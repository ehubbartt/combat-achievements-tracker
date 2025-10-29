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
import com.catracker.util.TierUtil;
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
		updateLayout();
	}

	public void updateLayout()
	{
		removeAll();

		boolean compactMode = plugin.getConfig().preferSmallerStatsPanel();

		if (compactMode)
		{
			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(5, 10, 5, 10));
			add(createCompactStatsPanel(), BorderLayout.CENTER);
		}
		else
		{
			setLayout(new GridLayout(3, 1, 0, 5));
			setBorder(new EmptyBorder(8, 10, 8, 10));

			setupStatsLabels();

			JPanel totalCard = createStatCard("Total Progress", totalPointsLabel, ColorScheme.BRAND_ORANGE, false);
			JPanel trackedCard = createStatCard("Tracked Tasks", trackedPointsLabel, new Color(50, 150, 150), false);
			JPanel goalCard = createStatCard("Goal Progress", goalLabel, new Color(200, 120, 255), false);

			add(totalCard);
			add(trackedCard);
			add(goalCard);
		}

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		revalidate();
		repaint();
	}

	private JPanel createCompactStatsPanel()
	{
		setupStatsLabels();

		JPanel compactPanel = new JPanel();
		compactPanel.setLayout(new BoxLayout(compactPanel, BoxLayout.Y_AXIS));
		compactPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		compactPanel.setBorder(BorderFactory.createCompoundBorder(
			new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(4, 6, 4, 6)
		));

		// Total Progress Row
		JPanel totalRow = createCompactRow(totalPointsLabel, ColorScheme.BRAND_ORANGE);
		compactPanel.add(totalRow);

		// Tracked Tasks Row
		JPanel trackedRow = createCompactRow(trackedPointsLabel, new Color(50, 150, 150));
		compactPanel.add(trackedRow);

		// Goal Progress Row
		JPanel goalRow = createCompactRow(goalLabel, new Color(200, 120, 255));
		compactPanel.add(goalRow);

		return compactPanel;
	}

	private JPanel createCompactRow(JLabel label, Color accentColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));

		JPanel accentLine = new JPanel();
		accentLine.setBackground(accentColor);
		accentLine.setPreferredSize(new Dimension(3, 0));

		label.setHorizontalAlignment(SwingConstants.LEFT);
		label.setBorder(new EmptyBorder(0, 6, 0, 0));

		row.add(accentLine, BorderLayout.WEST);
		row.add(label, BorderLayout.CENTER);

		return row;
	}

	private void setupStatsLabels()
	{
		totalPointsLabel.setFont(FontManager.getRunescapeSmallFont());
		totalPointsLabel.setForeground(Color.WHITE);

		trackedPointsLabel.setFont(FontManager.getRunescapeSmallFont());
		trackedPointsLabel.setForeground(Color.CYAN);

		goalLabel.setFont(FontManager.getRunescapeSmallFont());
		goalLabel.setForeground(new Color(200, 120, 255));
	}

	private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor, boolean compactMode)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (compactMode)
		{
			card.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
				new EmptyBorder(4, 6, 4, 6)
			));
		}
		else
		{
			card.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
				new EmptyBorder(6, 8, 6, 8)
			));
		}

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeSmallFont());
		titleLabel.setForeground(Color.LIGHT_GRAY);
		titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

		valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setBorder(null);

		JPanel accentLine = new JPanel();
		accentLine.setBackground(accentColor);
		accentLine.setPreferredSize(new Dimension(3, 0));

		JPanel content = new JPanel(new GridLayout(2, 1, 0, compactMode ? 1 : 2));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		content.setBorder(new EmptyBorder(0, compactMode ? 6 : 8, 0, 0));
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

		int allTotalPoints = allAchievements.stream()
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
		int pointGoal = TierUtil.getPointsFromGoal(tierGoal, totalCompletedPoints);
		String actualTierName = TierUtil.getActualTierName(tierGoal, totalCompletedPoints);

		boolean compactMode = plugin.getConfig().preferSmallerStatsPanel();

		if (compactMode)
		{
			totalPointsLabel.setText("Total: " + totalCompletedPoints + "/" + allTotalPoints + " pts" +
				" (" + allAchievements.size() + " tasks)");

			trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
				totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");
		}
		else
		{
			totalPointsLabel.setText(totalCompletedPoints + "/" + allTotalPoints + " pts" +
				" (" + allAchievements.size() + " tasks)");

			trackedPointsLabel.setText(completedTrackedPoints + "/" +
				totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");
		}

		if (totalCompletedPoints >= pointGoal)
		{
			String goalText = compactMode ? "Goal: " + actualTierName + " Completed! (" + totalCompletedPoints + " pts)" :
				actualTierName + " Completed! (" + totalCompletedPoints + " pts)";
			goalLabel.setText(goalText);
		}
		else
		{
			int remainingPoints = pointGoal - totalCompletedPoints;
			String goalText = compactMode ? "Goal: " + remainingPoints + " pts to " + actualTierName :
				remainingPoints + " pts to " + actualTierName;
			goalLabel.setText(goalText);
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

		boolean compactMode = plugin.getConfig().preferSmallerStatsPanel();

		if (compactMode)
		{
			totalPointsLabel.setText("Bosses: " + completedBosses + "/" + totalBosses + " complete");
		}
		else
		{
			totalPointsLabel.setText(completedBosses + "/" + totalBosses + " complete");
		}

		int totalTrackedPoints = trackedAchievements.stream()
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		int completedTrackedPoints = trackedAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		if (compactMode)
		{
			trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
				totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");
		}
		else
		{
			trackedPointsLabel.setText(completedTrackedPoints + "/" +
				totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");
		}

		int totalCompletedPoints = allAchievements.stream()
			.filter(CombatAchievement::isCompleted)
			.mapToInt(CombatAchievement::getPoints)
			.sum();

		CombatAchievementsConfig.TierGoal tierGoal = plugin.getTierGoal();
		int pointGoal = TierUtil.getPointsFromGoal(tierGoal, totalCompletedPoints);
		String actualTierName = TierUtil.getActualTierName(tierGoal, totalCompletedPoints);

		if (totalCompletedPoints >= pointGoal)
		{
			String goalText = compactMode ? "Goal: " + actualTierName + " Completed! (" + totalCompletedPoints + " pts)" :
				actualTierName + " Completed! (" + totalCompletedPoints + " pts)";
			goalLabel.setText(goalText);
		}
		else
		{
			int remainingPoints = pointGoal - totalCompletedPoints;
			String goalText = compactMode ? "Goal: " + remainingPoints + " pts to " + actualTierName :
				remainingPoints + " pts to " + actualTierName;
			goalLabel.setText(goalText);
		}
	}
}