package pl.pwr.jkaplone.fluidsim.render.ssfr;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class GaussianSmoothing implements SsfrSmoothing {

    private Material smoothMat;
    private Texture2D depthTexture;
    private Texture2D tempTexture;
    private FrameBuffer tempFbo;
    private Texture2D outputTexture;
    private FrameBuffer outputFbo;
    GaussianSmoothingBean bean;

    public GaussianSmoothing(
        AssetManager assetManager,
        GaussianSmoothingBean bean
    ) {
        this.smoothMat = new Material(assetManager, "materials/ssfr/FluidSmoothGaussian.j3md");
        this.bean = bean;
    }

    @Override
    public void smooth(RenderManager rm, Geometry fsQuad) {
        smoothMat.setFloat("smoothness", bean.getSmoothness());
        smoothMat.setFloat("depthDiffStrength", bean.getDepthDiffStrength());
        smoothMat.setInt("blurSize", bean.getBlurSize());

        fsQuad.setMaterial(smoothMat);
        rm.getRenderer().setFrameBuffer(tempFbo);
        smoothMat.setBoolean("horizontal", false);
        smoothMat.setTexture("DepthTex", depthTexture);
        rm.renderGeometry(fsQuad);
        rm.getRenderer().setFrameBuffer(outputFbo);
        smoothMat.setBoolean("horizontal", true);
        smoothMat.setTexture("DepthTex", tempTexture);
        rm.renderGeometry(fsQuad);
    }

    public void setDepthTexture(Texture2D depthTexture) {
        this.depthTexture = depthTexture;
        var w = depthTexture.getImage().getWidth();
        var h = depthTexture.getImage().getHeight();
        if (
            this.outputTexture == null
                || w != outputTexture.getImage().getWidth()
                || h != outputTexture.getImage().getHeight()
        ) {
            this.outputTexture = new Texture2D(w, h, Image.Format.R32F);
            this.outputFbo = new FrameBuffer(w, h, 1);
            this.outputFbo.setColorTexture(outputTexture);
            this.tempTexture = new Texture2D(w, h, Image.Format.R32F);
            this.tempFbo = new FrameBuffer(w, h, 1);
            this.tempFbo.setColorTexture(tempTexture);
        }
    }

    public Texture2D getOutputTexture() {
        return this.outputTexture;
    }
}
