package com.catracker;

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

    // Additional properties for your feature requirements
    private String bossName;
    private String type; // Kill Count, Perfection, Restriction, Mechanical, etc.
    private boolean soloOnly;
    private int userPriority; // 1-5 user assigned priority
    private int userDifficulty; // 1-5 user assigned difficulty
    private Float completionPercentage; // Wiki completion percentage
    private String wikiUrl;

    public CombatAchievement(int id, String name, String type, String description, String tier, int points, boolean completed, boolean tracked)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.tier = tier;
        this.points = points;
        this.completed = completed;
        this.tracked = tracked;
        this.completedTimestamp = completed ? System.currentTimeMillis() : 0;

        // Set defaults
        this.userPriority = 0; // Medium priority by default
        this.userDifficulty = 0; // Medium difficulty by default
        this.soloOnly = true; // Most combat achievements are solo
    }

    /**
     * Full constructor with all properties
     */
    public CombatAchievement(int id, String name, String description, String tier, int points,
                             boolean completed, boolean tracked, String bossName, String type,
                             boolean soloOnly, Float completionPercentage, String wikiUrl)
    {
        this(id, name, "unknown", description, tier, points, completed, tracked);
        this.bossName = bossName;
        this.type = type;
        this.soloOnly = soloOnly;
        this.completionPercentage = completionPercentage;
        this.wikiUrl = wikiUrl;
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
     * Get the color associated with this tier
     */
    public java.awt.Color getTierColor()
    {
        return getTierLevel().getColor();
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
        EASY("Easy", new java.awt.Color(139, 102, 78), 1),          // Muted brown
        MEDIUM("Medium", new java.awt.Color(128, 128, 128), 2),     // Grey
        HARD("Hard", new java.awt.Color(64, 64, 64), 3),            // Very dark grey
        ELITE("Elite", new java.awt.Color(70, 100, 150), 4),        // Muted blue
        MASTER("Master", new java.awt.Color(120, 70, 70), 5),       // Muted red
        GRANDMASTER("Grandmaster", new java.awt.Color(170, 170, 170), 6); // White grey

        private final String displayName;
        private final java.awt.Color color;
        private final int basePoints;

        TierLevel(String displayName, java.awt.Color color, int basePoints)
        {
            this.displayName = displayName;
            this.color = color;
            this.basePoints = basePoints;
        }

        public String getDisplayName()
        {
            return displayName;
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