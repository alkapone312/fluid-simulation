in vec2 v_TexCoord;
out vec4 fragColor;

uniform sampler2D m_SmoothedDepthTex;
uniform sampler2D m_ThicknessTex;
uniform sampler2D m_SceneTex;
uniform sampler2D m_SceneDepthTex;
uniform vec2 m_TexelSize;
uniform mat4 m_ProjectionMatrixInverse;
uniform vec3 m_LightDir; // View space light direction
uniform bool m_DebugDepth;
uniform bool m_DebugThickness;

// Reconstruct view space position from depth
vec3 reconstructPos(vec2 uv, float z) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, 0.5 /* dummy */, 1.0);
    vec4 viewPos = m_ProjectionMatrixInverse * clipPos;
    return vec3((viewPos.xy / viewPos.w) * -z, z); // Assuming z is negative in view space
}

float getLinearSceneDepth(float rawDepth) {
    // Konwersja z [0, 1] do NDC (Normalized Device Coordinates) [-1, 1]
    float ndcZ = rawDepth * 2.0 - 1.0;

    // Mnożenie przez odwrotność macierzy projekcji, aby wrócić do View Space
    vec4 viewPos = m_ProjectionMatrixInverse * vec4(0.0, 0.0, ndcZ, 1.0);

    // Podział perspektywiczny (perspetive divide)
    return viewPos.z / viewPos.w;
}

void main() {
    float z = texture(m_SmoothedDepthTex, v_TexCoord).r;
    float thickness = texture(m_ThicknessTex, v_TexCoord).r;

    // DEBUG
    if (m_DebugDepth) {
        fragColor = vec4(vec3(-z / 10), 1.0);
        return;
    }

    if (m_DebugThickness) {
        fragColor = vec4(vec3(thickness / 20.0), 1.0);
        return;
    }
    // DEBUG

    if (z >= -0.00001 || thickness == 0.0) discard; // Background

    float rawSceneDepth = texture(m_SceneDepthTex, v_TexCoord).r;

    // Konwersja do liniowej głębi w przestrzeni widoku (wartość ujemna)
    float sceneViewZ = getLinearSceneDepth(rawSceneDepth);

    // W OpenGL (i jME), oś Z przestrzeni widoku skierowana jest "w głąb" ekranu jako wartości ujemne.
    // Oznacza to, że -10 jest DALEJ od kamery niż -5.
    // Jeśli płyn (z) ma mniejszą (bardziej ujemną) wartość niż scena, to znaczy, że jest z tyłu i należy go odrzucić.
    float depthBias = 0.05; // Mały margines błędu zapobiegający efektowi Z-fighting na styku płynu i geometrii

    if (z < sceneViewZ - depthBias) {
        discard;
    }

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

    vec3 absorption = vec3(0.5, 0.5, 0.5);
    vec3 transmission = exp(-thickness * absorption);

    // Calculate overall opacity (Alpha)
    // As thickness increases, visibility decreases.
    float visibility = (transmission.r + transmission.g + transmission.b) / 3.0;
    float alpha = 1.0 - visibility;
    alpha = clamp(alpha, 0.2, 1.0); // Keep it slightly visible even when thin

    // --- 3. Shading ---
    vec3 viewDir = normalize(-pos);
    vec3 halfVector = normalize(m_LightDir + viewDir);

    // Fresnel (Reflections are stronger at grazing angles)
    float R0 = 0.02;
    float fresnel = R0 + (1.0 - R0) * pow(1.0 - max(dot(normal, viewDir), 0.0), 5.0);

    // Specular (Shiny highlights)
    float specular = pow(max(dot(normal, halfVector), 0.0), 128.0);

    // Colors
    vec3 waterColor = vec3(0.1, 0.5, 0.7); // Tints the light passing through
    vec3 reflectionColor = vec3(0.9, 0.9, 1.0);           // Sky/Environment color

    // --- 4. Refraction Calculation ---

    // We use the normal in view space to offset the UVs
    // Dividing by -z makes the refraction look deeper as the fluid gets further away
    vec2 refractionOffset = normal.xy * 0.1;
    vec2 refractedUV = v_TexCoord + refractionOffset;

    // Optional: Depth Check
    // Prevent "bleeding" - don't refract if the object at the new UV is
    // actually in FRONT of the fluid.
    float sceneDepthAtRefraction = getLinearSceneDepth(texture(m_SceneDepthTex, refractedUV).r);
    if (sceneDepthAtRefraction > z + 0.1) {
        refractedUV = v_TexCoord;
    }
    // Sample the background scene with the distorted UVs
    vec3 sceneColor = texture(m_SceneTex, refractedUV).rgb;

    // --- 5. Combine with Beer's Law ---

    // Instead of a static waterColor, we TINT the refracted scene color
    // using the transmission (Beer's Law) calculated earlier.
    vec3 fluidBackground = sceneColor * transmission;

    // Add a slight base tint so the water doesn't disappear if the floor is white
    vec3 deepWaterTint = vec3(0.0, 0.0, 0.2);
    fluidBackground = mix(fluidBackground, deepWaterTint, 0.2 * (1.0 - visibility));

    vec3 finalRGB = mix(fluidBackground, reflectionColor, fresnel);

    // Add specular highlight
    finalRGB += vec3(specular);

    fragColor = vec4(finalRGB, alpha);
}
