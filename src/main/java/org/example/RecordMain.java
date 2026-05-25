package org.example;

import com.jme3.system.AppSettings;

public class RecordMain {
    public static void main(String[] args) {
        Simulation3D app = new Simulation3D();
        app.getStateManager().attach(new VideoFrameRecorder("render_output", 30, 300));
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 1024);
        settings.setFullscreen(false);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }
}
