package com.dalvi.webcamhead.client.streaming;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Client for connecting to the video streaming signaling server using Socket.IO
 */
public class SignalingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private final Gson gson = new Gson();

    private Socket socket;
    private final String serverUrl;
    private final UUID playerUUID;
    private final String playerName;
    private final String roomId;

    // Callbacks
    private Consumer<PlayerJoinedEvent> onPlayerJoined;
    private Consumer<PlayerInfo> onNewPlayer;
    private Consumer<String> onPlayerLeft;
    private Consumer<WebcamStatusEvent> onWebcamStatus;
    private Consumer<VideoFrameEvent> onVideoFrame;

    public SignalingClient(String serverUrl, UUID playerUUID, String playerName, String roomId) {
        this.serverUrl = serverUrl;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.roomId = roomId;
    }

    /**
     * Connect to the signaling server
     */
    public void connect() {
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.timeout = 10000;

            socket = IO.socket(URI.create(serverUrl), options);

            setupEventHandlers();

            socket.connect();

            LOGGER.info("Connecting to signaling server at {}", serverUrl);
        } catch (Exception e) {
            LOGGER.error("Failed to connect to signaling server", e);
        }
    }

    /**
     * Setup Socket.IO event handlers
     */
    private void setupEventHandlers() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.info("Connected to signaling server");
                joinRoom();
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.warn("Disconnected from signaling server");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.error("Connection error: {}", args.length > 0 ? args[0] : "Unknown");
            }
        });

        socket.on("player:joined", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                handlePlayerJoined(args);
            }
        });

        socket.on("player:new", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                handleNewPlayer(args);
            }
        });

        socket.on("player:left", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                handlePlayerLeft(args);
            }
        });

        socket.on("webcam:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                handleWebcamStatus(args);
            }
        });

        socket.on("video:frame", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                handleVideoFrame(args);
            }
        });
    }

    /**
     * Join the room
     */
    private void joinRoom() {
        JsonObject data = new JsonObject();
        data.addProperty("minecraftUUID", playerUUID.toString());
        data.addProperty("playerName", playerName);
        data.addProperty("roomId", roomId);

        // Convert JsonObject to plain Object for Socket.IO
        String jsonString = gson.toJson(data);

        socket.emit("player:join", jsonString);
        LOGGER.info("Sent join request for room: {} with data: {}", roomId, jsonString);
    }

    /**
     * Send webcam toggle status
     */
    public void sendWebcamToggle(boolean active) {
        if (socket == null || !socket.connected()) {
            LOGGER.warn("Cannot send webcam toggle - not connected");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("active", active);

        String jsonString = gson.toJson(data);
        socket.emit("webcam:toggle", jsonString);
        LOGGER.info("Sent webcam toggle: {}", active);
    }

    /**
     * Send video frame
     */
    public void sendVideoFrame(String frameData) {
        if (socket == null || !socket.connected()) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("frameData", frameData);

        String jsonString = gson.toJson(data);
        socket.emit("video:frame", jsonString);
    }

    // Event handlers
    private void handlePlayerJoined(Object[] args) {
        if (args.length > 0 && onPlayerJoined != null) {
            try {
                PlayerJoinedEvent event = gson.fromJson(args[0].toString(), PlayerJoinedEvent.class);
                onPlayerJoined.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error parsing player:joined event", e);
            }
        }
    }

    private void handleNewPlayer(Object[] args) {
        if (args.length > 0 && onNewPlayer != null) {
            try {
                JsonObject json = gson.fromJson(args[0].toString(), JsonObject.class);
                PlayerInfo player = gson.fromJson(json.get("player"), PlayerInfo.class);
                onNewPlayer.accept(player);
            } catch (Exception e) {
                LOGGER.error("Error parsing player:new event", e);
            }
        }
    }

    private void handlePlayerLeft(Object[] args) {
        if (args.length > 0 && onPlayerLeft != null) {
            try {
                JsonObject json = gson.fromJson(args[0].toString(), JsonObject.class);
                String uuid = json.get("minecraftUUID").getAsString();
                onPlayerLeft.accept(uuid);
            } catch (Exception e) {
                LOGGER.error("Error parsing player:left event", e);
            }
        }
    }

    private void handleWebcamStatus(Object[] args) {
        if (args.length > 0 && onWebcamStatus != null) {
            try {
                WebcamStatusEvent event = gson.fromJson(args[0].toString(), WebcamStatusEvent.class);
                onWebcamStatus.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error parsing webcam:status event", e);
            }
        }
    }

    private void handleVideoFrame(Object[] args) {
        if (args.length > 0 && onVideoFrame != null) {
            try {
                VideoFrameEvent event = gson.fromJson(args[0].toString(), VideoFrameEvent.class);
                onVideoFrame.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error parsing video:frame event", e);
            }
        }
    }

    /**
     * Disconnect from the signaling server
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
            socket = null;
            LOGGER.info("Disconnected from signaling server");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    // Setters for callbacks
    public void setOnPlayerJoined(Consumer<PlayerJoinedEvent> callback) {
        this.onPlayerJoined = callback;
    }

    public void setOnNewPlayer(Consumer<PlayerInfo> callback) {
        this.onNewPlayer = callback;
    }

    public void setOnPlayerLeft(Consumer<String> callback) {
        this.onPlayerLeft = callback;
    }

    public void setOnWebcamStatus(Consumer<WebcamStatusEvent> callback) {
        this.onWebcamStatus = callback;
    }

    public void setOnVideoFrame(Consumer<VideoFrameEvent> callback) {
        this.onVideoFrame = callback;
    }

    // Event classes
    public static class PlayerJoinedEvent {
        public PlayerInfo player;
        public PlayerInfo[] existingPlayers;
    }

    public static class PlayerInfo {
        public String socketId;
        public String minecraftUUID;
        public String playerName;
        public String roomId;
        public boolean webcamActive;
        public long connectedAt;
    }

    public static class WebcamStatusEvent {
        public String minecraftUUID;
        public String playerName;
        public boolean active;
    }

    public static class VideoFrameEvent {
        public String fromUUID;
        public String fromName;
        public String frameData;
    }
}
