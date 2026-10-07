#version 330
uniform sampler2D Lattice;
uniform sampler2D Noise;
layout(std140) uniform TerrainLiveParams { vec4 Grid; vec4 Origin; vec4 Directions; vec4 Settings; };
out vec4 fragColor;
float groundHeight(ivec2 p) {
    p = clamp(p, ivec2(0), textureSize(Noise, 0) - 1);
    ivec2 q = p / 4;
    vec2 f = vec2(p % 4) * 0.25;
    float a = texelFetch(Lattice, q, 0).x;
    float b = texelFetch(Lattice, q + ivec2(1, 0), 0).x;
    float c = texelFetch(Lattice, q + ivec2(0, 1), 0).x;
    float d = texelFetch(Lattice, q + ivec2(1, 1), 0).x;
    return clamp(ceil(mix(mix(a, b, f.x), mix(c, d, f.x), f.y)), Settings.x + 1.0, Settings.y);
}
void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    float height = groundHeight(p);
    int biome = int(texelFetch(Lattice, p / 4, 0).y);
    vec4 noise = texelFetch(Noise, p, 0);
    ivec2 world = ivec2(Directions.zw) + p - ivec2(Settings.z);
    ivec2 local = world & ivec2(15);
    bool steep = max(groundHeight(p + ivec2(0, min(1, 15 - local.y))), Grid.w)
            >= max(groundHeight(p - ivec2(0, min(1, local.y))), Grid.w) + 4.0
        || max(groundHeight(p - ivec2(min(1, local.x), 0)), Grid.w)
            >= max(groundHeight(p + ivec2(min(1, 15 - local.x), 0)), Grid.w) + 4.0;
    int top = height < Grid.w ? 3 : 0;
    if (height >= Grid.w) {
        if (biome == 8 && noise.x >= 0.15) top = 8;
        else if ((biome == 3 || biome == 13) && steep) top = noise.y >= 0.35 ? 5 : 4;
        else if ((biome == 3 || biome == 13) && noise.y >= 0.45) top = 6;
        else if ((biome == 4 || biome == 14) && steep) top = 4;
        else if ((biome == 4 || biome == 14) && (noise.y >= 0.48 || biome == 14 && height >= 142.0)) top = 7;
        else if (biome == 5 && steep) top = 6;
        else if (biome == 5 && noise.y >= 0.40) top = 5;
        else if (biome == 6 && steep) top = 5;
        else if (steep) top = 4;
        else if (noise.x >= -0.54 && noise.x <= -0.48) top = 2;
    }
    int body = biome == 3 || biome == 13 ? (noise.y >= 0.35 ? 5 : 4) : 2;
    fragColor = vec4(height, float(biome), float(top), float(body));
}
