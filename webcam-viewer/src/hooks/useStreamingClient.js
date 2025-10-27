import { useState, useEffect, useRef } from 'react';
import { io } from 'socket.io-client';

// Use same origin when deployed, localhost:3000 for development
const SERVER_URL = import.meta.env.DEV ? 'http://localhost:3000' : window.location.origin;

export function useStreamingClient(roomId = 'default') {
  const [players, setPlayers] = useState({});
  const [connected, setConnected] = useState(false);
  const [stats, setStats] = useState({
    playersCount: 0,
    framesReceived: 0,
    fps: 0,
    bandwidth: 0
  });

  const socketRef = useRef(null);
  const frameCountRef = useRef(0);
  const lastFpsUpdateRef = useRef(Date.now());
  const frameSizesRef = useRef([]);

  useEffect(() => {
    // Create socket connection
    const socket = io(SERVER_URL, {
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000
    });

    socketRef.current = socket;

    // Connection events
    socket.on('connect', () => {
      console.log('[WebViewer] Connected to server');
      setConnected(true);

      // Join the Socket.IO room to receive broadcasts
      // We use a fake player UUID for the web viewer
      const viewerData = {
        minecraftUUID: 'web-viewer-' + socket.id,
        playerName: 'WebViewer',
        roomId: roomId
      };

      socket.emit('player:join', JSON.stringify(viewerData));
      console.log('[WebViewer] Joined room:', roomId);
    });

    socket.on('disconnect', () => {
      console.log('[WebViewer] Disconnected from server');
      setConnected(false);
    });

    socket.on('connect_error', (error) => {
      console.error('[WebViewer] Connection error:', error);
      setConnected(false);
    });

    // Player events
    socket.on('player:joined', (data) => {
      if (typeof data === 'string') data = JSON.parse(data);
      const { existingPlayers } = data;

      console.log('[WebViewer] Joined room with', existingPlayers?.length || 0, 'existing players');

      // Add existing players to state
      if (existingPlayers && existingPlayers.length > 0) {
        const newPlayers = {};
        existingPlayers.forEach(player => {
          newPlayers[player.minecraftUUID] = {
            uuid: player.minecraftUUID,
            name: player.playerName,
            webcamActive: player.webcamActive || false,
            frame: null,
            lastUpdate: Date.now()
          };
        });
        setPlayers(newPlayers);
      }
    });

    socket.on('player:new', (data) => {
      if (typeof data === 'string') data = JSON.parse(data);
      const { player } = data;

      console.log('[WebViewer] New player:', player.playerName);

      setPlayers(prev => ({
        ...prev,
        [player.minecraftUUID]: {
          uuid: player.minecraftUUID,
          name: player.playerName,
          webcamActive: player.webcamActive || false,
          frame: null,
          lastUpdate: Date.now()
        }
      }));
    });

    socket.on('player:left', (data) => {
      if (typeof data === 'string') data = JSON.parse(data);
      const { minecraftUUID } = data;

      console.log('[WebViewer] Player left:', minecraftUUID);

      setPlayers(prev => {
        const newPlayers = { ...prev };
        delete newPlayers[minecraftUUID];
        return newPlayers;
      });
    });

    socket.on('webcam:status', (data) => {
      if (typeof data === 'string') data = JSON.parse(data);
      const { minecraftUUID, active } = data;

      console.log('[WebViewer] Webcam status:', minecraftUUID, active);

      setPlayers(prev => ({
        ...prev,
        [minecraftUUID]: {
          ...prev[minecraftUUID],
          webcamActive: active
        }
      }));
    });

    // Video frame events
    socket.on('video:frame', (data) => {
      if (typeof data === 'string') data = JSON.parse(data);
      const { fromUUID, fromName, frameData } = data;

      // Update frame count for FPS calculation
      frameCountRef.current++;

      // Track frame size for bandwidth calculation
      const frameSize = frameData.length * 0.75; // Base64 to bytes approximation
      frameSizesRef.current.push(frameSize);
      if (frameSizesRef.current.length > 100) {
        frameSizesRef.current.shift();
      }

      // Update player frame
      setPlayers(prev => {
        const player = prev[fromUUID] || {
          uuid: fromUUID,
          name: fromName,
          webcamActive: true
        };

        return {
          ...prev,
          [fromUUID]: {
            ...player,
            frame: `data:image/jpeg;base64,${frameData}`,
            lastUpdate: Date.now()
          }
        };
      });
    });

    // Update stats every second
    const statsInterval = setInterval(() => {
      const now = Date.now();
      const elapsed = (now - lastFpsUpdateRef.current) / 1000;

      if (elapsed > 0) {
        const fps = Math.round(frameCountRef.current / elapsed);
        const avgFrameSize = frameSizesRef.current.length > 0
          ? frameSizesRef.current.reduce((a, b) => a + b, 0) / frameSizesRef.current.length
          : 0;
        const bandwidth = Math.round((avgFrameSize * fps) / 1024); // KB/s

        setStats({
          playersCount: Object.keys(players).length,
          framesReceived: frameCountRef.current,
          fps,
          bandwidth
        });

        frameCountRef.current = 0;
        lastFpsUpdateRef.current = now;
      }
    }, 1000);

    // Cleanup
    return () => {
      clearInterval(statsInterval);
      socket.disconnect();
    };
  }, [roomId]);

  return {
    players: Object.values(players),
    connected,
    stats
  };
}
