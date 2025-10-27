/**
 * WebRTC signaling logic
 */
export class SignalingManager {
    constructor(io, playerManager, roomManager) {
        this.io = io;
        this.playerManager = playerManager;
        this.roomManager = roomManager;
    }

    /**
     * Setup Socket.io event handlers
     */
    setupHandlers(socket) {
        console.log(`[Signaling] Setting up handlers for socket ${socket.id}`);

        // Player joins with Minecraft UUID
        socket.on('player:join', (data) => {
            console.log(`[Signaling] Received player:join event:`, typeof data, data);
            this.handlePlayerJoin(socket, data);
        });

        // Player leaves
        socket.on('disconnect', () => this.handlePlayerLeave(socket));

        // Webcam toggle
        socket.on('webcam:toggle', (data) => {
            console.log(`[Signaling] Received webcam:toggle event:`, typeof data, data);
            this.handleWebcamToggle(socket, data);
        });

        // Video streaming
        socket.on('video:frame', (data) => this.handleVideoFrame(socket, data));
    }

    /**
     * Handle player join
     */
    handlePlayerJoin(socket, data) {
        // Parse data if it's a string (from Java client)
        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch (e) {
                console.error('[Signaling] Failed to parse player:join data:', e);
                socket.emit('error', { message: 'Invalid JSON format' });
                return;
            }
        }

        const { minecraftUUID, playerName, roomId = 'default' } = data;

        if (!minecraftUUID || !playerName) {
            socket.emit('error', { message: 'Missing minecraftUUID or playerName' });
            return;
        }

        // Add player
        const player = this.playerManager.addPlayer(socket.id, {
            minecraftUUID,
            playerName,
            roomId
        });

        // Add to room
        this.roomManager.addPlayerToRoom(roomId, socket.id);

        // Join Socket.io room
        socket.join(roomId);

        // Send current players in room to new player
        const playersInRoom = this.roomManager.getPlayersInRoom(roomId)
            .map(socketId => this.playerManager.getPlayer(socketId))
            .filter(p => p && p.socketId !== socket.id);

        const joinedData = JSON.stringify({
            player,
            existingPlayers: playersInRoom
        });
        socket.emit('player:joined', joinedData);

        // Notify others in the room
        const newPlayerData = JSON.stringify({ player });
        socket.to(roomId).emit('player:new', newPlayerData);

        console.log(`[Signaling] Player ${playerName} joined room ${roomId}`);
    }

    /**
     * Handle player leave
     */
    handlePlayerLeave(socket) {
        const player = this.playerManager.getPlayer(socket.id);
        if (player) {
            const roomId = this.roomManager.getRoomForPlayer(socket.id);

            // Notify others in the room
            if (roomId) {
                const leftData = JSON.stringify({
                    minecraftUUID: player.minecraftUUID,
                    playerName: player.playerName
                });
                socket.to(roomId).emit('player:left', leftData);
            }

            // Remove from room and player list
            this.roomManager.removePlayerFromRoom(socket.id);
            this.playerManager.removePlayer(socket.id);

            console.log(`[Signaling] Player ${player.playerName} left`);
        }
    }

    /**
     * Handle webcam toggle
     */
    handleWebcamToggle(socket, data) {
        // Parse data if it's a string (from Java client)
        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch (e) {
                console.error('[Signaling] Failed to parse webcam:toggle data:', e);
                return;
            }
        }

        const { active } = data;
        const player = this.playerManager.getPlayer(socket.id);

        if (!player) {
            return;
        }

        this.playerManager.setWebcamStatus(socket.id, active);

        const roomId = this.roomManager.getRoomForPlayer(socket.id);
        if (roomId) {
            // Broadcast to all in room including sender
            const statusData = JSON.stringify({
                minecraftUUID: player.minecraftUUID,
                playerName: player.playerName,
                active
            });
            this.io.to(roomId).emit('webcam:status', statusData);
        }

        console.log(`[Signaling] Player ${player.playerName} webcam ${active ? 'ON' : 'OFF'}`);
    }

    /**
     * Handle video frame
     */
    handleVideoFrame(socket, data) {
        // Parse data if it's a string (from Java client)
        if (typeof data === 'string') {
            try {
                data = JSON.parse(data);
            } catch (e) {
                console.error('[Signaling] Failed to parse video:frame data:', e);
                return;
            }
        }

        const { frameData } = data;
        const fromPlayer = this.playerManager.getPlayer(socket.id);

        if (!fromPlayer) {
            // Player not registered yet, silently ignore (happens during initialization)
            return;
        }

        const roomId = this.roomManager.getRoomForPlayer(socket.id);
        if (roomId) {
            const playersInRoom = this.roomManager.getPlayersInRoom(roomId).length;

            // Broadcast frame to all other players in the room
            const framePayload = JSON.stringify({
                fromUUID: fromPlayer.minecraftUUID,
                fromName: fromPlayer.playerName,
                frameData: frameData
            });
            socket.to(roomId).emit('video:frame', framePayload);

            // Log occasionally (every 100 frames)
            if (!this.frameCount) this.frameCount = {};
            if (!this.frameCount[fromPlayer.minecraftUUID]) this.frameCount[fromPlayer.minecraftUUID] = 0;
            this.frameCount[fromPlayer.minecraftUUID]++;

            if (this.frameCount[fromPlayer.minecraftUUID] % 100 === 0) {
                console.log(`[Signaling] Forwarded ${this.frameCount[fromPlayer.minecraftUUID]} frames from ${fromPlayer.playerName} to ${playersInRoom - 1} player(s)`);
            }
        }
    }
}
