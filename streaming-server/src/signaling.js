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
        // Player joins with Minecraft UUID
        socket.on('player:join', (data) => this.handlePlayerJoin(socket, data));

        // Player leaves
        socket.on('disconnect', () => this.handlePlayerLeave(socket));

        // Webcam toggle
        socket.on('webcam:toggle', (data) => this.handleWebcamToggle(socket, data));

        // Video streaming
        socket.on('video:frame', (data) => this.handleVideoFrame(socket, data));
    }

    /**
     * Handle player join
     */
    handlePlayerJoin(socket, data) {
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

        socket.emit('player:joined', {
            player,
            existingPlayers: playersInRoom
        });

        // Notify others in the room
        socket.to(roomId).emit('player:new', { player });

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
                socket.to(roomId).emit('player:left', {
                    minecraftUUID: player.minecraftUUID,
                    playerName: player.playerName
                });
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
        const { active } = data;
        const player = this.playerManager.getPlayer(socket.id);

        if (!player) {
            return;
        }

        this.playerManager.setWebcamStatus(socket.id, active);

        const roomId = this.roomManager.getRoomForPlayer(socket.id);
        if (roomId) {
            // Broadcast to all in room including sender
            this.io.to(roomId).emit('webcam:status', {
                minecraftUUID: player.minecraftUUID,
                playerName: player.playerName,
                active
            });
        }

        console.log(`[Signaling] Player ${player.playerName} webcam ${active ? 'ON' : 'OFF'}`);
    }

    /**
     * Handle video frame
     */
    handleVideoFrame(socket, data) {
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
            socket.to(roomId).emit('video:frame', {
                fromUUID: fromPlayer.minecraftUUID,
                fromName: fromPlayer.playerName,
                frameData: frameData
            });

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
