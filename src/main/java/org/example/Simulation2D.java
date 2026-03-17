package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import org.example.bean.BeanEditor;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;

public class Simulation2D extends SimpleApplication {

    static int frameCount = 0;

    private int numParticles = 4032;

    private ComputeShader computeShader;

    private Geometry[] particles;

    private SimulationBean bean = new SimulationBean();

    public static void main(String[] args) {
        Simulation2D app = new Simulation2D();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        settings.setFullscreen(true);
        app.setSettings(settings);
        app.start();
    }

    private void setupCameraAndLight() {
        cam.setLocation(new Vector3f(0, 0, 15));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(5);
        flyCam.setDragToRotate(true);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
    }

    private void setupParticles() {
        particles = new Geometry[numParticles];

        Sphere sphere = new Sphere(10, 10, 0.05f);

        for (int i = 0; i < numParticles; i++) {
            Geometry g = new Geometry("Particle_" + i, sphere);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Yellow);
            g.setMaterial(mat);
            g.center();
            rootNode.attachChild(g);
            particles[i] = g;
        }
    }

    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        Panel panel = BeanEditor.open(bean);
        panel.setLocalTranslation(cam.getLocation().x, cam.getLocation().y, cam.getLocation().z);
        panel.setLocalScale(0.005f);
        rootNode.attachChild(panel);
        Quad quad = new Quad(bean.getBoundsX(), bean.getBoundsY());
        quad.setMode(Mesh.Mode.Lines);
        setupCameraAndLight();
        setupParticles();

        FloatBuffer particlePositions = BufferUtils.createFloatBuffer(numParticles * 2);
        FloatBuffer particlePredictedPositions = BufferUtils.createFloatBuffer(numParticles * 2);
        FloatBuffer velocitiesBuffer = BufferUtils.createFloatBuffer(numParticles * 2);
        FloatBuffer particleDensities = BufferUtils.createFloatBuffer(numParticles * 2);

        final float boxSize = 2f;
        int particlesPerRow = (int) Math.ceil(Math.sqrt(numParticles));
        float spacing = (2 * boxSize) / (particlesPerRow - 1);
        Random r = new Random();
        for (int i = 0; i < numParticles; i++) {
            int xIndex = i % particlesPerRow;
            int yIndex = i / particlesPerRow;
            float x = -boxSize + xIndex * spacing;
            float y = -boxSize + yIndex * spacing;
            particlePositions.put(x);
            particlePositions.put(y);
            velocitiesBuffer.put(0);
            velocitiesBuffer.put(0);
            particleDensities.put(0);
            particleDensities.put(0);
        }

        particlePredictedPositions.put(particlePositions);
        particlePositions.flip();
        velocitiesBuffer.flip();
        particleDensities.flip();

        try {
            computeShader = new ComputeShader(new String(getClass().getResourceAsStream("/simulation_2d.glsl").readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        computeShader.setData(0, particlePositions);
        computeShader.setData(1, particlePredictedPositions);
        computeShader.setData(2, velocitiesBuffer);
        computeShader.setData(3, particleDensities);
    }

    @Override
    public void simpleUpdate(float tpf) {
        float deltaTime = tpf / bean.getIterationsPerFrame();
        computeShader.bind();
        // KERNEL UNIFORMS

        computeShader.setUniform("SpikyPow3ScalingFactor", (float) (10f / (Math.PI * Math.pow(bean.getSmoothingRadius(), 5))));
        computeShader.setUniform("SpikyPow2ScalingFactor", (float) (6f / (Math.PI * Math.pow(bean.getSmoothingRadius(), 4))));
        computeShader.setUniform("SpikyPow3DerivativeScalingFactor", (float) (30f / (Math.pow(bean.getSmoothingRadius(), 5) * Math.PI)));
        computeShader.setUniform("SpikyPow2DerivativeScalingFactor", (float) (12f / (Math.pow(bean.getSmoothingRadius(), 4) * Math.PI)));
        computeShader.setUniform("Poly6ScalingFactor", (float) (4 / (Math.PI * Math.pow(bean.getSmoothingRadius(), 8))));

//        computeShader.setUniform("SpikyPow3ScalingFactor", (float) (15f / (Math.PI * Math.pow(bean.getSmoothingRadius(), 6))));
//        computeShader.setUniform("SpikyPow2ScalingFactor", (float) (15f / (2 * Math.PI * Math.pow(bean.getSmoothingRadius(), 5))));
//        computeShader.setUniform("SpikyPow3DerivativeScalingFactor", (float) (45f / (Math.pow(bean.getSmoothingRadius(), 6) * Math.PI)));
//        computeShader.setUniform("SpikyPow2DerivativeScalingFactor", (float) (15f / (Math.pow(bean.getSmoothingRadius(), 5) * Math.PI)));
//        computeShader.setUniform("Poly6ScalingFactor", (float) (315 / (64 * Math.PI * Math.pow(bean.getSmoothingRadius(), 9))));

        computeShader.setUniform("pressureMultiplier", bean.getPressureMultiplier());
        computeShader.setUniform("viscosityStrength", bean.getViscosityStrength());
        computeShader.setUniform("nearPressureMultiplier", bean.getNearPressureMultiplier());
        computeShader.setUniform("targetDensity", bean.getTargetDensity());
        computeShader.setUniform("collisionDamping", 0.8f);
        computeShader.setUniform("boundsSize", bean.getBoundsX(), bean.getBoundsY());
        computeShader.setUniform("numParticles", numParticles);
        computeShader.setUniform("smoothingRadius", bean.getSmoothingRadius());
        computeShader.setUniform("deltaTime", Math.min(deltaTime, 1.0f/60));
        computeShader.setUniform("gravity", 0, -bean.getGravityForce());

        for (int i = 0 ; i < bean.getIterationsPerFrame(); i++) {
            step(deltaTime);
        }
    }

    private void step(float deltaTime) {
        int groups = (numParticles + 63) / 64;
        computeShader.setUniform("task", 1);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 2);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 3);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 4);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 5);
        computeShader.dispatch(groups, 1, 1);

        FloatBuffer positions = computeShader.getData(0);
        FloatBuffer velocities = computeShader.getData(2);
        FloatBuffer densities = computeShader.getData(3);

        for (int i = 0; i < numParticles; i++) {
            float density = densities.get();
            float nearDensity = densities.get();
            float x = positions.get();
            float y = positions.get();
            particles[i].setLocalTranslation(x, y, 0f);

            float velocityX = velocities.get();
            float velocityY = velocities.get();
            float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            float maxSpeed = 10.0f;
            float t = Math.min(speed / maxSpeed, 1.0f);
            float red = t;
            float green = 0.0f;
            float blue = 1.0f - t;

            particles[i].getMaterial().setColor("Color", new ColorRGBA(red, green, blue, 1.0f));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (computeShader != null) {
            computeShader.cleanup();
        }
    }
}
