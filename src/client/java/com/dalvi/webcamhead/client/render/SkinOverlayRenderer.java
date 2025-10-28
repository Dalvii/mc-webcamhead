package com.dalvi.webcamhead.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    // High resolution skin to support 128x128 webcam on the face
    // Head face is 8x8 in standard 64x64 skin
    // To get 128x128 for the face, we need 128/8 = 16x scale, so 64 * 16 = 1024x1024 skin
    private static final int SKIN_RESOLUTION = 1024;
    private static final int SCALE = SKIN_RESOLUTION / 64; // 16x scale

    // Standard Minecraft skin face coordinates (scaled 16x)
    // - Front face: (8, 8) to (16, 16) in standard skin
    // - Overlay face (second layer): (40, 8) to (48, 16) in standard skin

    private static final int FACE_X = 8 * SCALE;      // 128
    private static final int FACE_Y = 8 * SCALE;      // 128
    private static final int FACE_WIDTH = 8 * SCALE;  // 128
    private static final int FACE_HEIGHT = 8 * SCALE; // 128

    // Overlay face (second layer)
    private static final int OVERLAY_FACE_X = 40 * SCALE; // 640
    private static final int OVERLAY_FACE_Y = 8 * SCALE;  // 128

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
        String playerName = player.getName().getString();

        if (modifiedSkins.containsKey(playerId)) {
            return; // Already initialized
        }

        try {
            // Get original skin texture ID
            Identifier originalSkinId = player.getSkinTextures().texture();
            MinecraftClient client = MinecraftClient.getInstance();

            // Check if the skin texture is loaded yet
            var textureManager = client.getTextureManager();
            var existingTexture = textureManager.getOrDefault(originalSkinId, null);

            if (existingTexture == null) {
                LOGGER.debug("Skin texture not loaded yet for player {}, will retry later", player.getName().getString());
                return; // Texture not loaded yet, will try again next frame
            }

            // Load the original skin texture
            NativeImage originalSkin = loadSkinTexture(client, originalSkinId, playerId, playerName);

            if (originalSkin == null) {
                LOGGER.error("Failed to load original skin texture for player {}", player.getName().getString());
                return;
            }

            LOGGER.info("Loaded original skin: {}x{} for player {}",
                originalSkin.getWidth(), originalSkin.getHeight(), player.getName().getString());

            // Convert NativeImage to BufferedImage for easier manipulation
            BufferedImage originalBuffered = nativeImageToBufferedImage(originalSkin);

            if (originalBuffered == null) {
                LOGGER.error("Failed to convert NativeImage to BufferedImage for player {}", player.getName().getString());
                return;
            }

            LOGGER.info("Converted to BufferedImage: {}x{}",
                originalBuffered.getWidth(), originalBuffered.getHeight());

            // Upscale the skin from 64x64 to 1024x1024 using Java AWT
            BufferedImage upscaledBuffered = resizeForSkin(originalBuffered, SKIN_RESOLUTION, SKIN_RESOLUTION);

            LOGGER.info("Upscaled to: {}x{}",
                upscaledBuffered.getWidth(), upscaledBuffered.getHeight());

            // Convert back to NativeImage
            NativeImage modifiedSkin = bufferedImageToNativeImage(upscaledBuffered);

            LOGGER.info("Successfully created modified skin ({}x{}) for player {}",
                SKIN_RESOLUTION, SKIN_RESOLUTION,
                player.getName().getString());

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
                int argb = webcamFrame.getRGB(x, y);

                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                if (a == 0) a = 255;

                // Try ARGB format instead (keep R and B in original positions)
                int color = (a << 24) | (r << 16) | (g << 8) | b;
                skinImage.fillRect(startX + x, startY + y, 1, 1, color);
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
     * Uses nearest neighbor for skin upscaling to preserve pixelated look
     */
    private static BufferedImage resizeForSkin(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        // Use nearest neighbor for skin upscaling to keep pixel art sharp
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    /**
     * Convert NativeImage to BufferedImage
     * Since we can't directly access NativeImage pixels, we write to a temporary file and read back
     */
    private static BufferedImage nativeImageToBufferedImage(NativeImage nativeImage) {
        try {
            // Create a temporary file
            java.io.File tempFile = java.io.File.createTempFile("skin_", ".png");
            tempFile.deleteOnExit();

            // Write NativeImage to PNG file
            nativeImage.writeTo(tempFile.toPath());

            // Read back as BufferedImage
            BufferedImage buffered = javax.imageio.ImageIO.read(tempFile);

            // Delete temp file
            tempFile.delete();

            return buffered;
        } catch (Exception e) {
            LOGGER.error("Error converting NativeImage to BufferedImage", e);
            // Return empty image as fallback
            return new BufferedImage(nativeImage.getWidth(), nativeImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
    }

    /**
     * Convert BufferedImage to NativeImage
     */
    private static NativeImage bufferedImageToNativeImage(BufferedImage buffered) {
        int width = buffered.getWidth();
        int height = buffered.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = buffered.getRGB(x, y);

                // BufferedImage.getRGB returns ARGB
                // NativeImage.fillRect expects ARGB
                // Just copy the color as-is, preserving transparency
                nativeImage.fillRect(x, y, 1, 1, argb);
            }
        }

        return nativeImage;
    }

    /**
     * Load skin texture from Minecraft's texture manager
     */
    private static NativeImage loadSkinTexture(MinecraftClient client, Identifier skinId, UUID playerId, String playerName) {
        try {
            // First, try to get the texture from the resource manager (for default skins)
            try {
                InputStream stream = client.getResourceManager().getResource(skinId)
                    .orElseThrow()
                    .getInputStream();
                NativeImage img = NativeImage.read(stream);
                LOGGER.info("Loaded skin from resources: {}x{}", img.getWidth(), img.getHeight());
                return img;
            } catch (Exception e) {
                LOGGER.debug("Could not load skin from resources: {}", e.getMessage());
            }

            // For player skins (downloaded from Mojang), we need to get the texture from the texture manager
            // The texture is already loaded by Minecraft, we just need to read it
            var textureManager = client.getTextureManager();
            var texture = textureManager.getOrDefault(skinId, null);

            if (texture == null) {
                LOGGER.warn("Texture not found in TextureManager for ID: {}", skinId);
                return createDefaultSkin();
            }

            LOGGER.info("Found texture of type: {}", texture.getClass().getName());

            // Try to extract NativeImage from the texture
            if (texture instanceof NativeImageBackedTexture nativeTexture) {
                NativeImage originalImage = nativeTexture.getImage();
                if (originalImage != null) {
                    // Create a copy of the image so we don't modify the original
                    NativeImage copy = new NativeImage(originalImage.getWidth(), originalImage.getHeight(), true);
                    originalImage.copyRect(copy, 0, 0, 0, 0, originalImage.getWidth(), originalImage.getHeight(), false, false);
                    LOGGER.info("Successfully loaded NativeImageBackedTexture: {}x{}", copy.getWidth(), copy.getHeight());
                    return copy;
                }
            }

            // For other texture types (like PlayerSkinTexture), we need to download from Mojang using UUID
            LOGGER.info("Attempting to download skin from Mojang API for texture type: {}", texture.getClass().getName());
            return downloadSkinFromMojangAPI(playerId, playerName);
        } catch (Exception e) {
            LOGGER.error("Error loading skin texture", e);
            return createDefaultSkin();
        }
    }

    private static NativeImage createDefaultSkin() {
        LOGGER.warn("Creating default skin (64x64)");
        NativeImage defaultSkin = new NativeImage(64, 64, true);
        // Fill with a skin color so we can see something
        defaultSkin.fillRect(0, 0, 64, 64, 0xFF8B7355); // Skin color in ABGR format
        return defaultSkin;
    }

    /**
     * Download skin texture from Mojang API using player UUID
     * Step 1: Get profile from https://sessionserver.mojang.com/session/minecraft/profile/<UUID>
     * Step 2: Decode base64 textures property to get skin URL
     * Step 3: Download skin from URL
     */
    private static NativeImage downloadSkinFromMojangAPI(UUID playerId, String playerName) {
        try {
            // Step 1: Get player profile
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + playerId.toString().replace("-", "");
            LOGGER.info("Fetching player profile from: {}", profileUrl);

            java.net.URL url = new java.net.URL(profileUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                LOGGER.warn("Failed to fetch profile, HTTP {}", responseCode);
                return createDefaultSkin();
            }

            // Read JSON response
            String jsonResponse;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                jsonResponse = response.toString();
            }

            LOGGER.info("Profile JSON received");

            // Step 2: Parse JSON using Gson
            Gson gson = new Gson();
            JsonObject profile = gson.fromJson(jsonResponse, JsonObject.class);

            // Get the properties array
            if (!profile.has("properties")) {
                LOGGER.warn("No properties found in profile");
                return createDefaultSkin();
            }

            var properties = profile.getAsJsonArray("properties");
            if (properties.size() == 0) {
                LOGGER.warn("Properties array is empty");
                return createDefaultSkin();
            }

            // Find the textures property
            String base64Textures = null;
            for (var element : properties) {
                JsonObject property = element.getAsJsonObject();
                if (property.has("name") && "textures".equals(property.get("name").getAsString())) {
                    base64Textures = property.get("value").getAsString();
                    break;
                }
            }

            if (base64Textures == null) {
                LOGGER.warn("No textures property found");
                return createDefaultSkin();
            }

            // Decode base64
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Textures);
            String texturesJson = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

            LOGGER.info("Decoded textures JSON");

            // Parse textures JSON
            JsonObject textures = gson.fromJson(texturesJson, JsonObject.class);
            if (!textures.has("textures") || !textures.getAsJsonObject("textures").has("SKIN")) {
                LOGGER.warn("No SKIN texture found");
                return createDefaultSkin();
            }

            JsonObject skin = textures.getAsJsonObject("textures").getAsJsonObject("SKIN");
            if (!skin.has("url")) {
                LOGGER.warn("No URL in SKIN texture");
                return createDefaultSkin();
            }

            String skinUrl = skin.get("url").getAsString();

            LOGGER.info("Downloading skin from: {}", skinUrl);

            // Step 3: Download skin
            java.net.URL skinUrlObj = new java.net.URL(skinUrl);
            java.net.HttpURLConnection skinConnection = (java.net.HttpURLConnection) skinUrlObj.openConnection();
            skinConnection.setRequestMethod("GET");
            skinConnection.setConnectTimeout(5000);
            skinConnection.setReadTimeout(5000);

            int skinResponseCode = skinConnection.getResponseCode();
            if (skinResponseCode != 200) {
                LOGGER.warn("Failed to download skin, HTTP {}", skinResponseCode);
                return createDefaultSkin();
            }

            try (InputStream stream = skinConnection.getInputStream()) {
                NativeImage downloadedSkin = NativeImage.read(stream);
                LOGGER.info("Successfully downloaded skin from Mojang API: {}x{}", downloadedSkin.getWidth(), downloadedSkin.getHeight());
                return downloadedSkin;
            }
        } catch (Exception e) {
            LOGGER.error("Error downloading skin from Mojang API", e);
            return createDefaultSkin();
        }
    }
}
