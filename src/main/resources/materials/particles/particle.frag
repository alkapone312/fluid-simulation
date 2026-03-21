in vec3 color;
out vec4 fragColor;

void main() {
    vec2 nCoord = 2.0 * gl_PointCoord - 1.0;
    float distSq = dot(nCoord, nCoord);

    if (distSq > 1.0) discard;

    float dist = sqrt(distSq);

    float borderThickness = 0.1;
    float edge = smoothstep(1.0 - borderThickness, 1.0, dist);

    vec3 finalColor = mix(color, color * 0.2, edge);

    fragColor = vec4(finalColor, 1.0);
}