package pl.pwr.jkaplone.fluidsim.render;

import com.jme3.material.Material;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import pl.pwr.jkaplone.fluidsim.benchmark.GpuProfilable;
import pl.pwr.jkaplone.fluidsim.benchmark.GpuProfiler;
import pl.pwr.jkaplone.fluidsim.benchmark.RenderPassListener;

public class PlainParticleProcessor implements SceneProcessor, GpuProfilable {
    private RenderManager rm;

    private Geometry geometry;

    public PlainParticleProcessor(
        Geometry geometry,
        Material particleMaterial
    ) {
        this.geometry = geometry;
        this.geometry.setMaterial(particleMaterial);
        this.geometry.setCullHint(Spatial.CullHint.Never);
    }


    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        rm = renderManager;
    }

    @Override
    public void reshape(ViewPort viewPort, int i, int i1) {}

    @Override
    public boolean isInitialized() { return false; }

    @Override
    public void preFrame(float v) {}

    @Override
    public void postQueue(RenderQueue renderQueue) {
        profiler.start("Render");
        rm.renderGeometry(this.geometry);
        profiler.stop("Render");
    }

    @Override
    public void postFrame(FrameBuffer frameBuffer) {}

    @Override
    public void cleanup() {}

    @Override
    public void setProfiler(AppProfiler appProfiler) {}

    private GpuProfiler profiler = new GpuProfiler();

    @Override
    public void setRenderPassListener(RenderPassListener listener) {
        profiler.setListener(listener);
    }
}
