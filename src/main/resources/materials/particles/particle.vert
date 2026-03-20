layout(std430, binding = 0) buffer PositionsBuffer {
    vec4 positions[];
};

layout(std430, binding = 1) buffer VelocitiesBuffer {
    vec4 velocities[];
};

uniform mat4 g_WorldViewProjectionMatrix;
// Note: JME3 provides WorldViewMatrix if you need pure view-space distance,
// but we can use gl_Position.w for a standard linear depth estimate.

out vec3 color;

void main() {
    uint id = gl_VertexID;
    vec3 worldPos = positions[id].xyz;

    // 1. Standard Projection
    gl_Position = g_WorldViewProjectionMatrix * vec4(worldPos, 1.0);
    float baseSize = 200.0;
    gl_PointSize = baseSize / gl_Position.w;

    // 2. Velocity-based Color
    float speed = length(velocities[id].xyz);
    float maxSpeed = 10.0;
    float speedNormalized = clamp(speed / maxSpeed, 0.0, 1.0);

    vec3 blue = vec3(0.0, 0.2, 1.0);
    vec3 red = vec3(1.0, 0.0, 0.0);
    vec3 baseColor = mix(blue, red, speedNormalized);

    // 3. Depth-based Darkening
    // gl_Position.w is the distance from the camera plane in World Units.
    float near = 0.0;  // Distance where darkening starts
    float far = 20.0; // Distance where it becomes totally black

    // Calculate a factor that is 1.0 at 'near' and 0.0 at 'far'
    float depthFactor = 1.0 - clamp((gl_Position.w - near) / (far - near), 0.0, 0.25);

    // Darken the color
    color = baseColor * depthFactor;
}