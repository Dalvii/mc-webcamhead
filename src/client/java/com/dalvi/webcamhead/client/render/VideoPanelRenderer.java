package com.dalvi.webcamhead.client.render;

import com.dalvi.webcamhead.client.video.PlayerVideoState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoPanelRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static final float PANEL_WIDTH = 0.5f; // Size of player head is 0.5 blocks
    private static final float PANEL_HEIGHT = 0.5f;
    private static final float PANEL_DEPTH = 0.01f; // Slight offset in front of face
    private static final float HEAD_HEIGHT = 1.5f; // Player eye level
    private static int renderCount = 0;

    public static void render(PlayerEntity player, PlayerVideoState videoState, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!videoState.isActive()) {
            return;
        }

        if (renderCount++ % 100 == 0) {
            LOGGER.info("Rendering video panel for player {} (frame {})", player.getName().getString(), renderCount);
        }

        matrices.push();

        // Position at player's head height
        matrices.translate(0.0, HEAD_HEIGHT, 0.0);

        // Rotate to match player's body yaw
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.bodyYaw));

        // Rotate to match player's head yaw (relative to body)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(player.headYaw - player.bodyYaw)));

        // Rotate to match player's pitch (looking up/down)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch()));

        // Offset slightly forward from the head center to be on the face
        matrices.translate(0.0, 0.0, -0.25 - PANEL_DEPTH);

        // Setup rendering
        RenderSystem.setShaderTexture(0, videoState.getTextureId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Calculate aspect ratio
        float aspectRatio = (float) videoState.getWidth() / videoState.getHeight();
        float adjustedWidth = PANEL_WIDTH;
        float adjustedHeight = PANEL_HEIGHT / aspectRatio;

        // Draw the quad using the vertex consumer provider
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(videoState.getTextureId()));
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // Bottom-left
        vertexConsumer.vertex(positionMatrix, -adjustedWidth / 2, 0, 0)
            .color(255, 255, 255, 255)
            .texture(0, 1)
            .light(light);

        // Bottom-right
        vertexConsumer.vertex(positionMatrix, adjustedWidth / 2, 0, 0)
            .color(255, 255, 255, 255)
            .texture(1, 1)
            .light(light);

        // Top-right
        vertexConsumer.vertex(positionMatrix, adjustedWidth / 2, adjustedHeight, 0)
            .color(255, 255, 255, 255)
            .texture(1, 0)
            .light(light);

        // Top-left
        vertexConsumer.vertex(positionMatrix, -adjustedWidth / 2, adjustedHeight, 0)
            .color(255, 255, 255, 255)
            .texture(0, 0)
            .light(light);

        RenderSystem.disableBlend();

        matrices.pop();
    }
}
