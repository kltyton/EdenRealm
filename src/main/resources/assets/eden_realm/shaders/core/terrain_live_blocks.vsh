#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
uniform sampler2D SurfaceField;
layout(std140) uniform TerrainLiveParams { vec4 Grid; vec4 Origin; vec4 Directions; vec4 Settings; };
out vec3 blockPoint;
flat out int face;
const ivec2 CORNERS[6] = ivec2[6](ivec2(0,0),ivec2(0,1),ivec2(1,1),ivec2(0,0),ivec2(1,1),ivec2(1,0));
vec4 surface(ivec2 p) { return texelFetch(SurfaceField, p + ivec2(Settings.z), 0); }
void main() {
    int index = gl_VertexID / 18;
    ivec2 p = ivec2(index % int(Grid.x), index / int(Grid.x));
    ivec2 corner = CORNERS[gl_VertexID % 6];
    vec4 h = surface(p);
    float top = Grid.z == 0.0 ? (int(h.w) == 11 ? h.z : h.x)
            : Grid.z == 1.0 ? h.y : int(h.w) == 11 ? h.x : -8192.0;
    if (top < -8000.0) { gl_Position = vec4(2.0, 2.0, 2.0, 1.0); return; }
    int quad = gl_VertexID % 18 / 6;
    if (quad == 0) {
        face = 0;
        blockPoint = vec3(vec2(p).x + float(corner.x), top, vec2(p).y + float(corner.y));
    } else if (quad == 1) {
        vec4 n = surface(p + ivec2(int(Directions.x), 0));
        float neighborTop = Grid.z == 0.0 ? (int(n.w) == 11 ? n.z : n.x)
                : Grid.z == 1.0 ? n.y : int(n.w) == 11 ? n.x : -8192.0;
        float bottom = Grid.z == 0.0 ? min(top, neighborTop)
                : Grid.z == 2.0 ? max(Grid.w - 1.0, min(top, neighborTop)) : max(h.z, min(top, neighborTop));
        face = Directions.x > 0.0 ? 1 : 2;
        blockPoint = vec3(float(p.x) + (Directions.x > 0.0 ? 1.0 : 0.0),
                mix(bottom, top, float(corner.x)), float(p.y) + float(corner.y));
    } else {
        vec4 n = surface(p + ivec2(0, int(Directions.y)));
        float neighborTop = Grid.z == 0.0 ? (int(n.w) == 11 ? n.z : n.x)
                : Grid.z == 1.0 ? n.y : int(n.w) == 11 ? n.x : -8192.0;
        float bottom = Grid.z == 0.0 ? min(top, neighborTop)
                : Grid.z == 2.0 ? max(Grid.w - 1.0, min(top, neighborTop)) : max(h.z, min(top, neighborTop));
        face = Directions.y > 0.0 ? 3 : 4;
        blockPoint = vec3(float(p.x) + float(corner.y),
                mix(bottom, top, float(corner.x)), float(p.y) + (Directions.y > 0.0 ? 1.0 : 0.0));
    }
    gl_Position = ProjMat * ModelViewMat * vec4(blockPoint + Origin.xyz, 1.0);
}
