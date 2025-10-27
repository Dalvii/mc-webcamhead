import { useEffect } from 'react';
import './FullscreenModal.css';

export function FullscreenModal({ player, onClose }) {
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  if (!player) return null;

  return (
    <div className="fullscreen-modal" onClick={onClose}>
      <div className="fullscreen-content" onClick={(e) => e.stopPropagation()}>
        <button className="close-btn" onClick={onClose} title="Close (ESC)">
          ✕
        </button>

        <div className="fullscreen-player-name">{player.name}</div>

        <div className="fullscreen-video">
          {player.frame ? (
            <img src={player.frame} alt={`${player.name}'s webcam`} />
          ) : (
            <div className="no-video-message">No video available</div>
          )}
        </div>
      </div>
    </div>
  );
}
