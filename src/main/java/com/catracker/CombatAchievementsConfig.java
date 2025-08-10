package com.catracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("combatachievements")
public interface CombatAchievementsConfig extends Config
{
    String CONFIG_GROUP_NAME = "combatachievements";

    @ConfigSection(
            name = "Display Settings",
            description = "Configure how achievements are displayed",
            position = 0
    )
    String displaySection = "display";

    @ConfigSection(
            name = "Notifications",
            description = "Configure completion notifications",
            position = 1
    )
    String notificationSection = "notifications";

    @ConfigSection(
            name = "Data Management",
            description = "Import/Export settings",
            position = 2
    )
    String dataSection = "data";

    // Display Settings
    @ConfigItem(
            keyName = "showCompletedAchievements",
            name = "Show Completed Achievements",
            description = "Whether to show completed achievements in the list",
            section = displaySection,
            position = 0
    )
    default boolean showCompletedAchievements()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showBorderColors",
            name = "Show Tier Border Colors",
            description = "Show colored borders based on achievement tier",
            section = displaySection,
            position = 1
    )
    default boolean showBorderColors()
    {
        return true;
    }

    @ConfigItem(
            keyName = "tierGoal",
            name = "Set Your Current Goal",
            description = "Set a CA tier goal",
            section = displaySection,
            position = 2
    )

    default TierGoal tierGoal()
    {
        return TierGoal.TIER_GRANDMASTER;
    }

    // Notifications
    @ConfigItem(
            keyName = "showProgressNotifications",
            name = "Show Progress Notifications",
            description = "Show notifications when completing achievements",
            section = notificationSection,
            position = 0
    )
    default boolean showProgressNotifications()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showChatMessages",
            name = "Show Chat Messages",
            description = "Show progress messages in chat",
            section = notificationSection,
            position = 1
    )
    default boolean showChatMessages()
    {
        return true;
    }

    @ConfigItem(
            keyName = "currentPointsGoal",
            name = "Current Points Goal",
            description = "Your target points for the next tier (e.g., 50, 100, 250, etc.)",
            section = notificationSection,
            position = 2
    )
    default int currentPointsGoal()
    {
        return 50; // Default to Easy tier
    }

    // Data Management
    @ConfigItem(
            keyName = "autoSaveEnabled",
            name = "Auto-save Tracked Achievements",
            description = "Automatically save your tracked achievements list",
            section = dataSection,
            position = 0
    )
    default boolean autoSaveEnabled()
    {
        return true;
    }

    @ConfigGroup("combatachievements")

    enum TierGoal
    {
        TIER_EASY("Easy"),
        TIER_MEDIUM("Medium"),
        TIER_HARD("Hard"),
        TIER_ELITE("Elite"),
        TIER_MASTER("Master"),
        TIER_GRANDMASTER("Grandmaster");


        private final String displayName;

        TierGoal(String displayName)
        {
            this.displayName = displayName;
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }
}