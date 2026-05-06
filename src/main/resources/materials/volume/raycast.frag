uniform sampler3D m_GridTexture;
uniform sampler2D m_DepthTexture;
uniform float m_NearPlane;
uniform float m_FarPlane;
uniform int m_NumSlices;
uniform vec3 m_LightDir;
uniform mat4x4 m_ProjectionMatrixInverse;
uniform sampler2D m_SceneTexture;
uniform sampler2D m_EnvMap;
uniform mat4x4 m_ViewMatrixInverse;

uniform float m_FluidDensity;

in vec2 v_TexCoord;
out vec4 fragColor;

float sz(float slice) {
    return m_NearPlane * pow(m_FarPlane / m_NearPlane, slice / float(m_NumSlices));
}

float sz_inv_tex(float zView) {
    return log(zView / m_NearPlane) / log(m_FarPlane / m_NearPlane);
}

float getLinearSceneDepth(float rawDepth) {
    float ndcZ = rawDepth * 2.0 - 1.0;
    vec4 viewPos = m_ProjectionMatrixInverse * vec4(0.0, 0.0, ndcZ, 1.0);

    return viewPos.z / viewPos.w;
}

vec2 getEquirectangularUV(vec3 v) {
    vec3 dir = normalize(v);
    // atan(dir.z, dir.x) -> długość geograficzna (-PI do PI)
    // asin(dir.y)        -> szerokość geograficzna (-PI/2 do PI/2)
    vec2 uv = vec2(atan(dir.z, dir.x), asin(dir.y));
    // (0.1591 to w przybliżeniu 1 / (2 * PI))
    // (0.3183 to w przybliżeniu 1 / PI)
    uv *= vec2(0.1591, 0.3183);
    uv += 0.5;

    return uv;
}

void main() {
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    float depthVal = texture(m_DepthTexture, v_TexCoord).r;
    float ndcDepth = depthVal * 2.0 - 1.0;
    float sceneZ = (2.0 * m_NearPlane * m_FarPlane) / (m_FarPlane + m_NearPlane - ndcDepth * (m_FarPlane - m_NearPlane));

    vec3 texSize = vec3(textureSize(m_GridTexture, 0));
    vec3 step = 1.0 / texSize;

    float extinctionScale = 0.001;
    vec3 waterBaseColor = vec3(0.1, 0.4, 0.8);

    float eta1 = 1.0;
    float eta2 = 1.333;
    float etaRatio = eta1 / eta2;

    bool wasInsideVolume = false;
    vec2 backgroundUV = v_TexCoord;

    for (int z = 0; z < m_NumSlices; z++) {
        float zNear = sz(float(z));
        float zFar = sz(float(z) + 1.0);
        float zMid = (zNear + zFar) * 0.5;
        if (zMid > sceneZ) break;

        float sliceThickness = abs(zFar - zNear);
        float zTex = (float(z) + 0.5) / float(m_NumSlices);
        vec3 currPos = vec3(v_TexCoord, zTex);
        float density = texture(m_GridTexture, currPos).r;
        bool currentInside = density > m_FluidDensity;

        if (currentInside) {
            float sampleAlpha = 1.0 - exp(-density * extinctionScale * sliceThickness);
            float dX = texture(m_GridTexture, currPos + vec3(step.x, 0, 0)).r - texture(m_GridTexture, currPos - vec3(step.x, 0, 0)).r;
            float dY = texture(m_GridTexture, currPos + vec3(0, step.y, 0)).r - texture(m_GridTexture, currPos - vec3(0, step.y, 0)).r;
            float dZ = texture(m_GridTexture, currPos + vec3(0, 0, step.z)).r - texture(m_GridTexture, currPos - vec3(0, 0, step.z)).r;
            vec3 normal = normalize(vec3(-dX, -dY, dZ) + vec3(0.00001));

            float diff = max(dot(normal, m_LightDir), 0.2);
            vec3 sampleColor = waterBaseColor * diff;

            if (!wasInsideVolume) {
                vec3 incident = normalize(vec3(v_TexCoord * 2.0 - 1.0, -1.0));

                vec3 reflDir = reflect(incident, normal);
                vec3 refrDir = refract(incident, normal, etaRatio);

                float R0 = pow((eta1 - eta2) / (eta1 + eta2), 2.0);
                float cosI = max(dot(-incident, normal), 0.0);
                float fresnel = R0 + (1.0 - R0) * pow(1.0 - cosI, 5.0);

                if (length(refrDir) < 0.001) fresnel = 1.0;

                float distortionStrength = 0.1;
                backgroundUV = v_TexCoord + (refrDir.xy * distortionStrength);

                vec3 worldReflDir = mat3(m_ViewMatrixInverse) * reflDir;
                vec2 envUV = getEquirectangularUV(worldReflDir);
                vec3 reflectionColor = texture(m_EnvMap, envUV).rgb;

                color += (1.0 - alpha) * reflectionColor * fresnel;
                alpha += (1.0 - alpha) * fresnel;

                vec3 viewDir = vec3(0.0, 0.0, 1.0);
                vec3 halfDir = normalize(m_LightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 128.0);
                sampleColor += vec3(0.6) * spec;
            }

            color += (1.0 - alpha) * sampleColor * sampleAlpha;
            alpha += (1.0 - alpha) * sampleAlpha;

            if (alpha > 0.99) break;
        }

        wasInsideVolume = currentInside;
    }

    vec3 backgroundColor = texture(m_SceneTexture, backgroundUV).rgb;
    vec3 finalColor = color + backgroundColor * (1.0 - alpha);

    fragColor = vec4(finalColor, 1.0);
}