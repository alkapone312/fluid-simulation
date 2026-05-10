package org.example;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.bounding.BoundingBox;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FluidSimulation {

    private final int numParticles;
    private final ComputeShader computeShader;
    private final Geometry geometry;
    private final SimulationBean bean;

    private final List<com.jme3.math.Matrix4f> prevTransforms = new ArrayList<>();
    private final List<Geometry> collidables = new ArrayList<>();
    private FloatBuffer transformBuffer;
    private FloatBuffer triangleBuffer;
    private int numTriangles = 0;

    public FluidSimulation(int numParticles, AssetManager assetManager, SimulationBean bean) {
        this.numParticles = numParticles;
        this.bean = bean;

        this.computeShader = ComputeShaderLoader.load("compute/simulation_3d.glsl");
        initBuffers();
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[numParticles * 3]);
        mesh.setBound(new BoundingBox(Vector3f.ZERO, 1000f, 1000f, 1000f));
        mesh.setStatic();

        this.geometry = new Geometry("FluidParticles", mesh);
    }

    private void initBuffers() {
        FloatBuffer posBuffer = BufferUtils.createFloatBuffer(numParticles * 4);
        FloatBuffer velBuffer = BufferUtils.createFloatBuffer(numParticles * 4);
        FloatBuffer densBuffer = BufferUtils.createFloatBuffer(numParticles * 2);
        IntBuffer spatialIndices = BufferUtils.createIntBuffer(numParticles);
        IntBuffer spatialKeys = BufferUtils.createIntBuffer(numParticles);
        IntBuffer spatialOffsets = BufferUtils.createIntBuffer(numParticles);
        IntBuffer countBuffer = BufferUtils.createIntBuffer(numParticles);
        IntBuffer groupSumBuffer = BufferUtils.createIntBuffer(numParticles);
        IntBuffer sortedItemsBuffer = BufferUtils.createIntBuffer(numParticles);
        IntBuffer sortedKeysBuffer = BufferUtils.createIntBuffer(numParticles);

        int particlesPerRow = (int) Math.round(Math.pow(numParticles, 1.0/3.0));
        float spacing = 3.0f / 15.0f;
        float boxSize = ((particlesPerRow - 1) * spacing) / 2.0f;

        for (int i = 0; i < numParticles; i++) {
            int x = i % particlesPerRow;
            int y = (i / particlesPerRow) % particlesPerRow;
            int z = i / (particlesPerRow * particlesPerRow);

            posBuffer.put(-boxSize + x * spacing).put(-boxSize + y * spacing).put(-boxSize + z * spacing).put(1.0f);
            velBuffer.put(0).put(0).put(0).put(0);
            densBuffer.put(0).put(0);
            spatialIndices.put(0);
            spatialKeys.put(0);
            spatialOffsets.put(0);
            countBuffer.put(0);
            groupSumBuffer.put(0);
            sortedItemsBuffer.put(0);
            sortedKeysBuffer.put(0);
        }
        posBuffer.flip();
        velBuffer.flip();
        densBuffer.flip();
        spatialIndices.flip();
        spatialKeys.flip();
        spatialOffsets.flip();
        countBuffer.flip();
        groupSumBuffer.flip();
        sortedItemsBuffer.flip();
        sortedKeysBuffer.flip();

        computeShader.setData(0, posBuffer);
        computeShader.setData(1, velBuffer);
        computeShader.setData(2, posBuffer);
        computeShader.setData(3, densBuffer);
        computeShader.setData(4, spatialIndices);
        computeShader.setData(5, spatialKeys);
        computeShader.setData(6, spatialOffsets);
        computeShader.setData(7, countBuffer);
        computeShader.setData(8, groupSumBuffer);
        computeShader.setData(9, sortedItemsBuffer);
        computeShader.setData(10, sortedKeysBuffer);
    }

    public void update(float tpf) {
        float deltaTime = Math.min(tpf / bean.getIterationsPerFrame(), 1.0f/60.0f);
        computeShader.bind();

        updateTransforms();
        updateUniforms(deltaTime);

        for (int i = 0; i < bean.getIterationsPerFrame(); i++) {
            step();
        }
    }

    public Geometry getGeometry() { return geometry; }

    public void cleanup() {
        if (computeShader != null) computeShader.cleanup();
    }

    private void updateTransforms() {
        if (collidables.isEmpty()) return;

        int requiredFloats = collidables.size() * 16 * 4;
        if (transformBuffer == null || transformBuffer.capacity() < requiredFloats) {
            transformBuffer = BufferUtils.createFloatBuffer(requiredFloats);
        }

        transformBuffer.clear();
        float[] matrixArray = new float[16];

        for (int i = 0; i < collidables.size(); i++) {
            Geometry geom = collidables.get(i);
            com.jme3.math.Matrix4f currentMat = geom.getWorldMatrix();
            com.jme3.math.Matrix4f prevMat = prevTransforms.get(i);

            com.jme3.math.Matrix4f currentInv = currentMat.invert();
            com.jme3.math.Matrix4f prevInv = prevMat.invert();

            currentMat.fillFloatArray(matrixArray, true);
            transformBuffer.put(matrixArray);

            prevMat.fillFloatArray(matrixArray, true);
            transformBuffer.put(matrixArray);

            currentInv.fillFloatArray(matrixArray, true);
            transformBuffer.put(matrixArray);

            prevInv.fillFloatArray(matrixArray, true);
            transformBuffer.put(matrixArray);

            prevTransforms.set(i, currentMat.clone());
        }

        transformBuffer.flip();
        computeShader.setData(12, transformBuffer);
    }

    private void updateUniforms(float deltaTime) {
        float r = bean.getSmoothingRadius();
//        computeShader.setUniform("SpikyPow3ScalingFactor", (float) (10f / (Math.PI * Math.pow(r, 5))));
//        computeShader.setUniform("SpikyPow2ScalingFactor", (float) (6f / (Math.PI * Math.pow(r, 4))));
//        computeShader.setUniform("SpikyPow3DerivativeScalingFactor", (float) (30f / (Math.pow(r, 5) * Math.PI)));
//        computeShader.setUniform("SpikyPow2DerivativeScalingFactor", (float) (12f / (Math.pow(r, 4) * Math.PI)));
//        computeShader.setUniform("Poly6ScalingFactor", (float) (4 / (Math.PI * Math.pow(r, 8))));

        computeShader.setUniform("SpikyPow3ScalingFactor", (float) (15f / (Math.PI * Math.pow(bean.getSmoothingRadius(), 6))));
        computeShader.setUniform("SpikyPow2ScalingFactor", (float) (15f / (2 * Math.PI * Math.pow(bean.getSmoothingRadius(), 5))));
        computeShader.setUniform("SpikyPow3DerivativeScalingFactor", (float) (45f / (Math.pow(bean.getSmoothingRadius(), 6) * Math.PI)));
        computeShader.setUniform("SpikyPow2DerivativeScalingFactor", (float) (15f / (Math.pow(bean.getSmoothingRadius(), 5) * Math.PI)));
        computeShader.setUniform("Poly6ScalingFactor", (float) (315 / (64 * Math.PI * Math.pow(bean.getSmoothingRadius(), 9))));

        computeShader.setUniform("pressureMultiplier", bean.getPressureMultiplier());
        computeShader.setUniform("viscosityStrength", bean.getViscosityStrength());
        computeShader.setUniform("nearPressureMultiplier", bean.getNearPressureMultiplier());
        computeShader.setUniform("targetDensity", bean.getTargetDensity());
        computeShader.setUniform("boundsSize", bean.getBoundsX(), bean.getBoundsY(), bean.getBoundsZ());
        computeShader.setUniform("numParticles", numParticles, ComputeShader.IntegerType.UNSIGNED);
        computeShader.setUniform("smoothingRadius", r);
        computeShader.setUniform("deltaTime", deltaTime);
        computeShader.setUniform("gravity", 0, -bean.getGravityForce(), 0);
        computeShader.setUniform("collisionDamping", 0.8f);
        computeShader.setUniform("numTriangles", numTriangles, ComputeShader.IntegerType.UNSIGNED);
    }

    private void step() {
        int groups = (numParticles + 63) / 64;
        runTask(1, groups); // Predict
        runTask(2, groups); // Hash
        runTask(3, groups); // Clear Counts
        runTask(4, groups); // Count
        blellochScan(computeShader.getBufferId(7), numParticles); // Scan
        runTask(7, groups); // Scatter
        runTask(8, groups); // CopyBack
        runTask(9, groups); // Offsets
        runTask(10, groups); // Density
        runTask(11, groups); // Pressure
        runTask(12, groups); // Viscosity
        runTask(13, groups); // Integrate
    }

    private HashMap<Integer, Integer> buffers = new HashMap<>();

    private void blellochScan(int gpuBufferId, int elementsSize) {
        int maxElementsPerGroup = 2 * 64;
        int numGroups = (int) Math.ceil((float) elementsSize / maxElementsPerGroup);

        int groupsSumGpuBufferId = buffers.computeIfAbsent(numGroups, computeShader::createGpuBuffer);
        computeShader.bindBufferToSlot(7, gpuBufferId);
        computeShader.bindBufferToSlot(8, groupsSumGpuBufferId);
        computeShader.setUniform("scanItemCount", elementsSize, ComputeShader.IntegerType.UNSIGNED);

        runTask(5, numGroups);
        if (numGroups > 1) {
            blellochScan(groupsSumGpuBufferId, numGroups);

            computeShader.bindBufferToSlot(7, gpuBufferId);
            computeShader.bindBufferToSlot(8, groupsSumGpuBufferId);
            computeShader.setUniform("scanItemCount", elementsSize, ComputeShader.IntegerType.UNSIGNED);
            runTask(6, numGroups);
        }
    }

    public void registerCollidable(Geometry geom) {
        int objectId = collidables.size();
        collidables.add(geom);
        prevTransforms.add(geom.getWorldMatrix().clone());
        Mesh mesh = geom.getMesh();
        int triCount = mesh.getTriangleCount();
        FloatBuffer newBuffer = BufferUtils.createFloatBuffer((numTriangles + triCount) * 3 * 4);

        if (triangleBuffer != null) {
            triangleBuffer.rewind();
            newBuffer.put(triangleBuffer);
        }

        for (int i = 0; i < triCount; i++) {
            com.jme3.math.Triangle tri = new com.jme3.math.Triangle();
            mesh.getTriangle(i, tri);

            Vector3f v1 = tri.get1();
            Vector3f v2 = tri.get2();
            Vector3f v3 = tri.get3();

            float floatObjId = Float.intBitsToFloat(objectId);

            newBuffer.put(v1.x).put(v1.y).put(v1.z).put(floatObjId);
            newBuffer.put(v2.x).put(v2.y).put(v2.z).put(floatObjId);
            newBuffer.put(v3.x).put(v3.y).put(v3.z).put(floatObjId);
        }

        newBuffer.flip();
        this.triangleBuffer = newBuffer;
        this.numTriangles += triCount;
        computeShader.setData(11, triangleBuffer);
    }

    private void runTask(int taskID, int groups) {
        computeShader.setUniform("task", taskID, ComputeShader.IntegerType.UNSIGNED);
        computeShader.dispatch(groups, 1, 1);
    }
}
