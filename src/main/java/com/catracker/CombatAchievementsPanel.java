package com.catracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    // UI Components
    private final JTextField searchField = new JTextField();
    private final JPanel headerPanel = new JPanel(new BorderLayout());
    private final JPanel filtersPanel = new JPanel();
    private final JPanel statsPanel = new JPanel();
    private final JScrollPane achievementsList = new JScrollPane();
    private final JPanel achievementsContainer = new JPanel();
    private final JButton refreshButton = new JButton("Refresh Data");

    // Stats labels
    private final JLabel totalPointsLabel = new JLabel("Total Points: 0");
    private final JLabel trackedPointsLabel = new JLabel("Tracked: 0");
    private final JLabel goalLabel = new JLabel("Progress: 0/50");

    // Filter components
    private final JComboBox<String> tierFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
//    private final JCheckBox soloOnlyFilter = new JCheckBox("Solo Only");

    // Sample data - Real data will be loaded by the plugin
    private List<CombatAchievement> allAchievements = new ArrayList<>();
    private List<CombatAchievement> trackedAchievements = new ArrayList<>();
    private String currentSearchText = "";

    public CombatAchievementsPanel(CombatAchievementsPlugin plugin)
    {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        initializeComponents();
        layoutComponents();
        setupEventHandlers();

        loadSampleData();
        refreshAchievementsList();
    }

    private void initializeComponents()
    {
        searchField.setFont(FontManager.getRunescapeSmallFont());
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setBorder(new EmptyBorder(5, 5, 5, 5));

        tierFilter.addItem("All Tiers");
        tierFilter.addItem("Easy");
        tierFilter.addItem("Medium");
        tierFilter.addItem("Hard");
        tierFilter.addItem("Elite");
        tierFilter.addItem("Master");
        tierFilter.addItem("Grandmaster");

        statusFilter.addItem("All");
        statusFilter.addItem("Completed");
        statusFilter.addItem("Incomplete");
        statusFilter.addItem("Tracked");

        typeFilter.addItem("All Types");
        typeFilter.addItem("Stamina");
        typeFilter.addItem("Perfection");
        typeFilter.addItem("Kill Count");
        typeFilter.addItem("Mechanical");
        typeFilter.addItem("Restriction");
        typeFilter.addItem("Speed");
        typeFilter.addItem("Other");

        totalPointsLabel.setFont(FontManager.getRunescapeSmallFont());
        totalPointsLabel.setForeground(Color.WHITE);

        trackedPointsLabel.setFont(FontManager.getRunescapeSmallFont());
        trackedPointsLabel.setForeground(Color.CYAN);

        goalLabel.setFont(FontManager.getRunescapeSmallFont());
        goalLabel.setForeground(Color.GREEN);

        achievementsContainer.setLayout(new BoxLayout(achievementsContainer, BoxLayout.Y_AXIS));
        achievementsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        achievementsContainer.setBorder(new EmptyBorder(5, 5, 5, 5)); // Reduced padding
        achievementsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        achievementsList.setViewportView(achievementsContainer);
        achievementsList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        achievementsList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        achievementsList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        achievementsList.setBorder(null); // Remove scroll pane border
    }

    private void layoutComponents()
    {
        JLabel titleLabel = new JLabel("Combat Achievements");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(new EmptyBorder(0, 10, 5, 10));
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(0, 10, 5, 10));
        refreshButton.setFont(FontManager.getRunescapeSmallFont());
        refreshButton.setToolTipText("Refresh Combat Achievements from game data");
        buttonPanel.add(refreshButton);

        JButton debugButton = new JButton("Debug");
        debugButton.setFont(FontManager.getRunescapeSmallFont());
        debugButton.setToolTipText("Debug tracked achievements");
        debugButton.addActionListener(e -> {
            debugTrackedState();

            // Show a popup with current state
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Check console for debug info. What would you like to do?",
                    "Debug Tracked Achievements",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Clear All Tracked", "Force Save", "Force Load", "Cancel"},
                    "Cancel"
            );

            switch (choice) {
                case 0: // Clear All
                    clearAllTracked();
                    JOptionPane.showMessageDialog(this, "All tracked achievements cleared!");
                    break;
                case 1: // Force Save
                    plugin.saveTrackedAchievements(trackedAchievements);
                    JOptionPane.showMessageDialog(this, "Force save completed - check console!");
                    break;
                case 2: // Force Load
                    plugin.loadTrackedAchievements();
                    refreshAchievementsList();
                    JOptionPane.showMessageDialog(this, "Force load completed - check console!");
                    break;
            }
        });

        buttonPanel.add(debugButton);

        JPanel searchAndButtonPanel = new JPanel(new BorderLayout());
        searchAndButtonPanel.add(searchPanel, BorderLayout.CENTER);
        searchAndButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(searchAndButtonPanel, BorderLayout.CENTER);

        filtersPanel.setLayout(new GridBagLayout());
        filtersPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        filtersPanel.add(new JLabel("Tier:"), gbc);
        gbc.gridx = 1;
        filtersPanel.add(tierFilter, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        filtersPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        filtersPanel.add(statusFilter, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        filtersPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        filtersPanel.add(typeFilter, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
//        filtersPanel.add(soloOnlyFilter, gbc);

        statsPanel.setLayout(new GridLayout(3, 1, 5, 5));
        statsPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        statsPanel.add(totalPointsLabel);
        statsPanel.add(trackedPointsLabel);
        statsPanel.add(goalLabel);

        JPanel topFixedContent = new JPanel(new BorderLayout());
        topFixedContent.add(headerPanel, BorderLayout.NORTH);

        JPanel filtersAndStats = new JPanel(new BorderLayout());
        filtersAndStats.add(filtersPanel, BorderLayout.NORTH);
        filtersAndStats.add(statsPanel, BorderLayout.CENTER);
        topFixedContent.add(filtersAndStats, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(topFixedContent, BorderLayout.NORTH);
        add(achievementsList, BorderLayout.CENTER); // Achievements list takes remaining space

        achievementsList.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 400));
    }

    private void setupEventHandlers()
    {
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                currentSearchText = searchField.getText().toLowerCase();
                refreshAchievementsList();
            }
        });

        tierFilter.addActionListener(e -> refreshAchievementsList());
        statusFilter.addActionListener(e -> refreshAchievementsList());
        typeFilter.addActionListener(e -> refreshAchievementsList());
//        soloOnlyFilter.addActionListener(e -> refreshAchievementsList());

        refreshButton.addActionListener(e -> {
            refreshButton.setText("Refreshing...");
            refreshButton.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                plugin.loadTrackedAchievements();
                refreshButton.setText("Refresh Data");
                refreshButton.setEnabled(true);
            });
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

            // Load tracked achievements AFTER we have the full achievements list
            plugin.loadTrackedAchievements();

            // Force a complete UI refresh
            log.info("About to call refreshAchievementsList...");
            refreshAchievementsList();
            log.info("refreshAchievementsList completed");
        });
    }

    private void refreshAchievementsList()
    {
        SwingUtilities.invokeLater(() -> {

            achievementsContainer.removeAll();

            List<CombatAchievement> filteredAchievements = getFilteredAchievements();

            if (filteredAchievements.isEmpty())
            {
                JLabel emptyLabel = new JLabel("No achievements match current filters");
                emptyLabel.setFont(FontManager.getRunescapeSmallFont());
                emptyLabel.setForeground(Color.GRAY);
                emptyLabel.setHorizontalAlignment(JLabel.CENTER);
                emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
                achievementsContainer.add(emptyLabel);
            }
            else
            {
                for (CombatAchievement achievement : filteredAchievements)
                {
                    try {
                        CombatAchievementPanel panel = new CombatAchievementPanel(plugin, achievement);

                        panel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 30, panel.getPreferredSize().height));
                        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

                        achievementsContainer.add(panel);
                    } catch (Exception e) {
                        log.error("Error creating panel for achievement: {}", achievement.getName(), e);
                    }
                }
            }

            updateStats();

            achievementsContainer.revalidate();
            achievementsContainer.repaint();

            revalidate();
            repaint();

            SwingUtilities.invokeLater(() -> {
                achievementsList.getVerticalScrollBar().setValue(0);
            });
        });
    }

    private List<CombatAchievement> getFilteredAchievements()
    {
        List<CombatAchievement> filtered = allAchievements.stream()
                .filter(this::matchesFilters)
                .sorted((a, b) -> {
                    // Sort by tier order (Easy -> Medium -> Hard -> Elite -> Master -> Grandmaster)
                    int tierComparison = Integer.compare(
                            a.getTierLevel().getOrder(),
                            b.getTierLevel().getOrder()
                    );

                    // If same tier, sort by name
                    if (tierComparison == 0) {
                        return a.getName().compareTo(b.getName());
                    }

                    return tierComparison;
                })
                .collect(Collectors.toList());

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
        if ("Tracked".equals(selectedStatus) && !achievement.isTracked())
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

        // Solo filter
//        if (soloOnlyFilter.isSelected() && !achievement.isSoloOnly())
//        {
//            return false;
//        }

        return true;
    }

    private void updateStats()
    {
        List<CombatAchievement> visibleAchievements = getFilteredAchievements();

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
        int pointGoal = getPointsFromGoal(tierGoal);
        totalPointsLabel.setText("Total: " + completedPoints + "/" + totalPoints
                + " (" + visibleAchievements.size() + " tasks)");
        trackedPointsLabel.setText("Tracked: " + completedTrackedPoints + "/" +
                totalTrackedPoints + " pts (" + trackedAchievements.size() + " tasks)");
        goalLabel.setText("Goal: " + (pointGoal - totalCompletedPoints) + " points to " + tierGoal);
    }

    public int getPointsFromGoal(CombatAchievementsConfig.TierGoal tierGoal) {
        switch (tierGoal)
        {
            case TIER_EASY: return 38;
            case TIER_MEDIUM: return 148;
            case TIER_HARD: return 394;
            case TIER_ELITE: return 1026;
            case TIER_MASTER: return 1841;
            default: return 2525;
        }
    }

    // Add this method to manually clear all tracked achievements for testing
    public void clearAllTracked()
    {
        log.info("Manually clearing all tracked achievements. Current size: {}", trackedAchievements.size());

        for (CombatAchievement achievement : new ArrayList<>(trackedAchievements)) {
            achievement.setTracked(false);
        }
        plugin.clearAllConfigData();
        trackedAchievements.clear();
//        saveTrackedAchievements();
        updateStats();
        refreshAchievementsList();

        log.info("All tracked achievements cleared");
    }

    // Add this method to debug the current state
    public void debugTrackedState()
    {
        log.info("=== TRACKED ACHIEVEMENTS DEBUG ===");
        log.info("trackedAchievements list size: {}", trackedAchievements.size());

        for (int i = 0; i < trackedAchievements.size(); i++) {
            CombatAchievement achievement = trackedAchievements.get(i);
            log.info("  [{}] {} (ID: {}) - tracked: {}",
                    i, achievement.getName(), achievement.getId(), achievement.isTracked());
        }

        // Count how many achievements in allAchievements are marked as tracked
        long trackedCount = allAchievements.stream()
                .filter(CombatAchievement::isTracked)
                .count();

        log.info("Total achievements marked as tracked in allAchievements: {}", trackedCount);
        log.info("=== END DEBUG ===");
    }

    public void onAchievementCompleted(String message)
    {
        log.info("Achievement completed notification: {}", message);
        // TODO: Parse the message and update the corresponding achievement
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

            plugin.saveTrackedAchievements(trackedAchievements);
            updateStats();
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

            plugin.saveTrackedAchievements(trackedAchievements);
            updateStats();
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