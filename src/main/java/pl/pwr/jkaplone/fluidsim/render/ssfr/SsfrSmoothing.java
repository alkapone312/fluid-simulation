package pl.pwr.jkaplone.fluidsim.render.ssfr;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture2D;

public interface SsfrSmoothing {
    void smooth(RenderManager rm, Geometry fsQuad);

    void setDepthTexture(Texture2D depthTexture);

    Texture2D getOutputTexture();
}
