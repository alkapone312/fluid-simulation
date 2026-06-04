package pl.pwr.jkaplone.fluidsim;

import com.jme3.system.AppSettings;
import pl.pwr.jkaplone.fluidsim.benchmark.BenchmarkSet;
import pl.pwr.jkaplone.fluidsim.benchmark.BenchmarkState;

public class BenchmarkMain {
    public static void main(String[] args) {
        Simulation3D app = new Simulation3D();
        BenchmarkState benchmarkState = new BenchmarkState();
        controlSet(app, benchmarkState);
        ssfrParticleSize(app, benchmarkState);
        ssfrGaussianBlurSize(app, benchmarkState);
        ssfrGaussianSmoothness(app, benchmarkState);
        curvatureFlowIterationSet(app, benchmarkState);
        curvatureFlowStepSet(app, benchmarkState);
        perspectiveVolumeResolution(app, benchmarkState);
        perspectiveVolumeParticleRadius(app, benchmarkState);
        perspectiveVolumeFluidDensity(app, benchmarkState);

        app.getStateManager().attach(benchmarkState);
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 1024);
        settings.setFullscreen(false);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }

    public static void controlSet(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("Control set");
        set.addScenario("Control sceario", () -> {
            app.bean.setRenderMode(0);
        });
        state.addBenchmarkSet(set);
    }

    public static void ssfrParticleSize(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("Particle Size");
        set.addScenario("Particle Size - 0.09 - Gauss", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setGaussianSmoothing(1);
            app.ssfrBean.setCurvatureFlowSmoothing(0);
            app.ssfrBean.setParticleRadius(0.09f);
            app.gaussianSmoothingBean.setBlurSize(30);
            app.gaussianSmoothingBean.setSmoothness(1f);
        });
        set.addScenario("Particle Size - 0.18 - Gauss", () -> {
            app.ssfrBean.setParticleRadius(0.18f);
        });
        set.addScenario("Particle Size - 0.36 - Gauss", () -> {
            app.ssfrBean.setParticleRadius(0.36f);
        });
        set.addScenario("Particle Size - 0.72 - Gauss", () -> {
            app.ssfrBean.setParticleRadius(0.72f);
        });
        set.addScenario("Particle Size - 1.44 - Gauss", () -> {
            app.ssfrBean.setParticleRadius(1.44f);
        });
        set.addScenario("Particle Size - 0.09 - Curvature flow", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setGaussianSmoothing(0);
            app.ssfrBean.setCurvatureFlowSmoothing(1);
            app.ssfrBean.setParticleRadius(0.09f);
            app.curvatureFlowSmoothingBean.setStep(0.005f);
            app.curvatureFlowSmoothingBean.setNumberOfIterations(400);
        });
        set.addScenario("Particle Size - 0.18 - Curvature flow", () -> {
            app.ssfrBean.setParticleRadius(0.18f);
        });
        set.addScenario("Particle Size - 0.36 - Curvature flow", () -> {
            app.ssfrBean.setParticleRadius(0.36f);
        });
        set.addScenario("Particle Size - 0.72 - Curvature flow", () -> {
            app.ssfrBean.setParticleRadius(0.72f);
        });
        set.addScenario("Particle Size - 1.44 - Curvature flow", () -> {
            app.ssfrBean.setParticleRadius(1.44f);
        });

        state.addBenchmarkSet(set);
    }

    public static void ssfrGaussianBlurSize(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("SSFR - Gauss Blur Size");
        set.addScenario("Gaussian Blur - Size - 10", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setParticleRadius(0.18f);
            app.ssfrBean.setGaussianSmoothing(1);
            app.ssfrBean.setCurvatureFlowSmoothing(0);
            app.gaussianSmoothingBean.setBlurSize(10);
            app.gaussianSmoothingBean.setSmoothness(1.0f);
        });
        set.addScenario("Gaussian Blur - Size - 30", () -> {
            app.gaussianSmoothingBean.setBlurSize(30);
        });
        set.addScenario("Gaussian Blur - Size - 50", () -> {
            app.gaussianSmoothingBean.setBlurSize(50);
        });
        set.addScenario("Gaussian Blur - Size - 70", () -> {
            app.gaussianSmoothingBean.setBlurSize(70);
        });
        set.addScenario("Gaussian Blur - Size - 90", () -> {
            app.gaussianSmoothingBean.setBlurSize(90);
        });

        state.addBenchmarkSet(set);
    }

    public static void ssfrGaussianSmoothness(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("SSFR - Gauss Blur Smoothness");
        set.addScenario("Gaussian Blur - Smoothness - 0.25", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setParticleRadius(0.18f);
            app.ssfrBean.setGaussianSmoothing(1);
            app.ssfrBean.setCurvatureFlowSmoothing(0);
            app.gaussianSmoothingBean.setBlurSize(30);
            app.gaussianSmoothingBean.setSmoothness(0.25f);
        });
        set.addScenario("Gaussian Blur - Smoothness - 0.5", () -> {
            app.gaussianSmoothingBean.setSmoothness(0.5f);
        });
        set.addScenario("Gaussian Blur - Smoothness - 1.0", () -> {
            app.gaussianSmoothingBean.setSmoothness(1.0f);
        });
        set.addScenario("Gaussian Blur - Smoothness - 2.0", () -> {
            app.gaussianSmoothingBean.setSmoothness(2.0f);
        });
        set.addScenario("Gaussian Blur - Smoothness - 4.0", () -> {
            app.gaussianSmoothingBean.setSmoothness(4.0f);
        });

        state.addBenchmarkSet(set);
    }

    public static void curvatureFlowIterationSet(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("SSFR - Curvature Flow - Number of iterations");
        set.addScenario("Curvature Flow - 25", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setParticleRadius(0.18f);
            app.ssfrBean.setCurvatureFlowSmoothing(1);
            app.ssfrBean.setGaussianSmoothing(0);
            app.curvatureFlowSmoothingBean.setStep(0.005f);
            app.curvatureFlowSmoothingBean.setNumberOfIterations(25);
        });
        set.addScenario("Curvature Flow - 50", () -> {
            app.curvatureFlowSmoothingBean.setNumberOfIterations(50);
        });
        set.addScenario("Curvature Flow - 100", () -> {
            app.curvatureFlowSmoothingBean.setNumberOfIterations(100);
        });
        set.addScenario("Curvature Flow - 200", () -> {
            app.curvatureFlowSmoothingBean.setNumberOfIterations(200);
        });
        set.addScenario("Curvature Flow - 400", () -> {
            app.curvatureFlowSmoothingBean.setNumberOfIterations(400);
        });
        state.addBenchmarkSet(set);
    }

    public static void curvatureFlowStepSet(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("SSFR Curvature Flow - Step size");
        set.addScenario("Curvature Flow - 0.0005", () -> {
            app.bean.setRenderMode(1);
            app.ssfrBean.setParticleRadius(0.18f);
            app.ssfrBean.setCurvatureFlowSmoothing(1);
            app.ssfrBean.setGaussianSmoothing(0);
            app.curvatureFlowSmoothingBean.setNumberOfIterations(100);
            app.curvatureFlowSmoothingBean.setStep(0.0005f);
        });
        set.addScenario("Curvature Flow - 0.001", () -> {
            app.curvatureFlowSmoothingBean.setStep(0.001f);
        });
        set.addScenario("Curvature Flow - 0.005", () -> {
            app.curvatureFlowSmoothingBean.setStep(0.005f);
        });
        set.addScenario("Curvature Flow - 0.01", () -> {
            app.curvatureFlowSmoothingBean.setStep(0.01f);
        });
        set.addScenario("Curvature Flow - 0.05", () -> {
            app.curvatureFlowSmoothingBean.setStep(0.05f);
        });
        state.addBenchmarkSet(set);
    }

    public static void perspectiveVolumeResolution(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("Perspective Volume - Resolution");
        set.addScenario("Perspective Volume - 16x16", () -> {
            app.bean.setRenderMode(2);
            app.volumeBean.setFluidDensity(0.1f);
            app.volumeBean.setParticleRadius(0.35f);
            app.volumeBean.setPerspectiveGridHeight(16);
            app.volumeBean.setPerspectiveGridWidth(16);
        });
        set.addScenario("Perspective Volume - 32x32", () -> {
            app.volumeBean.setPerspectiveGridHeight(32);
            app.volumeBean.setPerspectiveGridWidth(32);
        });
        set.addScenario("Perspective Volume - 64x64", () -> {
            app.volumeBean.setPerspectiveGridHeight(64);
            app.volumeBean.setPerspectiveGridWidth(64);
        });
        set.addScenario("Perspective Volume - 128x128", () -> {
            app.volumeBean.setPerspectiveGridHeight(128);
            app.volumeBean.setPerspectiveGridWidth(128);
        });
        set.addScenario("Perspective Volume - 256x256", () -> {
            app.volumeBean.setPerspectiveGridHeight(256);
            app.volumeBean.setPerspectiveGridWidth(256);
        });
        state.addBenchmarkSet(set);
    }

    public static void perspectiveVolumeParticleRadius(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("Perspective Volume - Particle Radius");
        set.addScenario("Perspective Volume - 0.09", () -> {
            app.bean.setRenderMode(2);
            app.volumeBean.setFluidDensity(0.1f);
            app.volumeBean.setParticleRadius(0.09f);
            app.volumeBean.setPerspectiveGridHeight(128);
            app.volumeBean.setPerspectiveGridWidth(128);
        });
        set.addScenario("Perspective Volume - 0.18", () -> {
            app.volumeBean.setParticleRadius(0.18f);
        });
        set.addScenario("Perspective Volume - 0.36", () -> {
            app.volumeBean.setParticleRadius(0.36f);
        });
        set.addScenario("Perspective Volume - 0.72", () -> {
            app.volumeBean.setParticleRadius(0.72f);
        });
        set.addScenario("Perspective Volume - 1.44", () -> {
            app.volumeBean.setParticleRadius(1.44f);
        });
        state.addBenchmarkSet(set);
    }

    public static void perspectiveVolumeFluidDensity(Simulation3D app, BenchmarkState state) {
        BenchmarkSet set = new BenchmarkSet("Perspective Volume - Fluid Density");
        set.addScenario("Perspective Volume - 0.0", () -> {
            app.bean.setRenderMode(2);
            app.volumeBean.setFluidDensity(0.0f);
            app.volumeBean.setParticleRadius(0.35f);
            app.volumeBean.setPerspectiveGridHeight(128);
            app.volumeBean.setPerspectiveGridWidth(128);
        });
        set.addScenario("Perspective Volume - 0.25", () -> {
            app.volumeBean.setFluidDensity(0.25f);
        });
        set.addScenario("Perspective Volume - 0.5", () -> {
            app.volumeBean.setFluidDensity(0.5f);
        });
        set.addScenario("Perspective Volume - 1.0", () -> {
            app.volumeBean.setFluidDensity(1.0f);
        });
        set.addScenario("Perspective Volume - 2.0", () -> {
            app.volumeBean.setFluidDensity(2.0f);
        });
        state.addBenchmarkSet(set);
    }
}
