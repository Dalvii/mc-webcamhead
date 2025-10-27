package com.dalvi.webcamhead.client;

import com.dalvi.webcamhead.client.command.WebcamCommand;
import com.dalvi.webcamhead.client.config.ModConfig;
import com.dalvi.webcamhead.client.video.PlayerVideoState;
import com.dalvi.webcamhead.client.video.VideoStateManager;
import com.dalvi.webcamhead.client.webcam.WebcamManager;
import com.dalvi.webcamhead.client.webcam.WebcamTextureManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

public class WebcamheadClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static final String KEY_CATEGORY = "key.categories.webcamhead";

    private WebcamManager webcamManager;
    private WebcamTextureManager textureManager;
    private KeyBinding toggleWebcamKey;
    private boolean webcamActive = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing WebcamHead mod");

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            WebcamCommand.register(dispatcher);
        });

        // Register keybinding
        toggleWebcamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.webcamhead.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
        ));

        // Register tick event for updating texture
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Register render event for rendering video panels
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST.register(context -> {
            if (context.world() == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            // Render video for all players in the world
            for (net.minecraft.entity.player.PlayerEntity player : context.world().getPlayers()) {
                com.dalvi.webcamhead.client.video.PlayerVideoState videoState =
                    com.dalvi.webcamhead.client.video.VideoStateManager.getInstance().getPlayerVideo(player);

                if (videoState != null && videoState.isActive()) {
                    // Get player position relative to camera
                    var camera = context.camera();
                    double cameraX = camera.getPos().x;
                    double cameraY = camera.getPos().y;
                    double cameraZ = camera.getPos().z;

                    context.matrixStack().push();
                    context.matrixStack().translate(
                        player.getX() - cameraX,
                        player.getY() - cameraY,
                        player.getZ() - cameraZ
                    );

                    com.dalvi.webcamhead.client.render.VideoPanelRenderer.render(
                        player,
                        videoState,
                        context.matrixStack(),
                        context.consumers(),
                        context.lightmapTextureManager().pack(15, 15)
                    );

                    context.matrixStack().pop();
                }
            }
        });

        LOGGER.info("WebcamHead mod initialized");
    }

    private void onClientTick(MinecraftClient client) {
        // Handle keybinding
        while (toggleWebcamKey.wasPressed()) {
            toggleWebcam(client);
        }

        // Update webcam texture if active
        if (webcamActive && webcamManager != null && textureManager != null) {
            BufferedImage frame = webcamManager.getLatestFrame();
            if (frame != null) {
                textureManager.updateTexture(frame);
            }
        }
    }

    private void toggleWebcam(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        webcamActive = !webcamActive;

        if (webcamActive) {
            startWebcam(client);
        } else {
            stopWebcam(client);
        }
    }

    private void startWebcam(MinecraftClient client) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§eStarting webcam..."), false);
        }

        // Start webcam in a separate thread to avoid blocking on permission dialog
        new Thread(() -> {
            try {
                // Initialize webcam manager
                webcamManager = new WebcamManager(
                    ModConfig.getCaptureWidth(),
                    ModConfig.getCaptureHeight(),
                    ModConfig.getCaptureFps(),
                    ModConfig.getDeviceIndex()
                );

                if (!webcamManager.start()) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§cFailed to start webcam"), false);
                    }
                    webcamActive = false;
                    return;
                }

                // Initialize texture manager on the main thread
                client.execute(() -> {
                    try {
                        textureManager = new WebcamTextureManager(
                            webcamManager.getWidth(),
                            webcamManager.getHeight()
                        );
                        textureManager.initialize();

                        // Register player video state
                        PlayerVideoState videoState = new PlayerVideoState(
                            textureManager.getTextureId(),
                            textureManager.getWidth(),
                            textureManager.getHeight()
                        );
                        VideoStateManager.getInstance().setPlayerVideo(client.player.getUuid(), videoState);

                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§aWebcam enabled"), false);
                        }

                        LOGGER.info("Webcam started for local player");
                    } catch (Exception e) {
                        LOGGER.error("Error initializing texture", e);
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§cError initializing webcam texture"), false);
                        }
                        webcamActive = false;
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Error starting webcam", e);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§cError starting webcam: " + e.getMessage()), false);
                }
                webcamActive = false;
            }
        }, "WebcamStarter").start();
    }

    private void stopWebcam(MinecraftClient client) {
        if (webcamManager != null) {
            webcamManager.stop();
            webcamManager = null;
        }

        if (textureManager != null) {
            textureManager.cleanup();
            textureManager = null;
        }

        if (client.player != null) {
            VideoStateManager.getInstance().removePlayerVideo(client.player.getUuid());
            client.player.sendMessage(Text.literal("§eWebcam disabled"), false);
        }

        LOGGER.info("Webcam stopped for local player");
    }
}
