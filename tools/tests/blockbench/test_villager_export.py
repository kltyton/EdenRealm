# /// script
# requires-python = ">=3.13"
# dependencies = ["pytest"]
# ///
# ─── How to run ───
# uv run --python 3.13 --with pytest python -m pytest tools/tests/blockbench/test_villager_export.py -q
# ──────────────────
"""Regression checks for geometry lost at the Blockbench/Bedrock boundary."""
from __future__ import annotations

from tools.blockbench.export_villager_assets import cube
from tools.blockbench.shared.blockbench_model import JsonObject


def test_coincident_source_layers_keep_distinct_inflation() -> None:
    base: JsonObject = {
        "from": [-4, 24, -4], "to": [4, 32, 4], "origin": [0, 24, 0],
        "faces": {"north": {"uv": [0, 0, 8, 8], "texture": 0}},
    }
    inner = cube({**base, "inflate": 0.5})
    outer = cube({**base, "inflate": 0.8})
    assert inner["origin"] == outer["origin"]
    assert inner["size"] == outer["size"]
    assert inner["inflate"] == 0.5
    assert outer["inflate"] == 0.8


def test_rotated_cube_keeps_pivot_and_uv_without_resizing() -> None:
    result = cube({
        "from": [-4, 1, -2], "to": [2, 4, 3], "origin": [2, 3, 4],
        "rotation": [10, 20, 30], "inflate": 0.2,
        "faces": {
            "north": {"uv": [1, 2, 7, 5], "texture": 0},
            "up": {"uv": [6, 9, 0, 4], "texture": 0},
        },
    })
    assert result["origin"] == [-2, 1, -2]
    assert result["size"] == [6, 3, 5]
    assert result["pivot"] == [-2, 3, 4]
    assert result["rotation"] == [-10, -20, 30]
    assert result["uv"] == {
        "north": {"uv": [1, 2], "uv_size": [6, 3]},
        "up": {"uv": [0, 4], "uv_size": [6, 5]},
    }
