import { useState } from 'react';
import { useStreamingClient } from './hooks/useStreamingClient';
import { PlayerGrid } from './components/PlayerGrid';
import { RoomSelector } from './components/RoomSelector';
import { StatsPanel } from './components/StatsPanel';
import { FullscreenModal } from './components/FullscreenModal';
import './App.css';

function App() {
  const [currentRoom, setCurrentRoom] = useState('default');
  const [fullscreenPlayer, setFullscreenPlayer] = useState(null);

  const { players, connected, stats } = useStreamingClient(currentRoom);

  const handleRoomChange = (newRoom) => {
    setCurrentRoom(newRoom);
  };

  const handleFullscreen = (player) => {
    setFullscreenPlayer(player);
  };

  const handleCloseFullscreen = () => {
    setFullscreenPlayer(null);
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1>📹 WebcamHead Viewer</h1>
          <RoomSelector currentRoom={currentRoom} onRoomChange={handleRoomChange} />
        </div>

        <StatsPanel stats={stats} connected={connected} />
      </header>

      <main className="app-main">
        <PlayerGrid players={players} onFullscreen={handleFullscreen} />
      </main>

      {fullscreenPlayer && (
        <FullscreenModal player={fullscreenPlayer} onClose={handleCloseFullscreen} />
      )}

      <footer className="app-footer">
        <p>
          WebcamHead Streaming Viewer · Connected to{' '}
          <code>localhost:3000</code> · Room: <code>{currentRoom}</code>
        </p>
      </footer>
    </div>
  );
}

export default App;
