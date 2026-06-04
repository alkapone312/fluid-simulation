package pl.pwr.jkaplone.fluidsim;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.Timer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

public class VideoFrameRecorder extends BaseAppState {

    private final File outputDir;
    private final int targetFps;
    private final int maxFrames;

    private SimpleApplication app;
    private Timer originalTimer;
    private FrameCaptureProcessor processor;

    public VideoFrameRecorder(String outputDirName, int targetFps, int maxFrames) {
        this.outputDir = new File(outputDirName);
        this.targetFps = targetFps;
        this.maxFrames = maxFrames;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Konfiguracja renderowania offline
        app.getContext().getSettings().setFrameRate(-1);
        app.getContext().getSettings().setVSync(false);
        this.app.setDisplayFps(false);
        this.app.setDisplayStatView(false);

        // Podmiana timera na stałokrokowy
        this.originalTimer = app.getTimer();
        app.setTimer(new FixedStepTimer(targetFps));

        // Tworzymy i rejestrujemy zewnętrzny procesor sceny
        this.processor = new FrameCaptureProcessor(outputDir, maxFrames, this.app);
        this.app.getViewPort().addProcessor(this.processor);
    }

    @Override
    protected void cleanup(Application app) {
        if (processor != null) {
            this.app.getViewPort().removeProcessor(processor);
        }
        if (originalTimer != null) {
            app.setTimer(originalTimer);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    // ==========================================
    // DEDYKOWANY PROCESOR KADRÓW (SCENE PROCESSOR)
    // ==========================================
    private static class FrameCaptureProcessor implements SceneProcessor {
        private final File outputDir;
        private final int maxFrames;
        private final SimpleApplication app;

        private RenderManager rm;
        private ViewPort vp;
        private ByteBuffer outBuf;
        private int frameCount = 0;
        private boolean isInitialized = false;

        public FrameCaptureProcessor(File outputDir, int maxFrames, SimpleApplication app) {
            this.outputDir = outputDir;
            this.maxFrames = maxFrames;
            this.app = app;
        }

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            this.rm = rm;
            this.vp = vp;
            // Alokujemy bufor dopiero przy pełnej inicjalizacji przez silnik jME
            int w = vp.getCamera().getWidth();
            int h = vp.getCamera().getHeight();
            this.outBuf = BufferUtils.createByteBuffer(w * h * 4);
            this.isInitialized = true;
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
            this.outBuf = BufferUtils.createByteBuffer(w * h * 4);
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }

        @Override
        public void preFrame(float tpf) {}

        @Override
        public void postQueue(RenderQueue rq) {}

        @Override
        public void postFrame(FrameBuffer out) {
            // Bezpiecznik gwarantujący, że nie wykonamy kodu bez inicjalizacji lub po limicie klatek
            if (!isInitialized || frameCount >= maxFrames) {
                return;
            }

            int width = vp.getCamera().getWidth();
            int height = vp.getCamera().getHeight();

            outBuf.clear();
            rm.getRenderer().readFrameBuffer(out, outBuf);

            final int currentFrame = frameCount;
            final ByteBuffer copyBuf = BufferUtils.clone(outBuf);

            new Thread(() -> saveFrameAsync(copyBuf, width, height, currentFrame)).start();

            frameCount++;
            if (frameCount >= maxFrames) {
                System.out.println("Nagrywanie zakończone! Wygenerowano " + maxFrames + " klatek.");
                app.stop();
            }
        }

        private void saveFrameAsync(ByteBuffer buffer, int width, int height, int frameIndex) {
            // 1. Tworzymy obrazek o formacie 4-bajtowym (RGBA/ABGR), pasującym do danych z GPU
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

            // Konwertujemy zrzut ekranu do przygotowanego bufora
            Screenshots.convertScreenShot(buffer, image);

            // 2. Do zapisu pliku PNG tworzymy obrazek docelowy (może być TYPE_3BYTE_BGR dla mniejszego rozmiaru pliku)
            BufferedImage flippedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            java.awt.Graphics2D g = flippedImage.createGraphics();

            // Rysujemy z odbiciem pionowym i jednoczesną konwersją formatu do RGB (bez kanału alfa)
            g.drawImage(image, 0, 0, width, height, 0, 0, width, height, null);
            g.dispose();

            File file = new File(outputDir, String.format("frame_%04d.png", frameIndex));
            try {
                ImageIO.write(flippedImage, "png", file);
            } catch (IOException e) {
                System.err.println("Błąd zapisu klatki " + frameIndex + ": " + e.getMessage());
            }
        }

        @Override
        public void cleanup() {
            isInitialized = false;
        }

        @Override
        public void setProfiler(com.jme3.profile.AppProfiler profiler) {}
    }

    // ==========================================
    // NIEZMIENNY TIMER JME
    // ==========================================
    private static class FixedStepTimer extends Timer {
        private final float stepSec;
        private long ticks = 0;

        public FixedStepTimer(int fps) {
            this.stepSec = 1f / fps;
        }

        @Override
        public long getTime() {
            return (long) (ticks * stepSec * getResolution());
        }

        @Override
        public long getResolution() {
            return 1000L;
        }

        @Override
        public float getFrameRate() {
            return 1f / stepSec;
        }

        @Override
        public float getTimePerFrame() {
            return stepSec;
        }

        @Override
        public void update() {
            ticks++;
        }

        @Override
        public void reset() {
            ticks = 0;
        }
    }
}