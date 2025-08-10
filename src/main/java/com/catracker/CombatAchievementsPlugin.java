package com.catracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Combat Achievements Tracker",
        description = "Track and manage combat achievements with filtering, sorting, and progress tracking",
        tags = {"combat", "achievements", "tracker", "progress"}
)
public class CombatAchievementsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private CombatAchievementsConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Getter
    private CombatAchievementsPanel panel;
    private NavigationButton navigationButton;

    private boolean needsDataLoad = false;
    private boolean dataLoadRequested = false;

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

    @Override
    protected void startUp() throws Exception
    {
        log.info("Combat Achievements Tracker starting up...");

        panel = new CombatAchievementsPanel(this);
        log.info("Panel created successfully");

        BufferedImage icon = null;
        try
        {
            icon = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "combat_achievements_icon.png");
        }
        catch (Exception e)
        {
            log.warn("Could not load icon, using text button instead", e);
        }

        if (icon != null)
        {
            navigationButton = NavigationButton.builder()
                    .tooltip("Combat Achievements Tracker")
                    .icon(icon)
                    .priority(6)
                    .panel(panel)
                    .build();
        }
        else
        {
            navigationButton = NavigationButton.builder()
                    .tooltip("Combat Achievements Tracker")
                    .priority(6)
                    .panel(panel)
                    .build();
        }
        clientToolbar.addNavigation(navigationButton);
        log.info("Combat Achievements Tracker started successfully!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Combat Achievements Tracker stopping...");

        if (panel != null) {
            try {
                log.info("Saved tracked achievements before shutdown");

                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Failed to save tracked achievements during shutdown", e);
            }
        }

        clientToolbar.removeNavigation(navigationButton);

        log.info("Combat Achievements Tracker stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        log.debug("Game state changed: {}", gameStateChanged.getGameState());

        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            needsDataLoad = true;
            log.info("Game logged in - will load Combat Achievements on next game tick");
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (needsDataLoad && !dataLoadRequested)
        {
            needsDataLoad = false;
            dataLoadRequested = true;
            log.info("Loading Combat Achievements on game tick...");
            loadCombatAchievementsFromClient();
        }
    }

    public CombatAchievementsConfig.TierGoal getTierGoal() {
        return config.tierGoal();
    }

    public void saveTrackedAchievements(List<CombatAchievement> trackedAchievements ) {
        try {
            log.info("saveTrackedAchievements called - current tracked list size: {}", trackedAchievements.size());

            List<Integer> trackedIds = trackedAchievements.stream()
                    .map(CombatAchievement::getId)
                    .collect(Collectors.toList());


            String trackedJson = new Gson().toJson(trackedIds);
            log.info("JSON to save (with timestamp): '{}'", trackedJson);

            try {
                if (configManager.getRSProfileKey() != null) {
                    configManager.setConfiguration(
                            CombatAchievementsConfig.CONFIG_GROUP_NAME,
                            "trackedAchievements",
                            trackedJson
                    );
                }
                log.info("Saved {} tracked achievements to config", trackedIds.size());
            } catch (Exception e) {
                log.error("Config save failed", e);
            }


        } catch (Exception e) {
            log.error("Failed to save tracked achievements", e);
        }
    }

    public void loadTrackedAchievements() {
        try {

            try {
                String v2Json = configManager.getConfiguration(
                        CombatAchievementsConfig.CONFIG_GROUP_NAME,
                        "trackedAchievements"
                );

                if (v2Json != null && !v2Json.isEmpty()) {
                    Type listType = new TypeToken<List<Integer>>(){}.getType();
                    List<Integer> configTrackedIds = new Gson().fromJson(v2Json, listType);
                    log.info(configTrackedIds.toString());
                }
            } catch (Exception e) {
                log.debug("V2 config not found or invalid: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to load tracked achievements", e);
        }
    }

    public void clearAllConfigData() {

            try {
                configManager.unsetConfiguration(
                        CombatAchievementsConfig.CONFIG_GROUP_NAME,
                        " trackedAchievements"
                );
            } catch (Exception e) {
                log.debug("Failed to clear");
            }
    }

    /**
     * Load combat achievements from client data (called from GameTick - client thread)
     */
    private void loadCombatAchievementsFromClient()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            log.warn("Client not ready for loading combat achievements - GameState: {}, LocalPlayer: {}",
                    client != null ? client.getGameState() : "null",
                    client != null && client.getLocalPlayer() != null ? "present" : "null");
            return;
        }

        try
        {
            log.info("Loading Combat Achievements from client data...");
            List<CombatAchievement> achievements = new ArrayList<>();
            int totalLoaded = 0;

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
                            id, name, type, description, tierName, points, completed, false
                    );

                    achievements.add(achievement);
                    totalLoaded++;
                }
            }

            SwingUtilities.invokeLater(() -> {
                if (panel != null)
                {
                    panel.updateAchievements(achievements);
                }
            });

            // Reset the data load flags
            dataLoadRequested = false;
        }
        catch (Exception e)
        {
            log.error("Failed to load Combat Achievements from client", e);
            // Reset flags even on error
            dataLoadRequested = false;
        }
    }

    private int getPointsForTier(String tier)
    {
        switch (tier.toLowerCase())
        {
            case "easy": return 1;
            case "medium": return 2;
            case "hard": return 3;
            case "elite": return 4;
            case "master": return 5;
            case "grandmaster": return 6;
            default: return 1;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE ||
                chatMessage.getType() == ChatMessageType.SPAM)
        {
            String message = chatMessage.getMessage();

            if (message.contains("Congratulations, you've completed") &&
                    message.contains("combat task"))
            {
                handleCombatAchievementCompletion(message);
            }
        }
    }

    private void handleCombatAchievementCompletion(String message)
    {
        log.info("Combat achievement completed: {}", message);

        SwingUtilities.invokeLater(() -> {
            if (panel != null)
            {
                panel.onAchievementCompleted(message);
            }
        });

        if (config.showProgressNotifications())
        {
            // TODO: Show notification with progress to next tier
        }
    }

    @Provides
    CombatAchievementsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CombatAchievementsConfig.class);
    }

    /**
     * Trigger a refresh of combat achievements data (for manual refresh button)
     */
    public void refreshCombatAchievements()
    {
        if (client != null && client.getGameState() == GameState.LOGGED_IN)
        {
            needsDataLoad = true;
            dataLoadRequested = false;
            log.info("Manual refresh requested - will load on next game tick");
        }
        else
        {
            log.warn("Cannot refresh - not logged in");
        }
    }
}