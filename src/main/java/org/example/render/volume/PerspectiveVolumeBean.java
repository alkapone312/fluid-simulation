package org.example.render.volume;

import org.example.bean.Control;
import org.example.bean.ControlType;

public class PerspectiveVolumeBean {
    private int perspectiveGridWidth = 128;
    private int perspectiveGridHeight = 128;

    private float fluidDensity = 1.0f;
    private float particleRadius = 0.35f;

    @Control(type = ControlType.RANGE, min = 1.0, max = 2000.0, step = 1)
    public float getFluidDensity() {
        return fluidDensity;
    }

    public void setFluidDensity(float fluidDensity) {
        this.fluidDensity = fluidDensity;
    }

    @Control(type = ControlType.RANGE, min = 16.0, max = 1024, step = 1)
    public int getPerspectiveGridWidth() {
        return perspectiveGridWidth;
    }

    public void setPerspectiveGridWidth(int perspectiveGridWidth) {
        this.perspectiveGridWidth = perspectiveGridWidth;
    }

    @Control(type = ControlType.RANGE, min = 16.0, max = 1024, step = 1)
    public int getPerspectiveGridHeight() {
        return perspectiveGridHeight;
    }

    public void setPerspectiveGridHeight(int perspectiveGridHeight) {
        this.perspectiveGridHeight = perspectiveGridHeight;
    }

    @Control(type = ControlType.RANGE, min = 0.25, max = 5.0, step = 0.1)
    public float getParticleRadius() {
        return particleRadius;
    }

    public void setParticleRadius(float particleRadius) {
        this.particleRadius = particleRadius;
    }
}