#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = ["pillow>=11,<13"]
# ///
# How to run: uv run --with pillow python -m tools.measurement.measure_crop_models
"""Measure authored visible faces; never alter model geometry or texture pixels."""

from __future__ import annotations

import math
from io import BytesIO
from typing import Final

from PIL import Image

from tools.blockbench.export_crop_assets import MODELS, REFERENCE
from tools.blockbench.shared.blockbench_model import (
    JsonObject,
    _as_array,
    _as_number,
    _as_object,
    _as_string,
    convert_crop_model,
    crop_texture_sources,
)

type Point = tuple[float, float, float]
AXES: Final = {"x": 0, "y": 1, "z": 2}


def numbers(element: JsonObject, key: str) -> tuple[float, ...]:
    """Read numeric components from an exported model field."""
    return tuple(_as_number(value, key) for value in _as_array(element[key], key))


def face_point(element: JsonObject, direction: str, u: float, v: float) -> Point:
    """Map a face's normalized UV position into original block pixel coordinates."""
    x, y, z = numbers(element, "from")
    xx, yy, zz = numbers(element, "to")
    points = {
        "north": (xx - u * (xx - x), yy - v * (yy - y), z),
        "south": (x + u * (xx - x), yy - v * (yy - y), zz),
        "west": (x, yy - v * (yy - y), z + u * (zz - z)),
        "east": (xx, yy - v * (yy - y), zz - u * (zz - z)),
        "up": (x + u * (xx - x), yy, z + v * (zz - z)),
        "down": (x + u * (xx - x), y, zz - v * (zz - z)),
    }
    point = list(points[direction])
    if "rotation" in element:
        rotation = _as_object(element["rotation"], "rotation")
        origin = numbers(rotation, "origin")
        for name, axis in AXES.items():
            a, b = (axis + 1) % 3, (axis + 2) % 3
            angle = math.radians(_as_number(rotation.get(name, 0), name))
            cosine, sine = math.cos(angle), math.sin(angle)
            scale = 1 / cosine if rotation.get("rescale") is True else 1.0
            pa, pb = (point[a] - origin[a]) * scale, (point[b] - origin[b]) * scale
            point[a], point[b] = (
                origin[a] + pa * cosine - pb * sine,
                origin[b] + pa * sine + pb * cosine,
            )
    return point[0], point[1], point[2]


def clip_y(points: list[Point], boundary: float, *, above: bool) -> list[Point]:
    """Clip a visible texel polygon against a half-block boundary."""
    result: list[Point] = []
    for start, end in zip(points, points[1:] + points[:1], strict=True):
        start_inside = start[1] >= boundary if above else start[1] <= boundary
        end_inside = end[1] >= boundary if above else end[1] <= boundary
        if start_inside:
            result.append(start)
        if start_inside != end_inside:
            fraction = (boundary - start[1]) / (end[1] - start[1])
            result.append(
                (
                    start[0] + fraction * (end[0] - start[0]),
                    boundary,
                    start[2] + fraction * (end[2] - start[2]),
                ),
            )
    return result


def visible_faces(model: JsonObject, textures: tuple[bytes, ...]) -> list[list[Point]]:
    """Resolve source alpha with reversed UVs and element rotations."""
    result: list[list[Point]] = []
    for raw in _as_array(model["elements"], "elements"):
        element = _as_object(raw, "element")
        for direction, raw_face in _as_object(element["faces"], "faces").items():
            face = _as_object(raw_face, "face")
            index = int(_as_string(face["texture"], "texture")[1:])
            with (
                BytesIO(textures[index]) as stream,
                Image.open(stream) as image,
                image.convert("RGBA") as rgba,rgba.getchannel("A") as alpha,
            ):
                u0, v0, u1, v1 = numbers(face, "uv")
                u0, u1 = u0 * image.width / 16, u1 * image.width / 16
                v0, v1 = v0 * image.height / 16, v1 * image.height / 16
                if u0 == u1 or v0 == v1:
                    continue
                for y in range(
                    max(0, math.floor(min(v0, v1))),
                    min(image.height, math.ceil(max(v0, v1))),
                ):
                    for x in range(
                        max(0, math.floor(min(u0, u1))),
                        min(image.width, math.ceil(max(u0, u1))),
                    ):
                        if not alpha.getpixel((x, y)):
                            continue
                        left, right = max(x, min(u0, u1)), min(x + 1, max(u0, u1))
                        top, bottom = max(y, min(v0, v1)), min(y + 1, max(v0, v1))
                        polygon: list[Point] = []
                        for px, py in (
                            (left, top),
                            (right, top),
                            (right, bottom),
                            (left, bottom),
                        ):
                            u, v = (px - u0) / (u1 - u0), (py - v0) / (v1 - v0)
                            for _ in range(
                                int(_as_number(face.get("rotation", 0), "rotation"))
                                // 90,
                            ):
                                u, v = v, 1 - u
                            polygon.append(face_point(element, direction, u, v))
                        result.append(polygon)
    return result


def main() -> None:
    """Print full and per-half visible bounds for each authored crop model."""
    for spec in MODELS:
        source = REFERENCE / "作物" / f"{spec.source}.bbmodel"
        faces = visible_faces(
            convert_crop_model(source, spec.group, spec.texture),
            crop_texture_sources(source),
        )
        bounds: list[str] = []
        for low, high in ((-16, 32), (0, 16), (16, 32)):
            points = [
                point
                for face in faces
                for point in clip_y(clip_y(face, low, above=True), high, above=False)
            ]
            if not points:
                bounds.append("empty")
                continue
            width = 2 * max(max(abs(p[0] - 8), abs(p[2] - 8)) for p in points)
            bottom = min(p[1] for p in points)
            top = max(p[1] for p in points)
            bounds.append(f"w={width:.3f},y={bottom:.3f}..{top:.3f}")
        print(f"{spec.name}: {'; '.join(bounds)}")


if __name__ == "__main__":
    main()
