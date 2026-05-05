in vec3 fragViewPos;
in vec3 particleViewCenter;
uniform float m_ParticleRadius;

out vec4 fragColor;

void main() {
    float dist = distance(fragViewPos, particleViewCenter);
    if (dist > m_ParticleRadius) {
        discard;
    }

    // Evaluate density using Poly6 Kernel
    float h = m_ParticleRadius;
    float h2 = h * h;
    float d2 = dist * dist;
    float density = (315.0 / (64.0 * 3.141592 * pow(h, 9.0))) * pow(h2 - d2, 3.0);

    // Output density. Blend state is Additive in Java.
    fragColor = vec4(density, 0.0, 0.0, 1.0);
}