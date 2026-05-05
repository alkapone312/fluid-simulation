layout(std430, binding = 0) buffer PositionsBuffer {
    vec4 positions[];
};

// Macierze dostarczane przez WorldParameters
uniform mat4 g_WorldViewMatrix;

// Wyjście do Shadera Geometrii
out vec3 v_ViewPos;

void main() {
    // Przekształcamy pozycję cząsteczki do View Space
    // Jest to kluczowe, ponieważ siatka perspektywiczna jest wyrównana z frustum kamery.
    uint id = gl_VertexID;
    vec3 worldPos = positions[id].xyz;

    vec4 viewPos = g_WorldViewMatrix * vec4(worldPos, 1.0);
    v_ViewPos = viewPos.xyz;

    // Przekazujemy pozycję dalej.
    // gl_Position nie musi być jeszcze w Clip Space,
    // ponieważ Geometry Shader wygeneruje nowe wierzchołki.
    gl_Position = viewPos;
}