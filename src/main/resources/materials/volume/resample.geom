layout(points) in;
layout(triangle_strip, max_vertices = 128) out;

uniform float m_NearPlane;
uniform float m_FarPlane;
uniform int m_NumSlices;
uniform float m_ParticleRadius;
uniform mat4 g_ProjectionMatrix;
uniform float m_ParticleMass = 1.0;
uniform int m_CurrentSlice;

in vec3 v_ViewPos[];
out vec3 fragViewPos;
out vec3 particleViewCenter;
out float fragRadius;
out float fragMass;

float sz_inv(float z) {
    return float(m_NumSlices) * log(z / m_NearPlane) / log(m_FarPlane / m_NearPlane);
}

float sz(float slice) {
    return m_NearPlane * pow(m_FarPlane / m_NearPlane, slice / float(m_NumSlices));
}

void main() {
    vec3 center = vec3(v_ViewPos[0]);
    float depth = -center.z;

    float r = m_ParticleRadius;
    float safeDepthMin = max(depth - r, m_NearPlane + 0.001);

    int s_min = int(ceil(sz_inv(safeDepthMin) - 0.5));
    int s_max = int(floor(sz_inv(depth + r) - 0.5));

    s_max = max(s_min, s_max);
    s_min = clamp(s_min, 0, m_NumSlices - 1);
    s_max = clamp(s_max, 0, m_NumSlices - 1);

    if (m_CurrentSlice >= s_min && m_CurrentSlice <= s_max) {
        float sliceZ = sz(float(m_CurrentSlice) + 0.5);
        float distZ = abs(depth - sliceZ);

        float localVoxelSizeZ = sliceZ * log(m_FarPlane / m_NearPlane) / float(m_NumSlices);
        float maxReachZ = max(r, localVoxelSizeZ * 0.6);

        if (distZ <= maxReachZ) {
            float ratio = clamp(distZ / r, 0.0, 1.0);
            float rCross = r * sqrt(1.0 - ratio * ratio);

            if (ratio >= 1.0) rCross = r * 0.1;

            vec4 offsets[4] = vec4[](
            vec4(-rCross, -rCross, 0.0, 0.0),
            vec4( rCross, -rCross, 0.0, 0.0),
            vec4(-rCross,  rCross, 0.0, 0.0),
            vec4( rCross,  rCross, 0.0, 0.0)
            );

            for (int v = 0; v < 4; v++) {
                vec3 vertPos = vec3(center.x, center.y, -sliceZ) + offsets[v].xyz;
                gl_Position = g_ProjectionMatrix * vec4(vertPos, 1.0);

                fragViewPos = vertPos;
                particleViewCenter = center;

                fragRadius = r;
                EmitVertex();
            }
            EndPrimitive();
        }
    }
}