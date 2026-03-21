#version 430
#include <compute/utils/sph_kernels.glsl>
#include <compute/utils/spatial_hash.glsl>

layout(local_size_x = 64) in;

layout(std430, binding = 0) buffer PositionsBuffer {
    vec3 positions[];
};

layout(std430, binding = 1) buffer VelocitiesBuffer {
    vec3 velocities[];
};

layout(std430, binding = 2) buffer PredictedPositionsBuffer {
    vec3 predictedPositions[];
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

layout(std430, binding = 7) buffer TriangleBuffer {
    vec4 triangles[]; // v0, v1, v2 packed as vec4s
};

layout(std430, binding = 8) buffer TransformBuffer {
    mat4 transforms[];
};

uniform float deltaTime;
uniform float smoothingRadius;
uniform float collisionDamping;
uniform vec3 boundsSize;
uniform uint numParticles;
uniform uint numTriangles; // collision triangles
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
    return nearPressureMultiplier * (nearDensity - targetDensity);
}

vec3 CalculatePressureForce(uint x)
{
    vec3 pos = vec3(predictedPositions[x]);
    float density = densities[x].x;
    float nearDensity = densities[x].y;

    float pressure = PressureFromDensity(density);
    float nearPressure = NearPressureFromDensity(nearDensity);

    vec3 pressureForce = vec3(0.0);

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
            float neighbourDensity = densities[neighbourIndex].x;

            vec3 offsetToNeighbour = neighbourPos - pos;
            float sqrDstToNeighbour = dot(offsetToNeighbour, offsetToNeighbour);

            if (sqrDstToNeighbour > sqrRadius) continue;

            float dst = sqrt(sqrDstToNeighbour);
            vec3 neighbourVelocity = vec3(velocities[neighbourIndex]);
            viscosityForce += (neighbourVelocity - velocity) * ViscosityKernel(dst, smoothingRadius) / neighbourDensity;

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

vec3 closestPointOnTriangle(vec3 p, vec3 a, vec3 b, vec3 c) {
    vec3 ab = b - a;
    vec3 ac = c - a;
    vec3 ap = p - a;

    // Check if P in vertex region outside A
    float d1 = dot(ab, ap);
    float d2 = dot(ac, ap);
    if (d1 <= 0.0 && d2 <= 0.0) return a;

    // Check if P in vertex region outside B
    vec3 bp = p - b;
    float d3 = dot(ab, bp);
    float d4 = dot(ac, bp);
    if (d3 >= 0.0 && d4 <= d3) return b;

    // Check if P in edge region of AB, if so return projection of P onto AB
    float vc = d1 * d4 - d3 * d2;
    if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0) {
        float v = d1 / (d1 - d3);
        return a + v * ab;
    }

    // Check if P in vertex region outside C
    vec3 cp = p - c;
    float d5 = dot(ab, cp);
    float d6 = dot(ac, cp);
    if (d6 >= 0.0 && d5 <= d6) return c;

    // Check if P in edge region of AC, if so return projection of P onto AC
    float vb = d5 * d2 - d1 * d6;
    if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0) {
        float w = d2 / (d2 - d6);
        return a + w * ac;
    }

    // Check if P in edge region of BC, if so return projection of P onto BC
    float va = d3 * d6 - d5 * d4;
    if (va <= 0.0 && (d4 - d3) >= 0.0 && (d5 - d6) >= 0.0) {
        float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
        return b + w * (c - b);
    }

    // P inside face region. Compute Q through barycentric coordinates (u, v, w)
    float denom = 1.0 / (va + vb + vc);
    float v_coord = vb * denom;
    float w_coord = vc * denom;

    return a + ab * v_coord + ac * w_coord;
}

void ResolveTriangleCollisions(uint particleIndex, vec3 oldPos) {
    vec3 vel = vec3(velocities[particleIndex]);
    vec3 pos = vec3(positions[particleIndex]);

    for (uint i = 0; i < numTriangles; i++) {
        // Read the object ID from the 'w' component of the first vertex
        float rawId = triangles[i * 3].w;
        uint objId = floatBitsToUint(rawId);
        mat4 modelMatrix = transforms[objId];

        // Transform local-space vertices to world-space on the GPU
        vec3 v0 = (modelMatrix * vec4(triangles[i*3].xyz, 1.0)).xyz;
        vec3 v1 = (modelMatrix * vec4(triangles[i*3+1].xyz, 1.0)).xyz;
        vec3 v2 = (modelMatrix * vec4(triangles[i*3+2].xyz, 1.0)).xyz;

        // --- PERFORMANCE: Trajectory AABB Check ---
        // Expand AABB to encompass the whole trajectory segment to prevent tunneling
        vec3 minTrajectory = min(oldPos, pos);
        vec3 maxTrajectory = max(oldPos, pos);

        vec3 tMin = min(v0, min(v1, v2)) - 0.1;
        vec3 tMax = max(v0, max(v1, v2)) + 0.1;

        if (any(lessThan(maxTrajectory, tMin)) || any(greaterThan(minTrajectory, tMax))) continue;

        // --- MATH: Continuous Collision Detection (Ray-Triangle) ---
        vec3 edge1 = v1 - v0;
        vec3 edge2 = v2 - v0;
        vec3 rayVector = pos - oldPos; // The movement vector for this frame
        vec3 h = cross(rayVector, edge2);
        float a = dot(edge1, h);

        bool rayHit = false;

        // If 'a' is near 0, the ray is parallel to the triangle
        if (abs(a) > 0.00001) {
            float f = 1.0 / a;
            vec3 s = oldPos - v0;
            float u = f * dot(s, h);

            if (u >= 0.0 && u <= 1.0) {
                vec3 q = cross(s, edge1);
                float v = f * dot(rayVector, q);

                if (v >= 0.0 && u + v <= 1.0) {
                    float t = f * dot(edge2, q);

                    // If t is between 0 and 1, the particle traveled through the triangle this frame
                    if (t > 0.0 && t <= 1.0) {
                        rayHit = true;
                        vec3 normal = normalize(cross(edge1, edge2));

                        // Ensure normal points against velocity
                        if (dot(rayVector, normal) > 0.0) {
                            normal = -normal;
                        }

                        // Place particle at impact point + radius offset
                        float radius = 0.05;
                        pos = (oldPos + rayVector * t) + normal * radius;

                        // Reflect velocity
                        float vDotN = dot(vel, normal);
                        if (vDotN < 0.0) {
                            vel -= (1.5 * vDotN * normal) * 0.5;
                        }
                    }
                }
            }
        }

        // --- MATH: Discrete Collision Fallback ---
        // Crucial for resting contact or particles grazing the very edge of the triangle
        if (!rayHit) {
            vec3 closest = closestPointOnTriangle(pos, v0, v1, v2);
            vec3 dir = pos - closest;
            float dist = length(dir);
            float radius = 0.05;

            if (dist < radius && dist > 0.0) {
                vec3 normal = dir / dist;
                pos = closest + normal * radius;

                float vDotN = dot(vel, normal);
                if (vDotN < 0.0) {
                    vel -= (1.5 * vDotN * normal) * 0.5;
                }
            }
        }
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
        vec3 oldPos = vec3(positions[i]);
        positions[i] += velocities[i] * deltaTime;
        HandleCollisions(i);
        ResolveTriangleCollisions(i, oldPos);

        return;
    }
}
