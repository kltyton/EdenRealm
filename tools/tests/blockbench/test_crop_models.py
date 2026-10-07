import json
from pathlib import Path
from typing import Final

from tools.blockbench.export_crop_assets import MODELS
from tools.blockbench.shared import blockbench_model

ROOT: Final = Path(__file__).resolve().parents[3] / "参考资料" / "作物"


def test_all_authored_cultivated_stages_have_exported_geometry() -> None:
    # Given the artist's four cultivated crops with four/five source stages.
    expected = {
        f"{crop}_stage_{stage}"
        for crop, stages in (
            ("star_pattern_yam", 4), ("moon_clover", 4),
            ("vine_bean", 5), ("crystal_dew_fruit", 5),
        )
        for stage in range(stages)
    }
    # When their stage models are selected and converted from the source assets.
    stages = [spec for spec in MODELS if spec.name in expected]
    actual = {
        spec.name: blockbench_model.convert_crop_model(
            ROOT / f"{spec.source}.bbmodel", spec.group, spec.texture,
        )
        for spec in stages
    }
    # Then no requested stage is missing or represented by empty geometry.
    assert set(actual) == expected
    assert all(model["elements"] for model in actual.values())


def test_ground_fruit_exports_use_each_authored_stack_group() -> None:
    # Given three fruit assets with four independently authored ground groups.
    expected = {
        f"{fruit}_ground_{count}": "果实方块fruit"
        if fruit == "cloud_crown_fruit" and count == 1
        else f"{count}果实方块fruit"
        for fruit in ("tide_song_coconut", "sacred_light_fruit", "cloud_crown_fruit")
        for count in range(1, 5)
    }
    # When the export manifest selects ground models.
    actual = {spec.name: spec.group for spec in MODELS if "_ground_" in spec.name}
    # Then all twelve authored groups are used without inventing twilight stacks.
    assert actual == expected


def test_hanging_bud_keeps_source_faces_and_texture_indices() -> None:
    # Given independently authored bud and bloom groups.
    source = ROOT / "圣辉果.bbmodel"
    # When only its bud group is exported.
    model = blockbench_model.convert_crop_model(source, "0花苞bud", None)
    # Then no other growth stage leaks into the model.
    assert isinstance(model["elements"], list)
    assert len(model["elements"]) == 2
    assert '"texture": "#0"' in json.dumps(model)
    assert '"texture": "#1"' not in json.dumps(model)
    bloom = blockbench_model.convert_crop_model(source, "1开花bloom", None)
    assert isinstance(bloom["elements"], list)
    assert len(bloom["elements"]) == 6
    assert '"texture": "#1"' in json.dumps(bloom)


def test_rice_texture_variant_keeps_geometry() -> None:
    # Given one rice mesh shared by both mature textures.
    source = ROOT / "露穗谷.bbmodel"
    # When the tall mature texture is selected.
    model = blockbench_model.convert_crop_model(source, None, 8)
    # Then all 40 authored elements remain, with the tall texture binding.
    assert isinstance(model["elements"], list)
    assert len(model["elements"]) == 40
    assert '"texture": "#8"' in json.dumps(model)
    normal = blockbench_model.convert_crop_model(source, None, 7)
    assert json.dumps(normal).replace('"#7"', '"#8"') == json.dumps(model)
