package org.example;

import org.example.bean.Control;
import org.example.bean.ControlType;

public class SimulationBean {
    private int iterationsPerFrame = 1;

    private float gravityForce = 12.0f;

    private float smoothingRadius = 0.35f;

    private float pressureMultiplier = 120.0f;

    private float nearPressureMultiplier = 20f;

    private float targetDensity = 60f;

    private float viscosityStrength = 0.05f;

    private float boundsX = 4f;

    private float boundsY = 3f;

    private float boundsZ = 3f;

    @Control(type = ControlType.RANGE, min = 0, max = 10.0, step = 1)
    public int getIterationsPerFrame() {
        return iterationsPerFrame;
    }

    public void setIterationsPerFrame(int iterationsPerFrame) {
        this.iterationsPerFrame = iterationsPerFrame;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 10.0, step = 0.1)
    public float getSmoothingRadius() {
        return smoothingRadius;
    }

    public void setSmoothingRadius(float smoothingRadius) {
        this.smoothingRadius = smoothingRadius;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 100.0, step = 0.1)
    public float getPressureMultiplier() {
        return pressureMultiplier;
    }

    public void setPressureMultiplier(float pressureMultiplier) {
        this.pressureMultiplier = pressureMultiplier;
    }

    @Control(type = ControlType.RANGE, min = 1.0, max = 100.0, step = 0.1)
    public float getNearPressureMultiplier() {
        return nearPressureMultiplier;
    }

    public void setNearPressureMultiplier(float nearPressureMultiplier) {
        this.nearPressureMultiplier = nearPressureMultiplier;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 1000.0, step = 0.1)
    public float getTargetDensity() {
        return targetDensity;
    }

    public void setTargetDensity(float targetDensity) {
        this.targetDensity = targetDensity;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 100.0, step = 0.1)
    public float getBoundsX() {
        return boundsX;
    }

    public void setBoundsX(float boundsX) {
        this.boundsX = boundsX;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 100.0, step = 0.1)
    public float getBoundsY() {
        return boundsY;
    }

    public void setBoundsY(float boundsY) {
        this.boundsY = boundsY;
    }

    @Control(type = ControlType.RANGE, min = 0.1, max = 100.0, step = 0.1)
    public float getBoundsZ() {
        return boundsZ;
    }

    public void setBoundsZ(float boundsZ) {
        this.boundsZ = boundsZ;
    }

    @Control(type = ControlType.RANGE, min = 0.0, max = 10.0, step = 0.1)
    public float getGravityForce() {
        return gravityForce;
    }

    public void setGravityForce(float gravityForce) {
        this.gravityForce = gravityForce;
    }

    @Control(type = ControlType.RANGE, min = 0.0, max = 1.0, step = 0.01)
    public float getViscosityStrength() {
        return viscosityStrength;
    }

    public void setViscosityStrength(float viscosityStrength) {
        this.viscosityStrength = viscosityStrength;
    }
}
