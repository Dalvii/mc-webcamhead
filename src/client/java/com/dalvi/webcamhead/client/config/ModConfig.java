package com.dalvi.webcamhead.client.config;

public class ModConfig {
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
    }

    public static String getRoomId() {
        return roomId;
    }

    public static void setRoomId(String id) {
        roomId = id;
    }

    public static boolean isMultiplayerEnabled() {
        return multiplayerEnabled;
    }

    public static void setMultiplayerEnabled(boolean enabled) {
        multiplayerEnabled = enabled;
    }

    /**
     * Check if the signaling server is configured
     */
    public static boolean isServerConfigured() {
        return signalingServerUrl != null && !signalingServerUrl.isEmpty();
    }
}
