package com.catracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Test runner for the Combat Achievements plugin
 * This allows running RuneLite with the plugin loaded directly
 */
public class CombatAchievementsPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(CombatAchievementsPlugin.class);

        RuneLite.main(args);
    }
}