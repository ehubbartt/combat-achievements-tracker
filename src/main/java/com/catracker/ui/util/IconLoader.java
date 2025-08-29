package com.catracker.ui.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * Centralized icon loading utility to handle resource loading consistently
 */
@Slf4j
public class IconLoader
{

	private static final String RESOURCE_PATH = "/";

	/**
	 * Load an icon from resources with specified dimensions
	 */
	public static ImageIcon loadIcon(String iconName, int width, int height)
	{
		try
		{
			// Use the plugin's main class for resource loading to ensure correct classpath
			BufferedImage originalIcon = ImageUtil.loadImageResource(
				com.catracker.CombatAchievementsPlugin.class,
				iconName
			);
			BufferedImage resizedIcon = ImageUtil.resizeImage(originalIcon, width, height);
			return new ImageIcon(resizedIcon);
		}
		catch (Exception e)
		{
			log.warn("Could not load icon '{}': {}", iconName, e.getMessage());
			return null;
		}
	}

	/**
	 * Load a tier icon (16x16)
	 */
	public static ImageIcon loadTierIcon(String tier)
	{
		String iconPath = tier.toLowerCase() + "_tier.png";
		return loadIcon(iconPath, 16, 16);
	}

	/**
	 * Load a tier button icon (20x20)
	 */
	public static ImageIcon loadTierButtonIcon(String tier)
	{
		String iconPath = tier.toLowerCase() + "_tier.png";
		return loadIcon(iconPath, 20, 20);
	}

	/**
	 * Load track add icon (12x12)
	 */
	public static ImageIcon loadTrackAddIcon()
	{
		return loadIcon("track_add.png", 12, 12);
	}

	/**
	 * Load track remove icon (12x12)
	 */
	public static ImageIcon loadTrackRemoveIcon()
	{
		return loadIcon("track_remove.png", 12, 12);
	}
}