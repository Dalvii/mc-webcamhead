# WebcamHead Streaming Server - Docker Deployment

Official Docker image for the WebcamHead video streaming relay server.

## Quick Start

### Pull and Run from Docker Hub

```bash
# Pull the latest image
docker pull <dockerhub-username>/webcamhead-server:latest

# Run the server
docker run -d \
  --name webcamhead-server \
  -p 3000:3000 \
  --restart unless-stopped \
  <dockerhub-username>/webcamhead-server:latest
```

### Using Docker Compose

1. Create a `.env` file (optional):
```env
PORT=3000
DOCKER_USERNAME=your-dockerhub-username
```

2. Run with docker-compose:
```bash
docker-compose up -d
```

3. View logs:
```bash
docker-compose logs -f
```

4. Stop the server:
```bash
docker-compose down
```

## Available Tags

- `latest` - Latest stable build from master branch
- `v1.0.0` - Specific version (semantic versioning)
- `v1.0.0.123` - Version with build number
- `master-<sha>` - Specific commit SHA

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3000` | Server port |
| `NODE_ENV` | `production` | Node environment |

### Custom Port

```bash
docker run -d \
  -p 8080:3000 \
  -e PORT=3000 \
  <dockerhub-username>/webcamhead-server:latest
```

## Health Check

The container includes a built-in health check that monitors the `/api/health` endpoint.

Check health status:
```bash
docker inspect --format='{{json .State.Health}}' webcamhead-server
```

## Endpoints

Once running, the server provides:

- **WebSocket**: `ws://localhost:3000`
- **API**: `http://localhost:3000/api`
  - `GET /api/health` - Health check
  - `GET /api/rooms` - List all rooms
  - `GET /api/rooms/:roomId/players` - List players in a room
- **Web Viewer**: `http://localhost:3000/viewer`

## Building Locally

If you want to build the image yourself:

```bash
# From the project root
cd streaming-server

# Build the image
docker build -t webcamhead-server .

# Run your local build
docker run -d -p 3000:3000 webcamhead-server
```

## Multi-Platform Support

The official images support both AMD64 and ARM64 architectures:

- `linux/amd64` - Intel/AMD processors
- `linux/arm64` - ARM processors (Raspberry Pi, Apple Silicon, etc.)

Docker will automatically pull the correct architecture for your system.

## Logs

View container logs:
```bash
# Follow logs
docker logs -f webcamhead-server

# Last 100 lines
docker logs --tail 100 webcamhead-server
```

## Troubleshooting

### Container won't start

Check logs for errors:
```bash
docker logs webcamhead-server
```

### Port already in use

Change the host port:
```bash
docker run -d -p 8080:3000 webcamhead-server
```

### Cannot connect from Minecraft mod

Make sure:
1. The container is running: `docker ps`
2. Port 3000 is accessible (check firewall)
3. Use the correct server URL in mod config

## Development

For development with hot reload:

```bash
# Install dependencies
npm install

# Run in development mode
npm run dev
```

## CI/CD

The Docker image is automatically built and published by GitHub Actions on every push to master.

Build triggers:
- Changes to `streaming-server/` directory
- Changes to `webcam-viewer/` directory (frontend)
- Manual workflow dispatch

## License

MIT
