# WebcamHead WebRTC Signaling Server

Serveur de signalisation WebRTC pour le mod Minecraft WebcamHead.

## Installation

```bash
cd webrtc-server
npm install
```

## Démarrage

### Production
```bash
npm start
```

### Développement (avec auto-reload)
```bash
npm run dev
```

Le serveur démarre sur le port **3000** par défaut.

## API REST

### Health Check
```
GET /api/health
```

### Liste des rooms
```
GET /api/rooms
```

### Joueurs dans une room
```
GET /api/rooms/:roomId/players
```

### Rejoindre (pré-validation)
```
POST /api/join
Body: {
  "minecraftUUID": "uuid",
  "playerName": "name",
  "roomId": "default"
}
```

## Événements Socket.IO

### Client → Serveur

- `player:join` - Rejoindre avec UUID Minecraft
  ```json
  {
    "minecraftUUID": "uuid",
    "playerName": "name",
    "roomId": "default"
  }
  ```

- `webcam:toggle` - Toggle webcam
  ```json
  { "active": true }
  ```

- `webrtc:offer` - Envoyer une offre SDP
  ```json
  {
    "targetUUID": "uuid",
    "offer": { /* SDP offer */ }
  }
  ```

- `webrtc:answer` - Répondre à une offre
  ```json
  {
    "targetUUID": "uuid",
    "answer": { /* SDP answer */ }
  }
  ```

- `webrtc:ice-candidate` - Envoyer ICE candidate
  ```json
  {
    "targetUUID": "uuid",
    "candidate": { /* ICE candidate */ }
  }
  ```

### Serveur → Client

- `player:joined` - Confirmation de join + liste des joueurs
- `player:new` - Nouveau joueur dans la room
- `player:left` - Joueur parti
- `webcam:status` - Statut webcam d'un joueur
- `webrtc:offer` - Offre SDP reçue
- `webrtc:answer` - Réponse SDP reçue
- `webrtc:ice-candidate` - ICE candidate reçu

## Configuration

Variables d'environnement :
- `PORT` - Port du serveur (défaut: 3000)

## Architecture

```
server.js              # Point d'entrée
src/
  player-manager.js    # Gestion des joueurs
  room-manager.js      # Gestion des rooms
  signaling.js         # Logique de signalisation WebRTC
public/                # Interface web (à venir)
```
