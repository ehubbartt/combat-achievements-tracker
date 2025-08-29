package com.catracker.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a single Combat Achievement
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CombatAchievement
{
	@EqualsAndHashCode.Include
	private final int id;

	private final String name;
	private final String description;
	private final String tier; // Easy, Medium, Hard, Elite, Master
	private final int points;

	private boolean completed;
	private boolean tracked;
	private long completedTimestamp;

	private String bossName;
	private String type; // Kill Count, Perfection, Restriction, Mechanical, etc.
	private int userDifficulty; // 1-5 user assigned difficulty
	private Float completionPercentage; // Wiki completion percentage
	private String wikiUrl;

	public CombatAchievement(int id, String name, String bossName, String type, String description, String tier, int points, boolean completed, boolean tracked)
	{
		this.id = id;
		this.name = name;
		this.bossName = bossName;
		this.type = type;
		this.description = description;
		this.tier = tier;
		this.points = points;
		this.completed = completed;
		this.tracked = tracked;
		this.completedTimestamp = completed ? System.currentTimeMillis() : 0;

		// Set defaults
		this.userDifficulty = 0; // Medium difficulty by default
	}

	/**
	 * Mark this achievement as completed
	 */
	public void markCompleted()
	{
		this.completed = true;
		this.completedTimestamp = System.currentTimeMillis();
	}

	/**
	 * Get the tier as an enum for easier comparison
	 */
	public TierLevel getTierLevel()
	{
		return TierLevel.fromString(tier);
	}

	/**
	 * Get the improved color associated with this tier (for better readability)
	 */
	public java.awt.Color getTierColor()
	{
		switch (tier.toLowerCase())
		{
			case "easy":
				return new java.awt.Color(205, 133, 63);  // Peru/Sandy brown
			case "medium":
				return new java.awt.Color(169, 169, 169); // Dark gray
			case "hard":
				return new java.awt.Color(105, 105, 105); // Dim gray
			case "elite":
				return new java.awt.Color(100, 149, 237); // Cornflower blue
			case "master":
				return new java.awt.Color(220, 20, 60);   // Crimson
			case "grandmaster":
				return new java.awt.Color(255, 215, 0);   // Gold
			default:
				return java.awt.Color.WHITE;
		}
	}

	/**
	 * Check if this achievement matches a search term
	 */
	public boolean matchesSearch(String searchTerm)
	{
		if (searchTerm == null || searchTerm.trim().isEmpty())
		{
			return true;
		}

		String term = searchTerm.toLowerCase();
		return name.toLowerCase().contains(term) ||
			description.toLowerCase().contains(term) ||
			(bossName != null && bossName.toLowerCase().contains(term)) ||
			tier.toLowerCase().contains(term);
	}

	/**
	 * Generate wiki URL if not set
	 */
	public String getWikiUrl()
	{
		if (wikiUrl != null && !wikiUrl.isEmpty())
		{
			return wikiUrl;
		}

		// Generate default wiki URL
		String encodedName = name.replace(" ", "_").replace("'", "%27");
		return "https://oldschool.runescape.wiki/w/" + encodedName;
	}

	/**
	 * Enum for tier levels with associated colors and point ranges
	 */
	public enum TierLevel
	{
		EASY("Easy", new java.awt.Color(205, 133, 63), 1),          // Peru/Sandy brown
		MEDIUM("Medium", new java.awt.Color(169, 169, 169), 2),     // Dark gray
		HARD("Hard", new java.awt.Color(105, 105, 105), 3),         // Dim gray
		ELITE("Elite", new java.awt.Color(100, 149, 237), 4),       // Cornflower blue
		MASTER("Master", new java.awt.Color(220, 20, 60), 5),       // Crimson
		GRANDMASTER("Grandmaster", new java.awt.Color(255, 215, 0), 6); // Gold

		private final String displayName;
		private final java.awt.Color color;
		private final int basePoints;

		TierLevel(String displayName, java.awt.Color color, int basePoints)
		{
			this.displayName = displayName;
			this.color = color;
			this.basePoints = basePoints;
		}

		public java.awt.Color getColor()
		{
			return color;
		}

		public int getBasePoints()
		{
			return basePoints;
		}

		public int getOrder()
		{
			return ordinal(); // Returns 0 for Easy, 1 for Medium, etc.
		}

		public static TierLevel fromString(String tier)
		{
			for (TierLevel level : values())
			{
				if (level.displayName.equalsIgnoreCase(tier))
				{
					return level;
				}
			}
			return EASY; // Default fallback
		}
	}
}