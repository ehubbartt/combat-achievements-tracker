package com.catracker.ui;

import com.catracker.CombatAchievementsPlugin;
import com.catracker.model.CombatAchievement;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

@Slf4j
public class CombatAchievementPanel extends JPanel
{
	private final CombatAchievementsPlugin plugin;
	private final CombatAchievement achievement;

	// UI Components
	private final JPanel container = new JPanel(new BorderLayout());
	private final JPanel body = new JPanel(new BorderLayout());
	private final JLabel nameLabel = new JLabel();
	private final JLabel descriptionLabel = new JLabel();
	private final JToggleButton trackButton = new JToggleButton();
	private final JLabel tierIconLabel = new JLabel();

	// Store references to all panels that need background color updates
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

		descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
		descriptionLabel.setForeground(Color.LIGHT_GRAY);
		descriptionLabel.setText("<html>" + achievement.getDescription() + "</html>");
		descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		centerPanel.add(descriptionLabel);

		body.add(topSection, BorderLayout.NORTH);
		body.add(centerPanel, BorderLayout.CENTER);

		container.add(body, BorderLayout.CENTER);
		add(container, BorderLayout.CENTER);

		setToolTipText(createTooltip());
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

		// Right-click context menu
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
		});
	}

	@SuppressWarnings("checkstyle:LeftCurly")
	private void showContextMenu(MouseEvent e)
	{
		JPopupMenu popup = new JPopupMenu();

		// Wiki link
		JMenuItem wikiItem = new JMenuItem("Open Wiki");
		wikiItem.addActionListener(event -> openWikiLink());
		popup.add(wikiItem);

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
			Desktop.getDesktop().browse(new URI(achievement.getWikiUrl()));
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
			// Fallback to text if icons can't be loaded
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

	private void updateAllBackgrounds()
	{
		Color bgColor = getBackgroundColor();

		container.setBackground(bgColor);
		body.setBackground(bgColor);
		topSection.setBackground(bgColor);
		nameLabelPanel.setBackground(bgColor);
		topRightPanel.setBackground(bgColor);

		Component[] components = nameLabelPanel.getComponents();
		for (Component comp : components)
		{
			if (comp instanceof JPanel)
			{
				comp.setBackground(bgColor);
			}
		}
	}

	private String createTooltip()
	{
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("<html>");
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
			tooltip.append("Player Completion: ").append(String.format("%.1f%%", achievement.getCompletionPercentage())).append("<br>");
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

	@SuppressWarnings("checkstyle:LeftCurly")
	public void refresh()
	{
		SwingUtilities.invokeLater(() ->
		{
			nameLabel.setText("<html>" + achievement.getName() + "</html>");
			nameLabel.setForeground(getNameColor());

			setupTierIcon();
			updateAllBackgrounds();
			updateTrackButton();
			setToolTipText(createTooltip());
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