package com.dalvi.webcamhead.client;

import com.dalvi.webcamhead.client.command.WebcamCommand;
import com.dalvi.webcamhead.client.config.ModConfig;
import com.dalvi.webcamhead.client.render.SkinOverlayRenderer;
import com.dalvi.webcamhead.client.video.PlayerVideoState;
import com.dalvi.webcamhead.client.video.VideoStateManager;
import com.dalvi.webcamhead.client.webcam.WebcamManager;
import com.dalvi.webcamhead.client.webcam.WebcamTextureManager;
import com.dalvi.webcamhead.client.streaming.SignalingClient;
import com.dalvi.webcamhead.client.streaming.VideoStreamClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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

    private static WebcamheadClient instance;

    private WebcamManager webcamManager;
    private WebcamTextureManager textureManager;
    private KeyBinding toggleWebcamKey;
    private boolean webcamActive = false;

    // Multiplayer streaming
    private SignalingClient signalingClient;
    private VideoStreamClient videoStreamClient;

    public static WebcamheadClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
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

        // Register disconnect event to stop webcam when leaving a world/server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onDisconnect(client);
        });

        // Register join event to initialize multiplayer when joining a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ModConfig.isMultiplayerEnabled()) {
                initializeMultiplayer();
            }
        });

        // Register render event for rendering video panels
        // DISABLED: We're using skin overlay mode instead of 3D panels
        /*
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
        */

        LOGGER.info("WebcamHead mod initialized");
    }

    private void onClientTick(MinecraftClient client) {
        // Handle keybinding
        while (toggleWebcamKey.wasPressed()) {
            toggleWebcam(client);
        }

        // Update webcam texture and skin if active
        if (webcamActive && webcamManager != null && textureManager != null && client.player != null) {
            BufferedImage frame = webcamManager.getLatestFrame();
            if (frame != null) {
                // Update the panel texture
                textureManager.updateTexture(frame);

                // Update the skin overlay
                SkinOverlayRenderer.updateSkinWithWebcam(client.player.getUuid(), frame);

                // Send frame to other players if multiplayer is enabled
                if (videoStreamClient != null) {
                    videoStreamClient.sendFrame(frame);
                }
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

                        // Initialize modified skin for overlay
                        if (client.player instanceof AbstractClientPlayerEntity) {
                            SkinOverlayRenderer.initializeModifiedSkin((AbstractClientPlayerEntity) client.player);
                        }

                        // Register player video state
                        PlayerVideoState videoState = new PlayerVideoState(
                            textureManager.getTextureId(),
                            textureManager.getWidth(),
                            textureManager.getHeight()
                        );
                        VideoStateManager.getInstance().setPlayerVideo(client.player.getUuid(), videoState);

                        // Notify signaling server if multiplayer is enabled
                        if (signalingClient != null && signalingClient.isConnected()) {
                            signalingClient.sendWebcamToggle(true);
                        }

                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§aWebcam enabled (skin overlay mode)"), false);
                        }

                        LOGGER.info("Webcam started for local player with skin overlay");
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
            // Clean up modified skin
            SkinOverlayRenderer.cleanupModifiedSkin(client.player.getUuid());

            VideoStateManager.getInstance().removePlayerVideo(client.player.getUuid());

            // Notify signaling server if multiplayer is enabled
            if (signalingClient != null && signalingClient.isConnected()) {
                signalingClient.sendWebcamToggle(false);
            }

            client.player.sendMessage(Text.literal("§eWebcam disabled"), false);
        }

        LOGGER.info("Webcam stopped for local player");
    }

    /**
     * Initialize multiplayer streaming
     */
    private void initializeMultiplayer() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Wait for player to join a world
        new Thread(() -> {
            // Wait for player to exist
            while (client.player == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }

            // Initialize on main thread
            client.execute(() -> {
                try {
                    String serverUrl = ModConfig.getSignalingServerUrl();
                    String roomId = ModConfig.getRoomId();

                    LOGGER.info("Connecting to signaling server at {} (room: {})", serverUrl, roomId);

                    signalingClient = new SignalingClient(
                        serverUrl,
                        client.player.getUuid(),
                        client.player.getName().getString(),
                        roomId
                    );

                    // Setup callbacks
                    setupSignalingCallbacks();

                    // Create video stream client
                    videoStreamClient = new VideoStreamClient(signalingClient);
                    setupVideoStreamCallbacks();

                    // Connect to server
                    signalingClient.connect();

                    LOGGER.info("Multiplayer streaming initialized");
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize multiplayer streaming", e);
                }
            });
        }, "MultiplayerInit").start();
    }

    /**
     * Setup signaling client callbacks
     */
    private void setupSignalingCallbacks() {
        MinecraftClient client = MinecraftClient.getInstance();

        signalingClient.setOnPlayerJoined((event) -> {
            LOGGER.info("Joined room with {} existing players", event.existingPlayers.length);
        });

        signalingClient.setOnNewPlayer((player) -> {
            LOGGER.info("Player joined: {} ({})", player.playerName, player.minecraftUUID);
        });

        signalingClient.setOnPlayerLeft((uuid) -> {
            LOGGER.info("Player left: {}", uuid);
            // Clean up their skin overlay
            try {
                java.util.UUID playerUUID = java.util.UUID.fromString(uuid);
                client.execute(() -> {
                    SkinOverlayRenderer.cleanupModifiedSkin(playerUUID);
                });
            } catch (Exception e) {
                LOGGER.error("Error cleaning up player skin", e);
            }
        });

        signalingClient.setOnWebcamStatus((event) -> {
            LOGGER.info("Player {} webcam: {}", event.playerName, event.active ? "ON" : "OFF");
        });
    }

    /**
     * Setup video stream client callbacks
     */
    private void setupVideoStreamCallbacks() {
        MinecraftClient client = MinecraftClient.getInstance();

        videoStreamClient.setOnFrameReceived((playerUUID, frame) -> {
            // Update received player's skin overlay on main thread
            client.execute(() -> {
                try {
                    // Initialize skin for this player if not already done
                    if (!SkinOverlayRenderer.hasModifiedSkin(playerUUID)) {
                        // Find the player entity
                        if (client.world != null) {
                            for (var player : client.world.getPlayers()) {
                                if (player.getUuid().equals(playerUUID) && player instanceof AbstractClientPlayerEntity) {
                                    SkinOverlayRenderer.initializeModifiedSkin((AbstractClientPlayerEntity) player);
                                    break;
                                }
                            }
                        }
                    }

                    // Update the skin with the received frame
                    SkinOverlayRenderer.updateSkinWithWebcam(playerUUID, frame);
                } catch (Exception e) {
                    LOGGER.error("Error updating remote player skin", e);
                }
            });
        });
    }

    // Public methods for commands
    public boolean isWebcamActive() {
        return webcamActive;
    }

    public boolean isSignalingConnected() {
        return signalingClient != null && signalingClient.isConnected();
    }

    public VideoStreamClient.VideoStats getStreamingStats() {
        return videoStreamClient != null ? videoStreamClient.getStats() : null;
    }

    public void reconnectSignaling() {
        // Disconnect current signaling client
        if (signalingClient != null) {
            signalingClient.disconnect();
        }

        // Reinitialize multiplayer
        if (ModConfig.isMultiplayerEnabled()) {
            initializeMultiplayer();
        }
    }

    /**
     * Called when the client disconnects from a world/server
     */
    private void onDisconnect(MinecraftClient client) {
        LOGGER.info("Disconnected from world/server, stopping webcam");

        // Stop webcam if active
        if (webcamActive) {
            webcamActive = false;
            stopWebcam(client);
        }

        // Disconnect from signaling server
        if (signalingClient != null) {
            signalingClient.disconnect();
            signalingClient = null;
        }

        // Reset video stream client
        videoStreamClient = null;
    }
}
