#version 430

layout(local_size_x = 1) in;

layout(std430, binding = 0) buffer PositionsBuffer {
    vec2 positions[];
};

layout(std430, binding = 1) buffer PredictedPositionsBuffer {
    vec2 predictedPositions[];
};

layout(std430, binding = 2) buffer VelocitiesBuffer {
    vec2 velocities[];
};

layout(std430, binding = 3) buffer DensitiesBuffer {
    vec2 densities[];
};

uniform float deltaTime;
uniform float smoothingRadius;
uniform float collisionDamping;
uniform vec2 boundsSize;
uniform int numParticles;
uniform int task;
uniform vec2 gravity;

// ===========================================================
// KERNELS
// ===========================================================

uniform float SpikyPow2ScalingFactor;
uniform float SpikyPow3ScalingFactor;
uniform float SpikyPow3DerivativeScalingFactor;
uniform float SpikyPow2DerivativeScalingFactor;
uniform float Poly6ScalingFactor;

float SpikyKernelPow2(float dst, float radius)
{
    if (dst < radius) {
        float v = radius - dst;
        return v * v * SpikyPow2ScalingFactor;
    }

    return 0.0;
}

float SpikyKernelPow3(float dst, float radius)
{
    if (dst < radius) {
        float v = radius - dst;
        return v * v * v * SpikyPow3ScalingFactor;
    }

    return 0.0;
}

float DerivativeSpikyPow3(float dst, float radius)
{
    if (dst <= radius) {
        float v = radius - dst;
        return -v * v * SpikyPow3DerivativeScalingFactor;
    }

    return 0;
}

float DerivativeSpikyPow2(float dst, float radius)
{
    if (dst <= radius) {
        float v = radius - dst;
        return -v * SpikyPow2DerivativeScalingFactor;
    }

    return 0;
}

float SmoothingKernelPoly6(float dst, float radius)
{
    if (dst < radius)
    {
        float v = radius * radius - dst * dst;
        return v * v * v * Poly6ScalingFactor;
    }
    return 0;
}

float DensityKernel(float dst, float radius)
{
    return SpikyKernelPow2(dst, radius);
}

float NearDensityKernel(float dst, float radius)
{
    return SpikyKernelPow3(dst, radius);
}

float DensityDerivative(float dst, float radius)
{
    return DerivativeSpikyPow2(dst, radius);
}

float NearDensityDerivative(float dst, float radius)
{
    return DerivativeSpikyPow3(dst, radius);
}

float ViscosityKernel(float dst, float radius)
{
    return SmoothingKernelPoly6(dst, smoothingRadius);
}

// ===========================================================
// ESSENTIAL FUNCTIONS
// ===========================================================
uniform float pressureMultiplier;
uniform float nearPressureMultiplier;
uniform float targetDensity;
uniform float viscosityStrength;

vec2 CalculateDensity(uint i)
{
    vec2 pos = vec2(predictedPositions[i]);
    float density = 0.0;
    float nearDensity = 0.0;
    float sqrRadius = smoothingRadius * smoothingRadius;

    for (uint neighbourIndex = 0u; neighbourIndex < numParticles; neighbourIndex++)
    {
        vec2 neighbourPos = vec2(predictedPositions[neighbourIndex]);
        vec2 offsetToNeighbour = neighbourPos - pos;
        float sqrDst = dot(offsetToNeighbour, offsetToNeighbour);

        if (sqrDst > sqrRadius) continue;

        float dst = sqrt(sqrDst);

        if (dst < smoothingRadius)
        {
            density += DensityKernel(dst, smoothingRadius);
            nearDensity += NearDensityKernel(dst, smoothingRadius);
        }
    }

    return vec2(density, nearDensity);
}

float PressureFromDensity(float density)
{
    return (density - targetDensity) * pressureMultiplier;
}

float NearPressureFromDensity(float nearDensity)
{
    return nearPressureMultiplier * (nearDensity - targetDensity);
}

vec2 CalculatePressureForce(uint i)
{
    vec2 pos = vec2(predictedPositions[i]);
    float density = densities[i].x;
    float densityNear = densities[i].y;

    float pressure = PressureFromDensity(density);
    float nearPressure = NearPressureFromDensity(densityNear);

    vec2 pressureForce = vec2(0.0);

    for (uint neighbourIndex = 0u; neighbourIndex < numParticles; neighbourIndex++) {
        if (neighbourIndex == i) continue;

        vec2 neighbourPos = vec2(predictedPositions[neighbourIndex]);
        vec2 offset = neighbourPos - pos;
        float sqrDst = dot(offset, offset);

        if (sqrDst > smoothingRadius * smoothingRadius) continue;

        float dst = sqrt(sqrDst);
        vec2 dir = dst > 0.0 ? offset / dst : vec2(0.0, 1.0);

        float neighbourDensity = densities[neighbourIndex].x;
        float neighbourNearDensity = densities[neighbourIndex].y;
        float neighbourPressure = PressureFromDensity(neighbourDensity);
        float neighbourNearPressure = NearPressureFromDensity(neighbourNearDensity);

        float sharedPressure = (pressure + neighbourPressure) * 0.5;
        float sharedNearPressure = (nearPressure + neighbourNearPressure) * 0.5;

        pressureForce += dir * DensityDerivative(dst, smoothingRadius) * sharedPressure / neighbourDensity;
        pressureForce += dir * NearDensityDerivative(dst, smoothingRadius) * sharedNearPressure / neighbourNearDensity;
    }

    return pressureForce / density * deltaTime;
}

vec2 CalculateViscosity(uint i)
{
    vec2 pos = vec2(predictedPositions[i]);
    float sqrRadius = smoothingRadius * smoothingRadius;

    vec2 viscosityForce = vec2(0, 0);
    vec2 velocity = vec2(velocities[i]);

        for (int neighbourIndex = 0; neighbourIndex < numParticles; neighbourIndex++) {
            if (neighbourIndex == i) continue;

            vec2 neighbourPos = vec2(predictedPositions[neighbourIndex]);
            vec2 offsetToNeighbour = neighbourPos - pos;
            float sqrDstToNeighbour = dot(offsetToNeighbour, offsetToNeighbour);

            if (sqrDstToNeighbour > sqrRadius) continue;

            float dst = sqrt(sqrDstToNeighbour);
            vec2 neighbourVelocity = vec2(velocities[neighbourIndex]);
            viscosityForce += (neighbourVelocity - velocity) * ViscosityKernel(dst, smoothingRadius);
        }

    return viscosityForce * viscosityStrength * deltaTime;
}

// ===========================================================
// MAIN
// ===========================================================

const float epsilon = 0.01;

void HandleCollisions(uint particleIndex)
{
    vec2 pos = vec2(positions[particleIndex]);
    vec2 vel = vec2(velocities[particleIndex]);

    const vec2 halfSize = boundsSize * 0.5;
    vec2 edgeDst = halfSize - abs(pos);

    if (edgeDst.x <= 0)
    {
        pos.x = halfSize.x * sign(pos.x);
        vel.x *= -1 * collisionDamping;
    }
    if (edgeDst.y <= 0)
    {
        pos.y = halfSize.y * sign(pos.y);
        vel.y *= -1 * collisionDamping;
    }

    positions[particleIndex] = pos;
    velocities[particleIndex] = vel;
}

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= numParticles) return;

    if (task == 1) {
        velocities[i] += gravity * deltaTime;
        predictedPositions[i] = positions[i] + velocities[i] / 120;
    }

    if (task == 2) {
        densities[i] = CalculateDensity(i);
    }

    if (task == 3) {
        velocities[i] += CalculatePressureForce(i);
    }

    if (task == 4) {
        velocities[i] += CalculateViscosity(i);
    }

    if (task == 4) {
        positions[i] += velocities[i] * deltaTime;
        HandleCollisions(i);
    }
}
