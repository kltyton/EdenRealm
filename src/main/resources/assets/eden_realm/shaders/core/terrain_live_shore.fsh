#version 330
uniform sampler2D GroundField;
layout(std140) uniform TerrainLiveParams { vec4 Grid; vec4 Origin; vec4 Directions; vec4 Settings; };
out vec4 fragColor;
void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    float distance = 1024.0;
    for (int dx = -24; dx <= 24; dx++) {
        ivec2 n = clamp(p + ivec2(dx, 0), ivec2(0), textureSize(GroundField, 0) - 1);
        vec4 g = texelFetch(GroundField, n, 0);
        if (g.x >= Grid.w && int(g.z) != 5 && int(g.z) != 6 && int(g.z) != 11)
            distance = min(distance, float(dx * dx));
    }
    fragColor = vec4(distance, 0.0, 0.0, 0.0);
}
