in vec2 v_TexCoord;
out float smoothedDepth;

uniform sampler2D m_DepthTex;
uniform bool m_horizontal;
uniform int m_blurSize;
uniform float m_smoothness;
uniform float m_depthDiffStrength;

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(m_DepthTex, 0));
    int kernelSize = m_blurSize * 2 + 1;
    float sigma = max(0.0000001, float(m_blurSize) / (6.0 * max(0.01, m_smoothness)));

    float result = 0.0;
    float weightSum = 0.0;
    float centreDepth = texture(m_DepthTex, v_TexCoord).r;
    for(int i = -m_blurSize; i <= m_blurSize; i++) {
        vec2 offset;
        if (m_horizontal) {
            offset = vec2(float(i), 0.0) * texelSize;
        } else {
            offset = vec2(0.0, float(i)) * texelSize;
        }
        float sampleDepth = texture(m_DepthTex, v_TexCoord + offset).r;
        float depthDifference = centreDepth - sampleDepth;
        float diffWeight = exp(-depthDifference * depthDifference * m_depthDiffStrength);

        float weight = gaussian(float(i), sigma);

        result += sampleDepth * weight * diffWeight;
        weightSum += weight * diffWeight;
    }

    result /= weightSum;

    smoothedDepth = result;
}