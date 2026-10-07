#version 330
uniform sampler2D GroundField;
uniform sampler2D SurfaceField;
uniform sampler2D Materials;
uniform sampler2D MaterialIds;
uniform sampler2D Sampler0;
layout(std140) uniform TerrainLiveParams { vec4 Grid; vec4 Origin; vec4 Directions; vec4 Settings; };
in vec3 blockPoint;
flat in int face;
out vec4 fragColor;
vec4 quad(int id, int offset, vec2 p, vec2 dx, vec2 dy) {
    vec4 uv = texelFetch(Materials, ivec2(face * 6 + offset, id), 0);
    vec4 axes = texelFetch(Materials, ivec2(face * 6 + offset + 1, id), 0);
    vec4 tint = texelFetch(Materials, ivec2(face * 6 + offset + 2, id), 0);
    if (tint.y == 0.0) return vec4(0.0);
    vec2 coord = uv.xy + uv.zw * p.x + axes.xy * p.y;
    return textureGrad(Sampler0, coord, uv.zw * dx.x + axes.xy * dx.y, uv.zw * dy.x + axes.xy * dy.y)
            * vec4(axes.zw, tint.xy);
}
void main() {
    vec2 normal = face == 1 ? vec2(1,0) : face == 2 ? vec2(-1,0) : face == 3 ? vec2(0,1) : face == 4 ? vec2(0,-1) : vec2(0);
    ivec2 cell = ivec2(floor(blockPoint.xz - normal * 0.001)) + ivec2(Settings.z);
    vec4 g = texelFetch(GroundField, cell, 0);
    vec4 s = texelFetch(SurfaceField, cell, 0);
    float y = face == 0 ? (Grid.z == 0.0 ? (int(s.w) == 11 ? g.x : s.x) : Grid.z == 1.0 ? s.y : s.x) - 0.001 : blockPoint.y;
    int family;
    if (Grid.z == 1.0) {
        if (y < g.x || y >= s.y) discard;
        family = 10;
    } else if (Grid.z == 2.0) {
        if (int(s.w) != 11 || y < Grid.w - 1.0 || y >= Grid.w) discard;
        family = 11;
    }
    else if (int(s.w) == 9 && y >= g.x && y < s.x) family = 9;
    else if (y >= g.x) discard;
    else if (y >= g.x - 1.0) family = int(g.z) == 0 && int(s.w) == 9 ? 1 : int(g.z);
    else if (y >= g.x - 4.0) family = int(g.w);
    else family = 4;
    int id = int(texelFetch(MaterialIds, ivec2(family, int(g.y)), 0).x);
    vec2 axes = face == 0 ? blockPoint.xz : face <= 2 ? blockPoint.zy : blockPoint.xy;
    vec2 p = fract(axes), dx = dFdx(axes), dy = dFdy(axes);
    vec4 a = quad(id, 0, p, dx, dy), b = quad(id, 3, p, dx, dy);
    vec4 color = vec4(mix(a.rgb, b.rgb, b.a), max(a.a, b.a));
    if (color.a < 0.01) discard;
    float shade = face == 0 ? 1.0 : face <= 2 ? 0.80 : 0.68;
    fragColor = vec4(color.rgb * shade, Grid.z == 0.0 ? 1.0 : color.a);
}
