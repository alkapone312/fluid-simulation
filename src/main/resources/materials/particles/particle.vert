layout(std430, binding = 0) buffer PositionsBuffer {
    vec4 positions[];
};

layout(std430, binding = 1) buffer VelocitiesBuffer {
    vec4 velocities[];
};

uniform mat4 g_WorldViewProjectionMatrix;

out vec3 color;

void main() {
    uint id = gl_VertexID;
    vec3 worldPos = positions[id].xyz;

    gl_Position = g_WorldViewProjectionMatrix * vec4(worldPos, 1.0);
    float baseSize = 200.0;
    gl_PointSize = baseSize / gl_Position.w;

    float speed = length(velocities[id].xyz);
    float maxSpeed = 10.0;
    float speedNormalized = clamp(speed / maxSpeed, 0.0, 1.0);

    vec3 blue = vec3(0.0, 0.2, 1.0);
    vec3 red = vec3(1.0, 0.0, 0.0);
    vec3 baseColor = mix(blue, red, speedNormalized);

    float near = 0.0;
    float far = 20.0;

    float depthFactor = 1.0 - clamp((gl_Position.w - near) / (far - near), 0.0, 0.25);

    color = baseColor * depthFactor;
}