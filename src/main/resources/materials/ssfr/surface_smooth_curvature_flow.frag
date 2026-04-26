in vec2 v_TexCoord;
out float smoothedDepth;

uniform sampler2D m_DepthTex;
uniform vec2 m_TexelSize; // 1.0 / resolution
uniform float m_Dt;       // Smoothing time step
uniform float m_Cx;       // 2.0 / (ViewportX * FocalLengthX)
uniform float m_Cy;       // 2.0 / (ViewportY * FocalLengthY)
float depthThreshold = 1.0; // Dostosuj tę wartość do skali swojego świata!

float getSafeZ(vec2 offset, float centerZ) {
    float neighborZ = texture(m_DepthTex, v_TexCoord + offset).r;

    // Jeśli sąsiad to tło LUB jest za daleko od obecnego piksela
    if (neighborZ <= -1000.0 || abs(neighborZ - centerZ) > depthThreshold) {
        return centerZ; // Wymusza zerową pochodną na granicy
    }

    return neighborZ;
}

void main() {
    float z = texture(m_DepthTex, v_TexCoord).r;

    // If it's the background (large depth), ignore it
    if (z <= -1000.0) {
        smoothedDepth = z;
        return;
    }

    // Bezpieczne pobranie próbek
    float z_r = getSafeZ(vec2(m_TexelSize.x, 0.0), z);
    float z_l = getSafeZ(vec2(-m_TexelSize.x, 0.0), z);
    float z_t = getSafeZ(vec2(0.0, m_TexelSize.y), z);
    float z_b = getSafeZ(vec2(0.0, -m_TexelSize.y), z);

    // Obliczenie pochodnych
    float z_dx = (z_r - z_l) * 0.5;
    float z_dy = (z_t - z_b) * 0.5;
    float z_dx2 = z_r - 2.0 * z + z_l;
    float z_dy2 = z_t - 2.0 * z + z_b;

    // Compute D (Equation 5)
    float Cy2 = m_Cy * m_Cy;
    float Cx2 = m_Cx * m_Cx;
    float D = Cy2 * (z_dx * z_dx) + Cx2 * (z_dy * z_dy) + Cx2 * Cy2 * (z * z);

    // Derivatives of D
    float D_dx = 2.0 * Cy2 * z_dx * z_dx2 + 2.0 * Cx2 * Cy2 * z * z_dx; // Simplified approximation
    float D_dy = 2.0 * Cx2 * z_dy * z_dy2 + 2.0 * Cx2 * Cy2 * z * z_dy;

    // Compute Ex and Ey (Equations 7 & 8)
    float Ex = 0.5 * z_dx * D_dx - z_dx2 * D;
    float Ey = 0.5 * z_dy * D_dy - z_dy2 * D;

    float H = 0.5 * (m_Cy * Ex + m_Cx * Ey) / pow(D, 1.5);

    // Limit H to prevent explosive instability
    H = clamp(H, -5.0, 5.0);

    // Euler integration (Equation 1)
    smoothedDepth = z - m_Dt * H;
}