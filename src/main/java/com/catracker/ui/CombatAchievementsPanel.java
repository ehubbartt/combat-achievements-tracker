package com.catracker.ui;

import com.catracker.CombatAchievementsPlugin;
import com.catracker.config.CombatAchievementsConfig;
import com.catracker.model.BossStats;
import com.catracker.model.CombatAchievement;
import com.catracker.ui.components.BossGridPanel;
import com.catracker.ui.components.FilterPanel;
import com.catracker.ui.components.StatsPanel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
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

	// UI Components
	@Getter
	private final IconTextField searchBar;
	private final JPanel headerPanel = new JPanel(new BorderLayout());
	private final JPanel tabButtonsPanel = new JPanel();
	private final JButton allTasksButton = new JButton("All Tasks");
	private final JButton trackedTasksButton = new JButton("Tracked");
	private final JButton bossesButton = new JButton("Bosses");
	private final JScrollPane contentScrollPane = new JScrollPane();
	private final JPanel contentContainer = new JPanel();
	private final JPanel bossHeaderPanel = new JPanel(new BorderLayout());
	private final JButton backButton = new JButton("<");
	private final JLabel bossTitle = new JLabel();

	// Component panels
	private final StatsPanel statsPanel;
	private final FilterPanel filterPanel;
	private final BossGridPanel bossGridPanel;

	// Data
	private List<CombatAchievement> allAchievements = new ArrayList<>();
	private List<CombatAchievement> trackedAchievements = new ArrayList<>();
	private String currentSearchText = "";
	private final Map<Integer, CombatAchievementPanel> achievementPanels = new HashMap<>();

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

		// Initialize components
		searchBar = new IconTextField();
		statsPanel = new StatsPanel(plugin);
		filterPanel = new FilterPanel();
		bossGridPanel = new BossGridPanel();

		initializeComponents();
		layoutComponents();
		setupEventHandlers();
		loadSampleData();
		refreshContent();
	}

	private void initializeComponents()
	{
		setupTabButtons();
		setupBossHeader();
		setupSearchBar();
		setupContentContainer();

		// Set up component callbacks
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

		backButton.setFont(FontManager.getRunescapeSmallFont());
		backButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		backButton.setForeground(Color.WHITE);
		backButton.setBorder(new EmptyBorder(5, 10, 5, 10));
		backButton.setFocusPainted(false);
		backButton.addActionListener(e -> {
			selectedBoss = null;
			refreshContent();
		});

		bossTitle.setFont(FontManager.getRunescapeBoldFont());
		bossTitle.setForeground(ColorScheme.BRAND_ORANGE);
		bossTitle.setHorizontalAlignment(SwingConstants.CENTER);

		bossHeaderPanel.add(backButton, BorderLayout.WEST);
		bossHeaderPanel.add(bossTitle, BorderLayout.CENTER);
	}

	private void setupContentContainer()
	{
		contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentContainer.setBorder(new EmptyBorder(0, 10, 0, 10));
		contentContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

		contentScrollPane.setViewportView(contentContainer);
		contentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		contentScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentScrollPane.setBorder(null);
		contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	}

	private void layoutComponents()
	{
		JLabel titleLabel = new JLabel("Combat Achievements");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));

		JPanel searchBarContainer = new JPanel(new BorderLayout());
		searchBarContainer.setBorder(new EmptyBorder(6, 10, 2, 10));
		searchBarContainer.add(searchBar, BorderLayout.CENTER);

		headerPanel.add(titleLabel, BorderLayout.NORTH);
		headerPanel.add(searchBarContainer, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel fixedContent = new JPanel();
		fixedContent.setLayout(new BoxLayout(fixedContent, BoxLayout.Y_AXIS));
		fixedContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

		fixedContent.add(headerPanel);
		fixedContent.add(statsPanel);
		fixedContent.add(filterPanel);
		fixedContent.add(tabButtonsPanel);
		fixedContent.add(bossHeaderPanel);

		add(fixedContent, BorderLayout.NORTH);
		add(contentScrollPane, BorderLayout.CENTER);
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

		refreshContent();
	}

	private void selectBoss(String bossName)
	{
		selectedBoss = bossName;
		refreshContent();
	}

	public void updateAchievements(List<CombatAchievement> newAchievements)
	{
		log.info("updateAchievements called with {} achievements", newAchievements.size());
		SwingUtilities.invokeLater(() -> {
			allAchievements.clear();
			allAchievements.addAll(newAchievements);
			achievementPanels.clear();
			loadTrackedAchievements();
			refreshContent();
			log.info("refreshContent completed");
		});
	}

	private void refreshContent()
	{
		refreshContent(true);
	}

	private void refreshContent(boolean resetScrollPosition)
	{
		SwingUtilities.invokeLater(() -> {
			contentContainer.removeAll();
			achievementPanels.clear();

			if (currentViewMode == ViewMode.BOSSES && selectedBoss != null)
			{
				bossTitle.setText(selectedBoss);
				bossHeaderPanel.setVisible(true);
			}
			else
			{
				bossHeaderPanel.setVisible(false);
			}

			switch (currentViewMode)
			{
				case BOSSES:
					if (selectedBoss == null)
					{
						displayBossGrid();
					}
					else
					{
						displayBossAchievements();
					}
					break;
				case ALL_TASKS:
				case TRACKED_TASKS:
					displayAchievementsList();
					break;
			}

			updateStats();
			contentContainer.revalidate();
			contentContainer.repaint();

			if (resetScrollPosition)
			{
				SwingUtilities.invokeLater(() -> {
					contentScrollPane.getVerticalScrollBar().setValue(0);
				});
			}
		});
	}

	private void displayBossGrid()
	{
		contentContainer.add(bossGridPanel);
		bossGridPanel.displayBossGrid(allAchievements, currentSearchText);
	}

	private void displayBossAchievements()
	{
		if (selectedBoss == null)
		{
			displayBossGrid();
			return;
		}

		List<CombatAchievement> bossAchievements = allAchievements.stream()
			.filter(achievement -> selectedBoss.equals(achievement.getBossName()))
			.collect(Collectors.toList());

		List<CombatAchievement> filteredAchievements = getFilteredAchievements(bossAchievements);
		displayAchievementPanels(filteredAchievements, "No achievements found for " + selectedBoss);
	}

	private void displayAchievementsList()
	{
		List<CombatAchievement> baseList = currentViewMode == ViewMode.TRACKED_TASKS ? trackedAchievements : allAchievements;
		List<CombatAchievement> filteredAchievements = getFilteredAchievements(baseList);

		String emptyMessage = currentViewMode == ViewMode.TRACKED_TASKS ?
			"No tracked achievements match current filters" :
			"No achievements match current filters";

		displayAchievementPanels(filteredAchievements, emptyMessage);
	}

	private void displayAchievementPanels(List<CombatAchievement> achievements, String emptyMessage)
	{
		if (achievements.isEmpty())
		{
			JLabel emptyLabel = new JLabel(emptyMessage);
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(Color.GRAY);
			emptyLabel.setHorizontalAlignment(JLabel.CENTER);
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
			contentContainer.add(emptyLabel);
		}
		else
		{
			for (CombatAchievement achievement : achievements)
			{
				try
				{
					CombatAchievementPanel panel = new CombatAchievementPanel(plugin, achievement);
					achievementPanels.put(achievement.getId(), panel);

					panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
					panel.setAlignmentX(Component.CENTER_ALIGNMENT);
					contentContainer.add(panel);
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

		// Apply sorting
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
				default:
					filtered.sort((a, b) -> {
						int tierComparison = Integer.compare(a.getTierLevel().getOrder(), b.getTierLevel().getOrder());
						return tierComparison == 0 ? a.getName().compareTo(b.getName()) : tierComparison;
					});
			}
		}
		return filtered;
	}

	private boolean matchesFilters(CombatAchievement achievement)
	{
		// Search filter
		if (!currentSearchText.isEmpty() &&
			!achievement.getName().toLowerCase().contains(currentSearchText) &&
			!achievement.getDescription().toLowerCase().contains(currentSearchText))
		{
			return false;
		}

		// Tier dropdown filter
		String selectedTier = filterPanel.getSelectedTierFilter();
		if (!"All Tiers".equals(selectedTier) && !achievement.getTier().equals(selectedTier))
		{
			return false;
		}

		// Tier toggle filter
		if (!filterPanel.getSelectedTiers().getOrDefault(achievement.getTier(), true))
		{
			return false;
		}

		// Status filter
		String selectedStatus = filterPanel.getSelectedStatusFilter();
		if ("Completed".equals(selectedStatus) && !achievement.isCompleted())
		{
			return false;
		}
		if ("Incomplete".equals(selectedStatus) && achievement.isCompleted())
		{
			return false;
		}

		// Type filter
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

	// Data persistence methods
	public void saveTrackedAchievements()
	{
		try
		{
			log.info("saveTrackedAchievements called - current tracked list size: {}", trackedAchievements.size());
			List<Integer> trackedIds = trackedAchievements.stream()
				.map(CombatAchievement::getId)
				.collect(Collectors.toList());
			String trackedJson = new Gson().toJson(trackedIds);
			log.info("JSON to save: '{}'", trackedJson);
			try
			{
				if (plugin.getConfigManager().getRSProfileKey() != null)
				{
					plugin.getConfigManager().setConfiguration(
						CombatAchievementsConfig.CONFIG_GROUP_NAME,
						"trackedAchievements",
						trackedJson
					);
				}
				log.info("Saved {} tracked achievements to config", trackedIds.size());
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
		try
		{
			String configJson = plugin.getConfigManager().getConfiguration(
				CombatAchievementsConfig.CONFIG_GROUP_NAME,
				"trackedAchievements"
			);
			if (configJson != null && !configJson.isEmpty())
			{
				Type listType = new TypeToken<List<Integer>>()
				{
				}.getType();
				List<Integer> configTrackedIds = new Gson().fromJson(configJson, listType);
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
			plugin.getConfigManager().unsetConfiguration(
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
		log.info("Achievement completed notification: {}", message);
		refreshContent();
	}

	public void addToTracked(CombatAchievement achievement)
	{
		log.info("addToTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
		if (!trackedAchievements.contains(achievement))
		{
			trackedAchievements.add(achievement);
			achievement.setTracked(true);
			log.info("Added achievement to tracked: {} (ID: {}). New size: {}",
				achievement.getName(), achievement.getId(), trackedAchievements.size());

			CombatAchievementPanel panel = achievementPanels.get(achievement.getId());
			if (panel != null)
			{
				panel.refresh();
			}

			updateStatsOnly();
			saveTrackedAchievements();
		}
	}

	public void removeFromTracked(CombatAchievement achievement)
	{
		log.info("removeFromTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
		if (trackedAchievements.remove(achievement))
		{
			achievement.setTracked(false);
			log.info("Removed achievement from tracked: {} (ID: {}). New size: {}",
				achievement.getName(), achievement.getId(), trackedAchievements.size());

			CombatAchievementPanel panel = achievementPanels.get(achievement.getId());
			if (panel != null)
			{
				panel.refresh();
			}

			updateStatsOnly();
			saveTrackedAchievements();

			if (currentViewMode == ViewMode.TRACKED_TASKS)
			{
				refreshContent(false);
			}
		}
	}

	private void updateStatsOnly()
	{
		SwingUtilities.invokeLater(this::updateStats);
	}

	private void loadSampleData()
	{
		allAchievements.add(new CombatAchievement(1, "Just Getting Started", "General", "test", "Kill any boss", "Easy", 1, false, false));
		allAchievements.add(new CombatAchievement(2, "Squashing Foot Soldiers", "General", "test", "Kill 5 goblins", "Easy", 1, false, false));
		allAchievements.add(new CombatAchievement(3, "Giant Mole Hunter", "Giant Mole", "test", "Kill the Giant Mole", "Medium", 2, false, false));
		allAchievements.add(new CombatAchievement(4, "Barrows Champion", "Barrows", "test", "Complete all Barrows brothers", "Hard", 4, false, false));
		allAchievements.add(new CombatAchievement(5, "Zuk Slayer", "TzKal-Zuk", "test", "Complete the Inferno", "Master", 6, false, false));
	}

}