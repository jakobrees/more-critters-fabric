#!/usr/bin/env python3
"""Writes the original mod's brewing recipes as 26.3 data (data/more_critters/recipe/brewing/).

The original registered them in code (recipes/brewing/*BrewingRecipe.java in the reference);
brewing is data-driven since 1.21.2+, so they live here. Run from the repository root.
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/data/more_critters/recipe/brewing'
MC = 'more_critters:'
BOTTLES = ['minecraft:potion', 'minecraft:splash_potion', 'minecraft:lingering_potion']

# Awkward potion + reagent -> potion, in all three bottle kinds.
POTIONS = {
    'spinal_fluid_bottle': 'brain_scent',
    'end_dust': 'ends_blessing_potion',
    'sackof_freezing': 'frostbite_potion',
    'gravedigger_appendage': 'haste_potion',
    'toxin_bladder_asphyxiation': 'asphyxiation_potion',
    'toxin_bladder_hallucinazium': 'hallucinazium_potion',
    'toxin_bladder_stagnation': 'stagnation_potion',
    'toxin_bladder_muscle_ache': 'muscle_ache_potion',
    'toxin_bladder_brittleness': 'brittleness_potion',
    'shriekbat_wing': 'shriek_resistance_potion',
}
# Cupcake + toxin bladder -> poisoned cupcake.
CUPCAKES = {
    'toxin_bladder_stagnation': 'cupcake_stagnation',
    'toxin_bladder_muscle_ache': 'cupcake_muscle_ache',
    'toxin_bladder_brittleness': 'cupcake_brittleness',
    'toxin_bladder_hallucinazium': 'cupcake_hallucinazium',
    'toxin_bladder_asphyxiation': 'cupcake_asphyxiation',
}


def write(name, recipe):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f'{name}.json').write_text(json.dumps(recipe, indent=2) + '\n')


def main():
    for reagent, potion in POTIONS.items():
        for bottle in BOTTLES:
            kind = bottle.split(':')[1]
            write(f'{kind}_{potion}_{reagent}', {
                'type': 'minecraft:brewing',
                'input': {'item': bottle, 'potion_contents': {'potions': 'minecraft:awkward'}},
                'reagent': {'item': MC + reagent},
                'output': {'id': bottle, 'components': {'minecraft:potion_contents': {'potion': MC + potion}}},
            })
    for reagent, cupcake in CUPCAKES.items():
        write(f'{cupcake}', {
            'type': 'minecraft:brewing',
            'input': {'item': MC + 'cupcake'},
            'reagent': {'item': MC + reagent},
            'output': {'id': MC + cupcake},
        })
    # As in the original: an awkward potion brewed with a mysterious virus bottle yields a
    # mysterious virus bottle (the reference recipe is written that way).
    for bottle in BOTTLES:
        kind = bottle.split(':')[1]
        write(f'{kind}_mysterious_virus_bottle', {
            'type': 'minecraft:brewing',
            'input': {'item': bottle, 'potion_contents': {'potions': 'minecraft:awkward'}},
            'reagent': {'item': MC + 'mysterious_virus_bottle'},
            'output': {'id': MC + 'mysterious_virus_bottle'},
        })
    print(len(list(OUT.glob('*.json'))), 'brewing recipes')


if __name__ == '__main__':
    main()
