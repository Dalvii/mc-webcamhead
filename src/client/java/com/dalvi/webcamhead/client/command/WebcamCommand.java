package com.dalvi.webcamhead.client.command;

import com.dalvi.webcamhead.client.config.ModConfig;
import com.dalvi.webcamhead.client.webcam.WebcamDeviceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
        return 1;
    }
}
