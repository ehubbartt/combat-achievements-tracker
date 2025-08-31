package com.catracker.util;

import com.catracker.model.CombatAchievement;
import com.catracker.ui.CombatAchievementsPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class CombatAchievementsDataLoader
{

	private final Client client;
	private final ClientThread clientThread;

	private boolean needsDataLoad = false;
	private boolean dataLoadRequested = false;
	private Consumer<List<CombatAchievement>> onDataLoadComplete;

	private static final Map<Integer, String> TIER_MAP = Map.of(
		3981, "Easy",
		3982, "Medium",
		3983, "Hard",
		3984, "Elite",
		3985, "Master",
		3986, "Grandmaster"
	);

	private static final Map<Integer, String> TYPE_MAP = Map.of(
		1, "Stamina",
		2, "Perfection",
		3, "Kill Count",
		4, "Mechanical",
		5, "Restriction",
		6, "Speed"
	);

	private static final int[] VARP_IDS = new int[]{
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1,
		VarPlayerID.CA_TASK_COMPLETED_2, VarPlayerID.CA_TASK_COMPLETED_3,
		VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7,
		VarPlayerID.CA_TASK_COMPLETED_8, VarPlayerID.CA_TASK_COMPLETED_9,
		VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13,
		VarPlayerID.CA_TASK_COMPLETED_14, VarPlayerID.CA_TASK_COMPLETED_15,
		VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19
	};

	public CombatAchievementsDataLoader(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	public void requestDataLoad()
	{
		needsDataLoad = true;
	}

	public void requestManualRefresh()
	{
		needsDataLoad = true;
		dataLoadRequested = false;
	}

	public void requestManualRefresh(Consumer<List<CombatAchievement>> callback)
	{
		this.onDataLoadComplete = callback;
		requestManualRefresh();
	}

	public void handleGameTick(CombatAchievementsPanel panel)
	{
		if (needsDataLoad && !dataLoadRequested)
		{
			needsDataLoad = false;
			dataLoadRequested = true;
			log.info("Loading Combat Achievements on game tick...");
			loadCombatAchievementsFromClient(panel);
		}
	}

	private void loadCombatAchievementsFromClient(CombatAchievementsPanel panel)
	{
		if (client == null || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			log.warn("Client not ready for loading combat achievements");
			return;
		}

		clientThread.invokeLater(() ->
		{
			try
			{
				log.info("Loading Combat Achievements from client data...");
				List<CombatAchievement> achievements = new ArrayList<>();

				for (Map.Entry<Integer, String> tierEntry : TIER_MAP.entrySet())
				{
					int enumId = tierEntry.getKey();
					String tierName = tierEntry.getValue();

					var enumComp = client.getEnum(enumId);
					if (enumComp == null)
					{
						log.warn("Could not find enum for tier: {} ({})", tierName, enumId);
						continue;
					}

					int[] structIds = enumComp.getIntVals();

					for (int structId : structIds)
					{
						var struct = client.getStructComposition(structId);
						if (struct == null)
						{
							log.warn("Could not find struct: {}", structId);
							continue;
						}

						String name = struct.getStringValue(1308);
						String description = struct.getStringValue(1309);
						int id = struct.getIntValue(1306);
						int typeId = struct.getIntValue(1311);
						String type = TYPE_MAP.get(typeId);
						int bossId = struct.getIntValue(1312);
						String bossName = getBossName(bossId);

						boolean completed = false;
						if (id >= 0 && id < VARP_IDS.length * 32)
						{
							int varpIndex = id / 32;
							int bitIndex = id % 32;
							if (varpIndex < VARP_IDS.length)
							{
								int varpValue = client.getVarpValue(VARP_IDS[varpIndex]);
								completed = (varpValue & (1 << bitIndex)) != 0;
							}
						}

						int points = getPointsForTier(tierName);
						CombatAchievement achievement = new CombatAchievement(
							id, name, bossName, type, description, tierName, points, completed, false
						);

						achievements.add(achievement);
					}
				}

				final List<CombatAchievement> finalAchievements = achievements;

				SwingUtilities.invokeLater(() ->
				{
					if (panel != null)
					{
						panel.updateAchievements(finalAchievements);
					}

					// Call completion callback if set
					if (onDataLoadComplete != null)
					{
						onDataLoadComplete.accept(finalAchievements);
						onDataLoadComplete = null; // Clear callback after use
					}
				});

				dataLoadRequested = false;
			}
			catch (Exception e)
			{
				log.error("Failed to load Combat Achievements from client", e);
				dataLoadRequested = false;
				onDataLoadComplete = null; // Clear callback on error
			}
		});
	}

	private String getBossName(int bossId)
	{
		try
		{
			var bossEnum = client.getEnum(3971);
			if (bossEnum != null)
			{
				return bossEnum.getStringValue(bossId);
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to get boss name for ID {}: {}", bossId, e.getMessage());
		}
		return "Unknown";
	}

	private int getPointsForTier(String tier)
	{
		switch (tier.toLowerCase())
		{
			case "easy":
				return 1;
			case "medium":
				return 2;
			case "hard":
				return 3;
			case "elite":
				return 4;
			case "master":
				return 5;
			case "grandmaster":
				return 6;
			default:
				return 1;
		}
	}
}