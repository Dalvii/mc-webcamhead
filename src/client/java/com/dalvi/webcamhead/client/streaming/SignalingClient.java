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
    private Consumer<String> onChatMessage; // Chat message callback

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
        LOGGER.info("=== SignalingClient.connect() CALLED ===");
        LOGGER.info("Server URL: {}", serverUrl);
        LOGGER.info("Player UUID: {}", playerUUID);
        LOGGER.info("Player Name: {}", playerName);
        LOGGER.info("Room ID: {}", roomId);

        try {
            LOGGER.info("Step 1: Creating IO.Options...");
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.timeout = 20000;
            options.forceNew = true;

            // Set transports to try websocket first, then polling
            options.transports = new String[]{"websocket", "polling"};
            LOGGER.info("Step 2: IO.Options created successfully");

            LOGGER.info("Step 3: Creating Socket.IO socket for URL: {}", serverUrl);
            socket = IO.socket(URI.create(serverUrl), options);
            LOGGER.info("Step 4: Socket created successfully: {}", socket != null ? "NOT NULL" : "NULL");

            LOGGER.info("Step 5: Setting up event handlers...");
            setupEventHandlers();
            LOGGER.info("Step 6: Event handlers set up successfully");

            LOGGER.info("Step 7: Calling socket.connect()...");
            socket.connect();
            LOGGER.info("Step 8: socket.connect() returned (async operation)");

            LOGGER.info("Step 9: Sending chat message...");
            sendChatMessage("§eConnecting to streaming server...");
            LOGGER.info("=== SignalingClient.connect() COMPLETED ===");
        } catch (Exception e) {
            LOGGER.error("!!! EXCEPTION in SignalingClient.connect() !!!", e);
            LOGGER.error("Exception type: {}", e.getClass().getName());
            LOGGER.error("Exception message: {}", e.getMessage());
            sendChatMessage("§cFailed to connect: " + e.getMessage());
        }
    }

    /**
     * Setup Socket.IO event handlers
     */
    private void setupEventHandlers() {
        LOGGER.info("Setting up Socket.IO event handlers...");

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.info(">>> EVENT_CONNECT fired! <<<");
                LOGGER.info("Connected to signaling server");
                sendChatMessage("§aConnected to streaming server");
                joinRoom();
            }
        });
        LOGGER.info("Registered EVENT_CONNECT handler");

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.warn(">>> EVENT_DISCONNECT fired! <<<");
                LOGGER.warn("Disconnected from signaling server");
                sendChatMessage("§cDisconnected from streaming server");
            }
        });
        LOGGER.info("Registered EVENT_DISCONNECT handler");

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.error(">>> EVENT_CONNECT_ERROR fired! <<<");
                LOGGER.error("Args length: {}", args.length);
                if (args.length > 0) {
                    LOGGER.error("Args[0] type: {}", args[0].getClass().getName());
                    Exception e = args[0] instanceof Exception ? (Exception) args[0] : null;
                    if (e != null) {
                        LOGGER.error("Connection error: {}", e.getMessage(), e);
                        sendChatMessage("§cStreaming connection error: " + e.getMessage());
                    } else {
                        String error = args[0].toString();
                        LOGGER.error("Connection error: {}", error);
                        sendChatMessage("§cStreaming connection error: " + error);
                    }
                } else {
                    LOGGER.error("Connection error: Unknown error (no args)");
                    sendChatMessage("§cStreaming connection error (check logs)");
                }
            }
        });
        LOGGER.info("Registered EVENT_CONNECT_ERROR handler");

        // Using string literals for events not defined in Socket.IO 2.1.0
        socket.on("connect_timeout", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.error(">>> connect_timeout fired! <<<");
                LOGGER.error("Connection timeout - server at {} did not respond", serverUrl);
                sendChatMessage("§cConnection timeout - server not responding");
            }
        });
        LOGGER.info("Registered connect_timeout handler");

        socket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.error(">>> error event fired! <<<");
                String error = args.length > 0 ? args[0].toString() : "Unknown";
                LOGGER.error("Socket error: {}", error);
                sendChatMessage("§cSocket error: " + error);
            }
        });
        LOGGER.info("Registered error handler");

        socket.on("reconnect_attempt", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.info(">>> reconnect_attempt fired! <<<");
                int attempt = args.length > 0 ? ((Number) args[0]).intValue() : 0;
                LOGGER.info("Reconnection attempt #{}", attempt);
            }
        });
        LOGGER.info("Registered reconnect_attempt handler");

        socket.on("reconnect_failed", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LOGGER.error(">>> reconnect_failed fired! <<<");
                LOGGER.error("Reconnection failed - giving up");
                sendChatMessage("§cFailed to reconnect to streaming server");
            }
        });
        LOGGER.info("Registered reconnect_failed handler");

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
                int count = event.existingPlayers != null ? event.existingPlayers.length : 0;
                sendChatMessage("§eJoined room §f" + roomId + " §e(" + count + " players already connected)");
                onPlayerJoined.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error parsing player:joined event", e);
                sendChatMessage("§cError joining room");
            }
        }
    }

    private void handleNewPlayer(Object[] args) {
        if (args.length > 0 && onNewPlayer != null) {
            try {
                JsonObject json = gson.fromJson(args[0].toString(), JsonObject.class);
                PlayerInfo player = gson.fromJson(json.get("player"), PlayerInfo.class);
                sendChatMessage("§a+ §f" + player.playerName + " §ajoined the stream");
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
                String playerName = json.has("playerName") ? json.get("playerName").getAsString() : "Unknown";
                sendChatMessage("§c- §f" + playerName + " §cleft the stream");
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

    /**
     * Send a message to chat (via callback)
     */
    private void sendChatMessage(String message) {
        LOGGER.info("sendChatMessage called: {}", message);
        LOGGER.info("onChatMessage callback is: {}", onChatMessage != null ? "SET" : "NULL");
        if (onChatMessage != null) {
            onChatMessage.accept(message);
            LOGGER.info("Chat message sent via callback");
        } else {
            LOGGER.warn("!!! onChatMessage is NULL - message not sent to chat !!!");
        }
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

    public void setOnChatMessage(Consumer<String> callback) {
        this.onChatMessage = callback;
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
