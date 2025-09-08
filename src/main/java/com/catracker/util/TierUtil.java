package com.catracker.util;

import com.catracker.config.CombatAchievementsConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TierUtil
{
	private static final Map<Integer, String> TIER_VARBIT_MAP = Map.of(
		4132, "Easy",
		10660, "Medium",
		10661, "Hard",
		14812, "Elite",
		14813, "Master",
		14814, "Grandmaster"
	);

	private static Map<String, Integer> cachedThresholds = null;

	public static void initializeTierThresholds(Client client)
	{
		Map<String, Integer> thresholds = new HashMap<>();

		for (Map.Entry<Integer, String> entry : TIER_VARBIT_MAP.entrySet())
		{
			int varbitId = entry.getKey();
			String tierName = entry.getValue();

			try
			{
				int value = client.getVarbitValue(varbitId);
				if (value > 0)
				{
					thresholds.put(tierName, value);
					log.debug("Loaded {} tier threshold: {} from varbit {}", tierName, value, varbitId);
				}
				else
				{
					log.warn("Varbit {} ({}) returned 0", varbitId, tierName);
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to read varbit {} for {} tier: {}", varbitId, tierName, e.getMessage());
			}
		}

		cachedThresholds = thresholds;
		log.debug("Initialized tier thresholds: {}", cachedThresholds);
	}

	public static int getPointsFromGoal(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints)
	{
		if (cachedThresholds == null)
		{
			log.warn("Tier thresholds not initialized - call initializeTierThresholds first");
			return 0;
		}

		if (tierGoal.toString().equalsIgnoreCase("AUTO"))
		{
			String[] tierOrder = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};

			for (String tier : tierOrder)
			{
				int threshold = cachedThresholds.getOrDefault(tier, 0);
				if (threshold > 0 && completedPoints < threshold)
				{
					return threshold;
				}
			}

			return cachedThresholds.getOrDefault("Grandmaster", 0);
		}

		String tierName = tierGoal.toString().replace("TIER_", "");
		tierName = tierName.substring(0, 1).toUpperCase() + tierName.substring(1).toLowerCase();

		return cachedThresholds.getOrDefault(tierName, 0);
	}

	public static String getActualTierName(CombatAchievementsConfig.TierGoal tierGoal, int completedPoints)
	{
		if (cachedThresholds == null)
		{
			log.warn("Tier thresholds not initialized - call initializeTierThresholds first");
			return "Unknown";
		}

		if (tierGoal.toString().equalsIgnoreCase("AUTO"))
		{
			String[] tierOrder = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};

			for (String tier : tierOrder)
			{
				int threshold = cachedThresholds.getOrDefault(tier, 0);
				if (threshold > 0 && completedPoints < threshold)
				{
					return tier;
				}
			}

			return "Grandmaster";
		}

		String tierName = tierGoal.toString().replace("TIER_", "");
		return tierName.substring(0, 1).toUpperCase() + tierName.substring(1).toLowerCase();
	}

	public static Map<String, Integer> getCachedThresholds()
	{
		return cachedThresholds != null ? new HashMap<>(cachedThresholds) : new HashMap<>();
	}
}