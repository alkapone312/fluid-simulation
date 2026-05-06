in vec3 fragViewPos;
in vec3 particleViewCenter;
in float fragRadius;
out vec4 fragColor;

void main() {
    float dist = length(fragViewPos - particleViewCenter);
    if (dist > fragRadius) {
        discard;
    }

    float h = fragRadius;
    float h2 = h * h;
    float d2 = dist * dist;

    float density = (315.0 / (64.0 * 3.141592 * pow(h, 9.0))) * pow(h2 - d2, 3.0);

    fragColor = vec4(density, 0.0, 0.0, 1.0);
}