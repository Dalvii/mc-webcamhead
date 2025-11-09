package com.dalvi.webcamhead.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/webcamhead.json";

    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 240;
    public static final int DEFAULT_FPS = 15;
    public static final int DEFAULT_DEVICE_INDEX = 0;
    public static final String DEFAULT_ROOM_ID = "default";

    private static int captureWidth = DEFAULT_WIDTH;
    private static int captureHeight = DEFAULT_HEIGHT;
    private static int captureFps = DEFAULT_FPS;
    private static int deviceIndex = DEFAULT_DEVICE_INDEX;
    private static RenderMode renderMode = RenderMode.PANEL_3D;
    private static String signalingServerUrl = null; // Not configured by default - user must set it
    private static String roomId = DEFAULT_ROOM_ID;
    private static boolean multiplayerEnabled = true;

    // Configuration data class for JSON serialization
    private static class ConfigData {
        int captureWidth = DEFAULT_WIDTH;
        int captureHeight = DEFAULT_HEIGHT;
        int captureFps = DEFAULT_FPS;
        int deviceIndex = DEFAULT_DEVICE_INDEX;
        String renderMode = RenderMode.PANEL_3D.name();
        String signalingServerUrl = null;
        String roomId = DEFAULT_ROOM_ID;
        boolean multiplayerEnabled = true;
    }

    public enum RenderMode {
        PANEL_3D,
        SKIN_OVERLAY,
        BOTH
    }

    public static int getCaptureWidth() {
        return captureWidth;
    }

    public static void setCaptureWidth(int width) {
        captureWidth = width;
    }

    public static int getCaptureHeight() {
        return captureHeight;
    }

    public static void setCaptureHeight(int height) {
        captureHeight = height;
    }

    public static int getCaptureFps() {
        return captureFps;
    }

    public static void setCaptureFps(int fps) {
        captureFps = fps;
    }

    public static RenderMode getRenderMode() {
        return renderMode;
    }

    public static void setRenderMode(RenderMode mode) {
        renderMode = mode;
    }

    public static int getDeviceIndex() {
        return deviceIndex;
    }

    public static void setDeviceIndex(int index) {
        deviceIndex = index;
    }

    public static String getSignalingServerUrl() {
        return signalingServerUrl;
    }

    public static void setSignalingServerUrl(String url) {
        signalingServerUrl = url;
        save();
    }

    public static String getRoomId() {
        return roomId;
    }

    public static void setRoomId(String id) {
        roomId = id;
        save();
    }

    public static boolean isMultiplayerEnabled() {
        return multiplayerEnabled;
    }

    public static void setMultiplayerEnabled(boolean enabled) {
        multiplayerEnabled = enabled;
        save();
    }

    /**
     * Check if the signaling server is configured
     */
    public static boolean isServerConfigured() {
        return signalingServerUrl != null && !signalingServerUrl.isEmpty();
    }

    /**
     * Load configuration from file
     */
    public static void load() {
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            LOGGER.info("Config file not found, using defaults");
            save(); // Create default config file
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);

            if (data != null) {
                captureWidth = data.captureWidth;
                captureHeight = data.captureHeight;
                captureFps = data.captureFps;
                deviceIndex = data.deviceIndex;
                signalingServerUrl = data.signalingServerUrl;
                roomId = data.roomId;
                multiplayerEnabled = data.multiplayerEnabled;

                try {
                    renderMode = RenderMode.valueOf(data.renderMode);
                } catch (IllegalArgumentException e) {
                    renderMode = RenderMode.PANEL_3D;
                }

                LOGGER.info("Loaded config from {}", CONFIG_FILE);
                LOGGER.info("Server URL: {}", signalingServerUrl != null ? signalingServerUrl : "[Not configured]");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config file", e);
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        File configFile = new File(CONFIG_FILE);
        File configDir = configFile.getParentFile();

        // Create config directory if it doesn't exist
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        ConfigData data = new ConfigData();
        data.captureWidth = captureWidth;
        data.captureHeight = captureHeight;
        data.captureFps = captureFps;
        data.deviceIndex = deviceIndex;
        data.renderMode = renderMode.name();
        data.signalingServerUrl = signalingServerUrl;
        data.roomId = roomId;
        data.multiplayerEnabled = multiplayerEnabled;

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
            LOGGER.debug("Saved config to {}", CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
}
