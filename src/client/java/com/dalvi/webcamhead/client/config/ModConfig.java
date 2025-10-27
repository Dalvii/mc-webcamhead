package com.dalvi.webcamhead.client.config;

public class ModConfig {
    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 240;
    public static final int DEFAULT_FPS = 15;
    public static final int DEFAULT_DEVICE_INDEX = 0;

    private static int captureWidth = DEFAULT_WIDTH;
    private static int captureHeight = DEFAULT_HEIGHT;
    private static int captureFps = DEFAULT_FPS;
    private static int deviceIndex = DEFAULT_DEVICE_INDEX;
    private static RenderMode renderMode = RenderMode.PANEL_3D;

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
}
