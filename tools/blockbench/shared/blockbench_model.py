from __future__ import annotations

import base64
import json
from dataclasses import dataclass
from enum import StrEnum
from pathlib import Path
from typing import Final, Protocol, TypeAlias, override

JsonScalar: TypeAlias = None | bool | int | float | str
JsonValue: TypeAlias = JsonScalar | list["JsonValue"] | dict[str, "JsonValue"]
JsonObject: TypeAlias = dict[str, JsonValue]
JsonArray: TypeAlias = list[JsonValue]

NAMESPACE: Final = "eden_realm"


class JsonLoader(Protocol):
    def __call__(self, source: str, /) -> JsonValue:
        ...


def _load_json(source: str, loader: JsonLoader) -> JsonValue:
    return loader(source)


@dataclass(frozen=True, slots=True)
class ModelFormatError(Exception):
    location: str
    detail: str

    @override
    def __str__(self) -> str:
        return f"{self.location}: {self.detail}"


class ModelPart(StrEnum):
    FULL = "full"
    BOTTOM = "bottom"
    TOP = "top"


@dataclass(frozen=True, slots=True)
class ModelSpec:
    source: Path
    output_name: str
    texture_name: str
    part: ModelPart


def _as_object(value: JsonValue, location: str) -> JsonObject:
    if isinstance(value, dict):
        return value
    raise ModelFormatError(location, "expected an object")


def _as_array(value: JsonValue, location: str) -> JsonArray:
    if isinstance(value, list):
        return value
    raise ModelFormatError(location, "expected an array")


def _as_string(value: JsonValue, location: str) -> str:
    if isinstance(value, str):
        return value
    raise ModelFormatError(location, "expected a string")


def _as_number(value: JsonValue, location: str) -> float:
    if isinstance(value, bool):
        raise ModelFormatError(location, "expected a number")
    if isinstance(value, int | float):
        return float(value)
    raise ModelFormatError(location, "expected a number")


def _coordinates(value: JsonValue, location: str, shift_y: bool) -> JsonArray:
    source = _as_array(value, location)
    if len(source) < 3:
        raise ModelFormatError(location, "expected three coordinates")
    offset = 16.0 if shift_y else 0.0
    return [
        _as_number(source[0], f"{location}[0]"),
        _as_number(source[1], f"{location}[1]") - offset,
        _as_number(source[2], f"{location}[2]"),
    ]


def _collect_element_ids(value: JsonValue | None, result: set[str], location: str) -> None:
    if value is None:
        return
    if isinstance(value, str):
        result.add(value)
        return
    if isinstance(value, list):
        for index, child in enumerate(value):
            _collect_element_ids(child, result, f"{location}[{index}]")
        return
    if isinstance(value, dict):
        _collect_element_ids(value.get("children"), result, f"{location}.children")
        return
    raise ModelFormatError(location, "invalid outliner child")


def _named_group_elements(root: JsonObject, expected_name: str) -> set[str]:
    group_names: dict[str, str] = {}
    groups_value = root.get("groups")
    if groups_value is not None:
        for index, raw_group in enumerate(_as_array(groups_value, "groups")):
            if not isinstance(raw_group, dict):
                continue
            group = raw_group
            uuid = group.get("uuid")
            name = group.get("name")
            if uuid is not None and name is not None:
                group_names[_as_string(uuid, "group.uuid")] = _as_string(name, "group.name")

    result: set[str] = set()
    outliner_value = root.get("outliner")
    if outliner_value is None:
        return result
    for index, raw_node in enumerate(_as_array(outliner_value, "outliner")):
        if not isinstance(raw_node, dict):
            continue
        node = raw_node
        uuid_value = node.get("uuid")
        if uuid_value is None:
            continue
        uuid = _as_string(uuid_value, f"outliner[{index}].uuid")
        if group_names.get(uuid, "").casefold() == expected_name.casefold():
            _collect_element_ids(node.get("children"), result, f"outliner[{index}].children")
    return result


def _scale_uv(value: JsonValue, root: JsonObject, location: str) -> JsonArray:
    uv = _as_array(value, location)
    if len(uv) < 4:
        raise ModelFormatError(location, "expected four UV coordinates")
    resolution_value = root.get("resolution")
    resolution = {} if resolution_value is None else _as_object(resolution_value, "resolution")
    width = _as_number(resolution.get("width", 16.0), "resolution.width")
    height = _as_number(resolution.get("height", 16.0), "resolution.height")
    if width <= 0.0 or height <= 0.0:
        raise ModelFormatError("resolution", "width and height must be positive")
    return [
        _as_number(uv[0], f"{location}[0]") * 16.0 / width,
        _as_number(uv[1], f"{location}[1]") * 16.0 / height,
        _as_number(uv[2], f"{location}[2]") * 16.0 / width,
        _as_number(uv[3], f"{location}[3]") * 16.0 / height,
    ]


def _convert_element(source: JsonObject, root: JsonObject, shift_y: bool, location: str) -> JsonObject:
    element: JsonObject = {
        "from": _coordinates(source.get("from"), f"{location}.from", shift_y),
        "to": _coordinates(source.get("to"), f"{location}.to", shift_y),
    }
    rotation_value = source.get("rotation")
    if rotation_value is not None:
        rotation = _as_array(rotation_value, f"{location}.rotation")
        if len(rotation) >= 3:
            angles = [_as_number(rotation[index], f"{location}.rotation[{index}]") for index in range(3)]
            if any(abs(angle) > 1.0e-7 for angle in angles):
                converted_rotation: JsonObject = {
                    "origin": _coordinates(source.get("origin"), f"{location}.origin", shift_y),
                    "x": angles[0],
                    "y": angles[1],
                    "z": angles[2],
                }
                if "rescale" in source:
                    converted_rotation["rescale"] = source["rescale"]
                element["rotation"] = converted_rotation
    if source.get("shade") is False:
        element["shade"] = False
    light_emission = source.get("light_emission")
    if light_emission is not None and _as_number(light_emission, f"{location}.light_emission") > 0:
        element["light_emission"] = int(_as_number(light_emission, f"{location}.light_emission"))

    faces: JsonObject = {}
    faces_value = source.get("faces")
    if faces_value is not None:
        for direction, raw_face in _as_object(faces_value, f"{location}.faces").items():
            face = _as_object(raw_face, f"{location}.faces.{direction}")
            if face.get("texture") is None:
                continue
            converted_face: JsonObject = {"texture": "#0"}
            uv = face.get("uv")
            if uv is not None:
                converted_face["uv"] = _scale_uv(uv, root, f"{location}.faces.{direction}.uv")
            for property_name in ("rotation", "tintindex", "cullface"):
                if property_name in face:
                    converted_face[property_name] = face[property_name]
            faces[direction] = converted_face
    element["faces"] = faces
    return element


def convert_model(source_path: Path, texture_name: str, part: ModelPart) -> JsonObject:
    raw = _load_json(source_path.read_text(encoding="utf-8"), json.loads)
    root = _as_object(raw, str(source_path))
    source_elements = _as_array(root.get("elements"), f"{source_path}.elements")
    if not source_elements:
        raise ModelFormatError(str(source_path), "model has no elements")

    grouped: set[str] = set() if part is ModelPart.FULL else _named_group_elements(root, part.value)
    spatial_fallback = part is not ModelPart.FULL and not grouped
    converted_elements: JsonArray = []
    for index, raw_element in enumerate(source_elements):
        if not isinstance(raw_element, dict):
            continue
        element = raw_element
        if element.get("export") is False:
            continue
        selected = part is ModelPart.FULL
        if not selected and not spatial_fallback:
            uuid = element.get("uuid")
            selected = uuid is not None and _as_string(uuid, "element.uuid") in grouped
        if not selected and spatial_fallback:
            coordinates = _as_array(element.get("from"), "element.from")
            minimum_y = _as_number(coordinates[1], "element.from[1]")
            selected = minimum_y < 16.0 if part is ModelPart.BOTTOM else minimum_y >= 16.0
        if selected:
            converted_elements.append(
                _convert_element(element, root, part is ModelPart.TOP, f"elements[{index}]")
            )
    if not converted_elements:
        raise ModelFormatError(str(source_path), f"{part.value} part has no elements")

    texture = f"{NAMESPACE}:block/{texture_name}"
    model: JsonObject = {
        "textures": {"0": texture, "particle": texture},
        "elements": converted_elements,
    }
    if "ambientocclusion" in root:
        model["ambientocclusion"] = root["ambientocclusion"]
    if "front_gui_light" in root:
        model["gui_light"] = "front" if root["front_gui_light"] is True else "side"
    return model


def render_model(spec: ModelSpec, reference_root: Path) -> str:
    model = convert_model(reference_root / spec.source, spec.texture_name, spec.part)
    return json.dumps(model, ensure_ascii=False, indent=2, sort_keys=True)


def convert_crop_model(source_path: Path, group_name: str | None, texture_override: int | None) -> JsonObject:
    """Export an authored growth group, or its shared mesh with a supplied stage texture."""
    root = _as_object(_load_json(source_path.read_text(encoding="utf-8"), json.loads), str(source_path))
    selected: set[str] = set() if group_name is None else _named_group_elements(root, group_name)
    if group_name is not None and not selected:
        raise ModelFormatError(str(source_path), f"missing crop group {group_name}")
    result: JsonArray = []
    for index, value in enumerate(_as_array(root.get("elements"), "elements")):
        element = _as_object(value, f"elements[{index}]")
        if element.get("export") is False:
            continue
        if group_name is not None and _as_string(element.get("uuid"), "element.uuid") not in selected:
            continue
        converted = _convert_element(element, root, False, f"elements[{index}]")
        source_faces = _as_object(element.get("faces"), "element.faces")
        for direction, face_value in _as_object(converted.get("faces"), "converted.faces").items():
            source_face = _as_object(source_faces[direction], "source.face")
            texture = int(_as_number(source_face.get("texture"), "face.texture"))
            _as_object(face_value, "converted.face")["texture"] = f"#{texture if texture_override is None else texture_override}"
        result.append(converted)
    if not result:
        raise ModelFormatError(str(source_path), "selected crop model has no elements")
    return {"ambientocclusion": False, "elements": result}


def crop_texture_sources(source_path: Path) -> tuple[bytes, ...]:
    """Decode original embedded PNGs without resizing, recoloring or changing their atlas layout."""
    root = _as_object(_load_json(source_path.read_text(encoding="utf-8"), json.loads), str(source_path))
    sources: list[bytes] = []
    for value in _as_array(root.get("textures"), "textures"):
        texture = _as_object(value, "texture")
        source = _as_string(texture.get("source"), "texture.source")
        prefix = "data:image/png;base64,"
        if not source.startswith(prefix):
            raise ModelFormatError(str(source_path), "texture must be an embedded PNG")
        sources.append(base64.b64decode(source[len(prefix):], validate=True))
    return tuple(sources)
