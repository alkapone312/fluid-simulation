in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D m_SmoothedDepthTex;
uniform sampler2D m_ThicknessTex;
uniform vec2 m_TexelSize;
uniform mat4 m_ProjectionMatrixInverse;
uniform vec3 m_LightDir; // View space light direction

// Reconstruct view space position from depth
vec3 reconstructPos(vec2 uv, float z) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, 0.5 /* dummy */, 1.0);
    vec4 viewPos = m_ProjectionMatrixInverse * clipPos;
    return vec3((viewPos.xy / viewPos.w) * -z, z); // Assuming z is negative in view space
}

void main() {
    float z = texture(m_SmoothedDepthTex, v_TexCoord).r;
    float thickness = texture(m_ThicknessTex, v_TexCoord).r;
    if (z >= -0.00001 || thickness == 0.0) discard; // Background

    // 1. Calculate Normals via finite differences
    // Using smallest absolute finite difference to avoid silhouette artifacts
    float z_right = texture(m_SmoothedDepthTex, v_TexCoord + vec2(m_TexelSize.x, 0.0)).r;
    float z_left  = texture(m_SmoothedDepthTex, v_TexCoord - vec2(m_TexelSize.x, 0.0)).r;
    float z_up    = texture(m_SmoothedDepthTex, v_TexCoord + vec2(0.0, m_TexelSize.y)).r;
    float z_down  = texture(m_SmoothedDepthTex, v_TexCoord - vec2(0.0, m_TexelSize.y)).r;

    float dz_dx = abs(z_right - z) < abs(z - z_left) ? (z_right - z) : (z - z_left);
    float dz_dy = abs(z_up - z) < abs(z - z_down) ? (z_up - z) : (z - z_down);

    vec3 pos = reconstructPos(v_TexCoord, z);
    vec3 pos_dx = reconstructPos(v_TexCoord + vec2(m_TexelSize.x, 0), z + dz_dx);
    vec3 pos_dy = reconstructPos(v_TexCoord + vec2(0, m_TexelSize.y), z + dz_dy);

    vec3 normal = normalize(cross(pos_dx - pos, pos_dy - pos));

    vec3 absorption = vec3(4.0, 2.0, 0.5);
    vec3 transmission = exp(-thickness * absorption);

    // Calculate overall opacity (Alpha)
    // As thickness increases, visibility decreases.
    float visibility = (transmission.r + transmission.g + transmission.b) / 3.0;
    float alpha = 1.0 - visibility;
    alpha = clamp(alpha, 0.2, 0.9); // Keep it slightly visible even when thin

    // --- 3. Shading ---
    vec3 viewDir = normalize(-pos);
    vec3 halfVector = normalize(m_LightDir + viewDir);

    // Fresnel (Reflections are stronger at grazing angles)
    float R0 = 0.02;
    float fresnel = R0 + (1.0 - R0) * pow(1.0 - max(dot(normal, viewDir), 0.0), 5.0);

    // Specular (Shiny highlights)
    float specular = pow(max(dot(normal, halfVector), 0.0), 128.0);

    // Colors
    vec3 waterColor = vec3(0.1, 0.5, 0.7) * transmission; // Tints the light passing through
    vec3 reflectionColor = vec3(0.9, 0.9, 1.0);           // Sky/Environment color

    // --- 4. Final Composition ---
    // We mix the water color with the reflection based on fresnel
    vec3 finalRGB = mix(waterColor, reflectionColor, fresnel);

    // Add specular on top (highlights are always bright)
    finalRGB += vec3(specular);

    fragColor = vec4(finalRGB, alpha);
}
