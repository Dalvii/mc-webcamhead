package com.dalvi.webcamhead.client.video;

import net.minecraft.util.Identifier;

public class PlayerVideoState {
    private final Identifier textureId;
    private final int width;
    private final int height;
    private boolean isActive;

    public PlayerVideoState(Identifier textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.isActive = true;
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}
