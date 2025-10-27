package com.dalvi.webcamhead.client.video;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VideoStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static VideoStateManager instance;

    private final Map<UUID, PlayerVideoState> playerVideos = new HashMap<>();

    private VideoStateManager() {}

    public static VideoStateManager getInstance() {
        if (instance == null) {
            instance = new VideoStateManager();
        }
        return instance;
    }

    public void setPlayerVideo(UUID playerId, PlayerVideoState videoState) {
        playerVideos.put(playerId, videoState);
        LOGGER.debug("Set video state for player {}", playerId);
    }

    public PlayerVideoState getPlayerVideo(UUID playerId) {
        return playerVideos.get(playerId);
    }

    public PlayerVideoState getPlayerVideo(PlayerEntity player) {
        return playerVideos.get(player.getUuid());
    }

    public void removePlayerVideo(UUID playerId) {
        playerVideos.remove(playerId);
        LOGGER.debug("Removed video state for player {}", playerId);
    }

    public boolean hasVideo(UUID playerId) {
        return playerVideos.containsKey(playerId);
    }

    public boolean hasVideo(PlayerEntity player) {
        return hasVideo(player.getUuid());
    }

    public UUID getLocalPlayerUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            return client.player.getUuid();
        }
        return null;
    }

    public void clear() {
        playerVideos.clear();
        LOGGER.info("Cleared all video states");
    }
}
