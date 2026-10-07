#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "pillow>=11,<13",
# ]
# ///

# --- How to run ---
# 1. Install uv: https://docs.astral.sh/uv/getting-started/installation/
# 2. Measure PNG files or folders:
#      uv run tools/measurement/measure_plant_textures.py <png-or-directory> [...]
# 3. The report includes full-image and 16x16 tile-local opaque bounds.
# ------------------

from __future__ import annotations

import sys
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Final, override

from PIL import Image

DEFAULT_TILE_SIZE: Final = 16


@dataclass(frozen=True, slots=True)
class OpaqueBounds:
    min_x: int
    min_y: int
    max_x: int
    max_y: int

    @property
    def width(self) -> int:
        return self.max_x - self.min_x

    @property
    def height(self) -> int:
        return self.max_y - self.min_y


@dataclass(frozen=True, slots=True)
class TileMeasurement:
    column: int
    row: int
    bounds: OpaqueBounds


@dataclass(frozen=True, slots=True)
class TextureMeasurement:
    path: Path
    width: int
    height: int
    image_bounds: OpaqueBounds | None
    tiles: tuple[TileMeasurement, ...]


@dataclass(frozen=True, slots=True)
class UsageError(Exception):
    @override
    def __str__(self) -> str:
        return "usage: measure_plant_textures.py <png-or-directory> [...]"


@dataclass(frozen=True, slots=True)
class TexturePathError(Exception):
    path: Path

    @override
    def __str__(self) -> str:
        return f"texture path does not exist or contains no PNG files: {self.path}"


def opaque_bounds(image: Image.Image) -> OpaqueBounds | None:
    alpha = image.getchannel("A")
    try:
        bounds = alpha.getbbox()
    finally:
        alpha.close()
    if bounds is None:
        return None
    return OpaqueBounds(*bounds)


def measure_texture(path: Path, tile_size: int = DEFAULT_TILE_SIZE) -> TextureMeasurement:
    with Image.open(path) as source, source.convert("RGBA") as image:
        tiles: list[TileMeasurement] = []
        for top in range(0, image.height, tile_size):
            for left in range(0, image.width, tile_size):
                box = (left, top, min(left + tile_size, image.width), min(top + tile_size, image.height))
                with image.crop(box) as tile:
                    bounds = opaque_bounds(tile)
                if bounds is not None:
                    tiles.append(TileMeasurement(left // tile_size, top // tile_size, bounds))
        return TextureMeasurement(
            path=path,
            width=image.width,
            height=image.height,
            image_bounds=opaque_bounds(image),
            tiles=tuple(tiles),
        )


def texture_paths(arguments: Sequence[str]) -> tuple[Path, ...]:
    if not arguments:
        raise UsageError

    textures: set[Path] = set()
    for argument in arguments:
        path = Path(argument)
        if path.is_file() and path.suffix.casefold() == ".png":
            textures.add(path)
        elif path.is_dir():
            textures.update(candidate for candidate in path.rglob("*.png") if candidate.is_file())
        else:
            raise TexturePathError(path)
    if not textures:
        raise TexturePathError(Path(arguments[0]))
    return tuple(sorted(textures))


def format_bounds(bounds: OpaqueBounds | None) -> str:
    if bounds is None:
        return "empty"
    return (
        f"x={bounds.min_x}..{bounds.max_x - 1}, y={bounds.min_y}..{bounds.max_y - 1}, "
        f"column={bounds.width}x{bounds.height}"
    )


def main(arguments: Sequence[str] = ()) -> int:
    for path in texture_paths(arguments):
        measurement = measure_texture(path)
        print(f"{path}: {measurement.width}x{measurement.height}, {format_bounds(measurement.image_bounds)}")
        for tile in measurement.tiles:
            print(f"  tile[{tile.column},{tile.row}]: {format_bounds(tile.bounds)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
