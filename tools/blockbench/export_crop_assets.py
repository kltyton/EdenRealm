#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
# How to run: uv run --python 3.12 python -m tools.blockbench.export_crop_assets [--check]
"""Export authored crop groups, original atlases and inventory icons offline."""

from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Final

from tools.blockbench.convert_blockbench_models import StaleModelsError, _check_mode
from tools.blockbench.shared.blockbench_model import (
    JsonObject,
    _as_array,
    _as_object,
    _as_string,
    convert_crop_model,
    crop_texture_sources,
)

ROOT: Final = Path(__file__).resolve().parents[2]
REFERENCE: Final = ROOT / "参考资料"
ASSETS: Final = ROOT / "src/main/resources/assets/eden_realm"


@dataclass(frozen=True, slots=True)
class CropModel:
    """One authored group or texture variant and its runtime model name."""

    source: str
    name: str
    group: str | None
    texture: int | None = None


FRUITS: Final = (
    (
        "潮歌椰",
        "tide_song_coconut",
        (
            "0花苞bud",
            "1开花bloom",
            "2结果bear_fruit",
            "3待成熟的果实unripe_fruit",
            "4成熟的果实",
        ),
    ),
    (
        "圣辉果",
        "sacred_light_fruit",
        (
            "0花苞bud",
            "1开花bloom",
            "2结果bear_fruit",
            "3待成熟的果实unripe_fruit",
            "4成熟的果实",
        ),
    ),
    (
        "云冠果",
        "cloud_crown_fruit",
        (
            "0花苞bud",
            "1开花bloom",
            "2结果bear_fruit",
            "3待成熟的果实unripe_fruit",
            "4成熟的果实",
        ),
    ),
    (
        "暮光榴果",
        "twilight_pomegranate",
        (
            "0树干花苞bud",
            "1开花bloom",
            "2结果bear_fruit",
            "3待成熟的果实unripe_fruit",
            "4成熟的果实，也是果实方块",
        ),
    ),
)
MODELS: Final = (
    *(
        CropModel(
            source,
            f"{name}_ground_{count}",
            "果实方块fruit"
            if name == "cloud_crown_fruit" and count == 1
            else f"{count}果实方块fruit",
        )
        for source, name, _ in FRUITS[:3]
        for count in range(1, 5)
    ),
    *(
        CropModel(source, f"{name}_stage_{age}", group)
        for source, name, groups in FRUITS
        for age, group in enumerate(groups)
    ),
    *(
        CropModel("露穗谷", f"dewspike_grain_stage_{age}", None, age)
        for age in range(8)
    ),
    CropModel("露穗谷", "dewspike_grain_stage_7_tall", None, 8),
    *(
        CropModel(source, f"{name}_stage_{age}", str(age))
        for source, name, stages in (
            ("星纹薯", "star_pattern_yam", 4),
            ("晶露果", "crystal_dew_fruit", 5),
            ("藤豆", "vine_bean", 5),
        )
        for age in range(stages)
    ),
    *(
        CropModel("月苜草", f"moon_clover_stage_{age}", None, age)
        for age in range(4)
    ),
    CropModel("星纹薯", "wild_star_pattern_yam", "wild"),
    CropModel("晶露果", "wild_crystal_dew_fruit", "wild"),
    CropModel("藤豆", "wild_vine_bean", "wild"),
    CropModel("月苜草", "wild_moon_clover", None, 4),
)
ICONS: Final = (
    ("作物/crop_星纹薯.png", "star_pattern_yam"),
    ("作物/fruit_晶露果.png", "crystal_dew_fruit"),
    ("作物/seed_晶露果种子.png", "crystal_dew_fruit_seeds"),
    ("作物/月苜草_flower.png", "moon_clover"),
    ("作物/月苜草_seed.png", "moon_clover_seeds"),
    ("作物/藤豆_bean.png", "vine_bean"),
    ("作物/藤豆_seed.png", "vine_bean_seeds"),
    ("作物/fruit_圣辉果.png", "sacred_light_fruit"),
    ("作物/wild_星纹薯.png", "wild_star_pattern_yam"),
    ("作物/wild_晶露果.png", "wild_crystal_dew_fruit"),
    ("作物/wild_藤豆.png", "wild_vine_bean"),
    ("作物/wild_月苜草.png", "wild_moon_clover"),
    ("作物/crop_露穗谷.png", "dewspike_grain"),
    ("作物/seed_露穗谷种子.png", "dewspike_grain_seeds"),
    ("作物/潮歌椰_fruit.png", "tide_song_coconut"),
    ("作物/fruit_云冠果.png", "cloud_crown_fruit"),
    ("作物/fruit_暮光榴果.png", "twilight_pomegranate"),
    ("方块/植物/水葱物品.png", "water_scallion"),
    ("方块/植物/伞花水蓑衣物品_grass.png", "umbrella_hygrophila"),
)


def asset_category(name: str) -> str:
    """Keep crop exports in the same content directories as their runtime assets."""
    if name.startswith("wild_"):
        return "plant/wild_crop"
    if name in ("water_scallion", "umbrella_hygrophila"):
        return "plant/aquatic"
    if name.startswith(("tide_song_coconut", "sacred_light_fruit", "cloud_crown_fruit", "twilight_pomegranate")):
        return "fruit"
    return "crop"


def model_assets(spec: CropModel) -> tuple[tuple[Path, bytes], ...]:
    """Return exact model bytes and only the original textures used by its faces."""
    source = REFERENCE / "作物" / f"{spec.source}.bbmodel"
    model = convert_crop_model(source, spec.group, spec.texture)
    textures = crop_texture_sources(source)
    used = sorted(
        {
            int(_as_string(_as_object(face, "face")["texture"], "texture")[1:])
            for element in _as_array(model["elements"], "elements")
            for face in _as_object(
                _as_object(element, "element")["faces"],
                "faces",
            ).values()
        },
    )
    category = asset_category(spec.name)
    bindings: JsonObject = {
        str(index): f"eden_realm:block/{category}/{spec.name}_{index}" for index in used
    }
    bindings["particle"] = f"#{used[0]}"
    model["textures"] = bindings
    encoded = json.dumps(model, ensure_ascii=False, indent=2).encode("utf-8")
    return (
        (ASSETS / "models/block" / category / f"{spec.name}.json", encoded),
        *(
            (ASSETS / "textures/block" / category / f"{spec.name}_{index}.png", data)
            for index in used
            for data in (textures[index],)
        ),
    )


def main() -> None:
    """Export assets or compare them byte-for-byte without writing in check mode."""
    check = _check_mode(sys.argv[1:])
    outputs = [asset for spec in MODELS for asset in model_assets(spec)]
    outputs.extend(
        (ASSETS / "textures/item" / asset_category(name) / f"{name}.png", (REFERENCE / source).read_bytes())
        for source, name in ICONS
    )
    for source, name in (
        ("潮歌树", "tide_song"),
        ("圣辉树", "sacred_light"),
        ("云冠树", "cloud_crown"),
        ("暮光树", "twilight_pomegranate"),
    ):
        outputs.append(
            (
                ASSETS / "textures/block/wood" / name / f"{name}_flowering_leaves.png",
                (
                    REFERENCE / "树木的基础方块,物品和实体" / source / "leaf_flower.png"
                ).read_bytes(),
            ),
        )
    stale: list[Path] = []
    for path, data in outputs:
        if check:
            if not path.is_file() or path.read_bytes() != data:
                stale.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            _ = path.write_bytes(data)
    if stale:
        raise StaleModelsError(tuple(stale))
    print(
        f"{'verified' if check else 'exported'} {len(MODELS)} crop models",
        "and original texture copies",
    )


if __name__ == "__main__":
    main()
