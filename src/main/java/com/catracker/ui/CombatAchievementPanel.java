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
package com.catracker.ui;

import com.catracker.CombatAchievementsPlugin;
import com.catracker.model.CombatAchievement;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Slf4j
public class CombatAchievementPanel extends JPanel
{
	private final CombatAchievementsPlugin plugin;
	private final CombatAchievement achievement;

	private final JPanel container = new JPanel(new BorderLayout());
	private final JPanel body = new JPanel(new BorderLayout());
	private final JLabel nameLabel = new JLabel();
	private final JTextArea descriptionArea = new JTextArea();
	private final JToggleButton trackButton = new JToggleButton();
	private final JLabel tierIconLabel = new JLabel();
	private final JPanel expandedPanel = new JPanel();
	private boolean isExpanded = false;

	private final JPanel topSection = new JPanel(new BorderLayout());
	private final JPanel nameLabelPanel = new JPanel(new BorderLayout());
	private final JPanel topRightPanel = new JPanel();

	public CombatAchievementPanel(CombatAchievementsPlugin plugin, CombatAchievement achievement)
	{
		super(new BorderLayout());
		this.plugin = plugin;
		this.achievement = achievement;
		createPanel();
		setupEventHandlers();
		refresh();
	}

	private void createPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(2, 2, 2, 2));
		container.setBackground(getBackgroundColor());
		body.setLayout(new BorderLayout());
		body.setBackground(getBackgroundColor());
		body.setBorder(new EmptyBorder(6, 6, 6, 6));

		topSection.setBackground(getBackgroundColor());

		nameLabelPanel.setBackground(getBackgroundColor());
		nameLabelPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 60, 20));
		nameLabelPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 60, 20));

		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(getNameColor());
		nameLabel.setText(achievement.getName());

		setupTierIcon();

		JPanel nameWithIconPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		nameWithIconPanel.setBackground(getBackgroundColor());
		nameWithIconPanel.add(tierIconLabel);
		nameWithIconPanel.add(Box.createHorizontalStrut(6));
		nameWithIconPanel.add(nameLabel);

		nameLabelPanel.add(nameWithIconPanel, BorderLayout.WEST);

		topRightPanel.setLayout(new BoxLayout(topRightPanel, BoxLayout.X_AXIS));
		topRightPanel.setBackground(getBackgroundColor());

		trackButton.setPreferredSize(new Dimension(12, 12));
		trackButton.setMaximumSize(new Dimension(12, 12));
		trackButton.setMinimumSize(new Dimension(12, 12));
		updateTrackButton();
		SwingUtil.removeButtonDecorations(trackButton);

		topRightPanel.add(trackButton);

		topSection.add(nameLabelPanel, BorderLayout.WEST);
		topSection.add(topRightPanel, BorderLayout.EAST);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBackground(getBackgroundColor());

		String bossTypeText = getBossTypeText();
		if (bossTypeText != null && !bossTypeText.isEmpty())
		{
			JLabel bossTypeLabel = new JLabel(bossTypeText);
			bossTypeLabel.setFont(FontManager.getRunescapeSmallFont());
			bossTypeLabel.setForeground(Color.LIGHT_GRAY);
			bossTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			bossTypeLabel.setBorder(new EmptyBorder(2, 0, 4, 0));
			centerPanel.add(bossTypeLabel);
		}

		// Create a custom panel to hold the description with ellipsis handling
		JPanel descriptionPanel = new JPanel(new BorderLayout());
		descriptionPanel.setBackground(getBackgroundColor());
		descriptionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		descriptionArea.setFont(FontManager.getRunescapeSmallFont());
		descriptionArea.setForeground(Color.LIGHT_GRAY);
		descriptionArea.setBackground(getBackgroundColor());
		descriptionArea.setText(truncateToTwoLines(achievement.getDescription()));
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setEditable(false);
		descriptionArea.setFocusable(false);
		descriptionArea.setEnabled(false);
		descriptionArea.setDisabledTextColor(Color.LIGHT_GRAY);
		descriptionArea.setBorder(null);
		descriptionArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		descriptionPanel.add(descriptionArea, BorderLayout.CENTER);
		centerPanel.add(descriptionPanel);

		body.add(topSection, BorderLayout.NORTH);
		body.add(centerPanel, BorderLayout.CENTER);

		// Create expanded details panel (initially hidden)
		setupExpandedPanel();

		// Make the container clickable with hover effect
		container.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		MouseAdapter clickAndHoverHandler = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Check for popup trigger (right-click) - show context menu
				if (e.isPopupTrigger())
				{
					showContextMenu(e);
					return;
				}

				// Only expand on left click, not right click (right click is for context menu)
				if (!SwingUtilities.isLeftMouseButton(e))
				{
					return;
				}

				// Check if click originated from track button - if so, ignore
				Component source = (Component) e.getSource();
				if (source == trackButton || SwingUtilities.isDescendingFrom(source, trackButton))
				{
					return;
				}
				toggleExpanded();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				// Check for popup trigger on release (for some platforms)
				if (e.isPopupTrigger())
				{
					showContextMenu(e);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				updateAllBackgrounds(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				updateAllBackgrounds(getBackgroundColor());
			}
		};

		// Add listener to container and all its children recursively
		addMouseListenerRecursively(container, clickAndHoverHandler);

		// Explicitly add listener to description area to ensure it responds to clicks
		descriptionArea.addMouseListener(clickAndHoverHandler);

		container.add(body, BorderLayout.CENTER);
		container.add(expandedPanel, BorderLayout.SOUTH);
		add(container, BorderLayout.CENTER);

		// Tooltip disabled temporarily
		// setToolTipText(createTooltip());
	}

	private void setupExpandedPanel()
	{
		expandedPanel.setLayout(new BorderLayout());
		expandedPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		expandedPanel.setVisible(false);

		// Content panel for info
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		contentPanel.setBorder(new EmptyBorder(5, 6, 5, 6));

		// Info rows (no description)
		contentPanel.add(createInfoRow("Tier:", achievement.getTier()));
		contentPanel.add(createInfoRow("Points:", String.valueOf(achievement.getPoints())));

		if (achievement.getBossName() != null && !achievement.getBossName().equals("Unknown"))
		{
			contentPanel.add(createInfoRow("Boss:", achievement.getBossName()));
		}

		if (achievement.getType() != null && !achievement.getType().isEmpty())
		{
			contentPanel.add(createInfoRow("Type:", achievement.getType()));
		}

		if (achievement.getCompletionPercentage() != null)
		{
			String completionText = String.format("%.1f%%", achievement.getCompletionPercentage());
			contentPanel.add(createInfoRow("Wiki Completion%:", completionText));
		}
		else
		{
			contentPanel.add(createInfoRow("Wiki Completion%:", "Unknown"));
		}

		contentPanel.add(Box.createVerticalStrut(5));

		// Status
		JLabel statusLabel = new JLabel();
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (achievement.isCompleted())
		{
			statusLabel.setText("Status: Completed");
			statusLabel.setForeground(Color.GREEN);
		}
		else
		{
			statusLabel.setText("Status: Incomplete");
			statusLabel.setForeground(Color.RED);
		}
		contentPanel.add(statusLabel);

		if (achievement.isTracked())
		{
			JLabel trackedLabel = new JLabel("Tracked");
			trackedLabel.setFont(FontManager.getRunescapeSmallFont());
			trackedLabel.setForeground(new Color(100, 149, 237));
			trackedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			contentPanel.add(trackedLabel);
		}

		expandedPanel.add(contentPanel, BorderLayout.CENTER);
	}

	private JPanel createInfoRow(String label, String value)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel labelComponent = new JLabel(label + " ");
		labelComponent.setFont(FontManager.getRunescapeSmallFont());
		labelComponent.setForeground(Color.LIGHT_GRAY);

		JLabel valueComponent = new JLabel(value);
		valueComponent.setFont(FontManager.getRunescapeSmallFont());
		valueComponent.setForeground(Color.WHITE);

		row.add(labelComponent);
		row.add(valueComponent);

		return row;
	}

	private String truncateToTwoLines(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}

		// Calculate approximate character limit for 2 lines
		// Average panel width is ~230px, average char width is ~7px
		// So roughly 33 chars per line * 2 lines = 66 chars
		int maxChars = 66;

		if (text.length() <= maxChars)
		{
			return text;
		}

		// Truncate and add ellipsis
		return text.substring(0, maxChars - 3) + "...";
	}

	private String getBossTypeText()
	{
		String bossName = achievement.getBossName();
		String type = achievement.getType();

		if (bossName != null && !bossName.equals("None") && !bossName.isEmpty())
		{
			if (type != null && !type.isEmpty())
			{
				return bossName + " - " + type;
			}
			else
			{
				return bossName;
			}
		}
		else if (type != null && !type.isEmpty())
		{
			return type;
		}

		return null;
	}

	private void setupTierIcon()
	{
		ImageIcon icon = com.catracker.ui.util.IconLoader.loadTierIcon(achievement.getTier());
		if (icon != null)
		{
			tierIconLabel.setIcon(icon);
			tierIconLabel.setToolTipText(achievement.getTier() + " Tier");
		}
		else
		{
			log.warn("Could not load tier icon for {}", achievement.getTier());
			tierIconLabel.setIcon(null);
		}
	}

	private Color getNameColor()
	{
		if (achievement.isCompleted())
		{
			return Color.GREEN;
		}
		else if (achievement.isTracked())
		{
			return new Color(100, 149, 237);
		}
		else
		{
			return ColorScheme.BRAND_ORANGE;
		}
	}

	private void setupEventHandlers()
	{
		trackButton.addActionListener(e ->
		{
			achievement.setTracked(!achievement.isTracked());
			updateTrackButton();
			if (achievement.isTracked())
			{
				plugin.getPanel().addToTracked(achievement);
			}
			else
			{
				plugin.getPanel().removeFromTracked(achievement);
			}
		});

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showContextMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showContextMenu(e);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				// Tooltip disabled temporarily
				// setToolTipText(createTooltip());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				// Tooltip disabled temporarily
				// setToolTipText(null);
			}
		});
	}

	private void showContextMenu(MouseEvent e)
	{
		JPopupMenu popup = new JPopupMenu();

		JMenuItem wikiItem = new JMenuItem("Open Wiki");
		wikiItem.addActionListener(event -> openWikiLink());
		popup.add(wikiItem);

		// Add "Show More Info" option
		JMenuItem moreInfoItem = new JMenuItem("Show More Info");
		moreInfoItem.addActionListener(event -> plugin.getPanel().showAchievementDetail(achievement));
		popup.add(moreInfoItem);

		// Add "Open in Bosses Tab" option if boss name is available
		String bossName = achievement.getBossName();
		if (bossName != null && !bossName.equals("Unknown") && !bossName.trim().isEmpty())
		{
			JMenuItem bossTabItem = new JMenuItem("Open in Bosses Tab");
			bossTabItem.addActionListener(event -> plugin.getPanel().openInBossesTab(bossName));
			popup.add(bossTabItem);
		}

		if (!achievement.isCompleted())
		{
			JMenuItem completeItem = new JMenuItem("Mark as Completed (Test)");
			completeItem.addActionListener(event ->
			{
				achievement.markCompleted();
				refresh();
			});
		}

		popup.show(e.getComponent(), e.getX(), e.getY());
	}

	private void openWikiLink()
	{
		try
		{
			LinkBrowser.browse(achievement.getWikiUrl());
		}
		catch (Exception ex)
		{
			log.error("Failed to open wiki link: {}", achievement.getWikiUrl(), ex);
		}
	}

	private void updateTrackButton()
	{
		ImageIcon icon;
		if (achievement.isTracked())
		{
			icon = com.catracker.ui.util.IconLoader.loadTrackRemoveIcon();
			trackButton.setToolTipText("Remove from tracking");
		}
		else
		{
			icon = com.catracker.ui.util.IconLoader.loadTrackAddIcon();
			trackButton.setToolTipText("Add to tracking");
		}

		if (icon != null)
		{
			trackButton.setIcon(icon);
			trackButton.setText("");
		}
		else
		{
			if (achievement.isTracked())
			{
				trackButton.setText("X");
				trackButton.setIcon(null);
				trackButton.setBackground(new Color(120, 70, 70));
				trackButton.setForeground(Color.WHITE);
				trackButton.setToolTipText("Remove from tracking");
			}
			else
			{
				trackButton.setText("+");
				trackButton.setIcon(null);
				trackButton.setBackground(new Color(80, 120, 80));
				trackButton.setForeground(Color.WHITE);
				trackButton.setToolTipText("Add to tracking");
			}
		}
	}

	private Color getBackgroundColor()
	{
		return ColorScheme.DARKER_GRAY_COLOR;
	}

	private void updateAllBackgrounds(Color bgColor)
	{
		container.setBackground(bgColor);
		body.setBackground(bgColor);
		topSection.setBackground(bgColor);
		nameLabelPanel.setBackground(bgColor);
		topRightPanel.setBackground(bgColor);
		descriptionArea.setBackground(bgColor);

		// Update all nested panels
		updateComponentBackgrounds(body, bgColor);
		updateComponentBackgrounds(expandedPanel, bgColor);
	}

	private void updateComponentBackgrounds(Container parent, Color bgColor)
	{
		for (Component comp : parent.getComponents())
		{
			if (comp instanceof JPanel)
			{
				comp.setBackground(bgColor);
				if (comp instanceof Container)
				{
					updateComponentBackgrounds((Container) comp, bgColor);
				}
			}
			else if (comp instanceof JTextArea)
			{
				comp.setBackground(bgColor);
			}
		}
	}

	private void addMouseListenerRecursively(Container parent, MouseAdapter listener)
	{
		parent.addMouseListener(listener);
		for (Component comp : parent.getComponents())
		{
			// Don't add listener to track button
			if (comp == trackButton)
			{
				continue;
			}
			comp.addMouseListener(listener);
			if (comp instanceof Container)
			{
				addMouseListenerRecursively((Container) comp, listener);
			}
		}
	}

	private void toggleExpanded()
	{
		isExpanded = !isExpanded;
		expandedPanel.setVisible(isExpanded);

		// Update description to show full text when expanded, truncated when collapsed
		if (isExpanded)
		{
			descriptionArea.setText(achievement.getDescription());
		}
		else
		{
			descriptionArea.setText(truncateToTwoLines(achievement.getDescription()));
		}

		revalidate();
		repaint();
	}

	private String createTooltip()
	{
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("<html><div style='width: 300px;'>");
		tooltip.append("<b>").append(achievement.getName()).append("</b><br>");
		tooltip.append(achievement.getDescription()).append("<br><br>");
		tooltip.append("Tier: ").append(achievement.getTier()).append("<br>");
		tooltip.append("Points: ").append(achievement.getPoints()).append("<br>");
		if (achievement.getBossName() != null)
		{
			tooltip.append("Boss: ").append(achievement.getBossName()).append("<br>");
		}
		if (achievement.getType() != null)
		{
			tooltip.append("Type: ").append(achievement.getType()).append("<br>");
		}

		if (achievement.getCompletionPercentage() != null)
		{
			tooltip.append("Wiki Completion%: ").append(String.format("%.1f%%", achievement.getCompletionPercentage())).append("<br>");
		}
		else
		{
			tooltip.append("Wiki Completion%: Unknown<br>");
		}

		if (achievement.isCompleted())
		{
			tooltip.append("<br><font color='green'>Completed</font>");
		}
		else
		{
			tooltip.append("<br><font color='red'>Incomplete</font>");
		}
		if (achievement.isTracked())
		{
			tooltip.append("<br><font color=#6495ED>Tracked</font>");
		}
		tooltip.append("</html>");
		return tooltip.toString();
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() ->
		{
			nameLabel.setText(achievement.getName());
			nameLabel.setForeground(getNameColor());

			setupTierIcon();
			updateAllBackgrounds(getBackgroundColor());
			updateTrackButton();
			// Don't set tooltip immediately, let hover handle it
			revalidate();
			repaint();
		});
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH - 20, getPreferredSize().height);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH - 20, super.getPreferredSize().height);
	}
}