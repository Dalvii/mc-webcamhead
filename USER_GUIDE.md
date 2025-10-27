# WebcamHead Mod - User Guide

[Version française ci-dessous](#guide-utilisateur---version-française)

## Table of Contents
1. [What is WebcamHead?](#what-is-webcamhead)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Server Setup](#server-setup)
5. [Using the Mod](#using-the-mod)
6. [Commands](#commands)
7. [Troubleshooting](#troubleshooting)

---

## What is WebcamHead?

WebcamHead is a Minecraft Fabric mod that displays your webcam feed directly on your player's face in-game. Other players can see your webcam in real-time, creating a unique multiplayer experience.

**Features:**
- Real-time webcam streaming on player skins
- Multiplayer support with rooms
- Web viewer to watch all players' webcams from a browser
- Low bandwidth usage (~80 KB/s per player)
- Simple setup with Socket.IO relay server

---

## Prerequisites

### For Players
- **Minecraft**: 1.21.3
- **Fabric Loader**: 0.17.3 or higher
- **Fabric API**: 0.114.1+1.21.3 or higher
- **Java**: 21 or higher
- **Webcam**: Any USB webcam or built-in camera

### For Server Hosts
- **Node.js**: 18.0 or higher
- **npm**: 9.0 or higher
- A public IP address or domain (for remote players)
- Port 3000 open (or custom port of your choice)

---

## Installation

### Step 1: Install the Mod

1. Download the latest `webcamhead-X.X.X.jar` from the releases page
2. Copy the JAR file to your `.minecraft/mods` folder
3. Launch Minecraft with Fabric Loader

### Step 2: Set Up the Streaming Server

The streaming server relays webcam frames between players. One person needs to host it.

#### Option A: Quick Local Test (Localhost)

If you just want to test locally:

```bash
cd streaming-server
npm install
npm start
```

The server will start on `http://localhost:3000`

#### Option B: Public Server (For Remote Players)

1. **Install dependencies:**
   ```bash
   cd streaming-server
   npm install
   ```

2. **Build the web viewer:**
   ```bash
   cd ../webcam-viewer
   npm install
   npm run build
   ```

3. **Start the server:**
   ```bash
   cd ../streaming-server
   npm start
   ```

4. **Configure your firewall/router:**
   - Open port 3000 (TCP)
   - Forward port 3000 to your server machine

5. **Share your server URL:**
   - If using a domain: `http://yourdomain.com:3000`
   - If using IP: `http://your.ip.address:3000`

---

## Using the Mod

### First Time Setup

1. **Join a Minecraft world or server**

2. **Configure the streaming server:**
   ```
   /webcam server http://YOUR_SERVER_URL:3000
   ```

   Examples:
   - Local: `/webcam server http://localhost:3000`
   - Remote: `/webcam server http://example.com:3000`
   - IP: `/webcam server http://192.168.1.100:3000`

3. **Check your configuration:**
   ```
   /webcam info
   ```

### Starting Your Webcam

Press the **V** key (default keybinding) to toggle your webcam on/off.

When you activate your webcam for the first time:
- Your OS may ask for camera permission - **allow it**
- A message will confirm activation: "Webcam enabled (skin overlay mode)"
- Your webcam feed will appear on your player's face (128x128 pixels)
- Other players in the same room will see your webcam in real-time

### Changing Rooms

Rooms allow you to group players. Only players in the same room can see each other's webcams.

```
/webcam join room-name
```

Example: `/webcam join party-room`

The mod will automatically reconnect to the new room.

---

## Commands

All commands start with `/webcam`:

### `/webcam server <url>`
Configure the streaming server URL.

**Example:**
```
/webcam server http://example.com:3000
```

**Notes:**
- URL must start with `http://` or `https://`
- You must configure this before activating your webcam
- Changes take effect immediately if already connected

### `/webcam info`
Display current configuration.

**Shows:**
- Device index
- Resolution (default: 320x240)
- FPS (default: 15)
- Render mode (default: SKIN_OVERLAY)
- Multiplayer status
- Server URL (green if configured, red if not)
- Current room

### `/webcam list`
List all available webcam devices.

**Example output:**
```
Found 2 webcam device(s):
[ACTIVE] [0] FaceTime HD Camera
[1] USB Webcam
```

### `/webcam device <index>`
Select a specific webcam device.

**Example:**
```
/webcam device 1
```

Restart your webcam (press V twice) for changes to take effect.

### `/webcam state`
Show current webcam and connection state.

**Shows:**
- Webcam active: true/false
- Signaling connected: true/false
- Current room

### `/webcam stats`
Display streaming statistics (only when webcam is active).

**Shows:**
- Frames sent
- Frames received
- Bytes sent
- Average frame size

### `/webcam join <roomId>`
Switch to a different room.

**Example:**
```
/webcam join party-room
```

---

## Troubleshooting

### "Signaling Server Not Configured"

**Problem:** You tried to activate your webcam without configuring the server.

**Solution:**
```
/webcam server http://YOUR_SERVER_URL:3000
```

### Webcam Won't Start

**Possible causes:**
1. **No camera permission:** Check your OS privacy settings
2. **Camera in use:** Close other apps using your webcam
3. **Wrong device index:** Use `/webcam list` to see available devices

### Can't See Other Players' Webcams

**Check:**
1. Are you in the same room? Use `/webcam state` to verify
2. Did they activate their webcam? (They should press V)
3. Is the server running? Check `/webcam state` for connection status

### Connection Issues

**If "Signaling connected: false":**
1. Verify the server URL is correct: `/webcam info`
2. Check the server is running
3. Verify firewall/port forwarding if using remote server
4. Try reconnecting by leaving and rejoining the world

### High Bandwidth Usage

**Default usage:** ~80 KB/s upload per player

**To reduce:**
- Use fewer players per room (recommended: max 10)
- Lower FPS in config (not yet exposed as command)
- Use a dedicated server with good internet connection

---

## Web Viewer

Access the web viewer at: `http://YOUR_SERVER_URL:3000/viewer`

**Features:**
- See all players' webcams in a grid
- Switch between rooms
- Real-time statistics (FPS, bandwidth)
- Click on a player for fullscreen view
- Auto-reconnects on connection loss

---

## Advanced Configuration

For developers and advanced users, see:
- [CLAUDE.md](CLAUDE.md) - Development guide
- [STREAMING_IMPLEMENTATION.md](STREAMING_IMPLEMENTATION.md) - Streaming architecture

---

# Guide Utilisateur - Version Française

## Table des matières
1. [Qu'est-ce que WebcamHead ?](#quest-ce-que-webcamhead-)
2. [Prérequis](#prérequis)
3. [Installation](#installation-1)
4. [Configuration du serveur](#configuration-du-serveur)
5. [Utilisation du mod](#utilisation-du-mod)
6. [Commandes](#commandes-1)
7. [Dépannage](#dépannage)

---

## Qu'est-ce que WebcamHead ?

WebcamHead est un mod Fabric pour Minecraft qui affiche le flux de votre webcam directement sur le visage de votre personnage dans le jeu. Les autres joueurs peuvent voir votre webcam en temps réel, créant une expérience multijoueur unique.

**Fonctionnalités :**
- Streaming webcam en temps réel sur les skins des joueurs
- Support multijoueur avec système de rooms
- Visionneuse web pour regarder les webcams depuis un navigateur
- Faible utilisation de bande passante (~80 KB/s par joueur)
- Configuration simple avec serveur relay Socket.IO

---

## Prérequis

### Pour les joueurs
- **Minecraft** : 1.21.3
- **Fabric Loader** : 0.17.3 ou supérieur
- **Fabric API** : 0.114.1+1.21.3 ou supérieur
- **Java** : 21 ou supérieur
- **Webcam** : N'importe quelle webcam USB ou caméra intégrée

### Pour les hébergeurs de serveur
- **Node.js** : 18.0 ou supérieur
- **npm** : 9.0 ou supérieur
- Une adresse IP publique ou un domaine (pour les joueurs distants)
- Port 3000 ouvert (ou port personnalisé de votre choix)

---

## Installation

### Étape 1 : Installer le mod

1. Téléchargez le dernier `webcamhead-X.X.X.jar` depuis la page des releases
2. Copiez le fichier JAR dans votre dossier `.minecraft/mods`
3. Lancez Minecraft avec Fabric Loader

### Étape 2 : Configurer le serveur de streaming

Le serveur de streaming relaie les images webcam entre les joueurs. Une personne doit l'héberger.

#### Option A : Test local rapide (Localhost)

Si vous voulez juste tester en local :

```bash
cd streaming-server
npm install
npm start
```

Le serveur démarrera sur `http://localhost:3000`

#### Option B : Serveur public (Pour les joueurs distants)

1. **Installer les dépendances :**
   ```bash
   cd streaming-server
   npm install
   ```

2. **Compiler la visionneuse web :**
   ```bash
   cd ../webcam-viewer
   npm install
   npm run build
   ```

3. **Démarrer le serveur :**
   ```bash
   cd ../streaming-server
   npm start
   ```

4. **Configurer votre pare-feu/routeur :**
   - Ouvrir le port 3000 (TCP)
   - Rediriger le port 3000 vers votre machine serveur

5. **Partager l'URL de votre serveur :**
   - Si vous utilisez un domaine : `http://votredomaine.com:3000`
   - Si vous utilisez une IP : `http://votre.adresse.ip:3000`

---

## Utilisation du mod

### Configuration initiale

1. **Rejoignez un monde Minecraft ou un serveur**

2. **Configurez le serveur de streaming :**
   ```
   /webcam server http://URL_DE_VOTRE_SERVEUR:3000
   ```

   Exemples :
   - Local : `/webcam server http://localhost:3000`
   - Distant : `/webcam server http://exemple.com:3000`
   - IP : `/webcam server http://192.168.1.100:3000`

3. **Vérifiez votre configuration :**
   ```
   /webcam info
   ```

### Démarrer votre webcam

Appuyez sur la touche **V** (raccourci par défaut) pour activer/désactiver votre webcam.

Lorsque vous activez votre webcam pour la première fois :
- Votre OS peut demander la permission d'accès à la caméra - **autorisez-la**
- Un message confirmera l'activation : "Webcam enabled (skin overlay mode)"
- Le flux de votre webcam apparaîtra sur le visage de votre personnage (128x128 pixels)
- Les autres joueurs dans la même room verront votre webcam en temps réel

### Changer de room

Les rooms permettent de grouper les joueurs. Seuls les joueurs dans la même room peuvent voir les webcams des autres.

```
/webcam join nom-de-la-room
```

Exemple : `/webcam join party-room`

Le mod se reconnectera automatiquement à la nouvelle room.

---

## Commandes

Toutes les commandes commencent par `/webcam` :

### `/webcam server <url>`
Configure l'URL du serveur de streaming.

**Exemple :**
```
/webcam server http://exemple.com:3000
```

**Notes :**
- L'URL doit commencer par `http://` ou `https://`
- Vous devez configurer ceci avant d'activer votre webcam
- Les changements prennent effet immédiatement si déjà connecté

### `/webcam info`
Affiche la configuration actuelle.

**Affiche :**
- Index du périphérique
- Résolution (par défaut : 320x240)
- FPS (par défaut : 15)
- Mode de rendu (par défaut : SKIN_OVERLAY)
- Statut multijoueur
- URL du serveur (vert si configuré, rouge sinon)
- Room actuelle

### `/webcam list`
Liste tous les périphériques webcam disponibles.

**Exemple de sortie :**
```
Found 2 webcam device(s):
[ACTIVE] [0] FaceTime HD Camera
[1] USB Webcam
```

### `/webcam device <index>`
Sélectionne un périphérique webcam spécifique.

**Exemple :**
```
/webcam device 1
```

Redémarrez votre webcam (appuyez sur V deux fois) pour que les changements prennent effet.

### `/webcam state`
Affiche l'état actuel de la webcam et de la connexion.

**Affiche :**
- Webcam active : vrai/faux
- Signalisation connectée : vrai/faux
- Room actuelle

### `/webcam stats`
Affiche les statistiques de streaming (uniquement quand la webcam est active).

**Affiche :**
- Images envoyées
- Images reçues
- Octets envoyés
- Taille moyenne des images

### `/webcam join <roomId>`
Change de room.

**Exemple :**
```
/webcam join party-room
```

---

## Dépannage

### "Signaling Server Not Configured"

**Problème :** Vous avez essayé d'activer votre webcam sans configurer le serveur.

**Solution :**
```
/webcam server http://URL_DE_VOTRE_SERVEUR:3000
```

### La webcam ne démarre pas

**Causes possibles :**
1. **Pas de permission caméra :** Vérifiez les paramètres de confidentialité de votre OS
2. **Caméra utilisée :** Fermez les autres applications utilisant votre webcam
3. **Mauvais index de périphérique :** Utilisez `/webcam list` pour voir les périphériques disponibles

### Je ne vois pas les webcams des autres joueurs

**Vérifiez :**
1. Êtes-vous dans la même room ? Utilisez `/webcam state` pour vérifier
2. Ont-ils activé leur webcam ? (Ils doivent appuyer sur V)
3. Le serveur fonctionne-t-il ? Vérifiez `/webcam state` pour le statut de connexion

### Problèmes de connexion

**Si "Signaling connected: false" :**
1. Vérifiez que l'URL du serveur est correcte : `/webcam info`
2. Vérifiez que le serveur fonctionne
3. Vérifiez le pare-feu/redirection de port si vous utilisez un serveur distant
4. Essayez de vous reconnecter en quittant et rejoignant le monde

### Utilisation élevée de bande passante

**Utilisation par défaut :** ~80 KB/s en upload par joueur

**Pour réduire :**
- Utilisez moins de joueurs par room (recommandé : max 10)
- Réduisez les FPS dans la config (pas encore exposé en commande)
- Utilisez un serveur dédié avec une bonne connexion internet

---

## Visionneuse Web

Accédez à la visionneuse web sur : `http://URL_DE_VOTRE_SERVEUR:3000/viewer`

**Fonctionnalités :**
- Voir toutes les webcams des joueurs en grille
- Changer de room
- Statistiques en temps réel (FPS, bande passante)
- Cliquer sur un joueur pour le voir en plein écran
- Reconnexion automatique en cas de perte de connexion

---

## Configuration avancée

Pour les développeurs et utilisateurs avancés, voir :
- [CLAUDE.md](CLAUDE.md) - Guide de développement
- [STREAMING_IMPLEMENTATION.md](STREAMING_IMPLEMENTATION.md) - Architecture du streaming
