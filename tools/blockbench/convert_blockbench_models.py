#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///

# --- How to run ---
# 1. Install uv: https://docs.astral.sh/uv/getting-started/installation/
# 2. Generate models: uv run --python 3.12 python -m tools.blockbench.convert_blockbench_models
# 3. Verify models: uv run --python 3.12 python -m tools.blockbench.convert_blockbench_models --check
# ------------------

from __future__ import annotations

import sys
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Final, override

from tools.blockbench.shared.blockbench_model import ModelPart, ModelSpec, render_model

REPOSITORY_ROOT: Final = Path(__file__).resolve().parents[2]
REFERENCE_ROOT: Final = REPOSITORY_ROOT / "参考资料"
OUTPUT_ROOT: Final = REPOSITORY_ROOT / "src/main/resources/assets/eden_realm/models/block"


@dataclass(frozen=True, slots=True)
class UsageError(Exception):
    arguments: tuple[str, ...]

    @override
    def __str__(self) -> str:
        return "usage: convert_blockbench_models.py [--check]"


@dataclass(frozen=True, slots=True)
class StaleModelsError(Exception):
    paths: tuple[Path, ...]

    @override
    def __str__(self) -> str:
        listed = "\n".join(f"  {path}" for path in self.paths)
        return f"generated Blockbench models are missing or stale:\n{listed}"


def _spec(source: str, output_name: str, part: ModelPart, texture_name: str | None = None) -> ModelSpec:
    category = "mushroom" if "/菌类/" in source else "plant"
    if output_name.startswith(("purple_glow_cattail", "gray_spike_reed", "water_scallion", "umbrella_hygrophila")):
        category = "plant/aquatic"
    return ModelSpec(
        source=Path(source),
        output_name=f"{category}/{output_name}",
        texture_name=f"{category}/{texture_name or output_name}",
        part=part,
    )


SPECS: Final = (
    _spec("方块/植物/金穗草.bbmodel", "golden_spike_grass_bottom", ModelPart.BOTTOM, "golden_spike_grass"),
    _spec("方块/植物/金穗草.bbmodel", "golden_spike_grass_top", ModelPart.TOP, "golden_spike_grass"),
    _spec("方块/植物/紫光香蒲.bbmodel", "purple_glow_cattail_bottom", ModelPart.BOTTOM, "purple_glow_cattail"),
    _spec("方块/植物/紫光香蒲.bbmodel", "purple_glow_cattail_top", ModelPart.TOP, "purple_glow_cattail"),
    _spec("方块/植物/（水生植物）灰穗芦苇.bbmodel", "gray_spike_reed_1_bottom", ModelPart.BOTTOM, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）灰穗芦苇.bbmodel", "gray_spike_reed_1_top", ModelPart.TOP, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）灰穗芦苇2.bbmodel", "gray_spike_reed_2_bottom", ModelPart.BOTTOM, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）灰穗芦苇2.bbmodel", "gray_spike_reed_2_top", ModelPart.TOP, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）灰穗芦苇3.bbmodel", "gray_spike_reed_3_bottom", ModelPart.BOTTOM, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）灰穗芦苇3.bbmodel", "gray_spike_reed_3_top", ModelPart.TOP, "gray_spike_reed"),
    _spec("方块/植物/（水生植物）水葱.bbmodel", "water_scallion_bottom", ModelPart.BOTTOM, "water_scallion"),
    _spec("方块/植物/（水生植物）水葱.bbmodel", "water_scallion_top", ModelPart.TOP, "water_scallion"),
    _spec("方块/植物/（水底植物）伞花水蓑衣.bbmodel", "umbrella_hygrophila_bottom", ModelPart.BOTTOM, "umbrella_hygrophila"),
    _spec("方块/植物/（水底植物）伞花水蓑衣.bbmodel", "umbrella_hygrophila_top", ModelPart.TOP, "umbrella_hygrophila"),
    _spec("方块/菌类/小伞菇1.bbmodel", "small_parasol_mushroom_1", ModelPart.FULL),
    _spec("方块/菌类/小伞菇2.bbmodel", "small_parasol_mushroom_2", ModelPart.FULL),
    _spec("方块/菌类/小伞菇3.bbmodel", "small_parasol_mushroom_3", ModelPart.FULL),
    _spec("方块/菌类/掉渣菇1.bbmodel", "crumbly_mushroom_1", ModelPart.FULL),
    _spec("方块/菌类/掉渣菇2.bbmodel", "crumbly_mushroom_2", ModelPart.FULL),
    _spec("方块/菌类/掉渣菇3.bbmodel", "crumbly_mushroom_3", ModelPart.FULL),
    _spec("方块/菌类/蓝荧菇1.bbmodel", "blue_glow_mushroom_1", ModelPart.FULL),
    _spec("方块/菌类/蓝荧菇2.bbmodel", "blue_glow_mushroom_2", ModelPart.FULL),
    _spec("方块/菌类/蓝荧菇3.bbmodel", "blue_glow_mushroom_3", ModelPart.FULL),
)


def _check_mode(arguments: Sequence[str]) -> bool:
    if not arguments:
        return False
    if tuple(arguments) != ("--check",):
        raise UsageError(tuple(arguments))
    return True


def main(arguments: Sequence[str] = ()) -> int:
    check_only = _check_mode(arguments)
    stale: list[Path] = []
    for spec in SPECS:
        output_path = OUTPUT_ROOT / f"{spec.output_name}.json"
        rendered = render_model(spec, REFERENCE_ROOT)
        if check_only:
            if not output_path.is_file() or output_path.read_text(encoding="utf-8") != rendered:
                stale.append(output_path)
            continue
        output_path.parent.mkdir(parents=True, exist_ok=True)
        _ = output_path.write_text(rendered, encoding="utf-8", newline="\n")

    if stale:
        raise StaleModelsError(tuple(stale))
    action = "verified" if check_only else "generated"
    print(f"{action} {len(SPECS)} Minecraft block models")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
