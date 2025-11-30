package com.catracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Test runner for the Combat Achievements plugin
 * This allows running RuneLite with the plugin loaded directly
 */
public class CombatAchievementsPluginTest {
	public static void main(String[] args) throws Exception {
		// Enable assertions (equivalent to -ea VM argument)
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

		ExternalPluginManager.loadBuiltin(CombatAchievementsPlugin.class);

		// Add developer mode and debug arguments
		String[] newArgs = new String[args.length + 2];
		System.arraycopy(args, 0, newArgs, 0, args.length);
		newArgs[args.length] = "--developer-mode";
		newArgs[args.length + 1] = "--debug";

		RuneLite.main(newArgs);
	}
}