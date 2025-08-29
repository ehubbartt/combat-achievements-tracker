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

import com.catracker.model.BossStats;
import com.catracker.model.CombatAchievement;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Panel that displays the boss grid view with completion statistics
 */
public class BossGridPanel extends JPanel
{

	private Consumer<String> bossClickCallback;

	public BossGridPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setAlignmentX(Component.CENTER_ALIGNMENT);
	}

	public void setBossClickCallback(Consumer<String> callback)
	{
		this.bossClickCallback = callback;
	}

	public void displayBossGrid(List<CombatAchievement> allAchievements, String currentSearchText)
	{
		removeAll();

		Map<String, BossStats> bossStatsMap = calculateBossStats(allAchievements);

		if (bossStatsMap.isEmpty())
		{
			JLabel emptyLabel = new JLabel("No boss data available");
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(Color.GRAY);
			emptyLabel.setHorizontalAlignment(JLabel.CENTER);
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
			add(emptyLabel);
			return;
		}

		List<String> sortedBosses = bossStatsMap.keySet().stream()
			.filter(boss -> boss != null && !boss.equals("Unknown") && !boss.trim().isEmpty())
			.filter(boss -> matchesBossSearch(boss, currentSearchText))
			.sorted()
			.collect(Collectors.toList());

		if (sortedBosses.isEmpty())
		{
			JLabel emptyLabel = new JLabel("No bosses match search criteria");
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(Color.GRAY);
			emptyLabel.setHorizontalAlignment(JLabel.CENTER);
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
			add(emptyLabel);
			return;
		}

		for (int i = 0; i < sortedBosses.size(); i += 2)
		{
			JPanel rowPanel = new JPanel(new GridLayout(1, 2, 10, 0));
			rowPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
			rowPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

			String boss1 = sortedBosses.get(i);
			BossStats stats1 = bossStatsMap.get(boss1);
			rowPanel.add(createBossCard(boss1, stats1));

			if (i + 1 < sortedBosses.size())
			{
				String boss2 = sortedBosses.get(i + 1);
				BossStats stats2 = bossStatsMap.get(boss2);
				rowPanel.add(createBossCard(boss2, stats2));
			}
			else
			{
				JPanel emptyCard = new JPanel();
				emptyCard.setBackground(ColorScheme.DARK_GRAY_COLOR);
				rowPanel.add(emptyCard);
			}

			add(rowPanel);
		}

		revalidate();
		repaint();
	}

	private JPanel createBossCard(String bossName, BossStats stats)
	{
		JPanel card = new JPanel();
		card.setLayout(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		card.setPreferredSize(new Dimension(0, 70));
		card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel nameLabel = new JLabel(bossName);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		nameLabel.setBorder(new EmptyBorder(8, 5, 2, 5));

		JLabel progressLabel = new JLabel(stats.completed + "/" + stats.total);
		progressLabel.setFont(FontManager.getRunescapeSmallFont());
		progressLabel.setForeground(Color.WHITE);
		progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

		JProgressBar progressBar = new JProgressBar(0, stats.total);
		progressBar.setValue(stats.completed);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progressBar.setForeground(stats.completed == stats.total ? Color.GREEN :
			stats.completed > 0 ? Color.YELLOW : Color.GRAY);
		progressBar.setBorderPainted(false);
		progressBar.setPreferredSize(new Dimension(0, 4));

		JPanel bottomSection = new JPanel(new BorderLayout());
		bottomSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bottomSection.add(progressLabel, BorderLayout.CENTER);
		bottomSection.add(progressBar, BorderLayout.SOUTH);
		bottomSection.setBorder(new EmptyBorder(0, 5, 8, 5));

		card.add(nameLabel, BorderLayout.NORTH);
		card.add(bottomSection, BorderLayout.SOUTH);

		card.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (bossClickCallback != null)
				{
					bossClickCallback.accept(bossName);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
				bottomSection.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				bottomSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

		return card;
	}

	private Map<String, BossStats> calculateBossStats(List<CombatAchievement> allAchievements)
	{
		Map<String, BossStats> bossStatsMap = new HashMap<>();
		for (CombatAchievement achievement : allAchievements)
		{
			String bossName = achievement.getBossName();
			if (bossName == null || bossName.equals("Unknown") || bossName.trim().isEmpty())
			{
				continue;
			}
			bossStatsMap.putIfAbsent(bossName, new BossStats());
			BossStats stats = bossStatsMap.get(bossName);
			stats.total++;
			if (achievement.isCompleted())
			{
				stats.completed++;
			}
		}
		return bossStatsMap;
	}

	private boolean matchesBossSearch(String bossName, String searchText)
	{
		if (searchText == null || searchText.isEmpty())
		{
			return true;
		}
		return bossName.toLowerCase().contains(searchText.toLowerCase());
	}
}