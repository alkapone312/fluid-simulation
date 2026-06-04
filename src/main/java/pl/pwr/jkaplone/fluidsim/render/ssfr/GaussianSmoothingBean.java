package pl.pwr.jkaplone.fluidsim.render.ssfr;

import pl.pwr.jkaplone.fluidsim.bean.Control;
import pl.pwr.jkaplone.fluidsim.bean.ControlType;

public class GaussianSmoothingBean {
    private int blurSize = 30;

    private float smoothness = 1.0f;

    private float depthDiffStrength = 0.5f;

    @Control(type = ControlType.RANGE, min = 1, max = 100.0, step = 1)
    public int getBlurSize() {
        return blurSize;
    }

    public void setBlurSize(int blurSize) {
        this.blurSize = blurSize;
    }

    @Control(type = ControlType.RANGE, min = 0.0, max = 10.0, step = 0.01)
    public float getSmoothness() {
        return smoothness;
    }

    public void setSmoothness(float smoothness) {
        this.smoothness = smoothness;
    }

    @Control(type = ControlType.RANGE, min = 0.0, max = 10.0, step = 0.01)
    public float getDepthDiffStrength() {
        return depthDiffStrength;
    }

    public void setDepthDiffStrength(float depthDiffStrength) {
        this.depthDiffStrength = depthDiffStrength;
    }
}
