package org.example.render.ssfr;

import org.example.bean.Control;
import org.example.bean.ControlType;

public class SsfrBean {
    private float particleRadius = 0.25f;

    private float thicknessMultiplier = 0.1f;

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
}
