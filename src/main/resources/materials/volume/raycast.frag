uniform sampler3D m_GridTexture;
uniform sampler2D m_DepthTexture; // Nowość: tekstura głębi sceny
uniform float m_NearPlane;
uniform float m_FarPlane;
uniform int m_NumSlices;
uniform vec3 m_LightDir;
uniform mat4x4 m_ProjectionMatrixInverse;

uniform float m_FluidDensity;

in vec2 v_TexCoord;
out vec4 fragColor;

// Mapowanie warstwy tekstury na współrzędną Z w View Space
float sz(float slice) {
    return m_NearPlane * pow(m_FarPlane / m_NearPlane, slice / float(m_NumSlices));
}

float getLinearSceneDepth(float rawDepth) {
    // Konwersja z [0, 1] do [-1, 1]
    float ndcZ = rawDepth * 2.0 - 1.0;

    // Mnożenie przez odwrotność macierzy projekcji, aby wrócić do View Space
    vec4 viewPos = m_ProjectionMatrixInverse * vec4(0.0, 0.0, ndcZ, 1.0);

    // Podział perspektywiczny (perspetive divide)
    return viewPos.z / viewPos.w;
}

void main() {
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // 1. Parametry sceny i tekstury
    float depthVal = texture(m_DepthTexture, v_TexCoord).r;
    float ndcDepth = depthVal * 2.0 - 1.0;
    float sceneZ = (2.0 * m_NearPlane * m_FarPlane) / (m_FarPlane + m_NearPlane - ndcDepth * (m_FarPlane - m_NearPlane));

    vec3 texSize = vec3(textureSize(m_GridTexture, 0));
    vec3 step = 1.0 / texSize;

    // Parametry optyczne
    float extinctionScale = 0.1; // Fizyczny współczynnik tłumienia
    vec3 waterBaseColor = vec3(0.1, 0.4, 0.8);

    bool insideVolume = false;
    bool wasInsideVolume = false;
    for (int z = 0; z < m_NumSlices; z++) {
        // Obliczamy Z-View dla początku i końca bieżącego plastra
        float zNear = sz(float(z));
        float zFar = sz(float(z) + 1.0);
        float zMid = (zNear + zFar) * 0.5;

        // Test głębi - sprawdzamy środek plastra
        if (zMid > sceneZ) break;

        // Obliczamy faktyczną grubość plastra w przestrzeni widoku
        // To jest kluczowe dla logarytmicznego rozkładu pomiarów!
        float sliceThickness = abs(zFar - zNear);

        float zTex = (float(z) + 0.5) / float(m_NumSlices);
        vec3 currPos = vec3(v_TexCoord, zTex);
        float density = texture(m_GridTexture, currPos).r;

        if (density > m_FluidDensity) {
            // 2. PRAWO BEERA-LAMBERTA (Kompensacja grubości)
            // Im cieńszy plaster (bliżej kamery), tym mniejsza alfa pojedynczej próbki.
            float sampleAlpha = 1.0 - exp(-density * extinctionScale * sliceThickness);

            // 3. Oświetlenie (Shading)
            float dX = texture(m_GridTexture, currPos + vec3(step.x, 0, 0)).r - texture(m_GridTexture, currPos - vec3(step.x, 0, 0)).r;
            float dY = texture(m_GridTexture, currPos + vec3(0, step.y, 0)).r - texture(m_GridTexture, currPos - vec3(0, step.y, 0)).r;
            float dZ = texture(m_GridTexture, currPos + vec3(0, 0, step.z)).r - texture(m_GridTexture, currPos - vec3(0, 0, step.z)).r;
            vec3 normal = normalize(vec3(dX, dY, dZ) + 0.0001);

            float diff = max(dot(normal, m_LightDir), 0.2);
            vec3 sampleColor = waterBaseColor * diff;
            if (!wasInsideVolume && insideVolume) {
                vec3 viewDir = vec3(0.0, 0.0, 1.0);
                vec3 halfDir = normalize(m_LightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 128.0);
                sampleColor += vec3(0.6) * spec;
            }

            // 4. SKŁADANIE FRONT-TO-BACK
            // Używamy zaktualizowanego sampleAlpha uwzględniającego logarytmiczny krok
            color += (1.0 - alpha) * sampleColor * sampleAlpha;
            alpha += (1.0 - alpha) * sampleAlpha;

            if (alpha > 0.99) break;
        }

        wasInsideVolume = insideVolume;
        insideVolume = density > m_FluidDensity;
    }

    if (alpha < 0.05) discard;
    fragColor = vec4(color, alpha);
}