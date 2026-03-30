layout(std430, binding = 0) buffer PositionsBuffer {
    vec4 positions[];
};

uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ProjectionMatrix;
uniform float m_particleRadius;
uniform float m_viewportHeight;

out vec3 v_ViewPos;
out float v_SphereRadius;

void main() {
    uint id = gl_VertexID;
    vec3 worldPos = positions[id].xyz;

    vec4 viewPos = g_WorldViewMatrix * vec4(worldPos, 1.0);
    v_ViewPos = viewPos.xyz;
    v_SphereRadius = m_particleRadius;

    gl_Position = g_ProjectionMatrix * viewPos;
    gl_PointSize = (m_particleRadius * g_ProjectionMatrix[1][1] * m_viewportHeight) / -viewPos.z;
}