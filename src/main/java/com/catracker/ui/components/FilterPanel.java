package com.catracker.ui.components;
import com.catracker.ui.util.IconLoader;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel containing tier toggles and filter dropdowns
 */
public class FilterPanel extends JPanel
{

	private boolean filtersExpanded = false;
	private boolean tiersExpanded = false;
	private boolean sortAscending = true;

	private final JPanel filtersSection = new JPanel();
	private final JButton filtersToggleButton = new JButton("Filters v");
	private final JPanel filtersPanel = new JPanel();
	private final JPanel tiersSection = new JPanel();
	private final JButton tiersToggleButton = new JButton("Tiers v");
	private final JPanel tiersPanel = new JPanel();

	private final JComboBox<String> tierFilter = new JComboBox<>();
	private final JComboBox<String> statusFilter = new JComboBox<>();
	private final JComboBox<String> typeFilter = new JComboBox<>();
	private final JComboBox<String> sortFilter = new JComboBox<>();
	private final JButton sortDirectionButton = new JButton("^/v");

	private final Map<String, Boolean> selectedTiers = new HashMap<>();
	private Consumer<Void> refreshCallback;

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
		setupTiersSection();
		setupFiltersSection();
	}

	private void setupFilterDropdowns()
	{
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

		sortDirectionButton.setFont(FontManager.getRunescapeSmallFont());
		sortDirectionButton.setPreferredSize(new Dimension(25, 20));
		sortDirectionButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortDirectionButton.setForeground(Color.WHITE);
		sortDirectionButton.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		sortDirectionButton.setFocusPainted(false);
	}

	private void setupTiersSection()
	{
		tiersSection.setLayout(new BorderLayout());
		tiersSection.setBorder(new EmptyBorder(0, 10, 5, 10));
		tiersSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

		tiersToggleButton.setFont(FontManager.getRunescapeSmallFont());
		tiersToggleButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tiersToggleButton.setForeground(Color.WHITE);
		tiersToggleButton.setBorder(new EmptyBorder(6, 8, 6, 8));
		tiersToggleButton.setFocusPainted(false);
		tiersToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
		tiersToggleButton.setText("Tiers                                                      v");

		tiersPanel.setLayout(new GridLayout(2, 3, 5, 5));
		tiersPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
		tiersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tiersPanel.setVisible(tiersExpanded);

		String[] tiers = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
		for (String tier : tiers)
		{
			JToggleButton tierButton = createTierButton(tier);
			tiersPanel.add(tierButton);
		}

		tiersSection.add(tiersToggleButton, BorderLayout.NORTH);
		tiersSection.add(tiersPanel, BorderLayout.CENTER);
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

		filtersToggleButton.setFont(FontManager.getRunescapeSmallFont());
		filtersToggleButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filtersToggleButton.setForeground(Color.WHITE);
		filtersToggleButton.setBorder(new EmptyBorder(6, 8, 6, 8));
		filtersToggleButton.setFocusPainted(false);
		filtersToggleButton.setHorizontalAlignment(SwingConstants.LEFT);
		filtersToggleButton.setText("Filters                                                    v");

		filtersPanel.setLayout(new GridBagLayout());
		filtersPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
		filtersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filtersPanel.setVisible(filtersExpanded);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 0, 2, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;

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

	private void layoutComponents()
	{
		add(tiersSection);
		add(filtersSection);
	}

	private void setupEventHandlers()
	{
		tiersToggleButton.addActionListener(e -> toggleTiers());
		filtersToggleButton.addActionListener(e -> toggleFilters());
		sortDirectionButton.addActionListener(e -> toggleSortDirection());

		tierFilter.addActionListener(e -> triggerRefresh());
		statusFilter.addActionListener(e -> triggerRefresh());
		typeFilter.addActionListener(e -> triggerRefresh());
		sortFilter.addActionListener(e -> triggerRefresh());
	}

	private void toggleTiers()
	{
		tiersExpanded = !tiersExpanded;
		tiersPanel.setVisible(tiersExpanded);
		tiersToggleButton.setText(tiersExpanded ?
			"Tiers                                                      ^" :
			"Tiers                                                      v");
		revalidate();
		repaint();
	}

	private void toggleFilters()
	{
		filtersExpanded = !filtersExpanded;
		filtersPanel.setVisible(filtersExpanded);
		filtersToggleButton.setText(filtersExpanded ?
			"Filters                                                    ^" :
			"Filters                                                    v");
		revalidate();
		repaint();
	}

	private void toggleSortDirection()
	{
		sortAscending = !sortAscending;
		sortDirectionButton.setText(sortAscending ? "^/v" : "v/^");
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

	public String getSelectedTierFilter()
	{
		return (String) tierFilter.getSelectedItem();
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