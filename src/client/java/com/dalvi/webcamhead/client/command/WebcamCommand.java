package com.dalvi.webcamhead.client.command;

import com.dalvi.webcamhead.client.config.ModConfig;
import com.dalvi.webcamhead.client.webcam.WebcamDeviceManager;
import com.dalvi.webcamhead.client.WebcamheadClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class WebcamCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("webcam")
            .then(literal("list")
                .executes(WebcamCommand::listDevices))
            .then(literal("device")
                .then(argument("index", IntegerArgumentType.integer(0, 10))
                    .executes(WebcamCommand::setDevice)))
            .then(literal("info")
                .executes(WebcamCommand::showInfo))
            .then(literal("state")
                .executes(WebcamCommand::showState))
            .then(literal("stats")
                .executes(WebcamCommand::showStats))
            .then(literal("join")
                .then(argument("roomId", StringArgumentType.string())
                    .executes(WebcamCommand::joinRoom)))
        );
    }

    private static int listDevices(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§eScanning for webcam devices..."));

        new Thread(() -> {
            try {
                List<WebcamDeviceManager.WebcamDevice> devices = WebcamDeviceManager.listAvailableDevices();

                if (devices.isEmpty()) {
                    context.getSource().sendFeedback(Text.literal("§cNo webcam devices found"));
                } else {
                    context.getSource().sendFeedback(Text.literal("§aFound " + devices.size() + " webcam device(s):"));
                    for (WebcamDeviceManager.WebcamDevice device : devices) {
                        String prefix = device.getIndex() == ModConfig.getDeviceIndex() ? "§a[ACTIVE] " : "§7";
                        context.getSource().sendFeedback(Text.literal(prefix + device.toString()));
                    }
                    context.getSource().sendFeedback(Text.literal("§eUse /webcam device <index> to select a camera"));
                }
            } catch (Exception e) {
                context.getSource().sendFeedback(Text.literal("§cError scanning devices: " + e.getMessage()));
            }
        }, "WebcamDeviceScanner").start();

        return 1;
    }

    private static int setDevice(CommandContext<FabricClientCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        ModConfig.setDeviceIndex(index);
        context.getSource().sendFeedback(Text.literal("§aSet webcam device index to " + index));
        context.getSource().sendFeedback(Text.literal("§eRestart your webcam (press V twice) for changes to take effect"));
        return 1;
    }

    private static int showInfo(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6=== Webcam Configuration ==="));
        context.getSource().sendFeedback(Text.literal("§eDevice Index: §f" + ModConfig.getDeviceIndex()));
        context.getSource().sendFeedback(Text.literal("§eResolution: §f" + ModConfig.getCaptureWidth() + "x" + ModConfig.getCaptureHeight()));
        context.getSource().sendFeedback(Text.literal("§eFPS: §f" + ModConfig.getCaptureFps()));
        context.getSource().sendFeedback(Text.literal("§eRender Mode: §f" + ModConfig.getRenderMode()));
        context.getSource().sendFeedback(Text.literal("§eMultiplayer: §f" + (ModConfig.isMultiplayerEnabled() ? "Enabled" : "Disabled")));
        context.getSource().sendFeedback(Text.literal("§eServer: §f" + ModConfig.getSignalingServerUrl()));
        context.getSource().sendFeedback(Text.literal("§eRoom: §f" + ModConfig.getRoomId()));
        return 1;
    }

    private static int showState(CommandContext<FabricClientCommandSource> context) {
        WebcamheadClient client = WebcamheadClient.getInstance();

        context.getSource().sendFeedback(Text.literal("§6=== Webcam State ==="));
        context.getSource().sendFeedback(Text.literal("§eWebcam Active: §f" + (client != null && client.isWebcamActive())));
        context.getSource().sendFeedback(Text.literal("§eSignaling Connected: §f" + (client != null && client.isSignalingConnected())));
        context.getSource().sendFeedback(Text.literal("§eCurrent Room: §f" + ModConfig.getRoomId()));

        return 1;
    }

    private static int showStats(CommandContext<FabricClientCommandSource> context) {
        WebcamheadClient client = WebcamheadClient.getInstance();

        if (client == null || !client.isWebcamActive()) {
            context.getSource().sendFeedback(Text.literal("§cWebcam is not active"));
            return 0;
        }

        var stats = client.getStreamingStats();
        if (stats != null) {
            context.getSource().sendFeedback(Text.literal("§6=== Streaming Statistics ==="));
            context.getSource().sendFeedback(Text.literal("§eFrames Sent: §f" + stats.framesSent));
            context.getSource().sendFeedback(Text.literal("§eFrames Received: §f" + stats.framesReceived));
            context.getSource().sendFeedback(Text.literal("§eBytes Sent: §f" + stats.bytesSent / 1024 + " KB"));
            context.getSource().sendFeedback(Text.literal("§eAvg Frame Size: §f" + stats.getAverageFrameSize() / 1024 + " KB"));
        }

        return 1;
    }

    private static int joinRoom(CommandContext<FabricClientCommandSource> context) {
        String roomId = StringArgumentType.getString(context, "roomId");
        ModConfig.setRoomId(roomId);

        context.getSource().sendFeedback(Text.literal("§aRoom changed to: §f" + roomId));
        context.getSource().sendFeedback(Text.literal("§eReconnecting to signaling server..."));

        WebcamheadClient client = WebcamheadClient.getInstance();
        if (client != null) {
            client.reconnectSignaling();
        }

        return 1;
    }
}
