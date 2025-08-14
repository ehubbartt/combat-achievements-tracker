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
    private final JToggleButton trackButton = new JToggleButton();
    private final JLabel tierIconLabel = new JLabel(); // NEW: Tier icon

    // Store references to all panels that need background color updates
    private final JPanel topSection = new JPanel(new BorderLayout());
    private final JPanel nameLabelPanel = new JPanel(new BorderLayout());
    private final JPanel topRightPanel = new JPanel();
    private final JPanel bottomSection = new JPanel(new BorderLayout());
    private final JPanel leftInfo = new JPanel();
    private final JLabel bossLabel = new JLabel();

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
        nameLabel.setText("<html>" + achievement.getName() + "</html>");

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

        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        descriptionLabel.setForeground(Color.LIGHT_GRAY);
        descriptionLabel.setText("<html>" + achievement.getDescription() + "</html>");

        bottomSection.setBackground(getBackgroundColor());

        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setBackground(getBackgroundColor());

        JLabel typeLabel = new JLabel();
        typeLabel.setFont(FontManager.getRunescapeSmallFont());
        typeLabel.setForeground(Color.LIGHT_GRAY);
        typeLabel.setText(achievement.getType() != null ? achievement.getType() : "Unknown");

        tierLabel.setFont(FontManager.getRunescapeSmallFont());
        tierLabel.setForeground(getImprovedTierColor());
        tierLabel.setText(achievement.getTier() + " - " + achievement.getType());

        leftInfo.add(tierLabel);

        String bossInfo = achievement.getBossName() != null ? achievement.getBossName() : "";
        if (!achievement.isSoloOnly()) {
            bossInfo += (bossInfo.isEmpty() ? "" : " • ") + "Group";
        }
        bossLabel.setText(bossInfo);
        bossLabel.setFont(FontManager.getRunescapeSmallFont());
        bossLabel.setForeground(Color.GRAY);

        pointsLabel.setFont(FontManager.getRunescapeSmallFont());
        pointsLabel.setForeground(Color.CYAN);
        pointsLabel.setText(achievement.getPoints() + " pts");

        bottomSection.add(leftInfo, BorderLayout.WEST);
        bottomSection.add(bossLabel, BorderLayout.CENTER);
        bottomSection.add(pointsLabel, BorderLayout.EAST);

        body.add(topSection, BorderLayout.NORTH);
        body.add(descriptionLabel, BorderLayout.CENTER);
        body.add(bottomSection, BorderLayout.SOUTH);

        container.add(body, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);

        setToolTipText(createTooltip());
    }

    // NEW: Setup tier icon based on achievement tier
    private void setupTierIcon() {
        try {
            String tierName = achievement.getTier().toLowerCase();
            String iconPath = tierName + "_tier.png";

            BufferedImage originalIcon = ImageUtil.loadImageResource(CombatAchievementPanel.class, iconPath);
            BufferedImage resizedIcon = ImageUtil.resizeImage(originalIcon, 16, 16);
            tierIconLabel.setIcon(new ImageIcon(resizedIcon));
            tierIconLabel.setToolTipText(achievement.getTier() + " Tier");
        } catch (Exception e) {
            log.warn("Could not load tier icon for {}: {}", achievement.getTier(), e.getMessage());
            // Fallback: no icon
            tierIconLabel.setIcon(null);
        }
    }

    private Color getImprovedTierColor() {
        switch (achievement.getTier().toLowerCase()) {
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

    private Color getNameColor() {
        if (achievement.isCompleted()) {
            return Color.GREEN;
        } else if (achievement.isTracked()) {
            return new Color(100, 149, 237);
        } else {
            return ColorScheme.BRAND_ORANGE;
        }
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
        return ColorScheme.DARKER_GRAY_COLOR;
    }

    // Method to update all panel backgrounds
    private void updateAllBackgrounds() {
        Color bgColor = getBackgroundColor();

        // Only update backgrounds of panels INSIDE the bordered container
        container.setBackground(bgColor);
        body.setBackground(bgColor);
        topSection.setBackground(bgColor);
        nameLabelPanel.setBackground(bgColor);
        topRightPanel.setBackground(bgColor);
        bottomSection.setBackground(bgColor);
        leftInfo.setBackground(bgColor);

        // Also update the name with icon panel background
        Component[] components = nameLabelPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                comp.setBackground(bgColor);
            }
        }
    }

    private String createTooltip() {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html>");
        tooltip.append("<b>").append(achievement.getName()).append("</b><br>");
        tooltip.append(achievement.getDescription()).append("<br><br>");
        tooltip.append("Tier: ").append(achievement.getTier()).append("<br>");
        tooltip.append("Points: ").append(achievement.getPoints()).append("<br>");
        if (achievement.getBossName() != null) {
            tooltip.append("Boss: ").append(achievement.getBossName()).append("<br>");
        }
        if (achievement.getType() != null) {
            tooltip.append("Type: ").append(achievement.getType()).append("<br>");
        }

        String difficultyText = achievement.getUserDifficulty() == 0 ? "Unrated" : achievement.getUserDifficulty() + "/5";
        tooltip.append("Your Difficulty: ").append(difficultyText).append("<br>");
        if (achievement.getCompletionPercentage() != null) {
            tooltip.append("Player Completion: ").append(String.format("%.1f%%", achievement.getCompletionPercentage())).append("<br>");
        }
        if (achievement.isCompleted()) {
            tooltip.append("<br><font color='green'>Completed</font>");
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            nameLabel.setText("<html>" + achievement.getName() + "</html>");
            nameLabel.setForeground(getNameColor());

            tierLabel.setForeground(getImprovedTierColor());

            setupTierIcon();

            updateAllBackgrounds();

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