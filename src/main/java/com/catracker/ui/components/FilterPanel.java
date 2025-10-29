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

import com.catracker.ui.util.IconLoader;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.client.util.ImageUtil;

/**
 * Panel containing tier toggles and filter dropdowns
 */
public class FilterPanel extends JPanel
{

	private boolean filtersExpanded = false;
	private boolean sortAscending = true;

	private final JPanel filtersSection = new JPanel();
	private final JButton filtersToggleButton = new JButton("Filters");
	private final JPanel filtersPanel = new JPanel();

	private final JComboBox<String> statusFilter = new JComboBox<>();
	private final JComboBox<String> typeFilter = new JComboBox<>();
	private final JComboBox<String> sortFilter = new JComboBox<>();
	private final JButton sortDirectionButton = new JButton();

	private final Map<String, Boolean> selectedTiers = new HashMap<>();
	private Consumer<Void> refreshCallback;

	private static final ImageIcon DOWN_ARROW;
	private static final ImageIcon UP_ARROW;
	private static final ImageIcon SORT_UP_ICON;
	private static final ImageIcon SORT_DOWN_ICON;

	static
	{
		ImageIcon rightArrow = IconLoader.loadArrowRight();
		BufferedImage rightArrowImg = (BufferedImage) rightArrow.getImage();
		DOWN_ARROW = new ImageIcon(ImageUtil.rotateImage(rightArrowImg, Math.PI / 2));
		UP_ARROW = new ImageIcon(ImageUtil.rotateImage(rightArrowImg, -Math.PI / 2));

		SORT_UP_ICON = IconLoader.loadSortUpIcon();
		SORT_DOWN_ICON = IconLoader.loadSortDownIcon();
	}

	public FilterPanel()
	{
		initializeTierFilters();
		initializeComponents();
		layoutComponents();
		setupEventHandlers();
	}

	public void setRefreshCallback(Consumer<Void> callback)
	{
		this.refreshCallback = callback;
	}

	private void initializeTierFilters()
	{
		selectedTiers.put("Easy", true);
		selectedTiers.put("Medium", true);
		selectedTiers.put("Hard", true);
		selectedTiers.put("Elite", true);
		selectedTiers.put("Master", true);
		selectedTiers.put("Grandmaster", true);
	}

	private void initializeComponents()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		setupFilterDropdowns();
		setupFiltersSection();
	}

	private void setupFilterDropdowns()
	{
		statusFilter.addItem("All");
		statusFilter.addItem("Completed");
		statusFilter.addItem("Incomplete");

		typeFilter.addItem("All Types");
		typeFilter.addItem("Stamina");
		typeFilter.addItem("Perfection");
		typeFilter.addItem("Kill Count");
		typeFilter.addItem("Mechanical");
		typeFilter.addItem("Restriction");
		typeFilter.addItem("Speed");
		typeFilter.addItem("Other");

		sortFilter.addItem("Tier");
		sortFilter.addItem("Name");
		sortFilter.addItem("Completion");
		sortFilter.addItem("Completion %");

		sortDirectionButton.setPreferredSize(new Dimension(25, 20));
		sortDirectionButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortDirectionButton.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		sortDirectionButton.setFocusPainted(false);
		sortDirectionButton.setIcon(SORT_UP_ICON);
	}


	private JToggleButton createTierButton(String tier)
	{
		JToggleButton button = new JToggleButton();
		button.setSelected(true);
		button.setPreferredSize(new Dimension(40, 30));
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		button.setFocusPainted(false);

		ImageIcon icon = IconLoader.loadTierButtonIcon(tier);
		if (icon != null)
		{
			button.setIcon(icon);
		}
		else
		{
			button.setText(tier.substring(0, 1));
			button.setFont(FontManager.getRunescapeSmallFont());
			button.setForeground(Color.WHITE);
		}

		button.setToolTipText(tier + " Tier");
		button.addActionListener(e ->
		{
			selectedTiers.put(tier, button.isSelected());
			updateTierButtonAppearance(button, tier);
			triggerRefresh();
		});

		updateTierButtonAppearance(button, tier);
		return button;
	}

	private void updateTierButtonAppearance(JToggleButton button, String tier)
	{
		if (button.isSelected())
		{
			button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			button.setBorder(new LineBorder(getTierColor(tier), 1));
		}
		else
		{
			button.setBackground(new Color(60, 60, 60));
			button.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		}
	}

	private Color getTierColor(String tier)
	{
		switch (tier.toLowerCase())
		{
			case "easy":
				return new Color(205, 133, 63);
			case "medium":
				return new Color(169, 169, 169);
			case "hard":
				return new Color(105, 105, 105);
			case "elite":
				return new Color(70, 100, 150);
			case "master":
				return new Color(120, 70, 70);
			case "grandmaster":
				return new Color(255, 215, 0);
			default:
				return Color.WHITE;
		}
	}

	private void setupFiltersSection()
	{
		filtersSection.setLayout(new BorderLayout());
		filtersSection.setBorder(new EmptyBorder(0, 10, 5, 10));
		filtersSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Create custom toggle button panel
		JPanel filtersTogglePanel = new JPanel(new BorderLayout());
		filtersTogglePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filtersTogglePanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		filtersTogglePanel.setPreferredSize(new Dimension(0, 30));

		JLabel filtersLabel = new JLabel("Filters");
		filtersLabel.setFont(FontManager.getRunescapeSmallFont());
		filtersLabel.setForeground(Color.WHITE);
		filtersLabel.setBorder(new EmptyBorder(6, 8, 6, 8));

		JLabel filtersArrowLabel = new JLabel();
		filtersArrowLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
		updateFiltersArrow(filtersArrowLabel);

		filtersTogglePanel.add(filtersLabel, BorderLayout.WEST);
		filtersTogglePanel.add(filtersArrowLabel, BorderLayout.EAST);

		// Make the whole panel clickable
		filtersTogglePanel.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				toggleFilters();
				updateFiltersArrow(filtersArrowLabel);
			}
		});

		filtersPanel.setLayout(new GridBagLayout());
		filtersPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
		filtersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filtersPanel.setVisible(filtersExpanded);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 0, 2, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;

		// Add tiers toggle buttons
		gbc.gridy = 0;
		filtersPanel.add(createTiersRow(), gbc);

		// Add dividing line
		gbc.gridy = 1;
		filtersPanel.add(createDivider(), gbc);

		gbc.gridy = 2;
		filtersPanel.add(createFilterRow("Status", statusFilter), gbc);

		gbc.gridy = 3;
		filtersPanel.add(createFilterRow("Type", typeFilter), gbc);

		gbc.gridy = 4;
		filtersPanel.add(createSortRow(), gbc);

		filtersSection.add(filtersTogglePanel, BorderLayout.NORTH);
		filtersSection.add(filtersPanel, BorderLayout.CENTER);
	}

	private void updateFiltersArrow(JLabel arrowLabel)
	{
		arrowLabel.setIcon(filtersExpanded ? UP_ARROW : DOWN_ARROW);
	}

	private void updateFiltersToggleButton()
	{
		if (filtersExpanded)
		{
			filtersToggleButton.setText("Filters");
			filtersToggleButton.setIcon(UP_ARROW);
			filtersToggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
			filtersToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
			filtersToggleButton.setIconTextGap(100);
		}
		else
		{
			filtersToggleButton.setText("Filters");
			filtersToggleButton.setIcon(DOWN_ARROW);
			filtersToggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
			filtersToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
			filtersToggleButton.setIconTextGap(100);
		}
	}

	private JPanel createFilterRow(String labelText, JComboBox<String> comboBox)
	{
		JPanel row = new JPanel(new BorderLayout(5, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel label = new JLabel(labelText + ":");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.LIGHT_GRAY);
		label.setPreferredSize(new Dimension(50, 20));

		row.add(label, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);

		return row;
	}

	private JPanel createTiersRow()
	{
		JPanel tiersPanel = new JPanel(new GridLayout(2, 3, 5, 5));
		tiersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tiersPanel.setBorder(new EmptyBorder(0, 0, 8, 0));

		String[] tiers = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
		for (String tier : tiers)
		{
			JToggleButton tierButton = createTierButton(tier);
			tiersPanel.add(tierButton);
		}

		return tiersPanel;
	}

	private JPanel createDivider()
	{
		JPanel dividerContainer = new JPanel(new BorderLayout());
		dividerContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dividerContainer.setBorder(new EmptyBorder(0, 0, 8, 0));

		JPanel divider = new JPanel();
		divider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		divider.setPreferredSize(new Dimension(0, 1));

		dividerContainer.add(divider, BorderLayout.CENTER);
		return dividerContainer;
	}

	private JPanel createSortRow()
	{
		JPanel row = new JPanel(new BorderLayout(5, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel label = new JLabel("Sort:");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.LIGHT_GRAY);
		label.setPreferredSize(new Dimension(50, 20));

		JPanel sortControls = new JPanel(new BorderLayout(3, 0));
		sortControls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sortControls.add(sortFilter, BorderLayout.CENTER);
		sortControls.add(sortDirectionButton, BorderLayout.EAST);

		row.add(label, BorderLayout.WEST);
		row.add(sortControls, BorderLayout.CENTER);
		return row;
	}

	private void layoutComponents()
	{
		add(filtersSection);
	}

	private void setupEventHandlers()
	{
		filtersToggleButton.addActionListener(e -> toggleFilters());
		sortDirectionButton.addActionListener(e -> toggleSortDirection());

		statusFilter.addActionListener(e -> triggerRefresh());
		typeFilter.addActionListener(e -> triggerRefresh());
		sortFilter.addActionListener(e -> triggerRefresh());
	}

	private void toggleFilters()
	{
		filtersExpanded = !filtersExpanded;
		filtersPanel.setVisible(filtersExpanded);
		revalidate();
		repaint();
	}

	private void toggleSortDirection()
	{
		sortAscending = !sortAscending;
		sortDirectionButton.setIcon(sortAscending ? SORT_UP_ICON : SORT_DOWN_ICON);
		triggerRefresh();
	}

	private void triggerRefresh()
	{
		if (refreshCallback != null)
		{
			refreshCallback.accept(null);
		}
	}

	// Getters for filter values
	public Map<String, Boolean> getSelectedTiers()
	{
		return selectedTiers;
	}

	public String getSelectedStatusFilter()
	{
		return (String) statusFilter.getSelectedItem();
	}

	public String getSelectedTypeFilter()
	{
		return (String) typeFilter.getSelectedItem();
	}

	public String getSelectedSortFilter()
	{
		return (String) sortFilter.getSelectedItem();
	}

	public boolean isSortAscending()
	{
		return sortAscending;
	}
}