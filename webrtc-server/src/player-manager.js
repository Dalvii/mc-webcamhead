/**
 * Manages connected players and their state
 */
export class PlayerManager {
    constructor() {
        // Map of socketId -> player data
        this.players = new Map();

        // Map of minecraftUUID -> socketId
        this.uuidToSocket = new Map();
    }

    /**
     * Add a player
     */
    addPlayer(socketId, playerData) {
        const player = {
            socketId,
            minecraftUUID: playerData.minecraftUUID,
            playerName: playerData.playerName,
            roomId: playerData.roomId || 'default',
            webcamActive: false,
            connectedAt: Date.now()
        };

        this.players.set(socketId, player);
        this.uuidToSocket.set(playerData.minecraftUUID, socketId);

        console.log(`[PlayerManager] Player ${playerData.playerName} (${playerData.minecraftUUID}) joined`);
        return player;
    }

    /**
     * Remove a player
     */
    removePlayer(socketId) {
        const player = this.players.get(socketId);
        if (player) {
            this.players.delete(socketId);
            this.uuidToSocket.delete(player.minecraftUUID);
            console.log(`[PlayerManager] Player ${player.playerName} left`);
        }
        return player;
    }

    /**
     * Get player by socket ID
     */
    getPlayer(socketId) {
        return this.players.get(socketId);
    }

    /**
     * Get player by Minecraft UUID
     */
    getPlayerByUUID(minecraftUUID) {
        const socketId = this.uuidToSocket.get(minecraftUUID);
        return socketId ? this.players.get(socketId) : null;
    }

    /**
     * Update player webcam status
     */
    setWebcamStatus(socketId, active) {
        const player = this.players.get(socketId);
        if (player) {
            player.webcamActive = active;
            console.log(`[PlayerManager] Player ${player.playerName} webcam: ${active ? 'ON' : 'OFF'}`);
        }
    }

    /**
     * Get all players
     */
    getAllPlayers() {
        return Array.from(this.players.values());
    }

    /**
     * Get players in a specific room
     */
    getPlayersInRoom(roomId) {
        return Array.from(this.players.values()).filter(p => p.roomId === roomId);
    }
}
