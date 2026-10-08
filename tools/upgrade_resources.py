#!/usr/bin/env python3
"""Brings the original mod's resources (1.20.1 / 1.21.1 formats) up to Minecraft 26.3.

Run from the repository root after changing anything under src/main/resources that
came from the original mod. Idempotent: it only writes files that would change.

What it does:
  * items/<name>.json           client item definitions, required since 1.21.4, one per item model
  * GeckoLib items              an item whose model descends from displaysettings/ (Blockbench
                                "builtin/entity") gets a minecraft:special definition with GeckoLib's
                                renderer, and the displaysettings model loses its builtin parent
  * spawn egg textures          since 1.21.5 spawn eggs have their own texture instead of two tint
                                colours; recoloured from the vanilla pig egg with the original colours
  * recipes                     ingredient objects -> strings, as the 1.21.2+ codec requires
  * worldgen block states       {"Name": .., "Properties": ..} -> "id" or {"id": .., "properties": ..}
  * loot tables                 {"min", "max"} number ranges need "type": "minecraft:uniform" (26.x);
                                MCreator's "drops minecraft:air" entries become empty entries
  * animation molang            Blockbench's stray "+NaN" terms and "*+n" are rejected by GeckoLib 5
  * armor equipment assets     textures/models/armor/<piece>_layer_N.png -> equipment/<material>.json and
                                textures/entity/equipment/humanoid[_leggings]/<material>.png (1.21.2+)
  * configured features         worldgen/configured_feature/x.json {"type", "config": {..}} ->
                                worldgen/feature/x.json with the config fields inline (26.x)
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'src/main/resources/assets/more_critters'
DATA = ROOT / 'src/main/resources/data/more_critters'
REFERENCE_ITEMS = ROOT.parent / 'more-critters-reference/src/main/java/com/morecritters/mod/init/MoreCrittersModItems.java'
VANILLA_EGG = ROOT / 'tools/reference-data/vanilla/pig_spawn_egg.png'

written = 0


def write_json(path: Path, value) -> None:
    global written
    text = json.dumps(value, indent=2) + '\n'
    if path.exists() and path.read_text() == text:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    written += 1


def item_definitions() -> None:
    for model in sorted((ASSETS / 'models/item').glob('*.json')):
        name = model.stem
        parent = json.loads(model.read_text()).get('parent', '')
        if 'displaysettings/' in parent:
            # Drawn by GeckoLib; the base model only carries the display transforms.
            write_json(ASSETS / 'items' / f'{name}.json',
                       {'model': {'type': 'minecraft:special', 'base': f'more_critters:item/{name}', 'model': {'type': 'geckolib:geckolib'}}})
        else:
            write_json(ASSETS / 'items' / f'{name}.json',
                       {'model': {'type': 'minecraft:model', 'model': f'more_critters:item/{name}'}})
    for model in sorted((ASSETS / 'models/displaysettings').glob('*.json')):
        data = json.loads(model.read_text())
        if data.get('parent') == 'builtin/entity':
            del data['parent']
            write_json(model, data)


def spawn_eggs() -> None:
    from PIL import Image
    colours = {}
    for m in re.finditer(r'"(\w+_spawn_egg)",\s*\(\)\s*->\s*new DeferredSpawnEggItem\([^,]+,\s*(-?\d+),\s*(-?\d+)', REFERENCE_ITEMS.read_text()):
        colours[m.group(1)] = (int(m.group(2)) & 0xFFFFFF, int(m.group(3)) & 0xFFFFFF)
    template = Image.open(VANILLA_EGG).convert('RGBA')
    # The pig egg is drawn in two colours (base 0xF0A5A2, spots 0xDB635F) plus shading of each;
    # pixels are sorted into the two by brightness and the shading is carried over as a ratio.
    pig_base, pig_spot = (0xF0, 0xA5, 0xA2), (0xDB, 0x63, 0x5F)
    def lum(c): return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    threshold = (lum(pig_base) + lum(pig_spot)) / 2

    for name, (base, spot) in sorted(colours.items()):
        base_rgb = ((base >> 16) & 255, (base >> 8) & 255, base & 255)
        spot_rgb = ((spot >> 16) & 255, (spot >> 8) & 255, spot & 255)
        out = Image.new('RGBA', template.size)
        for y in range(template.height):
            for x in range(template.width):
                r, g, b, a = template.getpixel((x, y))
                if a == 0:
                    out.putpixel((x, y), (0, 0, 0, 0))
                    continue
                ref, new = (pig_base, base_rgb) if lum((r, g, b)) >= threshold else (pig_spot, spot_rgb)
                ratio = lum((r, g, b)) / max(lum(ref), 1)
                out.putpixel((x, y), tuple(min(255, int(round(c * ratio))) for c in new) + (a,))
        texture = ASSETS / 'textures/item' / f'{name}.png'
        if not texture.exists():
            out.save(texture)
            global written
            written += 1
        write_json(ASSETS / 'models/item' / f'{name}.json',
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'more_critters:item/{name}'}})


def ingredient(value):
    """1.21.1 ingredient -> 1.21.2+: a string item id, '#tag', or a list of those."""
    if isinstance(value, list):
        flat = [ingredient(v) for v in value]
        return flat[0] if len(flat) == 1 else flat
    if isinstance(value, dict):
        if 'item' in value:
            return value['item']
        if 'tag' in value:
            return '#' + value['tag']
    return value


def recipes() -> None:
    for path in sorted((DATA / 'recipe').glob('*.json')):
        recipe = json.loads(path.read_text())
        if 'ingredients' in recipe:
            recipe['ingredients'] = [ingredient(i) for i in recipe['ingredients']]
        if 'ingredient' in recipe:
            recipe['ingredient'] = ingredient(recipe['ingredient'])
        if 'key' in recipe:
            recipe['key'] = {k: ingredient(v) for k, v in recipe['key'].items()}
        result = recipe.get('result')
        if isinstance(result, dict) and 'item' in result:
            result['id'] = result.pop('item')
        write_json(path, recipe)


def block_state(value):
    """Recursively rewrites the pre-1.21.2 block state form inside worldgen JSON."""
    if isinstance(value, dict):
        if 'Name' in value and set(value) <= {'Name', 'Properties'}:
            if value.get('Properties'):
                return {'id': value['Name'], 'properties': value['Properties']}
            return value['Name']
        return {k: block_state(v) for k, v in value.items()}
    if isinstance(value, list):
        return [block_state(v) for v in value]
    return value


def worldgen() -> None:
    for path in sorted((DATA / 'worldgen').rglob('*.json')):
        data = json.loads(path.read_text())
        write_json(path, block_state(data))
    old = DATA / 'worldgen/configured_feature'
    if old.exists():
        for path in sorted(old.glob('*.json')):
            data = json.loads(path.read_text())
            flat = {'type': data['type'], **data.get('config', {})}
            write_json(DATA / 'worldgen/feature' / path.name, flat)
            path.unlink()
        old.rmdir()


def no_air_drops(value):
    """An item entry for minecraft:air (MCreator's "nothing") is rejected since 26.x; use an empty entry."""
    if isinstance(value, dict):
        if value.get('type') == 'minecraft:item' and value.get('name') == 'minecraft:air':
            value = {k: v for k, v in value.items() if k != 'name'}
            value['type'] = 'minecraft:empty'
            return value
        return {k: no_air_drops(v) for k, v in value.items()}
    if isinstance(value, list):
        return [no_air_drops(v) for v in value]
    return value


def number_ranges(value):
    """Recursively gives untyped {min, max} ranges in loot tables their uniform type."""
    if isinstance(value, dict):
        if set(value) == {'min', 'max'}:
            return {'type': 'minecraft:uniform', 'min': number_ranges(value['min']), 'max': number_ranges(value['max'])}
        return {k: number_ranges(v) for k, v in value.items()}
    if isinstance(value, list):
        return [number_ranges(v) for v in value]
    return value


def loot_tables() -> None:
    for root in (DATA / 'loot_table', DATA.parent / 'minecraft/loot_table'):
        for path in sorted(root.rglob('*.json')):
            write_json(path, no_air_drops(number_ranges(json.loads(path.read_text()))))


def molang(value):
    if isinstance(value, dict):
        return {k: molang(v) for k, v in value.items()}
    if isinstance(value, list):
        return [molang(v) for v in value]
    if isinstance(value, str) and ('nan' in value.lower() or '*+' in value.replace(' ', '') or re.search(r'\d\.(?!\d)', value)):
        value = re.sub(r'\s*[+-]\s*nan', '', value, flags=re.I)
        value = re.sub(r'\*\s*\+\s*', '*', value)
    if isinstance(value, str):
        value = re.sub(r'(\d)\.(?!\d)', r'\1', value)   # "0." -> "0"
    return value


def animations() -> None:
    for path in sorted((ASSETS / 'geckolib/animations').glob('*.json')):
        write_json(path, molang(json.loads(path.read_text())))


# armor material -> the original layer textures (layer_1 = body/head/feet, layer_2 = leggings)
ARMOR = {
    'sturdy': 'sturdy_chestplate',
    'iropod_helmet': 'iropod_helmet',
    'nautical_helmet': 'nautical_helmet',
}


def equipment() -> None:
    import shutil
    for material, piece in ARMOR.items():
        layers = {}
        for layer, slot in (('1', 'humanoid'), ('2', 'humanoid_leggings')):
            src = ASSETS / 'textures/models/armor' / f'{piece}_layer_{layer}.png'
            if not src.exists():
                continue
            dst = ASSETS / 'textures/entity/equipment' / slot / f'{material}.png'
            if not dst.exists():
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy(src, dst)
                global written
                written += 1
            layers[slot] = [{'texture': f'more_critters:{material}'}]
        write_json(ASSETS / 'equipment' / f'{material}.json', {'layers': layers})


def main() -> int:
    equipment()
    animations()
    item_definitions()
    spawn_eggs()
    recipes()
    worldgen()
    loot_tables()
    print(f'{written} files written')
    return 0


if __name__ == '__main__':
    sys.exit(main())
