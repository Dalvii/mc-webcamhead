package com.dalvi.webcamhead.client.webcam;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

public class WebcamTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static final Identifier WEBCAM_TEXTURE_ID = Identifier.of("webcamhead", "webcam_feed");

    private NativeImageBackedTexture texture;
    private NativeImage nativeImage;
    private final int width;
    private final int height;

    public WebcamTextureManager(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void initialize() {
        if (nativeImage != null) {
            return;
        }

        nativeImage = new NativeImage(width, height, true);
        texture = new NativeImageBackedTexture(nativeImage);
        MinecraftClient.getInstance().getTextureManager().registerTexture(WEBCAM_TEXTURE_ID, texture);

        LOGGER.info("Initialized webcam texture ({}x{})", width, height);
    }

    public void updateTexture(BufferedImage frame) {
        if (frame == null || nativeImage == null) {
            return;
        }

        try {
            // Convert BufferedImage to NativeImage
            // NativeImage stores pixels in ABGR format
            for (int y = 0; y < Math.min(height, frame.getHeight()); y++) {
                for (int x = 0; x < Math.min(width, frame.getWidth()); x++) {
                    int argb = frame.getRGB(x, y);

                    // Extract ARGB components
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Ensure fully opaque
                    if (a == 0) a = 255;

                    // Convert to ABGR format that NativeImage uses
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;

                    // Use fillRect to set a single pixel (it's the only public method available)
                    nativeImage.fillRect(x, y, 1, 1, abgr);
                }
            }

            // Upload to GPU
            texture.upload();
        } catch (Exception e) {
            LOGGER.error("Error updating webcam texture", e);
        }
    }

    public Identifier getTextureId() {
        return WEBCAM_TEXTURE_ID;
    }

    public void cleanup() {
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(WEBCAM_TEXTURE_ID);
            texture = null;
        }
        if (nativeImage != null) {
            nativeImage.close();
            nativeImage = null;
        }
        LOGGER.info("Cleaned up webcam texture");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
