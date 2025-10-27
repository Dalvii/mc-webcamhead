import { PlayerCard } from './PlayerCard';
import './PlayerGrid.css';

export function PlayerGrid({ players, onFullscreen }) {
  if (players.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">👥</div>
        <h2>No players connected</h2>
        <p>Waiting for players to join and activate their webcams...</p>
      </div>
    );
  }

  return (
    <div className={`player-grid player-count-${Math.min(players.length, 4)}`}>
      {players.map(player => (
        <PlayerCard
          key={player.uuid}
          player={player}
          onFullscreen={onFullscreen}
        />
      ))}
    </div>
  );
}
