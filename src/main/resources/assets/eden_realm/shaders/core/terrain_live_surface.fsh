#version 330
uniform sampler2D GroundField;
uniform sampler2D Noise;
uniform sampler2D ShoreRadii;
uniform sampler2D ShoreDistance;
layout(std140) uniform TerrainLiveParams { vec4 Grid; vec4 Origin; vec4 Directions; vec4 Settings; };
out vec4 fragColor;
vec4 ground(ivec2 p) { return texelFetch(GroundField, clamp(p, ivec2(0), textureSize(GroundField, 0) - 1), 0); }
void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    vec4 g = ground(p);
    int biome = int(g.y), family = int(g.z);
    float top = g.x, water = -8192.0;
    if (g.x < Grid.w) {
        int radius = int(texelFetch(ShoreRadii, ivec2(biome, 0), 0).x);
        bool frozen = false;
        if (Grid.w - g.x <= 16.0) {
            for (int dz = -radius; dz <= radius; dz++) {
                ivec2 n = clamp(p + ivec2(0, dz), ivec2(0), textureSize(ShoreDistance, 0) - 1);
                if (radius > 0 && texelFetch(ShoreDistance, n, 0).x + float(dz * dz) <= float(radius * radius)) {
                    frozen = true; break;
                }
            }
        }
        water = frozen ? Grid.w - 1.0 : Grid.w;
        if (frozen) { top = Grid.w; family = 11; }
    } else {
        bool snow = family != 5 && family != 6 && texelFetch(Noise, p, 0).z > 0.5;
        if (snow) { top += 0.125; family = 9; }
    }
    fragColor = vec4(top, water, g.x, float(family));
}
