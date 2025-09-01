/*
 * Copyright (c) 2025, Ethan Hubbartt <ehubbartt@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
		BufferedImage originalIcon = ImageUtil.loadImageResource(
			com.catracker.CombatAchievementsPlugin.class,
			iconName
		);
		BufferedImage resizedIcon = ImageUtil.resizeImage(originalIcon, width, height);
		return new ImageIcon(resizedIcon);
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

	/**
	 * Load arrow right icon (rotated for up/down arrows)
	 */
	public static ImageIcon loadArrowRight()
	{
		return loadIcon("arrow_right.png", 16, 16);
	}

	/**
	 * Load sort up icon (24x24)
	 */
	public static ImageIcon loadSortUpIcon()
	{
		return loadIcon("sort_up.png", 16, 16);
	}

	/**
	 * Load sort down icon (24x24)
	 */
	public static ImageIcon loadSortDownIcon()
	{
		return loadIcon("sort_down.png", 16, 16);
	}
}