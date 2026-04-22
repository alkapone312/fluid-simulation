uniform mat4 g_ProjectionMatrix;

in vec3 v_ViewPos;
in float v_SphereRadius;

out float fragDepth;

void main() {
    vec2 nCoord = 2.0 * gl_PointCoord - 1.0;
    float distSq = dot(nCoord, nCoord);

    if (distSq > 1.0) discard;

    float z = sqrt(1.0 - distSq);

    vec3 pixelViewPos = v_ViewPos + vec3(nCoord * v_SphereRadius, z * v_SphereRadius);

    fragDepth = pixelViewPos.z;
}