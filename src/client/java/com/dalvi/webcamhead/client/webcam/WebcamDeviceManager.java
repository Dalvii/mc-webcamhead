package com.dalvi.webcamhead.client.webcam;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebcamDeviceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebcamHead");
    private static final int MAX_DEVICES_TO_CHECK = 5;

    public static List<WebcamDevice> listAvailableDevices() {
        List<WebcamDevice> devices = new ArrayList<>();

        for (int i = 0; i < MAX_DEVICES_TO_CHECK; i++) {
            try {
                OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(i);
                grabber.start();

                // If we can start it, it's a valid device
                String deviceName = "Camera " + i;
                devices.add(new WebcamDevice(i, deviceName));

                grabber.stop();
                grabber.release();

                LOGGER.info("Found webcam device: {} (index {})", deviceName, i);
            } catch (FrameGrabber.Exception e) {
                // Device doesn't exist or can't be accessed, stop checking
                break;
            }
        }

        LOGGER.info("Found {} webcam device(s)", devices.size());
        return devices;
    }

    public static class WebcamDevice {
        private final int index;
        private final String name;

        public WebcamDevice(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + " (index " + index + ")";
        }
    }
}
