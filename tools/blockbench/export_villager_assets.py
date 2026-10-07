#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.13"
# dependencies = []
# ///
# ─── How to run ───
# Install uv if needed: https://docs.astral.sh/uv/getting-started/installation/
# Run from repository root: uv run --python 3.13 python -m tools.blockbench.export_villager_assets
# ──────────────────
"""Offline export of the supplied villager; normal Gradle builds use runtime JSON."""
from __future__ import annotations

import base64
import json
from pathlib import Path
from typing import Final

from tools.blockbench.shared.blockbench_model import (
    JsonArray, JsonObject, JsonValue, ModelFormatError,
    _as_array, _as_number, _as_object, _as_string, _load_json,
)

ROOT: Final = Path(__file__).resolve().parents[2]
ASSETS: Final = ROOT / "src/main/resources/assets/eden_realm"
NAMES: Final = {name: name for name in (
    "walk", "happy", "idle", "shake_head", "sit_down", "sit", "sleep", "run",
    "plant", "talk", "sway", "conversation_1", "conversation_2", "agree_nod",
    "greeting_wave", "trade_screen", "happy_1", "happy_2", "puzzled",
    "disappointed", "confident", "complain", "thinking", "embarrassed",
    "bow_aim", "crossbow_load", "bow_hold", "bow_release_recover",
)}


def vector(value: JsonValue, signs: tuple[int, int, int] = (1, 1, 1)) -> JsonArray:
    source = _as_array(value, "vector")
    if len(source) != 3:
        raise ModelFormatError("vector", "expected three coordinates")
    return [_as_number(n, "coordinate") * sign for n, sign in zip(source, signs, strict=True)]


# GeckoLib bakes Geo bone and cube X with the opposite sign again. Apply the
# same conversion to every bone so the authored arm and torso animations agree.
def cube(element: JsonObject) -> JsonObject:
    start = _as_array(element["from"], "from")
    end = _as_array(element["to"], "to")
    faces: JsonObject = {}
    for name, raw in _as_object(element["faces"], "faces").items():
        face = _as_object(raw, name)
        if face.get("texture") is None:
            continue
        uv = [_as_number(n, "uv") for n in _as_array(face["uv"], "uv")]
        u, v, width, height = uv[0], uv[1], uv[2] - uv[0], uv[3] - uv[1]
        if name in {"up", "down"}:
            u, v, width, height = u + width, v + height, -width, -height
        faces[name] = {"uv": [u, v], "uv_size": [width, height]}
    return {
        "origin": [-_as_number(end[0], "x"), start[1], start[2]],
        "size": [_as_number(b, "to") - _as_number(a, "from") for a, b in zip(start, end, strict=True)],
        "pivot": vector(element["origin"], (-1, 1, 1)),
        "rotation": vector(element.get("rotation", [0, 0, 0]), (-1, -1, 1)),
        "inflate": element.get("inflate", 0),
        "uv": faces,
    }


def skeleton(source: JsonObject) -> tuple[JsonArray, dict[str, str]]:
    """Follow the editor hierarchy, preserving cube and bone order and pivots."""
    groups = {_as_string(g["uuid"], "uuid"): g for raw in _as_array(source["groups"], "groups")
              for g in [_as_object(raw, "group")]}
    elements = {_as_string(e["uuid"], "uuid"): e for raw in _as_array(source["elements"], "elements")
                for e in [_as_object(raw, "element")]}
    bones: JsonArray = []
    names: dict[str, str] = {}

    def visit(raw: JsonValue, parent: str | None) -> None:
        node = _as_object(raw, "outliner")
        uuid = _as_string(node["uuid"], "uuid")
        group = groups[uuid]
        name = _as_string(group["name"], "name")
        names[uuid] = name
        children = _as_array(node["children"], "children")
        cubes: JsonArray = [cube(elements[child]) for child in children if isinstance(child, str)]
        bone: JsonObject = {"name": name, "pivot": vector(group["origin"], (-1, 1, 1)),
                            "rotation": vector(group["rotation"], (-1, -1, 1)), "cubes": cubes}
        if parent is not None:
            bone["parent"] = parent
        bones.append(bone)
        for child in children:
            if not isinstance(child, str):
                visit(child, name)

    for root in _as_array(source["outliner"], "outliner"):
        visit(root, None)
    if len(names) != len(groups) or sum(len(_as_array(_as_object(b, "bone")["cubes"], "cubes")) for b in bones) != len(elements):
        raise ModelFormatError("skeleton", "unexported group or cube")
    return bones, names


def track_value(frame: JsonObject, channel: str) -> JsonArray:
    point = _as_object(_as_array(frame["data_points"], "points")[0], "point")
    values: JsonArray = [float(_as_string(point[axis], axis)) for axis in ("x", "y", "z")]
    signs = {"position": (-1, 1, 1), "rotation": (-1, -1, 1), "scale": (1, 1, 1)}
    return vector(values, signs[channel])


def animations(source: JsonObject, names: dict[str, str]) -> tuple[JsonObject, JsonArray]:
    """Apply Blockbench Bedrock keyframe pre/post and Catmull-Rom export semantics."""
    result: JsonObject = {}
    skipped: JsonArray = []
    for raw in _as_array(source["animations"], "animations"):
        animation = _as_object(raw, "animation")
        name = _as_string(animation["name"], "name")
        bones: JsonObject = {}
        for uuid, raw_animator in _as_object(animation["animators"], "animators").items():
            animator = _as_object(raw_animator, "animator")
            if uuid not in names:
                skipped.append({"animation": name, "uuid": uuid, "name": animator.get("name")})
                continue
            channels: JsonObject = {}
            frames = [_as_object(f, "frame") for f in _as_array(animator.get("keyframes", []), "frames")]
            for channel in ("position", "rotation", "scale"):
                frames_in_channel = sorted((f for f in frames if f["channel"] == channel), key=lambda f: _as_number(f["time"], "time"))
                keys: JsonObject = {}
                previous: JsonObject | None = None
                for frame in frames_in_channel:
                    value = track_value(frame, channel)
                    time = _as_number(frame["time"], "time")
                    exported: JsonValue = value
                    if frame["interpolation"] == "catmullrom":
                        exported = {"post": value, "lerp_mode": "catmullrom"}
                        if (previous is None and time > 0) or (previous is not None and previous["interpolation"] != "catmullrom"):
                            exported["pre"] = value
                    elif previous is not None and previous["interpolation"] == "step":
                        exported = {"pre": track_value(previous, channel), "post": value}
                    keys[str(time)] = exported
                    previous = frame
                if keys:
                    channels[channel] = (next(iter(keys.values())) if names[uuid] == "alive_flags"
                                         and channel == "scale" and list(keys) == ["0.0"] else keys)
            if channels:
                bones[names[uuid]] = channels
        assert name in NAMES, name
        loop = animation["loop"]
        assert loop in {"loop", "once", "hold"}, (name, loop)
        result[f"animation.plains_villager.{NAMES[name]}"] = {
            "loop": "hold_on_last_frame" if loop == "hold" else loop == "loop",
            "animation_length": animation["length"], "bones": bones,
        }
    return result, skipped


def write_json(path: Path, value: JsonObject) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    _ = path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    source = _as_object(_load_json((ROOT / "deliverables/plains_villager/plains_villager_master.bbmodel").read_text(encoding="utf-8"), json.loads), "model")
    bones, names = skeleton(source)
    exported, skipped = animations(source, names)
    write_json(ASSETS / "geckolib/models/entity/passive/plains_villager.geo.json", {
        "format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.plains_villager", "texture_width": 128, "texture_height": 128,
                            "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": bones,
        }],
    })
    write_json(ASSETS / "geckolib/animations/entity/passive/plains_villager.animation.json", {"format_version": "1.8.0", "animations": exported})
    texture = _as_object(_as_array(source["textures"], "textures")[0], "texture")
    path = ASSETS / "textures/entity/passive/plains_villager.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    _ = path.write_bytes(base64.b64decode(_as_string(texture["source"], "source").split(",", 1)[1], validate=True))
    write_json(ROOT / "build/lib-villager-integration/export-audit.json", {
        "bones": len(bones), "animations": len(exported), "unbound_animators": skipped,
        "codec": "Blockbench 47e633e4a1338f957ee7baa0acbcf54da11e77df bedrock.js/keyframe.js",
    })


if __name__ == "__main__":
    main()
