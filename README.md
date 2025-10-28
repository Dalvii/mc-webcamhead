# WebcamHead - Minecraft Webcam Streaming Mod

<div align="center">

**Display your webcam feed on your Minecraft player's face in real-time!**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.3-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.17.3-orange.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Node.js](https://img.shields.io/badge/Node.js-18+-green.svg)](https://nodejs.org/)

[English](#english) | [Français](#français)

</div>

---

## English

### Overview

WebcamHead is a Fabric mod for Minecraft 1.21.3 that allows players to stream their webcam feed directly onto their in-game player skin. Other players can see your live webcam feed on your character's face, creating a unique and immersive multiplayer experience.

**Key Features:**
- **Real-time webcam streaming** on player skins (128x128 resolution)
- **Multiplayer support** with room-based streaming
- **Web viewer** to watch all players from a browser
- **Low bandwidth** (~80 KB/s per player)
- **Easy setup** with Socket.IO relay server
- **Auto-disconnect** when leaving worlds
- **Multiple camera support**

### Screenshots

[Add screenshots here]

### Quick Start

1. **Install the mod** (requires Fabric Loader and Fabric API)
2. **Start the streaming server:**
   ```bash
   cd streaming-server
   npm install && npm start
   ```
3. **Configure in-game:**
   ```
   /webcam server http://localhost:3000
   ```
4. **Press V** to toggle your webcam

For detailed instructions, see [USER_GUIDE.md](USER_GUIDE.md)

### Project Structure

```
mc-webcamhead/
├── src/                          # Minecraft mod source code (Java)
│   ├── client/                   # Client-side code
│   │   ├── webcam/              # Webcam capture (JavaCV)
│   │   ├── streaming/           # Socket.IO client
│   │   ├── render/              # Skin overlay rendering
│   │   └── command/             # In-game commands
│   └── main/                    # Common code
├── streaming-server/             # Node.js Socket.IO relay server
│   ├── src/
│   │   ├── player-manager.js   # Player state management
│   │   ├── room-manager.js     # Room management
│   │   └── signaling.js        # Video relay logic
│   └── public/viewer/          # Compiled web viewer
├── webcam-viewer/               # React web viewer (source)
│   └── src/
│       ├── components/         # React components
│       └── hooks/              # Socket.IO hook
└── USER_GUIDE.md               # Complete user guide
```

### Documentation

- **[USER_GUIDE.md](USER_GUIDE.md)** - Complete installation and usage guide (English + Français)
- **[CLAUDE.md](CLAUDE.md)** - Development guide for contributors
- **[STREAMING_IMPLEMENTATION.md](STREAMING_IMPLEMENTATION.md)** - Streaming architecture details
- **[streaming-server/README.md](streaming-server/README.md)** - Server documentation

### Requirements

#### For Players
- Minecraft 1.21.3
- Fabric Loader 0.17.3+
- Fabric API 0.114.1+1.21.3+
- Java 21+
- Any webcam (USB or built-in)

#### For Server Hosts
- Node.js 18+
- npm 9+
- Open port 3000 (or custom port)

### Installation

#### Mod Installation
1. Download the latest release from the [Releases page](../../releases)
2. Install Fabric Loader and Fabric API
3. Copy `webcamhead-X.X.X.jar` to `.minecraft/mods/`
4. Launch Minecraft

#### macOS Camera Permissions ⚠️

**KNOWN ISSUE:** Due to macOS security restrictions, the camera permission popup does not appear when using the official Minecraft launcher. This is a macOS limitation, not a mod bug.

**Solutions:**

1. **Use Prism Launcher (Recommended for End Users):**
   - Download: [Prism Launcher](https://prismlauncher.org/)
   - ✅ **CONFIRMED WORKING** - Properly triggers the camera permission popup
   - Create a Fabric instance, add the mod, and launch
   - When you press V, macOS will ask for camera permission
   - Click "Allow" and the webcam will work perfectly!

2. **Use Development Mode (Recommended for Development):**
   ```bash
   ./gradlew runClient  # Works perfectly! ✅
   ```

3. **Other Alternative Launchers:**
   - [MultiMC](https://multimc.org/) (may also work)

**Why this happens:** macOS requires applications to be proper .app bundles with Info.plist to request camera access. The official Minecraft launcher's Java runtime doesn't meet this requirement, but Prism Launcher does.

**For more details:** See [MACOS_CAMERA_FIX.md](MACOS_CAMERA_FIX.md)

#### Server Setup
```bash
# Install and start the streaming server
cd streaming-server
npm install
npm start

# Build the web viewer (optional)
cd ../webcam-viewer
npm install
npm run build
```

Server will start on `http://localhost:3000`

### Commands

| Command | Description |
|---------|-------------|
| `/webcam server <url>` | Configure streaming server URL |
| `/webcam info` | Show current configuration |
| `/webcam list` | List available webcam devices |
| `/webcam device <index>` | Select a specific camera |
| `/webcam state` | Show connection state |
| `/webcam stats` | Display streaming statistics |
| `/webcam join <roomId>` | Switch to a different room |

**Key binding:** Press **V** to toggle webcam on/off

### Web Viewer

Access the web viewer at `http://YOUR_SERVER:3000/viewer` to see all players' webcams in a grid layout with real-time statistics.

### Building from Source

```bash
# Build the mod
./gradlew build

# Run Minecraft client with the mod
./gradlew runClient

# Build the web viewer
cd webcam-viewer
npm install
npm run build
```

### Contributing

Contributions are welcome! Please see [CLAUDE.md](CLAUDE.md) for development setup and guidelines.

### Releasing

This project uses automated releases via GitHub Actions. See [VERSION_GUIDE.md](VERSION_GUIDE.md) for details on version management and release process.

### License

[Add your license here]

### Credits

- Built with [Fabric](https://fabricmc.net/)
- Uses [JavaCV](https://github.com/bytedeco/javacv) for webcam capture
- Streaming powered by [Socket.IO](https://socket.io/)
- Web viewer built with [React](https://react.dev/)

---

## Français

### Présentation

WebcamHead est un mod Fabric pour Minecraft 1.21.3 qui permet aux joueurs de diffuser le flux de leur webcam directement sur le skin de leur personnage en jeu. Les autres joueurs peuvent voir votre webcam en direct sur le visage de votre personnage, créant une expérience multijoueur unique et immersive.

**Fonctionnalités principales :**
- **Streaming webcam en temps réel** sur les skins des joueurs (résolution 128x128)
- **Support multijoueur** avec streaming par room
- **Visionneuse web** pour regarder tous les joueurs depuis un navigateur
- **Faible bande passante** (~80 KB/s par joueur)
- **Configuration facile** avec serveur relay Socket.IO
- **Déconnexion automatique** en quittant les mondes
- **Support multi-caméras**

### Démarrage rapide

1. **Installez le mod** (nécessite Fabric Loader et Fabric API)
2. **Démarrez le serveur de streaming :**
   ```bash
   cd streaming-server
   npm install && npm start
   ```
3. **Configurez en jeu :**
   ```
   /webcam server http://localhost:3000
   ```
4. **Appuyez sur V** pour activer/désactiver votre webcam

Pour des instructions détaillées, voir [USER_GUIDE.md](USER_GUIDE.md)

### Structure du projet

```
mc-webcamhead/
├── src/                          # Code source du mod Minecraft (Java)
│   ├── client/                   # Code côté client
│   │   ├── webcam/              # Capture webcam (JavaCV)
│   │   ├── streaming/           # Client Socket.IO
│   │   ├── render/              # Rendu overlay skin
│   │   └── command/             # Commandes en jeu
│   └── main/                    # Code commun
├── streaming-server/             # Serveur relay Socket.IO (Node.js)
│   ├── src/
│   │   ├── player-manager.js   # Gestion état des joueurs
│   │   ├── room-manager.js     # Gestion des rooms
│   │   └── signaling.js        # Logique relay vidéo
│   └── public/viewer/          # Visionneuse web compilée
├── webcam-viewer/               # Visionneuse web React (source)
│   └── src/
│       ├── components/         # Composants React
│       └── hooks/              # Hook Socket.IO
└── USER_GUIDE.md               # Guide utilisateur complet
```

### Documentation

- **[USER_GUIDE.md](USER_GUIDE.md)** - Guide complet d'installation et d'utilisation (English + Français)
- **[CLAUDE.md](CLAUDE.md)** - Guide de développement pour contributeurs
- **[STREAMING_IMPLEMENTATION.md](STREAMING_IMPLEMENTATION.md)** - Détails architecture streaming
- **[streaming-server/README.md](streaming-server/README.md)** - Documentation serveur

### Prérequis

#### Pour les joueurs
- Minecraft 1.21.3
- Fabric Loader 0.17.3+
- Fabric API 0.114.1+1.21.3+
- Java 21+
- N'importe quelle webcam (USB ou intégrée)

#### Pour les hébergeurs de serveur
- Node.js 18+
- npm 9+
- Port 3000 ouvert (ou port personnalisé)

### Installation

#### Installation du mod
1. Téléchargez la dernière version depuis la [page Releases](../../releases)
2. Installez Fabric Loader et Fabric API
3. Copiez `webcamhead-X.X.X.jar` dans `.minecraft/mods/`
4. Lancez Minecraft

#### Permissions Caméra macOS ⚠️

**PROBLÈME CONNU :** En raison des restrictions de sécurité macOS, le popup de permission caméra n'apparaît pas avec le launcher Minecraft officiel. C'est une limitation macOS, pas un bug du mod.

**Solutions :**

1. **Utiliser Prism Launcher (Recommandé pour Utilisateurs Finaux) :**
   - Télécharger : [Prism Launcher](https://prismlauncher.org/)
   - ✅ **CONFIRMÉ FONCTIONNEL** - Déclenche correctement le popup de permission caméra
   - Créez une instance Fabric, ajoutez le mod, et lancez
   - Quand vous appuyez sur V, macOS demandera la permission caméra
   - Cliquez sur "Autoriser" et la webcam fonctionnera parfaitement !

2. **Utiliser le Mode Développement (Recommandé pour Développement) :**
   ```bash
   ./gradlew runClient  # Fonctionne parfaitement ! ✅
   ```

3. **Autres Launchers Alternatifs :**
   - [MultiMC](https://multimc.org/) (peut aussi fonctionner)

**Pourquoi cela arrive :** macOS exige que les applications soient des bundles .app avec Info.plist pour demander l'accès caméra. Le runtime Java du launcher Minecraft officiel ne répond pas à cette exigence, mais Prism Launcher oui.

**Pour plus de détails :** Voir [MACOS_CAMERA_FIX.md](MACOS_CAMERA_FIX.md)

#### Configuration du serveur
```bash
# Installer et démarrer le serveur de streaming
cd streaming-server
npm install
npm start

# Compiler la visionneuse web (optionnel)
cd ../webcam-viewer
npm install
npm run build
```

Le serveur démarre sur `http://localhost:3000`

### Commandes

| Commande | Description |
|----------|-------------|
| `/webcam server <url>` | Configurer l'URL du serveur de streaming |
| `/webcam info` | Afficher la configuration actuelle |
| `/webcam list` | Lister les webcams disponibles |
| `/webcam device <index>` | Sélectionner une caméra spécifique |
| `/webcam state` | Afficher l'état de connexion |
| `/webcam stats` | Afficher les statistiques de streaming |
| `/webcam join <roomId>` | Changer de room |

**Raccourci clavier :** Appuyez sur **V** pour activer/désactiver la webcam

### Visionneuse Web

Accédez à la visionneuse web sur `http://VOTRE_SERVEUR:3000/viewer` pour voir toutes les webcams des joueurs en grille avec statistiques temps réel.

### Compiler depuis les sources

```bash
# Compiler le mod
./gradlew build

# Lancer le client Minecraft avec le mod
./gradlew runClient

# Compiler la visionneuse web
cd webcam-viewer
npm install
npm run build
```

### Contribuer

Les contributions sont les bienvenues ! Voir [CLAUDE.md](CLAUDE.md) pour la configuration de développement et les directives.

### Licence

[Ajoutez votre licence ici]

### Crédits

- Construit avec [Fabric](https://fabricmc.net/)
- Utilise [JavaCV](https://github.com/bytedeco/javacv) pour la capture webcam
- Streaming propulsé par [Socket.IO](https://socket.io/)
- Visionneuse web construite avec [React](https://react.dev/)
