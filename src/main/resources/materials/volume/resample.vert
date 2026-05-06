layout(std430, binding = 0) buffer PositionsBuffer {
    vec4 positions[];
};

uniform mat4 g_WorldViewMatrix;

out vec3 v_ViewPos;

void main() {
    uint id = gl_VertexID;
    vec3 worldPos = positions[id].xyz;

    vec4 viewPos = g_WorldViewMatrix * vec4(worldPos, 1.0);
    v_ViewPos = viewPos.xyz;
    gl_Position = viewPos;
}