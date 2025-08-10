package com.catracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

@Slf4j
public class CombatAchievementPanel extends JPanel {
    private final CombatAchievementsPlugin plugin;
    private final CombatAchievement achievement;

    // UI Components
    private final JPanel container = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new BorderLayout());
    private final JLabel nameLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel tierLabel = new JLabel();
    private final JLabel pointsLabel = new JLabel();
    private final JPanel buttonPanel = new JPanel();
    private final JToggleButton trackButton = new JToggleButton();
    private final JButton priorityButton = new JButton();

    public CombatAchievementPanel(CombatAchievementsPlugin plugin, CombatAchievement achievement) {
        super(new BorderLayout());
        this.plugin = plugin;
        this.achievement = achievement;

        createPanel();
        setupEventHandlers();
        refresh();
    }

    private void createPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2, 2, 2, 2));

        container.setBorder(new LineBorder(achievement.getTierColor(), 1));
        container.setBackground(getBackgroundColor());

        body.setLayout(new BorderLayout());
        body.setBackground(getBackgroundColor());
        body.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(getBackgroundColor());

        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(achievement.isCompleted() ? Color.GREEN : Color.WHITE);

        String displayName = achievement.getName();
        nameLabel.setText("<html><div style='width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;'>" + displayName + "</div></html>");

        pointsLabel.setFont(FontManager.getRunescapeSmallFont());
        pointsLabel.setForeground(Color.CYAN);
        pointsLabel.setText(achievement.getPoints() + " pts");

        topSection.add(nameLabel, BorderLayout.WEST);
        topSection.add(pointsLabel, BorderLayout.EAST);

        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        descriptionLabel.setForeground(Color.LIGHT_GRAY);
        descriptionLabel.setText("<html><div style='width: 180px;'>" + achievement.getDescription() + "</div></html>");

        JPanel bottomSection = new JPanel(new BorderLayout());
        bottomSection.setBackground(getBackgroundColor());

        JPanel leftInfo = new JPanel();
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setBackground(getBackgroundColor());

        JLabel typeLabel = new JLabel();
        typeLabel.setFont(FontManager.getRunescapeSmallFont());
        typeLabel.setForeground(Color.LIGHT_GRAY);
        typeLabel.setText(achievement.getType() != null ? achievement.getType() : "Unknown");

        tierLabel.setFont(FontManager.getRunescapeSmallFont());
        tierLabel.setForeground(achievement.getTierColor());
        tierLabel.setText(achievement.getTier() + " - " + achievement.getType());

        leftInfo.add(tierLabel);

        String bossInfo = achievement.getBossName() != null ? achievement.getBossName() : "";
        if (!achievement.isSoloOnly()) {
            bossInfo += (bossInfo.isEmpty() ? "" : " • ") + "Group";
        }

        JLabel bossLabel = new JLabel(bossInfo);
        bossLabel.setFont(FontManager.getRunescapeSmallFont());
        bossLabel.setForeground(Color.GRAY);

        bottomSection.add(leftInfo, BorderLayout.WEST);
        bottomSection.add(bossLabel, BorderLayout.EAST);

        body.add(topSection, BorderLayout.NORTH);
        body.add(descriptionLabel, BorderLayout.CENTER);
        body.add(bottomSection, BorderLayout.SOUTH);

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(getBackgroundColor());
        buttonPanel.setBorder(new EmptyBorder(6, 0, 6, 6));

        trackButton.setPreferredSize(new Dimension(12, 12));
        trackButton.setMaximumSize(new Dimension(12, 12));
        trackButton.setMinimumSize(new Dimension(12, 12));
        updateTrackButton();
        SwingUtil.removeButtonDecorations(trackButton);

        // Priority button (shows user priority 0-5)
        priorityButton.setPreferredSize(new Dimension(12, 12));
        priorityButton.setMaximumSize(new Dimension(12, 12));
        priorityButton.setMinimumSize(new Dimension(12, 12));
        updatePriorityButton();
        SwingUtil.removeButtonDecorations(priorityButton);

        buttonPanel.add(trackButton);
        buttonPanel.add(Box.createVerticalStrut(3));
        buttonPanel.add(priorityButton);

        // Assemble container
        container.add(body, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.EAST);

        add(container, BorderLayout.CENTER);

        // Tooltip
        setToolTipText(createTooltip());
    }

    private void setupEventHandlers() {
        trackButton.addActionListener(e -> {
            achievement.setTracked(!achievement.isTracked());
            updateTrackButton();

            if (achievement.isTracked()) {
                plugin.getPanel().addToTracked(achievement);
            } else {
                plugin.getPanel().removeFromTracked(achievement);
            }
        });

        // Priority button - cycle through 0-5
        priorityButton.addActionListener(e -> {
            int currentPriority = achievement.getUserPriority();
            int newPriority = (currentPriority + 1) % 6; // Cycle 0->1->2->3->4->5->0
            achievement.setUserPriority(newPriority);
            updatePriorityButton();
        });

        // Right-click context menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        // Wiki link
        JMenuItem wikiItem = new JMenuItem("Open Wiki");
        wikiItem.addActionListener(event -> openWikiLink());
        popup.add(wikiItem);

        popup.addSeparator();

        JMenu difficultyMenu = new JMenu("Set Difficulty");

        // Add "Unrated" option
        JMenuItem unratedItem = new JMenuItem("Unrated");
        unratedItem.addActionListener(event -> {
            achievement.setUserDifficulty(0);
            setToolTipText(createTooltip());
        });
        difficultyMenu.add(unratedItem);

        for (int i = 1; i <= 5; i++) {
            final int difficulty = i;
            JMenuItem diffItem = new JMenuItem(difficulty + "/5" + (difficulty == achievement.getUserDifficulty() ? " ✓" : ""));
            diffItem.addActionListener(event -> {
                achievement.setUserDifficulty(difficulty);
                setToolTipText(createTooltip());
            });
            difficultyMenu.add(diffItem);
        }
        popup.add(difficultyMenu);

        // Mark completed (for testing)
        if (!achievement.isCompleted()) {
            popup.addSeparator();
            JMenuItem completeItem = new JMenuItem("Mark as Completed (Test)");
            completeItem.addActionListener(event -> {
                achievement.markCompleted();
                refresh();
            });
            popup.add(completeItem);
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void openWikiLink() {
        try {
            Desktop.getDesktop().browse(new URI(achievement.getWikiUrl()));
        } catch (Exception ex) {
            log.error("Failed to open wiki link: {}", achievement.getWikiUrl(), ex);
        }
    }

    private void updatePriorityButton() {
        int priority = achievement.getUserPriority();
        if (priority == 0) {
            priorityButton.setText("-");
            priorityButton.setToolTipText("Priority: Unrated (click to set)");
        } else {
            priorityButton.setText(String.valueOf(priority));
            priorityButton.setToolTipText("Priority: " + priority + "/5");
        }
        priorityButton.setFont(FontManager.getRunescapeSmallFont());
    }

    private void updateTrackButton() {
        try {
            BufferedImage originalIcon;
            if (achievement.isTracked()) {
                originalIcon = ImageUtil.loadImageResource(CombatAchievementPanel.class, "track_remove.png");
                trackButton.setToolTipText("Remove from tracking");
            } else {
                originalIcon = ImageUtil.loadImageResource(CombatAchievementPanel.class, "track_add.png");
                trackButton.setToolTipText("Add to tracking");
            }

            BufferedImage resizedIcon = ImageUtil.resizeImage(originalIcon, 12, 12);

            trackButton.setIcon(new ImageIcon(resizedIcon));
            trackButton.setText("");

        } catch (Exception e) {
            // Fallback to text if icons can't be loaded
            if (achievement.isTracked()) {
                trackButton.setText("X");
                trackButton.setIcon(null);
                trackButton.setBackground(new Color(120, 70, 70));
                trackButton.setForeground(Color.WHITE);
                trackButton.setToolTipText("Remove from tracking");
            } else {
                trackButton.setText("+");
                trackButton.setIcon(null);
                trackButton.setBackground(new Color(80, 120, 80));
                trackButton.setForeground(Color.WHITE);
                trackButton.setToolTipText("Add to tracking");
            }
        }
    }

    private Color getBackgroundColor() {
        if (achievement.isCompleted()) {
            return new Color(0, 40, 20);
        } else if (achievement.isTracked()) {
            return new Color(0, 30, 40);
        } else {
            return ColorScheme.DARKER_GRAY_COLOR;
        }
    }

    private String createTooltip() {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>");
        tooltip.append("<b>").append(achievement.getName()).append("</b><br>");
        tooltip.append(achievement.getDescription()).append("<br><br>");

        tooltip.append("<b>Tier:</b> ").append(achievement.getTier()).append("<br>");
        tooltip.append("<b>Points:</b> ").append(achievement.getPoints()).append("<br>");

        if (achievement.getBossName() != null) {
            tooltip.append("<b>Boss:</b> ").append(achievement.getBossName()).append("<br>");
        }

        if (achievement.getType() != null) {
            tooltip.append("<b>Type:</b> ").append(achievement.getType()).append("<br>");
        }

        tooltip.append("<b>Solo:</b> ").append(achievement.isSoloOnly() ? "Yes" : "No").append("<br>");

        String priorityText = achievement.getUserPriority() == 0 ? "Unrated" : achievement.getUserPriority() + "/5";
        String difficultyText = achievement.getUserDifficulty() == 0 ? "Unrated" : achievement.getUserDifficulty() + "/5";

        tooltip.append("<b>Your Priority:</b> ").append(priorityText).append("<br>");
        tooltip.append("<b>Your Difficulty:</b> ").append(difficultyText).append("<br>");

        if (achievement.getCompletionPercentage() != null) {
            tooltip.append("<b>Player Completion:</b> ").append(String.format("%.1f%%", achievement.getCompletionPercentage())).append("<br>");
        }

        if (achievement.isCompleted()) {
            tooltip.append("<br><b>Completed</b>");
        }

        tooltip.append("</html>");
        return tooltip.toString();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            nameLabel.setText(achievement.getName() + (achievement.isCompleted() ? " ✓" : ""));
            nameLabel.setForeground(achievement.isCompleted() ? Color.GREEN : Color.WHITE);

            container.setBackground(getBackgroundColor());
            body.setBackground(getBackgroundColor());
            buttonPanel.setBackground(getBackgroundColor());

            updateTrackButton();
            setToolTipText(createTooltip());

            revalidate();
            repaint();
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(PluginPanel.PANEL_WIDTH - 20, getPreferredSize().height);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PluginPanel.PANEL_WIDTH - 20, super.getPreferredSize().height);
    }
}