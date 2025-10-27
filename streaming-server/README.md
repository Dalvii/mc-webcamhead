# WebcamHead Streaming Server

Serveur de streaming vidéo Socket.IO pour le mod Minecraft WebcamHead.

**Note**: Ce serveur relaye les frames vidéo entre les joueurs via Socket.IO. Il ne s'agit pas d'un serveur WebRTC P2P classique, mais d'un relais centralisé plus simple à implémenter.

## Installation

```bash
cd streaming-server
npm install
```

## Démarrage

### Production
```bash
npm start
```

Le serveur démarre sur le port **3000** par défaut et sert :
- **WebSocket** : `ws://localhost:3000`
- **API REST** : `http://localhost:3000/api`
- **Web Viewer** : `http://localhost:3000/viewer`

### Développement (avec auto-reload)
```bash
npm run dev
```

### Première utilisation

1. **Compiler le web viewer** (interface web React)
   ```bash
   cd ../webcam-viewer
   npm install
   npm run build
   ```
   Les fichiers compilés seront automatiquement placés dans `streaming-server/public/viewer/`

2. **Démarrer le serveur**
   ```bash
   cd ../streaming-server
   npm install
   npm start
   ```

3. **Accéder au viewer**
   Ouvrez votre navigateur sur `http://localhost:3000/viewer`

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

- `video:frame` - Envoyer une frame vidéo
  ```json
  {
    "frameData": "base64-encoded-jpeg"
  }
  ```

### Serveur → Client

- `player:joined` - Confirmation de join + liste des joueurs
- `player:new` - Nouveau joueur dans la room
- `player:left` - Joueur parti
- `webcam:status` - Statut webcam d'un joueur
- `video:frame` - Frame vidéo reçue d'un autre joueur
  ```json
  {
    "fromUUID": "uuid",
    "fromName": "PlayerName",
    "frameData": "base64-encoded-jpeg"
  }
  ```

## Configuration

Variables d'environnement :
- `PORT` - Port du serveur (défaut: 3000)

## Architecture

```
server.js              # Point d'entrée (Express + Socket.IO)
src/
  player-manager.js    # Gestion des joueurs connectés
  room-manager.js      # Gestion des rooms de streaming
  signaling.js         # Logique de relais vidéo et événements Socket.IO
public/
  viewer/              # Interface web React (compilée depuis ../webcam-viewer)
```

## Web Viewer

Le Web Viewer est une interface React permettant de visualiser en temps réel tous les webcams des joueurs connectés.

### Fonctionnalités
- **Grille de joueurs** : Affichage en grille responsive de tous les joueurs avec webcam active
- **Sélection de room** : Dropdown pour changer de room en direct
- **Statistiques** : FPS, bande passante, nombre de joueurs
- **Mode plein écran** : Clic sur un joueur pour voir sa webcam en grand
- **Reconnexion automatique** : Se reconnecte automatiquement en cas de déconnexion

### Développement du viewer
```bash
cd ../webcam-viewer
npm install
npm run dev  # Démarre sur http://localhost:5173
```

Le mode développement utilise des proxies pour communiquer avec le serveur sur le port 3000.

## Performance

- **Bande passante**: ~80 KB/s par joueur en upload, ~80*(N-1) KB/s en download pour N joueurs
- **Recommandé**: Maximum 10 joueurs par room
- **Format vidéo**: JPEG Base64, 128x128px, 10 FPS, qualité 70%
- **Latence**: ~100-200ms selon le réseau

## Future: Migration vers WebRTC P2P

Pour améliorer les performances avec beaucoup de joueurs, une migration vers WebRTC P2P est envisageable :
- Le serveur ne servirait que pour la signalisation initiale
- Les connexions vidéo seraient P2P directes entre joueurs
- Réduction drastique de la charge serveur
- Meilleure latence
