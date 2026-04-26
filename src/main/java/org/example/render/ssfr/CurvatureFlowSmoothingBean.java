package org.example.render.ssfr;

import org.example.bean.Control;
import org.example.bean.ControlType;

public class CurvatureFlowSmoothingBean {
    private int numberOfIterations = 1;

    private float step = 0.001f;


    @Control(type = ControlType.RANGE, min = 0.0, max = 1000, step = 1)
    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    @Control(type = ControlType.RANGE, min = 0.0, max = 0.1, step = 0.001)
    public float getStep() {
        return step;
    }

    public void setStep(float step) {
        this.step = step;
    }
}
