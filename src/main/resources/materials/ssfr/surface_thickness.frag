in vec3 v_ViewPos;
in float v_SphereRadius;

out float fragThickness;

uniform float m_thicknessMultiplier;

void main() {
    vec2 nCoord = 2.0 * gl_PointCoord - 1.0;
    float distSq = dot(nCoord, nCoord);

    if (distSq > 1.0) discard;

    float z = sqrt(1.0 - distSq);

    fragThickness = 2.0 * z * v_SphereRadius * m_thicknessMultiplier;
}