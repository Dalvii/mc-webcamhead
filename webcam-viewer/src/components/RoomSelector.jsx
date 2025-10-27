import { useState, useEffect } from 'react';
import './RoomSelector.css';

export function RoomSelector({ currentRoom, onRoomChange }) {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRooms();
    const interval = setInterval(fetchRooms, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchRooms = async () => {
    try {
      const response = await fetch('http://localhost:3000/api/rooms');
      const data = await response.json();
      // Extract room IDs from room objects
      const roomIds = (data.rooms || []).map(room => room.id);
      setRooms(roomIds);
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="room-selector">
      <label htmlFor="room-select">Room:</label>
      <select
        id="room-select"
        value={currentRoom}
        onChange={(e) => onRoomChange(e.target.value)}
        disabled={loading}
      >
        <option value="default">default</option>
        {rooms.map(roomId => (
          <option key={roomId} value={roomId}>
            {roomId}
          </option>
        ))}
      </select>
      {loading && <span className="loading-spinner">⟳</span>}
    </div>
  );
}
