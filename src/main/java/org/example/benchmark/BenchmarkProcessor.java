package org.example.benchmark;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

public class BenchmarkProcessor implements SceneProcessor {

    private RenderManager rm;
    private BenchmarkState stateManager;

    private final int WARMUP_FRAMES = 200;
    private final int MEASURE_FRAMES = 500;

    private int frameCount = 0;
    private boolean isWarmingUp = true;

    // Zmienne do mierzenia całego cyklu klatki
    private long lastFrameTime = 0;
    private long totalFrameTime = 0;

    public BenchmarkProcessor(BenchmarkState stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
    }

    public void resetForNextTest() {
        this.frameCount = 0;
        this.totalFrameTime = 0;
        this.lastFrameTime = 0;
        this.isWarmingUp = true;
    }

    @Override
    public void postFrame(FrameBuffer out) {
        long currentTime = System.nanoTime();
        frameCount++;

        if (isWarmingUp) {
            if (frameCount >= WARMUP_FRAMES) {
                isWarmingUp = false;
                frameCount = 0;
            }
            // Zapamiętujemy czas ostatniej klatki przed wyjściem
            lastFrameTime = currentTime;
            return;
        }

        // Mierzymy czas od zakończenia poprzedniej klatki do zakończenia obecnej
        long frameDelta = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        totalFrameTime += frameDelta;

        if (frameCount >= MEASURE_FRAMES) {
            double avgFrameTimeMs = (totalFrameTime / (double) MEASURE_FRAMES) / 1_000_000.0;
            double avgFps = 1000.0 / avgFrameTimeMs; // Przeliczamy ms na FPS

            stateManager.onTestCompleted(avgFrameTimeMs, avgFps);
            isWarmingUp = true;
        }
    }

    // Puste implementacje
    @Override public void preFrame(float tpf) {}
    @Override public void reshape(ViewPort vp, int w, int h) {}
    @Override public boolean isInitialized() { return rm != null; }
    @Override public void postQueue(RenderQueue rq) {}
    @Override public void cleanup() {}
    @Override public void setProfiler(AppProfiler profiler) {}


    public boolean isWarmingUp() { return isWarmingUp; }

    public int getMeasureFrames() { return MEASURE_FRAMES; }
}