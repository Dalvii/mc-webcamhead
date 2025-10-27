package com.dalvi.webcamhead.client.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages video streaming - sending local frames and receiving remote frames
 */
public class VideoStreamClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");

    private final SignalingClient signalingClient;
    private BiConsumer<UUID, BufferedImage> onFrameReceived;

    // Frame rate control
    private long lastFrameSentTime = 0;
    private static final long FRAME_INTERVAL_MS = 100; // 10 FPS

    // JPEG compression quality (0.0 to 1.0)
    private static final float JPEG_QUALITY = 0.7f;

    // Stats
    private long framesSent = 0;
    private long framesReceived = 0;
    private long bytesSent = 0;

    public VideoStreamClient(SignalingClient signalingClient) {
        this.signalingClient = signalingClient;
        setupFrameReceiver();
    }

    /**
     * Setup receiver for incoming video frames
     */
    private void setupFrameReceiver() {
        signalingClient.setOnVideoFrame((event) -> {
            try {
                // Decode base64 frame data
                byte[] frameBytes = Base64.getDecoder().decode(event.frameData);

                // Decode JPEG to BufferedImage
                BufferedImage frame = ImageIO.read(new java.io.ByteArrayInputStream(frameBytes));

                if (frame != null && onFrameReceived != null) {
                    UUID senderUUID = UUID.fromString(event.fromUUID);
                    onFrameReceived.accept(senderUUID, frame);
                    framesReceived++;

                    if (framesReceived % 100 == 0) {
                        LOGGER.debug("Received {} frames from {}", framesReceived, event.fromName);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error decoding video frame", e);
            }
        });
    }

    /**
     * Send a video frame to all other players
     */
    public void sendFrame(BufferedImage frame) {
        if (!signalingClient.isConnected()) {
            return;
        }

        // Frame rate limiting
        long now = System.currentTimeMillis();
        if (now - lastFrameSentTime < FRAME_INTERVAL_MS) {
            return;
        }
        lastFrameSentTime = now;

        try {
            // Compress frame to JPEG
            byte[] jpegBytes = compressFrameToJPEG(frame);

            // Encode to base64
            String base64Frame = Base64.getEncoder().encodeToString(jpegBytes);

            // Send via signaling client
            signalingClient.sendVideoFrame(base64Frame);

            framesSent++;
            bytesSent += jpegBytes.length;

            if (framesSent % 100 == 0) {
                long avgSize = bytesSent / framesSent;
                LOGGER.debug("Sent {} frames, avg size: {} KB", framesSent, avgSize / 1024);
            }
        } catch (Exception e) {
            LOGGER.error("Error sending video frame", e);
        }
    }

    /**
     * Compress a BufferedImage to JPEG bytes
     */
    private byte[] compressFrameToJPEG(BufferedImage frame) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use ImageIO with JPEG compression
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
        }

        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(frame, null, null), param);

        ios.close();
        writer.dispose();

        return baos.toByteArray();
    }

    /**
     * Set callback for when a frame is received from another player
     */
    public void setOnFrameReceived(BiConsumer<UUID, BufferedImage> callback) {
        this.onFrameReceived = callback;
    }

    /**
     * Get statistics
     */
    public VideoStats getStats() {
        return new VideoStats(framesSent, framesReceived, bytesSent);
    }

    public static class VideoStats {
        public final long framesSent;
        public final long framesReceived;
        public final long bytesSent;

        public VideoStats(long framesSent, long framesReceived, long bytesSent) {
            this.framesSent = framesSent;
            this.framesReceived = framesReceived;
            this.bytesSent = bytesSent;
        }

        public long getAverageFrameSize() {
            return framesSent > 0 ? bytesSent / framesSent : 0;
        }
    }
}
