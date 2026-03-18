package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import org.example.bean.BeanEditor;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class Simulation3D extends SimpleApplication {

    static int frameCount = 0;

    private int numParticles = 20 * 20 * 20;

    private ComputeShader computeShader;

    private Geometry[] particles;

    private Geometry particleGeometry;

    private SimulationBean bean = new SimulationBean();

    public static void main(String[] args) {
        Simulation3D app = new Simulation3D();
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
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            //mat.setColor("Color", ColorRGBA.Red);
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.Yellow);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 1f);
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
        panel.setLocalTranslation(settings.getWidth() - panel.getPreferredSize().x, settings.getHeight(), 0);
        guiNode.attachChild(panel);
        setupCameraAndLight();
        setupParticles();

        FloatBuffer particlePositions = BufferUtils.createFloatBuffer(numParticles * 4);
        FloatBuffer particlePredictedPositions = BufferUtils.createFloatBuffer(numParticles * 4);
        FloatBuffer velocitiesBuffer = BufferUtils.createFloatBuffer(numParticles * 4);
        FloatBuffer particleDensities = BufferUtils.createFloatBuffer(numParticles * 2);
        IntBuffer spatialIndices = BufferUtils.createIntBuffer(numParticles * 4);
        IntBuffer spatialOffsets = BufferUtils.createIntBuffer(numParticles);

        final float boxSize = 2f;
        int particlesPerRow = (int) Math.ceil(Math.pow(numParticles, (double) 1 / 3));
        float spacing = (2 * boxSize) / (particlesPerRow - 1);
        for (int i = 0; i < numParticles; i++) {
            int xIndex = i % particlesPerRow;
            int yIndex = (i / particlesPerRow) % particlesPerRow;
            int zIndex = i / (particlesPerRow*particlesPerRow);
            float x = -boxSize + xIndex * spacing;
            float y = -boxSize + yIndex * spacing;
            float z = -boxSize + zIndex * spacing;
            particlePositions.put(x);
            particlePositions.put(y);
            particlePositions.put(z);
            particlePositions.put(0);
            velocitiesBuffer.put(0);
            velocitiesBuffer.put(0);
            velocitiesBuffer.put(0);
            velocitiesBuffer.put(0);
            particleDensities.put(0);
            particleDensities.put(0);
            spatialIndices.put(0);
            spatialIndices.put(0);
            spatialIndices.put(0);
            spatialIndices.put(0);
            spatialOffsets.put(0);
        }

        particlePredictedPositions.put(particlePositions);
        particlePositions.flip();
        velocitiesBuffer.flip();
        particleDensities.flip();
        spatialIndices.flip();
        spatialOffsets.flip();

        computeShader = ComputeShaderLoader.load("compute/simulation_3d.glsl");
        computeShader.setData(0, particlePositions);
        computeShader.setData(1, particlePredictedPositions);
        computeShader.setData(2, velocitiesBuffer);
        computeShader.setData(3, particleDensities);
        computeShader.setData(4, spatialIndices);
        computeShader.setData(5, spatialOffsets);
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
        computeShader.setUniform("boundsSize", bean.getBoundsX(), bean.getBoundsY(), bean.getBoundsZ());
        computeShader.setUniform("numParticles", numParticles, ComputeShader.IntegerType.UNSIGNED);
        computeShader.setUniform("smoothingRadius", bean.getSmoothingRadius());
        computeShader.setUniform("deltaTime", Math.min(deltaTime, 1.0f/60));
        computeShader.setUniform("gravity", 0, -bean.getGravityForce(), 0);

        for (int i = 0 ; i < bean.getIterationsPerFrame(); i++) {
            step(deltaTime);
        }
    }

    private void step(float deltaTime) {
        int groups = (numParticles + 63) / 64;
        computeShader.setUniform("task", 1, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 2, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
//        sortAndCalculateOffsetsCPU();
        computeShader.setUniform("task", 3, ComputeShader.IntegerType.UNSIGNED);
        int nextPow2 = Integer.highestOneBit(numParticles) << 1;
        int numStages = Integer.numberOfTrailingZeros(nextPow2);
        for (int i = 0; i < numStages; i++) {
            for (int j = 0; j <= i; j++) {
                int groupWidth = 1 << (i - j);
                int groupHeight = 2 * groupWidth - 1;

                computeShader.setUniform("groupWidth", groupWidth, ComputeShader.IntegerType.UNSIGNED);
                computeShader.setUniform("groupHeight", groupHeight, ComputeShader.IntegerType.UNSIGNED);
                computeShader.setUniform("stepIndex", j, ComputeShader.IntegerType.UNSIGNED);
                computeShader.dispatch(nextPow2 / 2, 1, 1);
            }
        }
        computeShader.setUniform("task", 4, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 5, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 6, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 7, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
        computeShader.setUniform("task", 8, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);

        FloatBuffer positions = computeShader.getData(0, FloatBuffer.class);
        FloatBuffer velocities = computeShader.getData(2, FloatBuffer.class);
        FloatBuffer densities = computeShader.getData(3, FloatBuffer.class);
        IntBuffer indices = computeShader.getData(4, IntBuffer.class);
        IntBuffer offsets = computeShader.getData(5, IntBuffer.class);
//        java.util.stream.IntStream.range(0, indices.limit()).filter(i -> i % 4 == 2).map(i -> indices.array()[i]).toArray()
        for (int i = 0; i < numParticles; i++) {
            float density = densities.get();
            float nearDensity = densities.get();
            float x = positions.get();
            float y = positions.get();
            float z = positions.get();
            float w = positions.get();
            particles[i].setLocalTranslation(x, y, z);
            float velocityX = velocities.get();
            float velocityY = velocities.get();
            float velocityZ = velocities.get();
            float velocityW = velocities.get();
            float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
            float maxSpeed = 10.0f;
            float t = Math.min(speed / maxSpeed, 1.0f);
            float red = t;
            float green = 0.0f;
            float blue = 1.0f - t;

            particles[i].getMaterial().setColor("Diffuse", new ColorRGBA(red, green, blue, 1.0f));
        }
    }

    private void sortAndCalculateOffsetsCPU() {
        // 1. Fetch the indices from GPU (Binding 4)
        IntBuffer buffer = computeShader.getData(4, IntBuffer.class);
        int[] rawData = new int[buffer.remaining()];
        buffer.get(rawData);

        // 2. Create an index array to sort
        Integer[] sortIndices = new Integer[numParticles];
        for (int i = 0; i < numParticles; i++) {
            sortIndices[i] = i;
        }

        // 3. Sort by the 'key' (the 3rd int in each uvec4 block)
        Arrays.sort(sortIndices, (a, b) -> {
            int keyA = rawData[a * 4 + 2];
            int keyB = rawData[b * 4 + 2];
            return Integer.compare(keyA, keyB);
        });

        // 4. Prepare buffers for upload
        IntBuffer sortedIndicesBuffer = BufferUtils.createIntBuffer(numParticles * 4);
        IntBuffer offsetsBuffer = BufferUtils.createIntBuffer(numParticles);

        // Initialize offsets with numParticles (the "null" marker)
        int[] offsetsArray = new int[numParticles];
        Arrays.fill(offsetsArray, numParticles);

        int previousKey = -1;

        for (int i = 0; i < numParticles; i++) {
            int oldBase = sortIndices[i] * 4;
            int currentKey = rawData[oldBase + 2];

            // Rebuild sorted indices buffer
            sortedIndicesBuffer.put(rawData[oldBase]);     // originalIndex
            sortedIndicesBuffer.put(rawData[oldBase + 1]); // hash
            sortedIndicesBuffer.put(currentKey);           // key
            sortedIndicesBuffer.put(0);                    // padding

            // 5. Calculate Offsets
            // If the key changed, this is the start of a new cell's particle list
            if (currentKey != previousKey && currentKey < numParticles) {
                offsetsArray[currentKey] = i;
                previousKey = currentKey;
            }
        }

        sortedIndicesBuffer.flip();
        offsetsBuffer.put(offsetsArray).flip();

        // 6. Upload both back to GPU
        computeShader.setData(4, sortedIndicesBuffer); // SpatialIndices
        computeShader.setData(5, offsetsBuffer);       // SpatialOffsets
    }

    @Override
    public void destroy() {
        super.destroy();
        if (computeShader != null) {
            computeShader.cleanup();
        }
    }
}
