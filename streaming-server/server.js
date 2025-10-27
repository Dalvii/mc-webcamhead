import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { PlayerManager } from './src/player-manager.js';
import { RoomManager } from './src/room-manager.js';
import { SignalingManager } from './src/signaling.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const httpServer = createServer(app);

// Configure Socket.IO with CORS
const io = new Server(httpServer, {
    cors: {
        origin: "*", // Allow all origins for development
        methods: ["GET", "POST"]
    }
});

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Initialize managers
const playerManager = new PlayerManager();
const roomManager = new RoomManager();
const signalingManager = new SignalingManager(io, playerManager, roomManager);

// REST API endpoints
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        players: playerManager.getAllPlayers().length,
        rooms: roomManager.getAllRooms().length
    });
});

app.get('/api/rooms', (req, res) => {
    res.json({
        rooms: roomManager.getAllRooms()
    });
});

app.get('/api/rooms/:roomId/players', (req, res) => {
    const { roomId } = req.params;
    const players = roomManager.getPlayersInRoom(roomId)
        .map(socketId => playerManager.getPlayer(socketId))
        .filter(p => p);

    res.json({
        roomId,
        players: players.map(p => ({
            minecraftUUID: p.minecraftUUID,
            playerName: p.playerName,
            webcamActive: p.webcamActive,
            connectedAt: p.connectedAt
        }))
    });
});

app.post('/api/join', (req, res) => {
    const { minecraftUUID, playerName, roomId = 'default' } = req.body;

    if (!minecraftUUID || !playerName) {
        return res.status(400).json({
            error: 'Missing minecraftUUID or playerName'
        });
    }

    res.json({
        success: true,
        roomId,
        message: 'Connect via WebSocket to complete join'
    });
});

// Serve web viewer on /viewer route
const viewerPath = path.join(__dirname, 'public', 'viewer');
app.use('/viewer', express.static(viewerPath));

// SPA fallback for /viewer routes - serve index.html for any unmatched /viewer/* routes
app.get('/viewer/*', (req, res) => {
    res.sendFile(path.join(viewerPath, 'index.html'));
});

// Socket.IO connection handling
io.on('connection', (socket) => {
    console.log(`[Server] New connection: ${socket.id}`);

    // Setup signaling handlers
    signalingManager.setupHandlers(socket);

    socket.on('error', (error) => {
        console.error(`[Server] Socket error for ${socket.id}:`, error);
    });
});

// Start server
const PORT = process.env.PORT || 3000;
httpServer.listen(PORT, () => {
    console.log(`
╔══════════════════════════════════════════════════════════╗
║  WebcamHead Video Streaming Server                      ║
║  Status: RUNNING                                         ║
║  Port: ${PORT}                                             ║
║  WebSocket: ws://localhost:${PORT}                        ║
║  API: http://localhost:${PORT}/api                        ║
║  Web Viewer: http://localhost:${PORT}/viewer              ║
╚══════════════════════════════════════════════════════════╝
    `);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('[Server] SIGTERM received, shutting down gracefully...');
    httpServer.close(() => {
        console.log('[Server] Server closed');
        process.exit(0);
    });
});
