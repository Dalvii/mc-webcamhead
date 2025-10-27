import './StatsPanel.css';

export function StatsPanel({ stats, connected }) {
  return (
    <div className="stats-panel">
      <div className={`stat-item connection-status ${connected ? 'connected' : 'disconnected'}`}>
        <span className="stat-label">Status:</span>
        <span className="stat-value">
          {connected ? '🟢 Connected' : '🔴 Disconnected'}
        </span>
      </div>

      <div className="stat-item">
        <span className="stat-label">Players:</span>
        <span className="stat-value">{stats.playersCount}</span>
      </div>

      <div className="stat-item">
        <span className="stat-label">FPS:</span>
        <span className="stat-value">{stats.fps}</span>
      </div>

      <div className="stat-item">
        <span className="stat-label">Bandwidth:</span>
        <span className="stat-value">{stats.bandwidth} KB/s</span>
      </div>

      <div className="stat-item">
        <span className="stat-label">Frames:</span>
        <span className="stat-value">{stats.framesReceived}</span>
      </div>
    </div>
  );
}
