package org.example.render.volume;

import org.example.bean.Control;
import org.example.bean.ControlType;

public class PerspectiveVolumeBean {
    private float fluidDensity = 100.0f;

    @Control(type = ControlType.RANGE, min = 1.0, max = 2000.0, step = 1)
    public float getFluidDensity() {
        return fluidDensity;
    }

    public void setFluidDensity(float fluidDensity) {
        this.fluidDensity = fluidDensity;
    }
}