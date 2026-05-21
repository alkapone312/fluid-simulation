package org.example.render.volume;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix4f;
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

    private FrameBuffer[] sliceFbos;
    private Texture3D gridTexture;

    private Material resampleMat;
    private Material raycastMat;
    private Geometry fsQuad;

    private FrameBuffer sceneFbo;
    private Texture2D sceneTex;
    private Texture2D sceneDepthTex;

    private int gridX;
    private int gridY;
    private int gridZ;

    private float nearPlane;
    private float farPlane;

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
        this.raycastMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Off);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        this.vp = vp;
        var w = vp.getCamera().getWidth();
        var h = vp.getCamera().getHeight();

        // Zostawiam Twoje 100, chociaż dla pełnej precyzji można by uzyć vp.getCamera().getFrustumFar()
        farPlane = 100;
        nearPlane = vp.getCamera().getFrustumNear();

        // Uruchamiamy generowanie tekstury DOPIERO po przypisaniu vp
        setupGridTexture();

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

    public void setupGridTexture() {
        // Z pobieramy z beana X oraz Y
        gridX = bean.getPerspectiveGridWidth();
        gridY = bean.getPerspectiveGridHeight();

        // -----------------------------------------------------------------
        // OBLICZANIE POPRAWNEJ LICZBY PLASTRÓW (Z) WG FRAEDRICH ET AL.
        // -----------------------------------------------------------------
        float n = nearPlane;
        float f = farPlane;

        // W JMonkeyEngine najszybciej i najdokładniej wyciągnąć tg(FOV/2) z frustuma:
        float top = vp.getCamera().getFrustumTop();

        // Krok 1: Obliczenie sigmy (współczynnika rozszerzania się perspektywy).
        // Równanie z artykułu: sigma = (2 * tan(fov_y / 2)) / res_y[cite: 119].
        // W JME: 2 * tan(fov_y / 2) to po prostu (top - bottom) / n
        float twoTanFovY = 2.0f * (top / n);
        float sigma = twoTanFovY / (float) gridY;

        // Krok 2: Wstępne wyliczenie liczby plastrów m[cite: 134].
        float m_initial = (float) Math.log(f / n) / sigma;

        // Krok 3: Obliczenie lambdy (korekcja dla promieni biegnących na brzegach frustuma)[cite: 131].
        float termX = (gridX * sigma) / 2.0f;
        float termY = (gridY * sigma) / 2.0f;
        float lambda = (float) Math.sqrt(termX * termX + termY * termY + 1.0f);

        // Krok 4: Ostateczna liczba plastrów (m musi być przemnożone przez lambdę)[cite: 137].
        gridZ = Math.round(m_initial * lambda);
        // -----------------------------------------------------------------

        System.out.println(gridX + " " + gridY + " " + gridZ);

        gridTexture = new Texture3D(gridX, gridY, gridZ, Image.Format.R16F);
        gridTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        gridTexture.setMagFilter(Texture.MagFilter.Bilinear);
        gridTexture.getImage().setData(new java.util.ArrayList<>());

        sliceFbos = new FrameBuffer[gridZ];
        for (int i = 0; i < gridZ; i++) {
            sliceFbos[i] = new FrameBuffer(gridX, gridY, 1);
            var textureTarget = FrameBuffer.FrameBufferTarget.newTarget(gridTexture);
            textureTarget.layer(i);
            sliceFbos[i].addColorTarget(textureTarget);
        }
    }

    @Override
    public void postQueue(RenderQueue rq) {}

    @Override
    public void reshape(ViewPort viewPort, int w, int h) {
        // Jeśli okno zmienia rozmiar, frustum też ulega zmianie,
        // więc teoretycznie należałoby tutaj przebudować siatkę (setupGridTexture).
    }

    @Override
    public boolean isInitialized() { return rm != null; }

    @Override
    public void preFrame(float tpf) {}

    @Override
    public void postFrame(FrameBuffer out) {
        int screenW = vp.getCamera().getWidth();
        int screenH = vp.getCamera().getHeight();

        rm.getRenderer().copyFrameBuffer(out, sceneFbo, true, true);

        raycastMat.setTexture("DepthTexture", sceneDepthTex);
        raycastMat.setTexture("SceneTexture", sceneTex);

        rm.getRenderer().setViewPort(0, 0, gridX, gridY);
        rm.getRenderer().clearBuffers(true, false, false);

        resampleMat.setFloat("NearPlane", nearPlane);
        resampleMat.setFloat("FarPlane", farPlane);
        resampleMat.setInt("NumSlices", gridZ);
        resampleMat.setFloat("ParticleRadius", bean.getParticleRadius());

        for (int i = 0; i < gridZ; i++) {
            rm.getRenderer().setFrameBuffer(sliceFbos[i]);
            rm.getRenderer().clearBuffers(true, false, false);

            resampleMat.setInt("CurrentSlice", i);

            rm.setForcedMaterial(resampleMat);
            rm.renderGeometry(particleGeometry);
        }
        rm.setForcedMaterial(null);

        rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
        // PRZYWRACAMY VIEWPORT NA PEŁNY EKRAN
        rm.getRenderer().setViewPort(0, 0, screenW, screenH);

        Matrix4f projInv = vp.getCamera().getProjectionMatrix().clone();
        projInv.invertLocal();

        Matrix4f viewInv = vp.getCamera().getViewMatrix().clone();
        viewInv.invertLocal();

        raycastMat.setTexture("GridTexture", gridTexture);
        raycastMat.setFloat("NearPlane", nearPlane);
        raycastMat.setFloat("FarPlane", farPlane);
        raycastMat.setInt("NumSlices", gridZ);
        raycastMat.setVector3("LightDir", new Vector3f(0.5f, 0.5f, -0.2f).normalizeLocal());
        raycastMat.setMatrix4("ProjectionMatrixInverse", projInv);
        raycastMat.setMatrix4("ViewMatrixInverse", viewInv);
        raycastMat.setFloat("FluidDensity", bean.getFluidDensity());

        fsQuad.setMaterial(raycastMat);
        rm.renderGeometry(fsQuad);
    }

    @Override
    public void cleanup() {}

    @Override
    public void setProfiler(AppProfiler profiler) {}
}