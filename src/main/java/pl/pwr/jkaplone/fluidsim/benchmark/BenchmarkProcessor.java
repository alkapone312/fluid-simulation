package pl.pwr.jkaplone.fluidsim.benchmark;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkProcessor implements SceneProcessor {

    private RenderManager rm;
    private BenchmarkState stateManager;

    private final int WARMUP_FRAMES = 200;
    private final int MEASURE_FRAMES = 300;

    private int frameCount = 0;
    private boolean isWarmingUp = true;

    private long lastFrameTime = 0;

    private final List<Double> frameTimeSamples = new ArrayList<>();

    public BenchmarkProcessor(BenchmarkState stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
    }

    public void resetForNextTest() {
        this.frameCount = 0;
        this.lastFrameTime = 0;
        this.frameTimeSamples.clear();
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
            lastFrameTime = currentTime;
            return;
        }

        long frameDelta = currentTime - lastFrameTime;
        lastFrameTime = currentTime;

        double frameTimeMs = frameDelta / 1_000_000.0;
        frameTimeSamples.add(frameTimeMs);

        if (frameCount >= MEASURE_FRAMES) {
            stateManager.onTestCompleted(frameTimeSamples);

            isWarmingUp = true;
        }
    }

    @Override public void preFrame(float tpf) {}
    @Override public void reshape(ViewPort vp, int w, int h) {}
    @Override public boolean isInitialized() { return rm != null; }
    @Override public void postQueue(RenderQueue rq) {}
    @Override public void cleanup() {}
    @Override public void setProfiler(AppProfiler profiler) {}

    public boolean isWarmingUp() { return isWarmingUp; }

    public int getMeasureFrames() { return frameTimeSamples.size() > 0 ? frameTimeSamples.size() : MEASURE_FRAMES; }
}