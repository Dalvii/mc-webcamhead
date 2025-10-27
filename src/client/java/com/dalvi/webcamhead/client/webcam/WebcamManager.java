package com.dalvi.webcamhead.client.webcam;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

public class WebcamManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");

    private FrameGrabber grabber;
    private OpenCVFrameConverter.ToMat converter;
    private boolean isRunning = false;
    private Thread captureThread;
    private volatile BufferedImage latestFrame;

    private final int targetWidth;
    private final int targetHeight;
    private final int targetFps;
    private final int deviceIndex;

    public WebcamManager(int width, int height, int fps, int deviceIndex) {
        this.targetWidth = width;
        this.targetHeight = height;
        this.targetFps = fps;
        this.deviceIndex = deviceIndex;
        this.converter = new OpenCVFrameConverter.ToMat();
    }

    public boolean start() {
        if (isRunning) {
            LOGGER.warn("Webcam is already running");
            return false;
        }

        try {
            grabber = new OpenCVFrameGrabber(deviceIndex);
            LOGGER.info("Attempting to start webcam device index: {}", deviceIndex);
            grabber.setImageWidth(targetWidth);
            grabber.setImageHeight(targetHeight);
            grabber.setFrameRate(targetFps);
            grabber.start();

            isRunning = true;
            startCaptureThread();

            LOGGER.info("Webcam started successfully ({}x{} @ {}fps)", targetWidth, targetHeight, targetFps);
            return true;
        } catch (FrameGrabber.Exception e) {
            LOGGER.error("Failed to start webcam", e);
            return false;
        }
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                LOGGER.error("Error stopping webcam", e);
            }
            grabber = null;
        }

        LOGGER.info("Webcam stopped");
    }

    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Frame frame = grabber.grab();
                    if (frame != null && frame.image != null) {
                        latestFrame = frameToBufferedImage(frame);
                    }

                    // Control frame rate
                    Thread.sleep(1000 / targetFps);
                } catch (FrameGrabber.Exception e) {
                    LOGGER.error("Error grabbing frame", e);
                    isRunning = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "WebcamCapture");

        captureThread.setDaemon(true);
        captureThread.start();
    }

    private BufferedImage frameToBufferedImage(Frame frame) {
        Mat mat = converter.convert(frame);
        if (mat == null) {
            return null;
        }

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        BufferedImage image = new BufferedImage(width, height,
            channels == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);

        byte[] data = new byte[width * height * channels];
        mat.data().get(data);

        byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(data, 0, imageData, 0, data.length);

        return image;
    }

    public BufferedImage getLatestFrame() {
        return latestFrame;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getWidth() {
        return targetWidth;
    }

    public int getHeight() {
        return targetHeight;
    }
}
