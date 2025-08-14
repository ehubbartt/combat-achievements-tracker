package com.catracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
public class CombatAchievementsPanel extends PluginPanel
{
    private final CombatAchievementsPlugin plugin;

    // View state
    private boolean showingTrackedOnly = false;
    private boolean filtersExpanded = false;
    private boolean sortAscending = true;

    // UI Components
    private final JTextField searchField = new JTextField();
    @Getter
    private final IconTextField searchBar;
    private final JPanel headerPanel = new JPanel(new BorderLayout());
    private final JPanel viewButtonsPanel = new JPanel();
    private final JButton allTasksButton = new JButton("All Tasks");
    private final JButton trackedTasksButton = new JButton("Tracked");
    private final JPanel filtersSection = new JPanel();
    private final JButton filtersToggleButton = new JButton("Filters ▼");
    private final JPanel filtersPanel = new JPanel();
    // Remove the tasksSection references and variables since we don't need them
    private final JPanel statsPanel = new JPanel();
    private final JScrollPane achievementsList = new JScrollPane();
    private final JPanel achievementsContainer = new JPanel();

    // Stats labels
    private final JLabel totalPointsLabel = new JLabel("Total Points: 0");
    private final JLabel trackedPointsLabel = new JLabel("Tracked: 0");
    private final JLabel goalLabel = new JLabel("Progress: 0/50");

    // Filter components
    private final JComboBox<String> tierFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> sortFilter = new JComboBox<>();
    private final JButton sortDirectionButton = new JButton("↑");

    // Sample data - Real data will be loaded by the plugin
    private List<CombatAchievement> allAchievements = new ArrayList<>();
    private List<CombatAchievement> trackedAchievements = new ArrayList<>();
    private String currentSearchText = "";

    // NEW: Keep track of panel references for efficient updates
    private final Map<Integer, CombatAchievementPanel> achievementPanels = new HashMap<>();

    public CombatAchievementsPanel(CombatAchievementsPlugin plugin)
    {
        super(false); // Critical: false prevents automatic scrolling
        this.plugin = plugin;
        searchBar = new IconTextField();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadSampleData();
        refreshAchievementsList();
    }

    private void initializeComponents()
    {
        // Search field
        searchField.setFont(FontManager.getRunescapeSmallFont());
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setBorder(new EmptyBorder(5, 5, 5, 5));

        // View toggle buttons
        setupViewButtons();

        // Filter dropdowns
        setupFilterDropdowns();

        // Stats labels
        setupStatsLabels();

        // Achievements container - NO PADDING inside scroll container
        achievementsContainer.setLayout(new BoxLayout(achievementsContainer, BoxLayout.Y_AXIS));
        achievementsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        achievementsContainer.setBorder(new EmptyBorder(0, 10, 0, 10)); // Add horizontal padding to center tasks
        achievementsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Scroll pane - ONLY for the tasks list, no padding
        achievementsList.setViewportView(achievementsContainer);
        achievementsList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        achievementsList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        achievementsList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        achievementsList.setBorder(null);
        achievementsList.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void setupViewButtons()
    {
        allTasksButton.setFont(FontManager.getRunescapeSmallFont());
        trackedTasksButton.setFont(FontManager.getRunescapeSmallFont());

        // Style the buttons with orange theme
        styleToggleButton(allTasksButton, true); // Default selected
        styleToggleButton(trackedTasksButton, false);

        viewButtonsPanel.setLayout(new GridLayout(1, 2, 2, 0));
        viewButtonsPanel.setBorder(new EmptyBorder(5, 10, 8, 10));
        viewButtonsPanel.add(allTasksButton);
        viewButtonsPanel.add(trackedTasksButton);
    }

    private void styleToggleButton(JButton button, boolean selected)
    {
        if (selected) {
            button.setBackground(ColorScheme.BRAND_ORANGE);
            button.setForeground(Color.WHITE);
            button.setBorder(new LineBorder(ColorScheme.BRAND_ORANGE_TRANSPARENT, 2));
        } else {
            button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            button.setForeground(Color.LIGHT_GRAY);
            button.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        }
        button.setFocusPainted(false);
        button.setOpaque(true);
    }

    private void setupFilterDropdowns()
    {
        // Tier filter
        tierFilter.addItem("All Tiers");
        tierFilter.addItem("Easy");
        tierFilter.addItem("Medium");
        tierFilter.addItem("Hard");
        tierFilter.addItem("Elite");
        tierFilter.addItem("Master");
        tierFilter.addItem("Grandmaster");

        // Status filter
        statusFilter.addItem("All");
        statusFilter.addItem("Completed");
        statusFilter.addItem("Incomplete");

        // Type filter
        typeFilter.addItem("All Types");
        typeFilter.addItem("Stamina");
        typeFilter.addItem("Perfection");
        typeFilter.addItem("Kill Count");
        typeFilter.addItem("Mechanical");
        typeFilter.addItem("Restriction");
        typeFilter.addItem("Speed");
        typeFilter.addItem("Other");

        // Sort filter
        sortFilter.addItem("Tier");
        sortFilter.addItem("Points");
        sortFilter.addItem("Name");
        sortFilter.addItem("Completion");

        // Sort direction button
        sortDirectionButton.setFont(FontManager.getRunescapeSmallFont());
        sortDirectionButton.setPreferredSize(new Dimension(25, 20));
        sortDirectionButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        sortDirectionButton.setForeground(Color.WHITE);
        sortDirectionButton.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        sortDirectionButton.setFocusPainted(false);
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

    private void layoutComponents()
    {
        // Title and search
        JLabel titleLabel = new JLabel("Combat Achievements");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));

        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(0, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                currentSearchText = searchBar.getText().toLowerCase();
                refreshAchievementsList();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                currentSearchText = searchBar.getText().toLowerCase();
                refreshAchievementsList();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                currentSearchText = searchBar.getText().toLowerCase();
                refreshAchievementsList();
            }
        });

        JPanel searchBarContainer = new JPanel(new BorderLayout());
        searchBarContainer.setBorder(new EmptyBorder(6, 10, 2, 10));
        searchBarContainer.add(searchBar, BorderLayout.CENTER);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(searchBarContainer, BorderLayout.CENTER);

        setupStatsPanel();

        setupFiltersSection();

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel fixedContent = new JPanel();
        fixedContent.setLayout(new BoxLayout(fixedContent, BoxLayout.Y_AXIS));
        fixedContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

        fixedContent.add(headerPanel);
        fixedContent.add(statsPanel);
        fixedContent.add(filtersSection);
        fixedContent.add(viewButtonsPanel);

        add(fixedContent, BorderLayout.NORTH);
        add(achievementsList, BorderLayout.CENTER);
    }

    private void setupStatsPanel()
    {
        statsPanel.setLayout(new GridLayout(3, 1, 0, 5));
        statsPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Create individual stat cards stacked vertically
        JPanel totalCard = createStatCard("Total Progress", totalPointsLabel, ColorScheme.BRAND_ORANGE);
        JPanel trackedCard = createStatCard("Tracked Tasks", trackedPointsLabel, new Color(50, 150, 150));
        JPanel goalCard = createStatCard("Goal Progress", goalLabel, new Color(120, 160, 80));

        statsPanel.add(totalCard);
        statsPanel.add(trackedCard);
        statsPanel.add(goalCard);
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

        // Add a subtle accent line at the left with proper spacing
        JPanel accentLine = new JPanel();
        accentLine.setBackground(accentColor);
        accentLine.setPreferredSize(new Dimension(3, 0));

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 2));
        content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        content.setBorder(new EmptyBorder(0, 8, 0, 0)); // Add left padding from accent line
        content.add(titleLabel);
        content.add(valueLabel);

        card.add(accentLine, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    private void setupFiltersSection()
    {
        filtersSection.setLayout(new BorderLayout());
        filtersSection.setBorder(new EmptyBorder(0, 10, 5, 10));
        filtersSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Toggle button with caret at the right
        filtersToggleButton.setFont(FontManager.getRunescapeSmallFont());
        filtersToggleButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filtersToggleButton.setForeground(Color.WHITE);
        filtersToggleButton.setBorder(new EmptyBorder(6, 8, 6, 8));
        filtersToggleButton.setFocusPainted(false);
        filtersToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        filtersToggleButton.setText("Filters                                                    v");

        // Filters content panel
        filtersPanel.setLayout(new GridBagLayout());
        filtersPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        filtersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        filtersPanel.setVisible(filtersExpanded);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Create filter rows
        gbc.gridy = 0;
        filtersPanel.add(createFilterRow("Tier", tierFilter), gbc);

        gbc.gridy = 1;
        filtersPanel.add(createFilterRow("Status", statusFilter), gbc);

        gbc.gridy = 2;
        filtersPanel.add(createFilterRow("Type", typeFilter), gbc);

        gbc.gridy = 3;
        filtersPanel.add(createSortRow(), gbc);

        filtersSection.add(filtersToggleButton, BorderLayout.NORTH);
        filtersSection.add(filtersPanel, BorderLayout.CENTER);
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

    private void toggleFilters()
    {
        filtersExpanded = !filtersExpanded;
        filtersPanel.setVisible(filtersExpanded);
        if (filtersExpanded) {
            filtersToggleButton.setText("Filters                                                    ^");
        } else {
            filtersToggleButton.setText("Filters                                                    v");
        }
        revalidate();
        repaint();
    }

    private void toggleSortDirection()
    {
        sortAscending = !sortAscending;
        sortDirectionButton.setText(sortAscending ? "^" : "v");
        refreshAchievementsList();
    }

    private void setupEventHandlers()
    {
        // View toggle buttons
        allTasksButton.addActionListener(e -> {
            showingTrackedOnly = false;
            styleToggleButton(allTasksButton, true);
            styleToggleButton(trackedTasksButton, false);
            refreshAchievementsList();
        });

        trackedTasksButton.addActionListener(e -> {
            showingTrackedOnly = true;
            styleToggleButton(allTasksButton, false);
            styleToggleButton(trackedTasksButton, true);
            refreshAchievementsList();
        });

        // Filters toggle
        filtersToggleButton.addActionListener(e -> toggleFilters());

        // Sort direction toggle
        sortDirectionButton.addActionListener(e -> toggleSortDirection());

        // Filter change handlers
        tierFilter.addActionListener(e -> refreshAchievementsList());
        statusFilter.addActionListener(e -> refreshAchievementsList());
        typeFilter.addActionListener(e -> refreshAchievementsList());
        sortFilter.addActionListener(e -> refreshAchievementsList());

        // Search field
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                currentSearchText = searchField.getText().toLowerCase();
                refreshAchievementsList();
            }
        });
    }

    public void updateAchievements(List<CombatAchievement> newAchievements)
    {
        log.info("updateAchievements called with {} achievements", newAchievements.size());
        SwingUtilities.invokeLater(() -> {
            log.info("Running updateAchievements on EDT");
            allAchievements.clear();
            allAchievements.addAll(newAchievements);
            log.info("Updated allAchievements list, now contains {} items", allAchievements.size());

            // Clear panel references when we reload all achievements
            achievementPanels.clear();

            loadTrackedAchievements();
            refreshAchievementsList();
            log.info("refreshAchievementsList completed");
        });
    }

    private void refreshAchievementsList()
    {
        refreshAchievementsList(true);
    }

    // NEW: Modified to optionally reset scroll position
    private void refreshAchievementsList(boolean resetScrollPosition)
    {
        SwingUtilities.invokeLater(() -> {
            achievementsContainer.removeAll();
            achievementPanels.clear(); // Clear panel references when rebuilding

            List<CombatAchievement> baseList = showingTrackedOnly ? trackedAchievements : allAchievements;
            List<CombatAchievement> filteredAchievements = getFilteredAchievements(baseList);

            if (filteredAchievements.isEmpty())
            {
                String emptyMessage = showingTrackedOnly ?
                        "No tracked achievements match current filters" :
                        "No achievements match current filters";

                JLabel emptyLabel = new JLabel(emptyMessage);
                emptyLabel.setFont(FontManager.getRunescapeSmallFont());
                emptyLabel.setForeground(Color.GRAY);
                emptyLabel.setHorizontalAlignment(JLabel.CENTER);
                emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
                achievementsContainer.add(emptyLabel);
            }
            else
            {
                for (CombatAchievement achievement : filteredAchievements)
                {
                    try {
                        CombatAchievementPanel panel = new CombatAchievementPanel(plugin, achievement);
                        achievementPanels.put(achievement.getId(), panel);

                        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
                        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        achievementsContainer.add(panel);
                    } catch (Exception e) {
                        log.error("Error creating panel for achievement: {}", achievement.getName(), e);
                    }
                }
            }

            updateStats();
            achievementsContainer.revalidate();
            achievementsContainer.repaint();

            if (resetScrollPosition) {
                SwingUtilities.invokeLater(() -> {
                    achievementsList.getVerticalScrollBar().setValue(0);
                });
            }
        });
    }

    private List<CombatAchievement> getFilteredAchievements(List<CombatAchievement> sourceList)
    {
        List<CombatAchievement> filtered = sourceList.stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());

        String sortOption = (String) sortFilter.getSelectedItem();
        if (sortOption != null) {
            switch (sortOption) {
                case "Tier":
                    if (sortAscending) {
                        filtered.sort((a, b) -> Integer.compare(a.getTierLevel().getOrder(), b.getTierLevel().getOrder()));
                    } else {
                        filtered.sort((a, b) -> Integer.compare(b.getTierLevel().getOrder(), a.getTierLevel().getOrder()));
                    }
                    break;
                case "Points":
                    if (sortAscending) {
                        filtered.sort((a, b) -> Integer.compare(a.getPoints(), b.getPoints()));
                    } else {
                        filtered.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
                    }
                    break;
                case "Name":
                    if (sortAscending) {
                        filtered.sort((a, b) -> a.getName().compareTo(b.getName()));
                    } else {
                        filtered.sort((a, b) -> b.getName().compareTo(a.getName()));
                    }
                    break;
                case "Completion":
                    if (sortAscending) {
                        filtered.sort((a, b) -> Boolean.compare(a.isCompleted(), b.isCompleted()));
                    } else {
                        filtered.sort((a, b) -> Boolean.compare(b.isCompleted(), a.isCompleted()));
                    }
                    break;
                default:
                    // Default sort by tier then name
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

        // Tier filter
        String selectedTier = (String) tierFilter.getSelectedItem();
        if (!"All Tiers".equals(selectedTier) && !achievement.getTier().equals(selectedTier))
        {
            return false;
        }

        // Status filter
        String selectedStatus = (String) statusFilter.getSelectedItem();
        if ("Completed".equals(selectedStatus) && !achievement.isCompleted())
        {
            return false;
        }
        if ("Incomplete".equals(selectedStatus) && achievement.isCompleted())
        {
            return false;
        }

        // Type filter
        String selectedType = (String) typeFilter.getSelectedItem();
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

    private void updateStatsOnly()
    {
        SwingUtilities.invokeLater(() -> {
            updateStats();
        });
    }

    private void updateStats()
    {
        List<CombatAchievement> visibleAchievements = showingTrackedOnly ? trackedAchievements : getFilteredAchievements(allAchievements);

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

        String viewContext = showingTrackedOnly ? "tracked" : "filtered";

        totalPointsLabel.setText("Total: " + completedPoints + "/" + totalPoints + " pts" +
                " (" + visibleAchievements.size() + " " + viewContext + ")");

        trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
                totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");

        if (totalCompletedPoints >= pointGoal) {
            goalLabel.setText("Goal: " + tierGoal + " Completed! (" + totalCompletedPoints + " pts)");
        } else {
            int remainingPoints = pointGoal - totalCompletedPoints;
            goalLabel.setText("Goal: " + remainingPoints + " pts to " + actualTierName);
        }
    }

    public int getPointsFromGoal(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints) {
        if (tierGoal.equals(CombatAchievementsConfig.TierGoal.TIER_AUTO)) {
            if (completedPoints < 38) {
                return 38; // Easy
            } else if (completedPoints < 148) {
                return 148; // Medium
            } else if (completedPoints < 394) {
                return 394; // Hard
            } else if (completedPoints < 1026) {
                return 1026; // Elite
            } else if (completedPoints < 1841) {
                return 1841; // Master
            } else {
                return 2525; // Grandmaster
            }
        }

        switch (tierGoal)
        {
            case TIER_EASY: return 38;
            case TIER_MEDIUM: return 148;
            case TIER_HARD: return 394;
            case TIER_ELITE: return 1026;
            case TIER_MASTER: return 1841;
            default: return 2525; // TIER_GRANDMASTER
        }
    }

    private String getActualTierName(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints) {
        if (tierGoal.toString().equalsIgnoreCase("AUTO")) {
            if (completedPoints < 38) {
                return "Easy";
            } else if (completedPoints < 148) {
                return "Medium";
            } else if (completedPoints < 394) {
                return "Hard";
            } else if (completedPoints < 1026) {
                return "Elite";
            } else if (completedPoints < 1841) {
                return "Master";
            } else {
                return "Grandmaster";
            }
        }

        return tierGoal.toString();
    }

    public void saveTrackedAchievements() {
        try {
            log.info("saveTrackedAchievements called - current tracked list size: {}", trackedAchievements.size());
            List<Integer> trackedIds = trackedAchievements.stream()
                    .map(CombatAchievement::getId)
                    .collect(Collectors.toList());
            String trackedJson = new Gson().toJson(trackedIds);
            log.info("JSON to save (with timestamp): '{}'", trackedJson);
            try {
                if (plugin.getConfigManager().getRSProfileKey() != null) {
                    plugin.getConfigManager().setConfiguration(
                            CombatAchievementsConfig.CONFIG_GROUP_NAME,
                            "trackedAchievements",
                            trackedJson
                    );
                }
                log.info("Saved {} tracked achievements to config", trackedIds.size());
            } catch (Exception e) {
                log.error("Config save failed", e);
            }
        } catch (Exception e) {
            log.error("Failed to save tracked achievements", e);
        }
    }

    public void loadTrackedAchievements() {
        try {
            try {
                String configJson = plugin.getConfigManager().getConfiguration(
                        CombatAchievementsConfig.CONFIG_GROUP_NAME,
                        "trackedAchievements"
                );
                if (configJson != null && !configJson.isEmpty()) {
                    Type listType = new TypeToken<List<Integer>>(){}.getType();
                    List<Integer> configTrackedIds = new Gson().fromJson(configJson, listType);
                    for (CombatAchievement achievement : allAchievements) {
                        if (configTrackedIds.contains(achievement.getId())) {
                            achievement.setTracked(true);
                            trackedAchievements.add(achievement);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("config not found or invalid: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to load tracked achievements", e);
        }
    }

    public void clearAllConfigData() {
        try {
            plugin.getConfigManager().unsetConfiguration(
                    CombatAchievementsConfig.CONFIG_GROUP_NAME,
                    " trackedAchievements"
            );
        } catch (Exception e) {
            log.debug("Failed to clear");
        }
    }

    public void clearAllTracked()
    {
        log.info("Manually clearing all tracked achievements. Current size: {}", trackedAchievements.size());
        for (CombatAchievement achievement : new ArrayList<>(trackedAchievements)) {
            achievement.setTracked(false);
        }
        clearAllConfigData();
        trackedAchievements.clear();
        updateStats();
        refreshAchievementsList();
        log.info("All tracked achievements cleared");
    }

    public void onAchievementCompleted(String message)
    {
        log.info("Achievement completed notification: {}", message);
        refreshAchievementsList();
    }

    public void addToTracked(CombatAchievement achievement)
    {
        log.info("addToTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
        log.debug("Current tracked list size before add: {}", trackedAchievements.size());
        if (!trackedAchievements.contains(achievement))
        {
            trackedAchievements.add(achievement);
            achievement.setTracked(true);
            log.info("Added achievement to tracked: {} (ID: {}). New size: {}",
                    achievement.getName(), achievement.getId(), trackedAchievements.size());

            CombatAchievementPanel panel = achievementPanels.get(achievement.getId());
            if (panel != null) {
                panel.refresh();
                log.debug("Refreshed individual panel for achievement: {}", achievement.getName());
            }

            updateStatsOnly();
            saveTrackedAchievements();
        }
        else
        {
            log.warn("Achievement {} (ID: {}) was already in tracked list!", achievement.getName(), achievement.getId());
        }
    }

    public void removeFromTracked(CombatAchievement achievement)
    {
        log.info("removeFromTracked called for: {} (ID: {})", achievement.getName(), achievement.getId());
        log.debug("Current tracked list size before remove: {}", trackedAchievements.size());
        if (trackedAchievements.remove(achievement))
        {
            achievement.setTracked(false);
            log.info("Removed achievement from tracked: {} (ID: {}). New size: {}",
                    achievement.getName(), achievement.getId(), trackedAchievements.size());

            CombatAchievementPanel panel = achievementPanels.get(achievement.getId());
            if (panel != null) {
                panel.refresh();
                log.debug("Refreshed individual panel for achievement: {}", achievement.getName());
            }

            updateStatsOnly();
            saveTrackedAchievements();

            if (showingTrackedOnly) {
                log.debug("In tracked view - rebuilding list to remove untracked item");
                refreshAchievementsList(false);
            }
        }
        else
        {
            log.warn("Achievement {} (ID: {}) was not in tracked list!", achievement.getName(), achievement.getId());
        }
    }

    private void loadSampleData()
    {
        allAchievements.add(new CombatAchievement(1, "Just Getting Started","", "Kill any boss", "Easy", 1, false, false));
        allAchievements.add(new CombatAchievement(2, "Squashing Foot Soldiers", "","Kill 5 goblins", "Easy", 1, false, false));
        allAchievements.add(new CombatAchievement(3, "Giant Mole Hunter","", "Kill the Giant Mole", "Medium", 2, false, false));
        allAchievements.add(new CombatAchievement(4, "Barrows Champion","", "Complete all Barrows brothers", "Hard", 4, false, false));
        allAchievements.add(new CombatAchievement(5, "Zuk Slayer","", "Complete the Inferno", "Master", 6, false, false));
    }

    public void refreshCombatAchievements()
    {
        plugin.refreshCombatAchievements();
    }
}