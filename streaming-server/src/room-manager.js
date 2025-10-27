/**
 * Manages rooms (game servers) and player assignments
 */
export class RoomManager {
    constructor() {
        // Map of roomId -> room data
        this.rooms = new Map();
    }

    /**
     * Create or get a room
     */
    getOrCreateRoom(roomId) {
        if (!this.rooms.has(roomId)) {
            const room = {
                id: roomId,
                players: new Set(),
                createdAt: Date.now()
            };
            this.rooms.set(roomId, room);
            console.log(`[RoomManager] Room created: ${roomId}`);
        }
        return this.rooms.get(roomId);
    }

    /**
     * Add a player to a room
     */
    addPlayerToRoom(roomId, socketId) {
        const room = this.getOrCreateRoom(roomId);
        room.players.add(socketId);
        console.log(`[RoomManager] Player ${socketId} joined room ${roomId} (${room.players.size} players)`);
        return room;
    }

    /**
     * Remove a player from their room
     */
    removePlayerFromRoom(socketId) {
        for (const [roomId, room] of this.rooms.entries()) {
            if (room.players.has(socketId)) {
                room.players.delete(socketId);
                console.log(`[RoomManager] Player ${socketId} left room ${roomId} (${room.players.size} players remaining)`);

                // Clean up empty rooms
                if (room.players.size === 0) {
                    this.rooms.delete(roomId);
                    console.log(`[RoomManager] Room ${roomId} deleted (empty)`);
                }
                return roomId;
            }
        }
        return null;
    }

    /**
     * Get all socket IDs in a room
     */
    getPlayersInRoom(roomId) {
        const room = this.rooms.get(roomId);
        return room ? Array.from(room.players) : [];
    }

    /**
     * Get room for a socket ID
     */
    getRoomForPlayer(socketId) {
        for (const [roomId, room] of this.rooms.entries()) {
            if (room.players.has(socketId)) {
                return roomId;
            }
        }
        return null;
    }

    /**
     * Get all rooms
     */
    getAllRooms() {
        return Array.from(this.rooms.entries()).map(([id, room]) => ({
            id,
            playerCount: room.players.size,
            createdAt: room.createdAt
        }));
    }
}
