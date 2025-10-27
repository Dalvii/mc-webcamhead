package com.dalvi.webcamhead.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders webcam feed by modifying the player's skin texture directly.
 * This replaces the face region of the skin with the webcam video.
 */
public class SkinOverlayRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");

    // High resolution skin (128x128 texture instead of standard 64x64)
    // This gives us 2x resolution for the face
    private static final int SKIN_RESOLUTION = 128;

    // Front face coordinates (scaled 2x from standard)
    // Standard face is at (8,8) with size 8x8, so 2x is (16,16) with size 16x16
    private static final int FACE_X = 16;
    private static final int FACE_Y = 16;
    private static final int FACE_WIDTH = 16;
    private static final int FACE_HEIGHT = 16;

    // Overlay face (second layer): scaled 2x from (40,8)
    private static final int OVERLAY_FACE_X = 80;
    private static final int OVERLAY_FACE_Y = 16;

    // Cache of modified skin textures
    private static final Map<UUID, ModifiedSkinData> modifiedSkins = new HashMap<>();

    private static class ModifiedSkinData {
        Identifier textureId;
        NativeImage originalSkin;
        NativeImageBackedTexture modifiedTexture;
        Identifier originalTextureId;
    }

    /**
     * Initialize modified skin texture for a player
     */
    public static void initializeModifiedSkin(AbstractClientPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (modifiedSkins.containsKey(playerId)) {
            return; // Already initialized
        }

        try {
            // Get original skin texture ID
            Identifier originalSkinId = player.getSkinTextures().texture();

            // Load the original skin texture
            MinecraftClient client = MinecraftClient.getInstance();
            NativeImage originalSkin = loadSkinTexture(client, originalSkinId);

            if (originalSkin == null) {
                LOGGER.error("Failed to load original skin texture for player {}", player.getName().getString());
                return;
            }

            // Create a high-resolution skin (128x128) by upscaling the original
            NativeImage modifiedSkin = new NativeImage(SKIN_RESOLUTION, SKIN_RESOLUTION, true);

            // Upscale the original skin to 2x resolution by copying each pixel as a 2x2 block
            // Since NativeImage doesn't have public getPixel methods, we'll use fillRect
            // to write and rely on the existing pixel reading in the update method
            int originalWidth = originalSkin.getWidth();
            int originalHeight = originalSkin.getHeight();

            // First, copy the original at 2x scale using copyRect for each row/column
            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {
                    // Copy a 1x1 rect from original to a 2x2 location
                    // We'll need to do this pixel by pixel using a workaround
                    // Copy the 1x1 pixel 4 times (2x2 block)
                    originalSkin.copyRect(modifiedSkin, x, y, x * 2, y * 2, 1, 1, false, false);
                    originalSkin.copyRect(modifiedSkin, x, y, x * 2 + 1, y * 2, 1, 1, false, false);
                    originalSkin.copyRect(modifiedSkin, x, y, x * 2, y * 2 + 1, 1, 1, false, false);
                    originalSkin.copyRect(modifiedSkin, x, y, x * 2 + 1, y * 2 + 1, 1, 1, false, false);
                }
            }

            // Create texture from modified image
            NativeImageBackedTexture texture = new NativeImageBackedTexture(modifiedSkin);
            Identifier modifiedTextureId = Identifier.of("webcamhead", "modified_skin_" + playerId.toString());

            client.getTextureManager().registerTexture(modifiedTextureId, texture);

            // Store data
            ModifiedSkinData data = new ModifiedSkinData();
            data.textureId = modifiedTextureId;
            data.originalSkin = originalSkin;
            data.modifiedTexture = texture;
            data.originalTextureId = originalSkinId;

            modifiedSkins.put(playerId, data);

            LOGGER.info("Initialized high-res ({}x{}) modified skin for player {}",
                SKIN_RESOLUTION, SKIN_RESOLUTION, player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error initializing modified skin", e);
        }
    }

    /**
     * Update the face region of the modified skin with webcam frame
     */
    public static void updateSkinWithWebcam(UUID playerId, BufferedImage webcamFrame) {
        ModifiedSkinData data = modifiedSkins.get(playerId);
        if (data == null) {
            return;
        }

        if (webcamFrame == null) {
            return;
        }

        try {
            // Resize webcam frame to face size (16x16 for high-res skin)
            BufferedImage resizedFrame = resizeForSkin(webcamFrame, FACE_WIDTH, FACE_HEIGHT);

            // Get the modified skin's NativeImage
            NativeImage skinImage = data.modifiedTexture.getImage();
            if (skinImage == null) {
                return;
            }

            // Copy webcam frame to face regions (front face and overlay)
            copyToSkinFace(skinImage, resizedFrame, FACE_X, FACE_Y);
            copyToSkinFace(skinImage, resizedFrame, OVERLAY_FACE_X, OVERLAY_FACE_Y);

            // Upload the modified texture to GPU
            data.modifiedTexture.upload();

        } catch (Exception e) {
            LOGGER.error("Error updating skin with webcam", e);
        }
    }

    /**
     * Copy webcam frame to a specific region of the skin
     */
    private static void copyToSkinFace(NativeImage skinImage, BufferedImage webcamFrame, int startX, int startY) {
        int width = Math.min(FACE_WIDTH, webcamFrame.getWidth());
        int height = Math.min(FACE_HEIGHT, webcamFrame.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = webcamFrame.getRGB(x, y);

                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (a == 0) a = 255;

                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                skinImage.fillRect(startX + x, startY + y, 1, 1, abgr);
            }
        }
    }

    /**
     * Get the modified skin texture ID for a player
     */
    public static Identifier getModifiedSkinTexture(UUID playerId) {
        ModifiedSkinData data = modifiedSkins.get(playerId);
        return data != null ? data.textureId : null;
    }

    /**
     * Check if a player has a modified skin
     */
    public static boolean hasModifiedSkin(UUID playerId) {
        return modifiedSkins.containsKey(playerId);
    }

    /**
     * Clean up modified skin for a player
     */
    public static void cleanupModifiedSkin(UUID playerId) {
        ModifiedSkinData data = modifiedSkins.remove(playerId);
        if (data != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(data.textureId);
            if (data.originalSkin != null) {
                data.originalSkin.close();
            }
            LOGGER.info("Cleaned up modified skin for player {}", playerId);
        }
    }

    /**
     * Resize and prepare webcam frame for skin overlay
     */
    private static BufferedImage resizeForSkin(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /**
     * Load skin texture from Minecraft's texture manager
     */
    private static NativeImage loadSkinTexture(MinecraftClient client, Identifier skinId) {
        try {
            // Try to get the texture from the resource manager
            InputStream stream = client.getResourceManager().getResource(skinId)
                .orElseThrow()
                .getInputStream();

            return NativeImage.read(stream);
        } catch (Exception e) {
            LOGGER.debug("Could not load skin from resources, trying default: {}", e.getMessage());

            // If that fails, create a default 64x64 skin
            return new NativeImage(64, 64, true);
        }
    }
}
