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
            name = "List Settings",
            description = "Configure how achievements are displayed",
            position = 0
    )
    String listSection = "list";

    @ConfigSection(
            name = "Notifications",
            description = "Configure completion notifications",
            position = 1
    )
    String notificationSection = "notifications";

    @ConfigItem(
            keyName = "tierGoal",
            name = "Set Your Current Goal",
            description = "Set a CA tier goal",
            section = listSection,
            position = 2
    )

    default TierGoal tierGoal()
    {
        return TierGoal.TIER_AUTO;
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


    @ConfigGroup("combatachievements")
    enum TierGoal
    {
        TIER_AUTO("Auto"),
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