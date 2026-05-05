layout(points) in;
layout(triangle_strip, max_vertices = 128) out; // Supports up to 32 slices per particle (4 verts per quad)

uniform float m_NearPlane;
uniform float m_FarPlane;
uniform int m_NumSlices;
uniform float m_ParticleRadius;
uniform mat4 g_ProjectionMatrix;

in vec3 v_ViewPos[];

out vec3 fragViewPos;
out vec3 particleViewCenter;

// Mapping view-space Z to texture slice Z'
float sz_inv(float z) {
    return float(m_NumSlices) * log(z / m_NearPlane) / log(m_FarPlane / m_NearPlane);
}

// Mapping texture slice Z' to view-space Z
float sz(float slice) {
    return m_NearPlane * pow(m_FarPlane / m_NearPlane, slice / float(m_NumSlices));
}

void main() {
    vec3 center = vec3(v_ViewPos[0]);
    float h = m_ParticleRadius;
    float depth = -center.z; // OpenGL view space is looking down -Z

    // Calculate covered slices
    int s_min = int(ceil(sz_inv(depth - h) - 0.5));
    int s_max = int(floor(sz_inv(depth + h) - 0.5));

    s_min = clamp(s_min, 0, m_NumSlices - 1);
    s_max = clamp(s_max, 0, m_NumSlices - 1);

    for (int i = s_min; i <= s_max; i++) {
        gl_Layer = i; // Route output to the i-th slice of the 3D texture
        float sliceZ = sz(float(i) + 0.5);

        // Radius of the cross-section between the particle sphere and the slice
        float distZ = abs(depth - sliceZ);
        if (distZ > h) continue;
        float rCross = sqrt(h * h - distZ * distZ);

        // Emit a quad covering the cross-section
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
            EmitVertex();
        }
        EndPrimitive();
    }
}