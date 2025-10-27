import { useState } from 'react';
import './PlayerCard.css';

export function PlayerCard({ player, onFullscreen }) {
  const [imageError, setImageError] = useState(false);

  const handleFullscreen = () => {
    if (onFullscreen) {
      onFullscreen(player);
    }
  };

  const hasFrame = player.frame && !imageError;

  return (
    <div className={`player-card ${!hasFrame ? 'no-video' : ''}`}>
      <div className="player-video">
        {hasFrame ? (
          <img
            src={player.frame}
            alt={`${player.name}'s webcam`}
            onError={() => setImageError(true)}
            onLoad={() => setImageError(false)}
          />
        ) : (
          <div className="no-video-placeholder">
            <div className="placeholder-icon">📷</div>
            <div className="placeholder-text">
              {player.webcamActive ? 'Waiting for video...' : 'Camera off'}
            </div>
          </div>
        )}
      </div>

      <div className="player-info">
        <div className="player-name">
          <span className={`status-indicator ${player.webcamActive ? 'active' : 'inactive'}`} />
          {player.name}
        </div>
        {hasFrame && (
          <button className="fullscreen-btn" onClick={handleFullscreen} title="Fullscreen">
            ⛶
          </button>
        )}
      </div>
    </div>
  );
}
