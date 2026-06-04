package pl.pwr.jkaplone.fluidsim.render.ssfr;

import pl.pwr.jkaplone.fluidsim.bean.Control;
import pl.pwr.jkaplone.fluidsim.bean.ControlType;

public class SsfrBean {
    private float particleRadius = 0.18f;

    private float thicknessMultiplier = 0.1f;

    private int applySmoothing = 1;

    private int debugDepth = 0;

    private int debugThickness = 0;

    private int debugNormal = 0;

    private int gaussianSmoothing = 1;

    private int curvatureFlowSmoothing = 0;

    @Control(type = ControlType.RANGE, min = 0, max = 1.0, step = 0.01)
    public float getParticleRadius() {
        return particleRadius;
    }

    public void setParticleRadius(float particleRadius) {
        this.particleRadius = particleRadius;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1.0, step = 0.001)
    public float getThicknessMultiplier() {
        return thicknessMultiplier;
    }

    public void setThicknessMultiplier(float thicknessMultiplier) {
        this.thicknessMultiplier = thicknessMultiplier;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getApplySmoothing() {
        return applySmoothing;
    }

    public void setApplySmoothing(int applySmoothing) {
        this.applySmoothing = applySmoothing;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getDebugDepth() {
        return debugDepth;
    }

    public void setDebugDepth(int debugDepth) {
        this.debugDepth = debugDepth;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getDebugThickness() {
        return debugThickness;
    }

    public void setDebugThickness(int debugThickness) {
        this.debugThickness = debugThickness;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getDebugNormal() {
        return debugNormal;
    }

    public void setDebugNormal(int debugNormal) {
        this.debugNormal = debugNormal;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getGaussianSmoothing() {
        return gaussianSmoothing;
    }

    public void setGaussianSmoothing(int gaussianSmoothing) {
        this.gaussianSmoothing = gaussianSmoothing;
    }

    @Control(type = ControlType.RANGE, min = 0, max = 1, step = 1)
    public int getCurvatureFlowSmoothing() {
        return curvatureFlowSmoothing;
    }

    public void setCurvatureFlowSmoothing(int curvatureFlowSmoothing) {
        this.curvatureFlowSmoothing = curvatureFlowSmoothing;
    }
}
