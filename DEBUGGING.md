# Guide de Debug - Streaming Multijoueur

**Note**: Ce système utilise Socket.IO pour relayer les frames vidéo via un serveur Node.js, pas WebRTC P2P.

## Problèmes Résolus ✅

1. **"Invalid video frame: player not found"**
   - Corrigé : Le serveur ignore maintenant silencieusement les frames qui arrivent avant l'enregistrement complet du joueur

2. **Commandes manquantes**
   - Ajouté : `/webcam state`, `/webcam stats`, `/webcam join <roomId>`

3. **Parsing JSON**
   - Corrigé : Envoi direct des objets JSON au lieu de les sérialiser en string

## Comment Tester le Multijoueur

### Étape 1 : Démarrer le Serveur
```bash
cd streaming-server
npm start
```

Tu devrais voir :
```
╔══════════════════════════════════════════════════════════╗
║  WebcamHead Streaming Server                            ║
║  Status: RUNNING                                         ║
║  Port: 3000                                              ║
╚══════════════════════════════════════════════════════════╝
```

### Étape 2 : Lancer 2 Instances de Minecraft

**Instance 1 :**
1. Lance Minecraft
2. Crée un monde en LAN (Échap → Ouvrir en LAN)
3. Note le port (ex: 25565)

**Instance 2 :**
1. Lance une 2ème instance de Minecraft
2. Multijoueur → Direct Connect
3. Connecte au premier joueur (localhost:25565)

### Étape 3 : Vérifier la Connexion au Serveur

Sur chaque instance, tape dans le chat :
```
/webcam state
```

Tu devrais voir :
```
=== Webcam State ===
Webcam Active: false
Signaling Connected: true  ← IMPORTANT : doit être "true"
Current Room: default
```

Si "Signaling Connected: false", vérifie que :
- Le serveur Node.js est bien démarré
- Le port 3000 n'est pas bloqué

### Étape 4 : Activer les Webcams

**Sur Instance 1 :**
1. Appuie sur **V** pour activer la webcam
2. Tu devrais voir ta webcam sur ton propre skin

**Logs serveur attendus :**
```
[PlayerManager] Player <Nom1> joined
[Signaling] Player <Nom1> webcam ON
```

**Sur Instance 2 :**
1. Tu devrais maintenant voir la webcam de Joueur 1 sur son skin
2. Appuie sur **V** pour activer ta webcam
3. Joueur 1 devrait voir ta webcam sur ton skin

**Logs serveur attendus :**
```
[PlayerManager] Player <Nom2> joined
[Signaling] Player <Nom2> webcam ON
[Signaling] Forwarded 100 frames from <Nom1> to 1 player(s)
[Signaling] Forwarded 100 frames from <Nom2> to 1 player(s)
```

### Étape 5 : Vérifier les Statistiques

Sur chaque instance :
```
/webcam stats
```

Tu devrais voir :
```
=== Streaming Statistics ===
Frames Sent: 450
Frames Received: 380
Bytes Sent: 3600 KB
Avg Frame Size: 8 KB
```

## Commandes Disponibles

### `/webcam info`
Affiche la configuration actuelle
```
=== Webcam Configuration ===
Device Index: 0
Resolution: 320x240
FPS: 15
Render Mode: PANEL_3D
Multiplayer: Enabled
Server: http://localhost:3000
Room: default
```

### `/webcam state`
Affiche l'état actuel de la webcam et de la connexion
```
=== Webcam State ===
Webcam Active: true
Signaling Connected: true
Current Room: default
```

### `/webcam stats`
Affiche les statistiques de streaming (seulement si webcam active)
```
=== Streaming Statistics ===
Frames Sent: 1250
Frames Received: 890
Bytes Sent: 10000 KB
Avg Frame Size: 8 KB
```

### `/webcam join <roomId>`
Change de room et reconnecte au serveur
```
/webcam join myserver123
```

### `/webcam list`
Liste les caméras disponibles
```
Found 2 webcam device(s):
[ACTIVE] [0] FaceTime HD Camera (1280x720)
[1] OBS Virtual Camera (1920x1080)
```

### `/webcam device <index>`
Change de caméra
```
/webcam device 1
Set webcam device index to 1
Restart your webcam (press V twice) for changes to take effect
```

## Logs à Surveiller

### Serveur Node.js

**Bon signe :**
```
[Server] New connection: abc123
[PlayerManager] Player Steve joined
[Signaling] Player Steve joined room default
[Signaling] Player Steve webcam ON
[Signaling] Forwarded 100 frames from Steve to 1 player(s)
```

**Mauvais signe :**
```
[Signaling] Invalid video frame: player not found  ← Ne devrait plus apparaître
```

### Client Minecraft (logs.txt)

**Bon signe :**
```
[WebcamHead] Connecting to signaling server at http://localhost:3000 (room: default)
[WebcamHead] Connected to signaling server
[WebcamHead] Joined room with 1 existing players
[WebcamHead] Player joined: Alex (uuid...)
[WebcamHead] Sent 100 frames, avg size: 8 KB
[WebcamHead] Received 100 frames from Alex
```

**Mauvais signe :**
```
[WebcamHead] Connection error: ...
[WebcamHead] Failed to initialize multiplayer streaming
```

## Problèmes Courants

### "Webcam Active: true mais Signaling Connected: false"
**Cause :** Le serveur Node.js n'est pas démarré ou pas accessible
**Solution :**
1. Vérifie que le serveur tourne : `cd webrtc-server && npm start`
2. Vérifie l'URL : `/webcam info` → devrait montrer `http://localhost:3000`

### "Je vois ma webcam mais pas celle des autres"
**Cause :** Les joueurs ne sont pas dans la même room
**Solution :**
1. Vérifie la room : `/webcam state` sur les deux instances
2. Assure-toi qu'ils sont dans la même room (par défaut "default")
3. Si différent, change : `/webcam join default`

### "Frames Sent > 0 mais Frames Received = 0"
**Cause :** Le serveur ne relaie pas les frames ou l'autre joueur n'a pas activé sa webcam
**Solution :**
1. Vérifie les logs serveur : "Forwarded X frames to Y player(s)"
2. Si Y = 0, aucun autre joueur dans la room
3. Assure-toi que l'autre joueur a aussi activé sa webcam (V)

### "Latence / Saccades"
**Cause :** Trop de données ou connexion lente
**Solution :**
1. Réduire la qualité : modifier `VideoStreamClient.JPEG_QUALITY` (0.5 au lieu de 0.7)
2. Réduire le FPS : modifier `VideoStreamClient.FRAME_INTERVAL_MS` (200 au lieu de 100 = 5 FPS)
3. Réduire la résolution du skin (mais déjà optimisée)

## Tests à Faire

### Test 1 : Solo (1 joueur)
- ✅ Serveur démarre
- ✅ Client se connecte au serveur
- ✅ Webcam s'affiche sur le skin local
- ✅ `/webcam state` montre "Signaling Connected: true"
- ✅ `/webcam stats` montre des frames envoyées

### Test 2 : Multijoueur Local (2 joueurs sur même PC)
- ✅ 2 instances se connectent au serveur
- ✅ Les deux voient "1 existing players"
- ✅ Joueur 1 active webcam → Joueur 2 la voit
- ✅ Joueur 2 active webcam → Joueur 1 la voit
- ✅ Serveur log "Forwarded to 1 player(s)"

### Test 3 : Rooms
- ✅ Joueur 1 dans room "room1" : `/webcam join room1`
- ✅ Joueur 2 dans room "room2" : `/webcam join room2`
- ✅ Ils ne se voient pas
- ✅ Joueur 2 rejoint room1 : `/webcam join room1`
- ✅ Maintenant ils se voient

## Informations Techniques

### Format des Frames
- Résolution : 128x128 pixels
- Format : JPEG (qualité 70%)
- Taille moyenne : ~8 KB par frame
- Encodage : Base64 pour transport
- Frame rate : 10 FPS (100ms entre frames)

### Bande Passante
- 1 joueur : ~80 KB/s upload
- 2 joueurs : ~160 KB/s total (80 up + 80 down chacun)
- 5 joueurs : ~400 KB/s au serveur

### Architecture
```
Joueur A                    Serveur Node.js                 Joueur B
   |                              |                              |
   |--[video:frame]-------------->|                              |
   |  (JPEG Base64)               |--[video:frame]-------------->|
   |                              |  (relayé)                    |
   |                              |                              |
   |                              |<--[video:frame]--------------|
   |<--[video:frame]--------------|                              |
   |  (relayé)                    |                              |
```

## Prochaines Étapes

Si tout fonctionne :
1. Tester avec plus de joueurs (3-5)
2. Optimiser la qualité/bande passante si nécessaire
3. Créer l'interface web React (optionnel)
4. Ajouter des effets visuels sur les skins (optionnel)

Si ça ne fonctionne pas :
1. Partage les logs serveur + client
2. Essaie les commandes de debug ci-dessus
3. Vérifie que les deux joueurs sont bien dans la même room
