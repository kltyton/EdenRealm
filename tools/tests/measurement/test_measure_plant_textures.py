from pathlib import Path

from PIL import Image

from tools.measurement.measure_plant_textures import measure_texture


def test_measure_texture_reports_opaque_bounds_when_pixels_are_visible(tmp_path: Path) -> None:
    texture = tmp_path / "plant.png"
    image = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
    image.putpixel((1, 1), (255, 255, 255, 255))
    image.putpixel((2, 3), (255, 255, 255, 255))
    image.save(texture)

    measurement = measure_texture(texture, tile_size=4)

    assert measurement.image_bounds is not None
    assert measurement.image_bounds.width == 2
    assert measurement.image_bounds.height == 3
    assert measurement.image_bounds.min_x == 1
    assert measurement.image_bounds.min_y == 1


def test_measure_texture_reports_each_nonempty_tile_when_texture_is_an_atlas(tmp_path: Path) -> None:
    texture = tmp_path / "tall_plant.png"
    image = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
    image.putpixel((0, 0), (255, 255, 255, 255))
    image.putpixel((3, 3), (255, 255, 255, 255))
    image.save(texture)

    measurement = measure_texture(texture, tile_size=2)

    assert [(tile.column, tile.row) for tile in measurement.tiles] == [(0, 0), (1, 1)]
    assert all(tile.bounds.width == 1 and tile.bounds.height == 1 for tile in measurement.tiles)
