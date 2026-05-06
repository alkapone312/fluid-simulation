in vec2 v_TexCoord;
out float smoothedDepth;

uniform sampler2D m_DepthTex;
uniform vec2 m_TexelSize;
uniform float m_Dt;
uniform float m_Cx;
uniform float m_Cy;
float depthThreshold = 1.0;

float getSafeZ(vec2 offset, float centerZ) {
    float neighborZ = texture(m_DepthTex, v_TexCoord + offset).r;

    if (neighborZ <= -1000.0 || abs(neighborZ - centerZ) > depthThreshold) {
        return centerZ;
    }

    return neighborZ;
}

void main() {
    float z = texture(m_DepthTex, v_TexCoord).r;

    if (z <= -1000.0) {
        smoothedDepth = z;
        return;
    }

    float z_r = getSafeZ(vec2(m_TexelSize.x, 0.0), z);
    float z_l = getSafeZ(vec2(-m_TexelSize.x, 0.0), z);
    float z_t = getSafeZ(vec2(0.0, m_TexelSize.y), z);
    float z_b = getSafeZ(vec2(0.0, -m_TexelSize.y), z);

    float z_dx = (z_r - z_l) * 0.5;
    float z_dy = (z_t - z_b) * 0.5;
    float z_dx2 = z_r - 2.0 * z + z_l;
    float z_dy2 = z_t - 2.0 * z + z_b;

    float Cy2 = m_Cy * m_Cy;
    float Cx2 = m_Cx * m_Cx;
    float D = Cy2 * (z_dx * z_dx) + Cx2 * (z_dy * z_dy) + Cx2 * Cy2 * (z * z);

    float D_dx = 2.0 * Cy2 * z_dx * z_dx2 + 2.0 * Cx2 * Cy2 * z * z_dx;
    float D_dy = 2.0 * Cx2 * z_dy * z_dy2 + 2.0 * Cx2 * Cy2 * z * z_dy;

    float Ex = 0.5 * z_dx * D_dx - z_dx2 * D;
    float Ey = 0.5 * z_dy * D_dy - z_dy2 * D;

    float H = 0.5 * (m_Cy * Ex + m_Cx * Ey) / pow(D, 1.5);

    H = clamp(H, -5.0, 5.0);
    smoothedDepth = z - m_Dt * H;
}