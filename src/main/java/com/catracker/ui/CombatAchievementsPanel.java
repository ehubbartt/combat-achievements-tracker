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
import com.catracker.config.CombatAchievementsConfig;
import com.catracker.model.BossStats;
import com.catracker.model.CombatAchievement;
import com.catracker.ui.components.BossGridPanel;
import com.catracker.ui.components.FilterPanel;
import com.catracker.ui.components.StatsPanel;
import com.catracker.ui.util.IconLoader;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CombatAchievementsPanel extends PluginPanel
{
	private final CombatAchievementsPlugin plugin;

	private ViewMode currentViewMode = ViewMode.ALL_TASKS;
	private String selectedBoss = null;
	private CombatAchievement selectedAchievement = null;

	@Getter
	private final IconTextField searchBar;
	private final JPanel headerPanel = new JPanel(new BorderLayout());
	private final JButton togglePanelsButton = new JButton();
	private final JPanel tabButtonsPanel = new JPanel();
	private final JButton allTasksButton = new JButton("All Tasks");
	private final JButton trackedTasksButton = new JButton("Tracked");
	private final JButton bossesButton = new JButton("Bosses");

	// CardLayout for tab switching with scroll preservation
	private final JPanel cardPanel = new JPanel(new CardLayout());
	private final JScrollPane allTasksScrollPane = new JScrollPane();
	private final JScrollPane trackedScrollPane = new JScrollPane();
	private final JScrollPane bossesScrollPane = new JScrollPane();
	private final JPanel allTasksContainer = new JPanel();
	private final JPanel trackedContainer = new JPanel();
	private final JPanel bossesContainer = new JPanel();

	private final JPanel bossHeaderPanel = new JPanel(new BorderLayout());
	private final JButton backButton = new JButton();
	private final JLabel bossTitle = new JLabel();

	private final StatsPanel statsPanel;
	private final FilterPanel filterPanel;
	private final BossGridPanel bossGridPanel;

	private static final ImageIcon LEFT_ARROW;
	private static final ImageIcon HIDE_ICON;
	private static final ImageIcon SHOW_ICON;
	private static final ImageIcon HIDE_ICON_HOVER;
	private static final ImageIcon SHOW_ICON_HOVER;

	static
	{
		ImageIcon rightArrow = IconLoader.loadArrowRight();
		BufferedImage rightArrowImg = (BufferedImage) rightArrow.getImage();
		LEFT_ARROW = new ImageIcon(ImageUtil.rotateImage(rightArrowImg, Math.PI));

		BufferedImage hideImg = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "hide.png");
		BufferedImage showImg = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "show.png");
		HIDE_ICON = new ImageIcon(ImageUtil.resizeImage(hideImg, 16, 16));
		SHOW_ICON = new ImageIcon(ImageUtil.resizeImage(showImg, 16, 16));
		HIDE_ICON_HOVER = new ImageIcon(ImageUtil.resizeImage(ImageUtil.luminanceScale(hideImg, 1.5f), 16, 16));
		SHOW_ICON_HOVER = new ImageIcon(ImageUtil.resizeImage(ImageUtil.luminanceScale(showImg, 1.5f), 16, 16));
	}

	private List<CombatAchievement> allAchievements = new ArrayList<>();
	private List<CombatAchievement> trackedAchievements = new ArrayList<>();
	private String currentSearchText = "";
	private final Map<Integer, CombatAchievementPanel> allTasksPanels = new HashMap<>();
	private final Map<Integer, CombatAchievementPanel> trackedPanels = new HashMap<>();
	private boolean statsAndFiltersVisible = true;

	// Dirty flags to track which tabs need content refresh
	private boolean allTasksDirty = true;
	private boolean trackedDirty = true;
	private boolean bossesDirty = true;

	// Store boss grid scroll position when drilling into a boss
	private int bossGridScrollPosition = 0;

	private enum ViewMode
	{
		ALL_TASKS,
		TRACKED_TASKS,
		BOSSES
	}

	public CombatAchievementsPanel(CombatAchievementsPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		searchBar = new IconTextField();
		statsPanel = new StatsPanel(plugin);
		filterPanel = new FilterPanel();
		bossGridPanel = new BossGridPanel();

		initializeComponents();
		layoutComponents();
		setupEventHandlers();
		loadSampleData();
		buildAllTabs();
	}

	private void initializeComponents()
	{
		setupTabButtons();
		setupBossHeader();
		setupSearchBar();
		setupContentContainer();

		filterPanel.setRefreshCallback((v) -> refreshContent());
		bossGridPanel.setBossClickCallback(this::selectBoss);
	}

	private void setupSearchBar()
	{
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(0, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSearchText();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSearchText();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSearchText();
			}
		});
	}

	private void updateSearchText()
	{
		currentSearchText = searchBar.getText().toLowerCase();
		refreshContent();
	}

	private void setupTabButtons()
	{
		allTasksButton.setFont(FontManager.getRunescapeSmallFont());
		trackedTasksButton.setFont(FontManager.getRunescapeSmallFont());
		bossesButton.setFont(FontManager.getRunescapeSmallFont());

		styleTabButton(allTasksButton, true);
		styleTabButton(trackedTasksButton, false);
		styleTabButton(bossesButton, false);

		tabButtonsPanel.setLayout(new GridLayout(1, 3, 1, 0));
		tabButtonsPanel.setBorder(new EmptyBorder(5, 10, 8, 10));
		tabButtonsPanel.add(allTasksButton);
		tabButtonsPanel.add(trackedTasksButton);
		tabButtonsPanel.add(bossesButton);
	}

	private void styleTabButton(JButton button, boolean selected)
	{
		if (selected)
		{
			button.setBackground(ColorScheme.BRAND_ORANGE);
			button.setForeground(Color.WHITE);
			button.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(ColorScheme.BRAND_ORANGE_TRANSPARENT, 2),
				new EmptyBorder(2, 4, 2, 4)
			));
		}
		else
		{
			button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			button.setForeground(Color.LIGHT_GRAY);
			button.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
				new EmptyBorder(2, 4, 2, 4)
			));
		}
		button.setFocusPainted(false);
		button.setOpaque(true);
	}

	private void setupBossHeader()
	{
		bossHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bossHeaderPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
		bossHeaderPanel.setVisible(false);

		backButton.setIcon(LEFT_ARROW);
		backButton.setFont(FontManager.getRunescapeSmallFont());
		backButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		backButton.setForeground(Color.WHITE);
		backButton.setBorder(new EmptyBorder(5, 10, 5, 10));
		backButton.setFocusPainted(false);
		backButton.addActionListener(e ->
		{
			if (selectedAchievement != null)
			{
				selectedAchievement = null;
			}
			else
			{
				selectedBoss = null;
			}
			// Mark bosses as dirty so it rebuilds, don't reset scroll
			// (scroll restoration is handled in refreshContent)
			bossesDirty = true;
			refreshContent(false);
		});

		bossTitle.setFont(FontManager.getRunescapeBoldFont());
		bossTitle.setForeground(ColorScheme.BRAND_ORANGE);
		bossTitle.setHorizontalAlignment(SwingConstants.CENTER);

		bossHeaderPanel.add(backButton, BorderLayout.WEST);
		bossHeaderPanel.add(bossTitle, BorderLayout.CENTER);
	}

	private void setupContentContainer()
	{
		// Setup each tab's container and scroll pane
		setupTabScrollPane(allTasksContainer, allTasksScrollPane);
		setupTabScrollPane(trackedContainer, trackedScrollPane);
		setupTabScrollPane(bossesContainer, bossesScrollPane);

		// Add scroll panes to card panel
		cardPanel.add(allTasksScrollPane, ViewMode.ALL_TASKS.name());
		cardPanel.add(trackedScrollPane, ViewMode.TRACKED_TASKS.name());
		cardPanel.add(bossesScrollPane, ViewMode.BOSSES.name());
	}

	private void setupTabScrollPane(JPanel container, JScrollPane scrollPane)
	{
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);
		container.setBorder(new EmptyBorder(0, 10, 0, 10));
		container.setAlignmentX(Component.LEFT_ALIGNMENT);

		scrollPane.setViewportView(container);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
	}

	private void layoutComponents()
	{
		JLabel titleLabel = new JLabel("Combat Achievements");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));

		// Setup toggle button
		togglePanelsButton.setIcon(HIDE_ICON);
		togglePanelsButton.setFont(FontManager.getRunescapeSmallFont());
		togglePanelsButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
		togglePanelsButton.setForeground(Color.WHITE);
		togglePanelsButton.setBorder(new EmptyBorder(5, 10, 5, 10));
		togglePanelsButton.setFocusPainted(false);
		togglePanelsButton.setBorderPainted(false);
		togglePanelsButton.setContentAreaFilled(false);
		togglePanelsButton.setOpaque(false);
		togglePanelsButton.setToolTipText("Hide stats and filters panels");
		togglePanelsButton.addActionListener(e -> toggleStatsAndFilters());

		// Add hover effect
		togglePanelsButton.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				if (statsAndFiltersVisible)
				{
					togglePanelsButton.setIcon(HIDE_ICON_HOVER);
				}
				else
				{
					togglePanelsButton.setIcon(SHOW_ICON_HOVER);
				}
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				if (statsAndFiltersVisible)
				{
					togglePanelsButton.setIcon(HIDE_ICON);
				}
				else
				{
					togglePanelsButton.setIcon(SHOW_ICON);
				}
			}
		});

		JPanel titleContainer = new JPanel(new BorderLayout());
		titleContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titleContainer.add(titleLabel, BorderLayout.CENTER);
		titleContainer.add(togglePanelsButton, BorderLayout.EAST);

		JPanel searchBarContainer = new JPanel(new BorderLayout());
		searchBarContainer.setBorder(new EmptyBorder(6, 10, 6, 10));
		searchBarContainer.add(searchBar, BorderLayout.CENTER);

		headerPanel.add(titleContainer, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel fixedContent = new JPanel();
		fixedContent.setLayout(new BoxLayout(fixedContent, BoxLayout.Y_AXIS));
		fixedContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

		fixedContent.add(headerPanel);
		fixedContent.add(statsPanel);
		fixedContent.add(filterPanel);
		fixedContent.add(searchBarContainer);
		fixedContent.add(tabButtonsPanel);
		fixedContent.add(bossHeaderPanel);

		add(fixedContent, BorderLayout.NORTH);
		add(cardPanel, BorderLayout.CENTER);
	}

	private void setupEventHandlers()
	{
		allTasksButton.addActionListener(e -> switchToView(ViewMode.ALL_TASKS));
		trackedTasksButton.addActionListener(e -> switchToView(ViewMode.TRACKED_TASKS));
		bossesButton.addActionListener(e -> switchToView(ViewMode.BOSSES));
	}

	private void switchToView(ViewMode viewMode)
	{
		currentViewMode = viewMode;
		selectedBoss = null;

		styleTabButton(allTasksButton, viewMode == ViewMode.ALL_TASKS);
		styleTabButton(trackedTasksButton, viewMode == ViewMode.TRACKED_TASKS);
		styleTabButton(bossesButton, viewMode == ViewMode.BOSSES);

		// Show the selected tab using CardLayout
		CardLayout cl = (CardLayout) cardPanel.getLayout();
		cl.show(cardPanel, viewMode.name());

		// Only refresh if the tab content is dirty
		boolean needsRefresh = false;
		switch (viewMode)
		{
			case ALL_TASKS:
				needsRefresh = allTasksDirty;
				break;
			case TRACKED_TASKS:
				needsRefresh = trackedDirty;
				break;
			case BOSSES:
				needsRefresh = bossesDirty;
				break;
		}

		if (needsRefresh)
		{
			// Reset scroll to top when first loading a tab's content
			refreshContent(true);
		}
		else
		{
			// Still update stats even if content doesn't need refresh
			updateStats();
		}
	}

	private void selectBoss(String bossName)
	{
		// Save current scroll position before drilling into boss
		bossGridScrollPosition = bossesScrollPane.getVerticalScrollBar().getValue();
		selectedBoss = bossName;
		// Mark bosses as dirty and reset scroll for the new boss view
		bossesDirty = true;
		refreshContent(true);
	}

	public void updateAchievements(List<CombatAchievement> newAchievements)
	{
		log.debug("updateAchievements called with {} achievements", newAchievements.size());
		SwingUtilities.invokeLater(() ->
		{
			allAchievements.clear();
			allAchievements.addAll(newAchievements);
			loadTrackedAchievements();
			// Rebuild all tabs with new data
			buildAllTabs();
		});
	}

	public void refreshContent()
	{
		// Mark all tabs as dirty and refresh current view
		allTasksDirty = true;
		trackedDirty = true;
		bossesDirty = true;
		refreshContent(true);
	}

	private void refreshContent(boolean resetScrollPosition)
	{
		SwingUtilities.invokeLater(() ->
		{
			// Update boss header visibility
			if (selectedAchievement != null)
			{
				bossTitle.setText(selectedAchievement.getName());
				bossHeaderPanel.setVisible(true);
			}
			else if (currentViewMode == ViewMode.BOSSES && selectedBoss != null)
			{
				bossTitle.setText(selectedBoss);
				bossHeaderPanel.setVisible(true);
			}
			else
			{
				bossHeaderPanel.setVisible(false);
			}

			// Get the current container, scroll pane, and dirty state based on view mode
			JPanel currentContainer;
			JScrollPane currentScrollPane;
			boolean isDirty;
			switch (currentViewMode)
			{
				case ALL_TASKS:
					currentContainer = allTasksContainer;
					currentScrollPane = allTasksScrollPane;
					isDirty = allTasksDirty;
					break;
				case TRACKED_TASKS:
					currentContainer = trackedContainer;
					currentScrollPane = trackedScrollPane;
					isDirty = trackedDirty;
					break;
				case BOSSES:
					currentContainer = bossesContainer;
					currentScrollPane = bossesScrollPane;
					isDirty = bossesDirty;
					break;
				default:
					currentContainer = allTasksContainer;
					currentScrollPane = allTasksScrollPane;
					isDirty = allTasksDirty;
			}

			if (isDirty || selectedAchievement != null)
			{
				currentContainer.removeAll();

				if (selectedAchievement != null)
				{
					displayAchievementDetail(currentContainer);
				}
				else
				{
					switch (currentViewMode)
					{
						case BOSSES:
							if (selectedBoss == null)
							{
								displayBossGrid(currentContainer);
							}
							else
							{
								displayBossAchievements(currentContainer);
							}
							bossesDirty = false;
							break;
						case ALL_TASKS:
							displayAllTasksList(currentContainer);
							allTasksDirty = false;
							break;
						case TRACKED_TASKS:
							displayTrackedList(currentContainer);
							trackedDirty = false;
							break;
					}
				}

				currentContainer.revalidate();
				currentContainer.repaint();

				// Set scroll position after revalidate using double invokeLater
				// to ensure layout is fully complete
				final JScrollPane scrollPane = currentScrollPane;
				final boolean isReturningToBossGrid = (currentViewMode == ViewMode.BOSSES && selectedBoss == null && !resetScrollPosition);

				SwingUtilities.invokeLater(() ->
					SwingUtilities.invokeLater(() ->
					{
						if (isReturningToBossGrid)
						{
							// Restore saved scroll position when returning to boss grid
							scrollPane.getVerticalScrollBar().setValue(bossGridScrollPosition);
						}
						else if (resetScrollPosition)
						{
							scrollPane.getVerticalScrollBar().setValue(0);
						}
					})
				);
			}

			updateStats();
		});
	}

	private void buildAllTabs()
	{
		SwingUtilities.invokeLater(() ->
		{
			// Build All Tasks tab
			allTasksContainer.removeAll();
			allTasksPanels.clear();
			List<CombatAchievement> allFiltered = getFilteredAchievements(allAchievements);
			displayAchievementPanels(allTasksContainer, allFiltered, "No achievements match current filters", allTasksPanels);
			allTasksContainer.revalidate();
			allTasksContainer.repaint();
			allTasksDirty = false;

			// Build Tracked tab
			trackedContainer.removeAll();
			trackedPanels.clear();
			List<CombatAchievement> trackedFiltered = getFilteredAchievements(trackedAchievements);
			displayAchievementPanels(trackedContainer, trackedFiltered, "No tracked achievements match current filters", trackedPanels);
			trackedContainer.revalidate();
			trackedContainer.repaint();
			trackedDirty = false;

			// Build Bosses tab
			bossesContainer.removeAll();
			bossesContainer.add(bossGridPanel);
			String statusFilter = filterPanel.getSelectedStatusFilter();
			String typeFilter = filterPanel.getSelectedTypeFilter();
			String sortOption = filterPanel.getSelectedSortFilter();
			boolean sortAscending = filterPanel.isSortAscending();
			Map<String, Boolean> selectedTiers = filterPanel.getSelectedTiers();
			bossGridPanel.displayBossGrid(allAchievements, currentSearchText, statusFilter, typeFilter, sortOption, sortAscending, selectedTiers);
			bossesContainer.revalidate();
			bossesContainer.repaint();
			bossesDirty = false;

			updateStats();

			// Set scroll positions after layout is complete
			SwingUtilities.invokeLater(() ->
			{
				allTasksScrollPane.getVerticalScrollBar().setValue(0);
				trackedScrollPane.getVerticalScrollBar().setValue(0);
				bossesScrollPane.getVerticalScrollBar().setValue(0);
			});
		});
	}

	private void displayBossGrid(JPanel container)
	{
		container.add(bossGridPanel);
		String statusFilter = filterPanel.getSelectedStatusFilter();
		String typeFilter = filterPanel.getSelectedTypeFilter();
		String sortOption = filterPanel.getSelectedSortFilter();
		boolean sortAscending = filterPanel.isSortAscending();
		Map<String, Boolean> selectedTiers = filterPanel.getSelectedTiers();
		bossGridPanel.displayBossGrid(allAchievements, currentSearchText, statusFilter, typeFilter, sortOption, sortAscending, selectedTiers);
	}

	private void displayBossAchievements(JPanel container)
	{
		if (selectedBoss == null)
		{
			displayBossGrid(container);
			return;
		}

		List<CombatAchievement> bossAchievements = allAchievements.stream()
			.filter(achievement -> selectedBoss.equals(achievement.getBossName()))
			.collect(Collectors.toList());

		List<CombatAchievement> filteredAchievements = getFilteredAchievements(bossAchievements);
		displayAchievementPanels(container, filteredAchievements, "No achievements found for " + selectedBoss + " given current filter settings", null);
	}

	private void displayAchievementDetail(JPanel container)
	{
		if (selectedAchievement == null)
		{
			return;
		}

		// Create outer panel with BorderLayout to anchor content to top
		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create detail panel with achievement info
		JPanel detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		detailPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Add tier icon and name
		JPanel nameSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		nameSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameSection.setAlignmentX(Component.LEFT_ALIGNMENT);

		ImageIcon tierIcon = com.catracker.ui.util.IconLoader.loadTierIcon(selectedAchievement.getTier());
		if (tierIcon != null)
		{
			JLabel tierIconLabel = new JLabel(tierIcon);
			nameSection.add(tierIconLabel);
		}

		JLabel nameLabel = new JLabel(selectedAchievement.getName());
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(selectedAchievement.isCompleted() ? Color.GREEN :
			selectedAchievement.isTracked() ? new Color(100, 149, 237) : ColorScheme.BRAND_ORANGE);
		nameSection.add(nameLabel);

		detailPanel.add(nameSection);
		detailPanel.add(Box.createVerticalStrut(5));

		// Description
		JTextArea descriptionArea = new JTextArea(selectedAchievement.getDescription());
		descriptionArea.setFont(FontManager.getRunescapeSmallFont());
		descriptionArea.setForeground(Color.LIGHT_GRAY);
		descriptionArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		descriptionArea.setEditable(false);
		descriptionArea.setFocusable(false);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setLineWrap(true);
		descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
		descriptionArea.setBorder(new EmptyBorder(0, 0, 3, 0));
		detailPanel.add(descriptionArea);

		// Info rows
		detailPanel.add(createInfoRow("Tier:", selectedAchievement.getTier()));
		detailPanel.add(createInfoRow("Points:", String.valueOf(selectedAchievement.getPoints())));

		if (selectedAchievement.getBossName() != null && !selectedAchievement.getBossName().equals("Unknown"))
		{
			detailPanel.add(createInfoRow("Boss:", selectedAchievement.getBossName()));
		}

		if (selectedAchievement.getType() != null && !selectedAchievement.getType().isEmpty())
		{
			detailPanel.add(createInfoRow("Type:", selectedAchievement.getType()));
		}

		if (selectedAchievement.getCompletionPercentage() != null)
		{
			String completionText = String.format("%.1f%%", selectedAchievement.getCompletionPercentage());
			detailPanel.add(createInfoRow("Wiki Completion%:", completionText));
		}
		else
		{
			detailPanel.add(createInfoRow("Wiki Completion%:", "Unknown"));
		}

		detailPanel.add(Box.createVerticalStrut(5));

		// Status
		JLabel statusLabel = new JLabel();
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (selectedAchievement.isCompleted())
		{
			statusLabel.setText("Status: Completed");
			statusLabel.setForeground(Color.GREEN);
		}
		else
		{
			statusLabel.setText("Status: Incomplete");
			statusLabel.setForeground(Color.RED);
		}
		detailPanel.add(statusLabel);

		if (selectedAchievement.isTracked())
		{
			JLabel trackedLabel = new JLabel("Tracked");
			trackedLabel.setFont(FontManager.getRunescapeSmallFont());
			trackedLabel.setForeground(new Color(100, 149, 237));
			trackedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			detailPanel.add(trackedLabel);
		}

		detailPanel.add(Box.createVerticalStrut(10));

		// Open in Wiki button
		JButton wikiButton = new JButton("Open in Wiki");
		wikiButton.setFont(FontManager.getRunescapeSmallFont());
		wikiButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		wikiButton.setForeground(Color.WHITE);
		wikiButton.setBorder(BorderFactory.createCompoundBorder(
			new LineBorder(ColorScheme.BRAND_ORANGE, 1),
			new EmptyBorder(5, 10, 5, 10)
		));
		wikiButton.setFocusPainted(false);
		wikiButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		wikiButton.addActionListener(e -> openWikiForAchievement(selectedAchievement));
		detailPanel.add(wikiButton);

		// Add detailPanel to the top (NORTH) of outerPanel
		outerPanel.add(detailPanel, BorderLayout.NORTH);

		container.add(outerPanel);
	}

	private void openWikiForAchievement(CombatAchievement achievement)
	{
		try
		{
			net.runelite.client.util.LinkBrowser.browse(achievement.getWikiUrl());
		}
		catch (Exception ex)
		{
			log.error("Failed to open wiki link: {}", achievement.getWikiUrl(), ex);
		}
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

	private void displayAllTasksList(JPanel container)
	{
		List<CombatAchievement> filteredAchievements = getFilteredAchievements(allAchievements);
		allTasksPanels.clear();
		displayAchievementPanels(container, filteredAchievements, "No achievements match current filters", allTasksPanels);
	}

	private void displayTrackedList(JPanel container)
	{
		List<CombatAchievement> filteredAchievements = getFilteredAchievements(trackedAchievements);
		trackedPanels.clear();
		displayAchievementPanels(container, filteredAchievements, "No tracked achievements match current filters", trackedPanels);
	}

	private void displayAchievementPanels(JPanel container, List<CombatAchievement> achievements, String emptyMessage, Map<Integer, CombatAchievementPanel> panelMap)
	{
		if (achievements.isEmpty())
		{
			JTextArea emptyLabel = new JTextArea(emptyMessage);
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(Color.GRAY);
			emptyLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			emptyLabel.setEditable(false);
			emptyLabel.setFocusable(false);
			emptyLabel.setWrapStyleWord(true);
			emptyLabel.setLineWrap(true);
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
			container.add(emptyLabel);
		}
		else
		{
			for (CombatAchievement achievement : achievements)
			{
				try
				{
					CombatAchievementPanel panel = new CombatAchievementPanel(plugin, achievement);
					if (panelMap != null)
					{
						panelMap.put(achievement.getId(), panel);
					}

					panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
					panel.setAlignmentX(Component.CENTER_ALIGNMENT);
					container.add(panel);
				}
				catch (Exception e)
				{
					log.error("Error creating panel for achievement: {}", achievement.getName(), e);
				}
			}
		}
	}

	private List<CombatAchievement> getFilteredAchievements(List<CombatAchievement> sourceList)
	{
		List<CombatAchievement> filtered = sourceList.stream()
			.filter(this::matchesFilters)
			.collect(Collectors.toList());

		String sortOption = filterPanel.getSelectedSortFilter();
		if (sortOption != null)
		{
			switch (sortOption)
			{
				case "Tier":
					if (filterPanel.isSortAscending())
					{
						filtered.sort((a, b) -> Integer.compare(a.getTierLevel().getOrder(), b.getTierLevel().getOrder()));
					}
					else
					{
						filtered.sort((a, b) -> Integer.compare(b.getTierLevel().getOrder(), a.getTierLevel().getOrder()));
					}
					break;
				case "Points":
					if (filterPanel.isSortAscending())
					{
						filtered.sort((a, b) -> Integer.compare(a.getPoints(), b.getPoints()));
					}
					else
					{
						filtered.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
					}
					break;
				case "Name":
					if (filterPanel.isSortAscending())
					{
						filtered.sort((a, b) -> a.getName().compareTo(b.getName()));
					}
					else
					{
						filtered.sort((a, b) -> b.getName().compareTo(a.getName()));
					}
					break;
				case "Completion":
					if (filterPanel.isSortAscending())
					{
						filtered.sort((a, b) -> Boolean.compare(a.isCompleted(), b.isCompleted()));
					}
					else
					{
						filtered.sort((a, b) -> Boolean.compare(b.isCompleted(), a.isCompleted()));
					}
					break;
				case "Wiki Completion %":
					filtered.sort((a, b) ->
					{
						Double aComp = a.getCompletionPercentage();
						Double bComp = b.getCompletionPercentage();
						if (aComp == null && bComp == null) return 0;
						if (aComp == null) return 1;
						if (bComp == null) return -1;
						if (filterPanel.isSortAscending())
						{
							return Double.compare(bComp, aComp);
						}
						else
						{
							return Double.compare(aComp, bComp);
						}
					});
					break;
				default:
					filtered.sort((a, b) ->
					{
						int tierComparison = Integer.compare(a.getTierLevel().getOrder(), b.getTierLevel().getOrder());
						return tierComparison == 0 ? a.getName().compareTo(b.getName()) : tierComparison;
					});
			}
		}
		return filtered;
	}

	private boolean matchesFilters(CombatAchievement achievement)
	{
		if (!currentSearchText.isEmpty() &&
			!achievement.getName().toLowerCase().contains(currentSearchText) &&
			!achievement.getDescription().toLowerCase().contains(currentSearchText))
		{
			return false;
		}

		if (!filterPanel.getSelectedTiers().getOrDefault(achievement.getTier(), true))
		{
			return false;
		}

		String selectedStatus = filterPanel.getSelectedStatusFilter();
		if ("Completed".equals(selectedStatus) && !achievement.isCompleted())
		{
			return false;
		}
		if ("Incomplete".equals(selectedStatus) && achievement.isCompleted())
		{
			return false;
		}

		String selectedType = filterPanel.getSelectedTypeFilter();
		if (!"All Types".equals(selectedType))
		{
			String achievementType = achievement.getType();
			if (achievementType == null || !achievementType.equals(selectedType))
			{
				return false;
			}
		}

		return true;
	}

	private void updateStats()
	{
		if (currentViewMode == ViewMode.BOSSES && selectedBoss == null)
		{
			Map<String, BossStats> bossStatsMap = calculateBossStats();
			statsPanel.updateBossStats(bossStatsMap, allAchievements, trackedAchievements);
			return;
		}

		List<CombatAchievement> visibleAchievements;
		if (currentViewMode == ViewMode.TRACKED_TASKS)
		{
			visibleAchievements = trackedAchievements;
		}
		else if (selectedBoss != null)
		{
			visibleAchievements = allAchievements.stream()
				.filter(achievement -> selectedBoss.equals(achievement.getBossName()))
				.collect(Collectors.toList());
		}
		else
		{
			visibleAchievements = getFilteredAchievements(allAchievements);
		}

		String viewContext = getViewContext();
		statsPanel.updateStats(allAchievements, trackedAchievements, visibleAchievements, viewContext);
	}

	private Map<String, BossStats> calculateBossStats()
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

	private String getViewContext()
	{
		if (selectedBoss != null)
		{
			return selectedBoss + " tasks";
		}
		else if (currentViewMode == ViewMode.TRACKED_TASKS)
		{
			return "tracked";
		}
		else
		{
			return "filtered";
		}
	}

	public void saveTrackedAchievements()
	{
		try
		{
			log.debug("saveTrackedAchievements called - current tracked list size: {}", trackedAchievements.size());
			List<Integer> trackedIds = trackedAchievements.stream()
				.map(CombatAchievement::getId)
				.collect(Collectors.toList());
			String trackedJson = plugin.getGson().toJson(trackedIds);
			try
			{
				if (plugin.getConfigManager().getRSProfileKey() != null)
				{
					plugin.getConfigManager().setRSProfileConfiguration(
						CombatAchievementsConfig.CONFIG_GROUP_NAME,
						"trackedAchievements",
						trackedJson
					);
					log.debug("Saved RSProfile tracked achievements");
				}
			}
			catch (Exception e)
			{
				log.error("Config save failed", e);
			}
		}
		catch (Exception e)
		{
			log.error("Failed to save tracked achievements", e);
		}
	}

	public void loadTrackedAchievements()
	{
		// Clear tracked list first to avoid duplicates when reloading
		trackedAchievements.clear();

		try
		{
			String configJson = plugin.getConfigManager().getRSProfileConfiguration(
				CombatAchievementsConfig.CONFIG_GROUP_NAME,
				"trackedAchievements"
			);
			if (configJson != null && !configJson.isEmpty())
			{
				Type listType = new TypeToken<List<Integer>>()
				{
				}.getType();
				List<Integer> configTrackedIds = plugin.getGson().fromJson(configJson, listType);
				for (CombatAchievement achievement : allAchievements)
				{
					if (configTrackedIds.contains(achievement.getId()))
					{
						achievement.setTracked(true);
						trackedAchievements.add(achievement);
					}
				}
			}
		}
		catch (Exception e)
		{
			log.debug("config not found or invalid: {}", e.getMessage());
		}
	}

	public void clearAllConfigData()
	{
		try
		{
			plugin.getConfigManager().unsetRSProfileConfiguration(
				CombatAchievementsConfig.CONFIG_GROUP_NAME,
				"trackedAchievements"
			);
		}
		catch (Exception e)
		{
			log.debug("Failed to clear");
		}
	}

	public void onAchievementCompleted(String message)
	{
		log.debug("Achievement completed notification: {}", message);
		// Reload tracked achievements to sync with updated allAchievements data
		loadTrackedAchievements();
		refreshContent();
	}

	public void addToTracked(CombatAchievement achievement)
	{
		log.debug("addToTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
		if (!trackedAchievements.contains(achievement))
		{
			trackedAchievements.add(achievement);
			achievement.setTracked(true);

			// Refresh the panel in the current view
			CombatAchievementPanel panel = allTasksPanels.get(achievement.getId());
			if (panel != null)
			{
				panel.refresh();
			}

			// Mark tracked tab as dirty since we added an item
			trackedDirty = true;

			updateStatsOnly();
			saveTrackedAchievements();
		}
	}

	public void removeFromTracked(CombatAchievement achievement)
	{
		log.debug("removeFromTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
		if (trackedAchievements.remove(achievement))
		{
			achievement.setTracked(false);

			// Refresh panels in both views if they exist
			CombatAchievementPanel allTasksPanel = allTasksPanels.get(achievement.getId());
			if (allTasksPanel != null)
			{
				allTasksPanel.refresh();
			}
			CombatAchievementPanel trackedPanel = trackedPanels.get(achievement.getId());
			if (trackedPanel != null)
			{
				trackedPanel.refresh();
			}

			updateStatsOnly();
			saveTrackedAchievements();

			if (currentViewMode == ViewMode.TRACKED_TASKS)
			{
				// Mark as dirty and refresh to remove the item from view
				trackedDirty = true;
				refreshContent(false);
			}
			else
			{
				// Mark tracked tab as dirty for when user switches to it
				trackedDirty = true;
			}
		}
	}

	private void updateStatsOnly()
	{
		SwingUtilities.invokeLater(this::updateStats);
	}

	private void loadSampleData()
	{
	}

	public void refreshCombatAchievements()
	{
		plugin.refreshCombatAchievements();
	}

	public List<CombatAchievement> getAllAchievements()
	{
		return allAchievements;
	}

	private void toggleStatsAndFilters()
	{
		statsAndFiltersVisible = !statsAndFiltersVisible;
		statsPanel.setVisible(statsAndFiltersVisible);
		filterPanel.setVisible(statsAndFiltersVisible);

		// Update the toggle button icon and tooltip
		// Check if mouse is over the button to show hover icon
		boolean isMouseOver = togglePanelsButton.getModel().isRollover();
		if (statsAndFiltersVisible)
		{
			togglePanelsButton.setIcon(isMouseOver ? HIDE_ICON_HOVER : HIDE_ICON);
			togglePanelsButton.setToolTipText("Hide stats and filters panels");
		}
		else
		{
			togglePanelsButton.setIcon(isMouseOver ? SHOW_ICON_HOVER : SHOW_ICON);
			togglePanelsButton.setToolTipText("Show stats and filters panels");
		}

		revalidate();
		repaint();
	}

	public void onConfigChanged()
	{
		SwingUtilities.invokeLater(() ->
		{
			statsPanel.updateLayout();
			revalidate();
			repaint();
		});
	}

	public void openInBossesTab(String bossName)
	{
		SwingUtilities.invokeLater(() ->
		{
			// Switch to bosses view
			currentViewMode = ViewMode.BOSSES;
			selectedBoss = bossName;

			styleTabButton(allTasksButton, false);
			styleTabButton(trackedTasksButton, false);
			styleTabButton(bossesButton, true);

			// Show the bosses card
			CardLayout cl = (CardLayout) cardPanel.getLayout();
			cl.show(cardPanel, ViewMode.BOSSES.name());

			// Mark bosses as dirty since we're selecting a specific boss
			bossesDirty = true;

			// Refresh content to show the boss achievements
			refreshContent();
		});
	}

	public void showAchievementDetail(CombatAchievement achievement)
	{
		SwingUtilities.invokeLater(() ->
		{
			selectedAchievement = achievement;
			refreshContent();
		});
	}
}