# Implémentation du Streaming d'Images - Terminé ✅

**Architecture**: Socket.IO avec serveur de relais Node.js (pas WebRTC P2P)

## Architecture

### Mode de Fonctionnement
Le système utilise un **streaming d'images via serveur** :
1. Joueur A active sa webcam (touche V)
2. Frames capturées → compressées en JPEG → encodées en Base64
3. Envoyées au serveur Node.js via Socket.IO
4. Serveur broadcast aux autres joueurs de la même room
5. Joueurs B,C,D reçoivent → décodent → affichent sur le skin

### Spécifications Techniques
- **Résolution** : 128x128 pixels sur le skin (1024x1024 skin texture)
- **Compression** : JPEG qualité 70% (~5-10 KB par frame)
- **Frame rate** : 10 FPS (limiteur côté client)
- **Format** : Base64 pour transport via Socket.IO
- **Latence estimée** : ~100-200ms selon réseau

## Composants Créés

### 1. Serveur Node.js (/streaming-server)

#### Fichiers
- `server.js` - Serveur Express + Socket.IO
- `src/signaling.js` - Gestion des événements (+ `video:frame`)
- `src/player-manager.js` - Tracking des joueurs
- `src/room-manager.js` - Gestion des rooms

#### Événements Socket.IO
**Client → Serveur :**
- `player:join` - Rejoindre avec UUID Minecraft
- `webcam:toggle` - Toggle webcam on/off
- `video:frame` - Envoyer une frame vidéo

**Serveur → Client :**
- `player:joined` - Confirmation + liste joueurs existants
- `player:new` - Nouveau joueur dans la room
- `player:left` - Joueur parti
- `webcam:status` - Statut webcam d'un joueur
- `video:frame` - Frame vidéo reçue d'un autre joueur

#### Démarrage
```bash
cd streaming-server
npm install
npm start
# Serveur écoute sur http://localhost:3000
```

### 2. Classes Java Côté Mod

#### SignalingClient.java
- Client Socket.IO pour Java
- Connexion au serveur de signalisation
- Gestion de tous les événements
- Callbacks pour les différents événements

#### VideoStreamClient.java
- Compression des frames en JPEG
- Encodage Base64
- Limiteur de frame rate (10 FPS)
- Décodage des frames reçues
- Statistiques de streaming

#### Modifications WebcamheadClient.java
- Initialisation du multiplayer au démarrage
- Connexion automatique au serveur signaling
- Envoi des frames locales
- Réception et affichage des frames distantes
- Gestion des joueurs qui rejoignent/quittent

#### Modifications ModConfig.java
- `SIGNALING_SERVER_URL` - URL du serveur (défaut: localhost:3000)
- `ROOM_ID` - Identifiant de la room (défaut: "default")
- `MULTIPLAYER_ENABLED` - Activer/désactiver le multiplayer

### 3. Dépendances Ajoutées (build.gradle)
```gradle
implementation 'io.socket:socket.io-client:2.1.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'org.java-websocket:Java-WebSocket:1.5.6'
```

## Flux de Données

### Activation de la Webcam
```
1. Joueur presse V
2. Webcam démarre (local)
3. SignalingClient.sendWebcamToggle(true)
4. Serveur broadcast "webcam:status" aux autres
5. Autres joueurs reçoivent la notification
```

### Streaming Vidéo
```
1. WebcamheadClient.onClientTick() [chaque tick]
2. Récupère frame depuis webcamManager
3. Met à jour le skin local
4. VideoStreamClient.sendFrame(frame)
   ↓ Compression JPEG
   ↓ Encodage Base64
   ↓ SignalingClient.sendVideoFrame(base64)
5. Serveur reçoit "video:frame"
6. Serveur broadcast aux autres joueurs de la room
7. Autres joueurs reçoivent "video:frame"
8. VideoStreamClient décode la frame
9. Callback onFrameReceived(playerUUID, frame)
10. SkinOverlayRenderer.updateSkinWithWebcam(playerUUID, frame)
```

## Configuration

### Serveur
Le serveur est configuré pour accepter toutes les origines (CORS) en développement.
Port par défaut : 3000

### Client (ModConfig)
```java
// URL du serveur de signalisation
ModConfig.setSignalingServerUrl("http://localhost:3000");

// Room ID (peut être l'IP du serveur Minecraft par exemple)
ModConfig.setRoomId("default");

// Activer/désactiver le multiplayer
ModConfig.setMultiplayerEnabled(true);
```

## Utilisation

### 1. Démarrer le serveur
```bash
cd streaming-server
npm install
npm start
```

### 2. Lancer Minecraft avec le mod
- Le mod se connecte automatiquement au serveur signaling
- Appuyer sur V pour activer la webcam
- La webcam s'affiche sur votre propre skin
- Les autres joueurs dans la même room voient votre webcam

### 3. Tester avec plusieurs clients
- Lancer plusieurs instances de Minecraft
- Tous se connectent à la même room (défaut: "default")
- Activer la webcam sur chaque instance
- Chaque joueur voit les webcams des autres sur leurs skins

## Rooms et Groupement

### Concept de Room
Une "room" regroupe les joueurs qui doivent se voir entre eux.
Par défaut, tous les joueurs sont dans la room "default".

### Personnalisation
Pour grouper par serveur Minecraft, modifier `ModConfig.setRoomId()` :
```java
// Exemple: utiliser l'IP du serveur comme room ID
String serverIP = client.getCurrentServerEntry().address;
ModConfig.setRoomId(serverIP);
```

## Performance

### Bande Passante
- Frame 128x128 JPEG @ 70% qualité ≈ 8 KB
- 10 FPS = 80 KB/s par joueur
- 5 joueurs avec webcam = 400 KB/s upload serveur

### Optimisations Possibles
1. **Réduire la résolution** : 64x64 au lieu de 128x128 (divise par 4)
2. **Réduire le FPS** : 5 FPS au lieu de 10 (divise par 2)
3. **Qualité JPEG** : 50% au lieu de 70% (réduit de ~30%)
4. **Compression Delta** : Envoyer seulement les changements (complexe)

### Limitations
- Maximum recommandé : **10 joueurs** dans une room
- Au-delà : considérer P2P WebRTC ou serveur dédié plus puissant

## Tests

### Test en Local
1. Démarrer le serveur Node.js
2. Lancer Minecraft en mode solo
3. Activer la webcam (V)
4. Vérifier les logs :
   - Serveur : "Player X joined room default"
   - Client : "Connected to signaling server"
   - Client : "Sent X frames"

### Test Multijoueur
1. Démarrer le serveur Node.js
2. Lancer 2 instances de Minecraft
3. Les deux rejoignent le même monde (LAN ou serveur)
4. Sur instance 1 : activer webcam
5. Sur instance 2 : voir la webcam de joueur 1 sur son skin
6. Sur instance 2 : activer webcam
7. Sur instance 1 : voir la webcam de joueur 2 sur son skin

## Prochaines Étapes Possibles

### Interface Web (React)
- Créer une app web pour voir les joueurs depuis un navigateur
- Socket.IO client en JavaScript
- Affichage grille de toutes les webcams actives

### Améliorations
- Ajuster qualité JPEG dynamiquement selon bande passante
- Mode "spectateur" (recevoir sans envoyer)
- Détection automatique de la room selon le serveur Minecraft
- Filtres vidéo et effets

### Migration vers WebRTC P2P (Futur)
Pour de meilleures performances avec beaucoup de joueurs :
- Remplacer le relais serveur par des connexions P2P directes
- Utiliser libjitsi ou webrtc-java côté client
- Garder le serveur Node.js uniquement pour la signalisation
- Ajouter l'audio avec codec Opus
- Gestion de la proximité 3D (volume selon distance)

## Dépannage

### "Failed to connect to signaling server"
- Vérifier que le serveur Node.js est démarré
- Vérifier l'URL dans ModConfig (défaut: http://localhost:3000)
- Vérifier que le port 3000 n'est pas bloqué par le firewall

### "No frames received from other players"
- Vérifier que les deux joueurs sont dans la même room
- Vérifier les logs serveur pour voir si les frames arrivent
- Vérifier que l'autre joueur a bien activé sa webcam

### Performance dégradée
- Réduire le nombre de joueurs dans la room
- Réduire la qualité JPEG (modifier VideoStreamClient.JPEG_QUALITY)
- Réduire le FPS (modifier VideoStreamClient.FRAME_INTERVAL_MS)

## Logs Utiles

### Serveur Node.js
```
[Server] New connection: <socketId>
[PlayerManager] Player <name> joined
[Signaling] Player <name> webcam ON
[Signaling] Forwarded <N> frames
```

### Client Minecraft
```
[WebcamHead] Connecting to signaling server at http://localhost:3000
[WebcamHead] Connected to signaling server
[WebcamHead] Joined room with <N> existing players
[WebcamHead] Sent <N> frames, avg size: <X> KB
[WebcamHead] Received <N> frames from <player>
```

## Conclusion

Le système de streaming d'images est **opérationnel et prêt à tester** ! 🎉

L'implémentation est plus simple que WebRTC P2P mais fonctionne bien pour :
- Petits groupes de joueurs (< 10)
- Réseaux locaux (LAN)
- Tests et prototypage

Pour une utilisation en production avec beaucoup de joueurs, considérer :
- Migration vers WebRTC P2P
- Serveur dédié plus puissant
- CDN pour distribuer la charge
