#version 430
#include <compute/utils/sph_kernels.glsl>
#include <compute/utils/spatial_hash.glsl>

layout(local_size_x = 64) in;

layout(std430, binding = 0) buffer PositionsBuffer {
    vec3 positions[];
};

layout(std430, binding = 1) buffer PredictedPositionsBuffer {
    vec3 predictedPositions[];
};

layout(std430, binding = 2) buffer VelocitiesBuffer {
    vec3 velocities[];
};

layout(std430, binding = 3) buffer DensitiesBuffer {
    vec2 densities[];
};

layout(std430, binding = 4) buffer SpatialIndicesBuffer {
    uvec3 spatialIndices[];
};

layout(std430, binding = 5) buffer SpatialOffsetsBuffer {
    uint spatialOffsets[];
};

uniform float deltaTime;
uniform float smoothingRadius;
uniform float collisionDamping;
uniform vec3 boundsSize;
uniform uint numParticles;
uniform uint task;
uniform vec3 gravity;
uniform float pressureMultiplier;
uniform float nearPressureMultiplier;
uniform float targetDensity;
uniform float viscosityStrength;


// ===========================================================
// SPATIAL LOOKUP
// ===========================================================
uniform uint groupWidth;
uniform uint groupHeight;
uniform uint stepIndex;

void sort(uint i) {
    uint hIndex = i & (groupWidth - 1);
    uint indexLeft = hIndex + (groupHeight + 1) * (i / groupWidth);
    uint rightStepSize = (stepIndex == 0) ? (groupHeight - 2 * hIndex) : ((groupHeight + 1) / 2);
    uint indexRight = indexLeft + rightStepSize;

    if (indexRight >= numParticles) return;

    if (spatialIndices[indexLeft].z > spatialIndices[indexRight].z) {
        uvec3 temp = uvec3(spatialIndices[indexLeft]);
        spatialIndices[indexLeft] = spatialIndices[indexRight];
        spatialIndices[indexRight] = temp;
    }
}

void calculateOffsets(uint i) {
    if (i >= numParticles) return;

    uint key = spatialIndices[i].z;
    uint keyPrev = (i == 0) ? numParticles : spatialIndices[i - 1].z;

    if (key != keyPrev) {
        spatialOffsets[key] = i;
    }
}

void UpdateSpatialHash(uint i)
{
    if (i >= numParticles) return;
    // Reset offsets (using numParticles as a 'null' or 'empty' marker)
    spatialOffsets[i] = uint(numParticles);

    uint index = i;

    ivec3 cell = GetCell3D(predictedPositions[index].xyz, smoothingRadius);
    uint hash = HashCell3D(cell);
    uint key = KeyFromHash(hash, uint(numParticles));

    spatialIndices[i] = uvec3(index, hash, key);
}

// ===========================================================
// SPH
// ===========================================================

vec2 CalculateDensity(uint x)
{
    vec3 pos = vec3(predictedPositions[x]);
    float density = 0.0;
    float nearDensity = 0.0;
    float sqrRadius = smoothingRadius * smoothingRadius;

    // Neighbour search
    ivec3 originCell = GetCell3D(pos, smoothingRadius);
    for (int i = 0; i < 27; i ++)
    {
        uint hash = HashCell3D(originCell + offsets3D[i]);
        uint key = KeyFromHash(hash, numParticles);
        uint currIndex = spatialOffsets[key];

        while (currIndex < numParticles)
        {
            uvec3 indexData = uvec3(spatialIndices[currIndex]);
            currIndex++;
            if (indexData.z != key) break;
            if (indexData.y != hash) continue;
            uint neighbourIndex = indexData.x;

            // CODE
            vec3 neighbourPos = vec3(predictedPositions[neighbourIndex]);
            vec3 offsetToNeighbour = neighbourPos - pos;
            float sqrDst = dot(offsetToNeighbour, offsetToNeighbour);

            if (sqrDst > sqrRadius) continue;

            float dst = sqrt(sqrDst);

            if (dst < smoothingRadius)
            {
                density += DensityKernel(dst, smoothingRadius);
                nearDensity += NearDensityKernel(dst, smoothingRadius);
            }
            // CODE
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
    return nearPressureMultiplier * nearDensity;
}

vec3 CalculatePressureForce(uint x)
{
    vec3 pos = vec3(predictedPositions[x]);
    float density = densities[x].x;
    float densityNear = densities[x].y;

    float pressure = PressureFromDensity(density);
    float nearPressure = NearPressureFromDensity(densityNear);

    vec3 pressureForce = vec3(0.0);

//    for (uint neighbourIndex = 0u; neighbourIndex < numParticles; neighbourIndex++) {
//
//    }

    // Neighbour search
    ivec3 originCell = GetCell3D(pos, smoothingRadius);
    for (int i = 0; i < 27; i++)
    {
        uint hash = HashCell3D(originCell + offsets3D[i]);
        uint key = KeyFromHash(hash, numParticles);
        uint currIndex = spatialOffsets[key];

        while (currIndex < numParticles)
        {
            uvec3 indexData = uvec3(spatialIndices[currIndex]);
            currIndex++;
            if (indexData.z != key) break;
            if (indexData.y != hash) continue;
            uint neighbourIndex = indexData.x;
            // CODE

            if (neighbourIndex == x) continue;

            vec3 neighbourPos = vec3(predictedPositions[neighbourIndex]);
            vec3 offset = neighbourPos - pos;
            float sqrDst = dot(offset, offset);

            if (sqrDst > smoothingRadius * smoothingRadius) continue;

            float dst = sqrt(sqrDst);
            vec3 dir = dst > 0.0 ? offset / dst : vec3(0.0, 1.0, 0.0);

            float neighbourDensity = densities[neighbourIndex].x;
            float neighbourNearDensity = densities[neighbourIndex].y;
            float neighbourPressure = PressureFromDensity(neighbourDensity);
            float neighbourNearPressure = NearPressureFromDensity(neighbourNearDensity);

            float sharedPressure = (pressure + neighbourPressure) * 0.5;
            float sharedNearPressure = (nearPressure + neighbourNearPressure) * 0.5;

            pressureForce += dir * DensityDerivative(dst, smoothingRadius) * sharedPressure / neighbourDensity;
            pressureForce += dir * NearDensityDerivative(dst, smoothingRadius) * sharedNearPressure / neighbourNearDensity;

            // CODE
        }
    }

    return pressureForce / density * deltaTime;
}

vec3 CalculateViscosity(uint x)
{
    vec3 pos = vec3(predictedPositions[x]);
    float sqrRadius = smoothingRadius * smoothingRadius;

    vec3 viscosityForce = vec3(0, 0, 0);
    vec3 velocity = vec3(velocities[x]);

    // Neighbour search
    ivec3 originCell = GetCell3D(pos, smoothingRadius);
    for (int i = 0; i < 27; i ++)
    {
        uint hash = HashCell3D(originCell + offsets3D[i]);
        uint key = KeyFromHash(hash, numParticles);
        uint currIndex = spatialOffsets[key];

        while (currIndex < numParticles)
        {
            uvec3 indexData = uvec3(spatialIndices[currIndex]);
            currIndex++;
            if (indexData[2] != key) break;
            if (indexData[1] != hash) continue;
            uint neighbourIndex = indexData.x;
            // CODE

            if (neighbourIndex == x) continue;

            vec3 neighbourPos = vec3(predictedPositions[neighbourIndex]);
            vec3 offsetToNeighbour = neighbourPos - pos;
            float sqrDstToNeighbour = dot(offsetToNeighbour, offsetToNeighbour);

            if (sqrDstToNeighbour > sqrRadius) continue;

            float dst = sqrt(sqrDstToNeighbour);
            vec3 neighbourVelocity = vec3(velocities[neighbourIndex]);
            viscosityForce += (neighbourVelocity - velocity) * ViscosityKernel(dst, smoothingRadius);

            // CODE
        }
    }

    return viscosityForce * viscosityStrength * deltaTime;
}

// ===========================================================
// MAIN
// ===========================================================

void HandleCollisions(uint particleIndex)
{
    vec3 pos = vec3(positions[particleIndex]);
    vec3 vel = vec3(velocities[particleIndex]);

    const vec3 halfSize = boundsSize * 0.5;
    vec3 edgeDst = halfSize - abs(pos);

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

    if (edgeDst.z <= 0)
    {
        pos.z = halfSize.z * sign(pos.z);
        vel.z *= -1 * collisionDamping;
    }

    positions[particleIndex] = pos;
    velocities[particleIndex] = vel;
}

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= numParticles) return;

    if (task == 1) {
        velocities[i] += gravity * deltaTime;
        predictedPositions[i] = positions[i] + velocities[i] * deltaTime;

        return;
    }

    if (task == 2) {
        UpdateSpatialHash(i);

        return;
    }

    if (task == 3) {
        sort(i);

        return;
    }

    if (task == 4) {
        calculateOffsets(i);

        return;
    }

    if (task == 5) {
        densities[i] = CalculateDensity(i);

        return;
    }

    if (task == 6) {
        velocities[i] += CalculatePressureForce(i);

        return;
    }

    if (task == 7) {
        velocities[i] += CalculateViscosity(i);

        return;
    }

    if (task == 8) {
        positions[i] += velocities[i] * deltaTime;
        HandleCollisions(i);

        return;
    }
}
