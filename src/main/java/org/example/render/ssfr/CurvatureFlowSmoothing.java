package org.example.render.ssfr;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class CurvatureFlowSmoothing implements SsfrSmoothing {

    private Material smoothMat;
    private Texture2D depthTexture;

    private FrameBuffer smoothFboA;
    private Texture2D smoothTexA;
    private FrameBuffer smoothFboB;
    private Texture2D smoothTexB;

    private Texture2D currentTexture;
    private int width;
    private int height;

    private CurvatureFlowSmoothingBean bean;

    public CurvatureFlowSmoothing(
        AssetManager assetManager,
        CurvatureFlowSmoothingBean bean
    ) {
        this.bean = bean;
        this.smoothMat = new Material(assetManager, "materials/ssfr/FluidSmoothCurvatureFlow.j3md");
    }

    @Override
    public void smooth(RenderManager rm, Geometry fsQuad) {
        final float fov = rm.getCurrentCamera().getFov();
        float aspect = (float)width / height;

        float invFocalLenY = (float)Math.tan(Math.toRadians(fov) * 0.5);
        float invFocalLenX = invFocalLenY * aspect;

        float cx = (2.0f * invFocalLenX) / width;
        float cy = (2.0f * invFocalLenY) / height;

        smoothMat.setVector2("TexelSize", new Vector2f(1f/width, 1f/height));
        smoothMat.setFloat("Cx", cx);
        smoothMat.setFloat("Cy", cy);

        smoothMat.setFloat("Dt", bean.getStep());

        this.currentTexture = depthTexture;
        fsQuad.setMaterial(smoothMat);
        for (int i = 0; i < bean.getNumberOfIterations(); i++) {
            FrameBuffer target = (i % 2 == 0) ? smoothFboA : smoothFboB;
            rm.getRenderer().setFrameBuffer(target);
            smoothMat.setTexture("DepthTex", currentTexture);
            rm.renderGeometry(fsQuad);
            currentTexture = (i % 2 == 0) ? smoothTexA : smoothTexB;
        }
    }

    public void setDepthTexture(Texture2D depthTexture) {
        this.depthTexture = depthTexture;
        this.currentTexture = depthTexture;
        width = depthTexture.getImage().getWidth();
        height = depthTexture.getImage().getHeight();

        if (
            this.smoothTexA == null
            || width != smoothTexA.getImage().getWidth()
            || height != smoothTexA.getImage().getHeight()
        ) {
            this.smoothTexA = new Texture2D(width, height, Image.Format.R32F);
            this.smoothFboA = new FrameBuffer(width, height, 1);
            this.smoothFboA.setColorTexture(this.smoothTexA);

            this.smoothTexB = new Texture2D(width, height, Image.Format.R32F);
            this.smoothFboB = new FrameBuffer(width, height, 1);
            this.smoothFboB.setColorTexture(this.smoothTexB);
        }
    }

    public Texture2D getOutputTexture() {
        return this.currentTexture;
    }
}
