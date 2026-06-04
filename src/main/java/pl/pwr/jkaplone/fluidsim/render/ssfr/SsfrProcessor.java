package pl.pwr.jkaplone.fluidsim.render.ssfr;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import pl.pwr.jkaplone.fluidsim.benchmark.GpuProfilable;
import pl.pwr.jkaplone.fluidsim.benchmark.GpuProfiler;
import pl.pwr.jkaplone.fluidsim.benchmark.RenderPassListener;
import pl.pwr.jkaplone.fluidsim.utils.TextureUtils;

public class SsfrProcessor implements SceneProcessor, GpuProfilable {
    private RenderManager rm;
    private ViewPort vp;

    private Geometry particleGeometry;

    private FrameBuffer depthFbo;
    private Texture2D depthTex;

    private FrameBuffer thicknessFbo;
    private Texture2D thicknessTex;

    private FrameBuffer sceneFbo;
    private Texture2D sceneTex;
    private Texture2D sceneDepthTex;

    private Material depthMat;
    private Material thicknessMat;
    private Material shadeMat;

    private Geometry fsQuad;

    private SsfrBean ssfrBean;
    private SsfrSmoothing ssfrSmoothing;

    private int w;
    private int h;

    private GpuProfiler profiler = new GpuProfiler();

    public SsfrProcessor(
        AssetManager assetManager,
        Geometry particleGeometry,
        SsfrBean ssfrBean,
        SsfrSmoothing smoothing
    ) {
        this.particleGeometry = particleGeometry;
        this.depthMat = new Material(assetManager, "materials/ssfr/FluidDepth.j3md");
        this.shadeMat =  new Material(assetManager, "materials/ssfr/FluidShade.j3md");
        this.thicknessMat = new Material(assetManager, "materials/ssfr/FluidThickness.j3md");
        this.ssfrBean = ssfrBean;
        this.ssfrSmoothing = smoothing;
    }

    public void setSsfrSmoothing(SsfrSmoothing ssfrSmoothing) {
        this.ssfrSmoothing = ssfrSmoothing;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        this.vp = vp;
        w = vp.getCamera().getWidth();
        h = vp.getCamera().getHeight();

        depthTex = new Texture2D(w, h, Image.Format.R32F);
        depthFbo = new FrameBuffer(w, h, 1);
        depthFbo.setDepthBuffer(Image.Format.Depth);
        depthFbo.setColorTexture(depthTex);

        thicknessTex = new Texture2D(w, h, Image.Format.R32F);
        thicknessFbo = new FrameBuffer(w, h, 1);
        thicknessFbo.setColorTexture(thicknessTex);

        sceneTex = new Texture2D(w, h, Image.Format.RGBA32F);
        sceneDepthTex = new Texture2D(w, h, Image.Format.Depth);
        sceneFbo = new FrameBuffer(w, h, 1);
        sceneFbo.setDepthBuffer(Image.Format.Depth);
        sceneFbo.setDepthTexture(sceneDepthTex);
        sceneFbo.setColorTexture(sceneTex);

        Quad q = new Quad(w, h);
        fsQuad = new Geometry("FullscreenQuad", q);
        fsQuad.setLocalTranslation(0, 0, -1);
        System.out.println("Screen resolution: " + w + "x" + h);
        System.out.println("Depth texture size: " + Math.ceil(TextureUtils.getTexture2DSizeInMB(depthTex) * 100) / 100f + "MB");
        System.out.println("Thickness texture size: " + Math.ceil(TextureUtils.getTexture2DSizeInMB(depthTex) * 100) / 100f + "MB");
    }

    @Override
    public void postQueue(RenderQueue rq) {
    }

    @Override
    public void reshape(ViewPort viewPort, int i, int i1) {}

    @Override
    public boolean isInitialized() {
        return rm != null;
    }

    @Override
    public void preFrame(float v) {}
    @Override
    public void postFrame(FrameBuffer frameBuffer) {
        if (rm == null) {
            return;
        }

        rm.getRenderer().copyFrameBuffer(frameBuffer, sceneFbo, true, true);

        // 1. Render Depth
        profiler.start("Depth");
        depthMat.setFloat("viewportHeight", h);
        depthMat.setFloat("particleRadius", ssfrBean.getParticleRadius());
        rm.getRenderer().setFrameBuffer(depthFbo);
        rm.getRenderer().clearBuffers(true, true, true);
        rm.setForcedMaterial(depthMat);
        rm.renderGeometry(particleGeometry);
        rm.setForcedMaterial(null);
        profiler.stop("Depth");

        // 2. Render thickness
        profiler.start("Thickness");
        thicknessMat.setFloat("viewportHeight", h);
        thicknessMat.setFloat("particleRadius", ssfrBean.getParticleRadius());
        thicknessMat.setFloat("thicknessMultiplier", ssfrBean.getThicknessMultiplier());
        rm.getRenderer().setFrameBuffer(thicknessFbo);
        rm.getRenderer().clearBuffers(true, true, true);
        rm.setForcedMaterial(thicknessMat);
        rm.renderGeometry(particleGeometry);
        rm.setForcedMaterial(null);
        profiler.stop("Thickness");

        // 3. Smooth
        profiler.start("Smooth");
        ssfrSmoothing.setDepthTexture(depthTex);
        ssfrSmoothing.smooth(rm, fsQuad);
        profiler.stop("Smooth");

        // 4. Final Shade
        profiler.start("Final Shade");
        rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
        shadeMat.setBoolean("DebugDepth", ssfrBean.getDebugDepth() == 1);
        shadeMat.setBoolean("DebugThickness", ssfrBean.getDebugThickness() == 1);
        shadeMat.setBoolean("DebugNormal", ssfrBean.getDebugNormal() == 1);
        shadeMat.setTexture("SceneTex", sceneTex);
        shadeMat.setTexture("SceneDepthTex", sceneDepthTex);
        shadeMat.setTexture("SmoothedDepthTex", ssfrBean.getApplySmoothing() == 1 ? ssfrSmoothing.getOutputTexture() : depthTex);
        shadeMat.setTexture("ThicknessTex", thicknessTex);
        shadeMat.setVector2("TexelSize", new Vector2f(1f/w, 1f/h));

        Vector3f worldLightDir = new Vector3f(0.5f, 0.5f, -0.2f).normalizeLocal();
        Vector3f viewSpaceLightDir = new Vector3f();
        vp.getCamera().getViewMatrix().multNormal(worldLightDir, viewSpaceLightDir);
        shadeMat.setVector3("LightDir", viewSpaceLightDir);

        shadeMat.setMatrix4("ProjectionMatrixInverse", vp.getCamera().getProjectionMatrix().invert());

        fsQuad.setMaterial(shadeMat);
        rm.renderGeometry(fsQuad);
        profiler.stop("Final Shade");
    }

    @Override
    public void cleanup() {}

    @Override
    public void setProfiler(AppProfiler appProfiler) {}

    @Override
    public void setRenderPassListener(RenderPassListener listener) {
        this.profiler.setListener(listener);
    }
}
