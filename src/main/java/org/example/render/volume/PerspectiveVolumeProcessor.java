package org.example.render.volume;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.*;

public class PerspectiveVolumeProcessor implements SceneProcessor {
    private RenderManager rm;
    private ViewPort vp;
    private Geometry particleGeometry;

    private FrameBuffer gridFbo;
    private Texture3D gridTexture;

    private Material resampleMat;
    private Material raycastMat;
    private Geometry fsQuad;

    private FrameBuffer sceneFbo;
    private Texture2D sceneTex;
    private Texture2D sceneDepthTex;

    private int gridX = 256;
    private int gridY = 256;
    private int gridZ = 128; // Number of slices (m)

    private float nearPlane;
    private float farPlane;
    private float particleRadius = 0.5f;

    private PerspectiveVolumeBean bean;

    public PerspectiveVolumeProcessor(
        AssetManager assetManager,
        Geometry particleGeometry,
        PerspectiveVolumeBean bean
    ) {
        this.bean = bean;
        this.particleGeometry = particleGeometry;

        this.resampleMat = new Material(assetManager, "materials/volume/Resample.j3md");
        this.resampleMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
        this.resampleMat.getAdditionalRenderState().setDepthTest(false);
        this.resampleMat.getAdditionalRenderState().setDepthWrite(false);

        this.raycastMat = new Material(assetManager, "materials/volume/Raycast.j3md");
        this.raycastMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.PremultAlpha);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        this.vp = vp;
        var w = vp.getCamera().getWidth();
        var h = vp.getCamera().getHeight();
        farPlane = vp.getCamera().getFrustumFar();
        nearPlane = vp.getCamera().getFrustumNear();
        // Create the 3D Perspective Grid Texture
        gridTexture = new Texture3D(gridX, gridY, gridZ, Image.Format.R16F);
        gridTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        gridTexture.setMagFilter(Texture.MagFilter.Bilinear);
        gridTexture.getImage().setData(new java.util.ArrayList<>());

        // FBO for layered rendering into the 3D texture
        gridFbo = new FrameBuffer(gridX, gridY, 1);
        var textureTarget = FrameBuffer.FrameBufferTarget.newTarget(gridTexture);
        textureTarget.layer(0);
        gridFbo.addColorTarget(textureTarget);

        sceneTex = new Texture2D(w, h, Image.Format.RGBA32F);
        sceneDepthTex = new Texture2D(w, h, Image.Format.Depth);
        sceneFbo = new FrameBuffer(w, h, 1);
        sceneFbo.setDepthBuffer(Image.Format.Depth);
        sceneFbo.setDepthTexture(sceneDepthTex);
        sceneFbo.setColorTexture(sceneTex);

        Quad q = new Quad(vp.getCamera().getWidth(), vp.getCamera().getHeight());
        fsQuad = new Geometry("FullscreenQuad", q);
        fsQuad.updateGeometricState();
        fsQuad.setLocalTranslation(0, 0, -1);
    }

    @Override
    public void postQueue(RenderQueue rq) {}

    @Override
    public void reshape(ViewPort viewPort, int w, int h) {}
    @Override
    public boolean isInitialized() { return rm != null; }
    @Override
    public void preFrame(float tpf) {}
    @Override
    public void postFrame(FrameBuffer out) {
        int screenW = vp.getCamera().getWidth();
        int screenH = vp.getCamera().getHeight();
        rm.getRenderer().copyFrameBuffer(out, sceneFbo, true, true);

        // Pobranie tekstury głębi z głównego bufora (jeśli silnik na to pozwala)
        // lub przekazanie jej z poprzedniego passu
        raycastMat.setTexture("DepthTexture", sceneDepthTex);

        // 1. Pass: Voxelizacja do tekstury 3D
        rm.getRenderer().setFrameBuffer(gridFbo);
        // USTAWIAMY VIEWPORT NA ROZMIAR SIATKI
        rm.getRenderer().setViewPort(0, 0, gridX, gridY);
        rm.getRenderer().clearBuffers(true, false, false);

        resampleMat.setFloat("NearPlane", nearPlane);
        resampleMat.setFloat("FarPlane", farPlane);
        resampleMat.setInt("NumSlices", gridZ);
        resampleMat.setFloat("ParticleRadius", particleRadius);

        rm.setForcedMaterial(resampleMat);
        rm.renderGeometry(particleGeometry);
        rm.setForcedMaterial(null);

        // 2. Pass: Ray-casting na ekran
        rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
        // PRZYWRACAMY VIEWPORT NA PEŁNY EKRAN
        rm.getRenderer().setViewPort(0, 0, screenW, screenH);

        raycastMat.setTexture("GridTexture", gridTexture);
        raycastMat.setFloat("NearPlane", nearPlane);
        raycastMat.setFloat("FarPlane", farPlane);
        raycastMat.setInt("NumSlices", gridZ);
        raycastMat.setVector3("LightDir", new Vector3f(0.5f, 0.5f, 0.5f).normalizeLocal());
        raycastMat.setMatrix4("ProjectionMatrixInverse", vp.getCamera().getProjectionMatrix().invert());
        raycastMat.setFloat("FluidDensity", bean.getFluidDensity());

        fsQuad.setMaterial(raycastMat);
        rm.renderGeometry(fsQuad);
    }

    @Override
    public void cleanup() {}
    @Override
    public void setProfiler(AppProfiler profiler) {}
}