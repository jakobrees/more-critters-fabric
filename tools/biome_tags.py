#!/usr/bin/env python3
"""Writes the biome tags that decide where critters spawn and where structures and features generate.

The original names vanilla biomes only. Terralith and Biomes O' Plenty replace much of the overworld
with biomes of their own, so each tag also lists the ones that resemble the original's choice, as
optional entries: they count when that mod is installed and are skipped when it is not.

    python3 tools/biome_tags.py      (rewrites data/more_critters/tags/worldgen/biome/)
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/data/more_critters/tags/worldgen/biome'

# Groups of biomes: vanilla ids first (required), then the look-alikes from other mods (optional).
DESERTS = (['desert'],
           ['terralith:ancient_sands', 'terralith:desert_canyon', 'terralith:desert_oasis', 'terralith:desert_spires',
            'terralith:gravel_desert', 'terralith:lush_desert', 'terralith:sandstone_valley',
            'biomesoplenty:dryland', 'biomesoplenty:lush_desert'])
BADLANDS = (['badlands', 'eroded_badlands', 'wooded_badlands'],
            ['terralith:bryce_canyon', 'terralith:painted_mountains', 'terralith:savanna_badlands',
             'terralith:snowy_badlands', 'terralith:warped_mesa', 'terralith:white_mesa'])
JUNGLES = (['jungle', 'sparse_jungle'],
           ['terralith:amethyst_rainforest', 'terralith:jungle_mountains', 'terralith:rocky_jungle',
            'terralith:tropical_jungle', 'biomesoplenty:rainforest', 'biomesoplenty:rocky_rainforest',
            'biomesoplenty:tropics'])
DARK_FORESTS = (['dark_forest'], ['biomesoplenty:ominous_woods'])
SNOWY = (['snowy_plains', 'snowy_taiga', 'ice_spikes', 'snowy_slopes'],
         ['terralith:alpha_islands_winter', 'terralith:frozen_cliffs', 'terralith:glacial_chasm', 'terralith:ice_marsh',
          'terralith:siberian_grove', 'terralith:siberian_taiga', 'terralith:skylands_winter',
          'terralith:snowy_cherry_grove', 'terralith:snowy_maple_forest', 'terralith:snowy_shield',
          'terralith:wintry_forest', 'terralith:wintry_lowlands', 'biomesoplenty:muskeg',
          'biomesoplenty:snowblossom_grove', 'biomesoplenty:snowy_coniferous_forest', 'biomesoplenty:snowy_fir_clearing',
          'biomesoplenty:snowy_maple_woods', 'biomesoplenty:tundra', 'biomesoplenty:wintry_origin_valley'])
SNOWY_PLAINS = (['snowy_plains'], ['terralith:snowy_shield', 'terralith:wintry_lowlands', 'biomesoplenty:tundra'])
BIRCH_FORESTS = (['birch_forest', 'old_growth_birch_forest'], ['terralith:birch_taiga'])
SWAMPS = (['swamp'], ['terralith:orchid_swamp', 'biomesoplenty:bayou', 'biomesoplenty:bog', 'biomesoplenty:marsh',
                      'biomesoplenty:wetland'])
RIVERS_AND_SWAMPS = (['river', 'swamp'], ['terralith:warm_river'] + SWAMPS[1] + ['biomesoplenty:floodplain'])
PLAINS = (['plains'], ['terralith:blooming_valley', 'terralith:lavender_valley', 'terralith:valley_clearing',
                       'biomesoplenty:field', 'biomesoplenty:pasture', 'biomesoplenty:prairie'])
SAVANNAS = (['savanna'], ['terralith:ashen_savanna', 'terralith:fractured_savanna', 'terralith:hot_shrubland',
                          'terralith:savanna_slopes', 'biomesoplenty:lush_savanna', 'biomesoplenty:scrubland'])
TAIGAS = (['old_growth_pine_taiga', 'old_growth_spruce_taiga', 'taiga'],
          ['terralith:shield', 'terralith:shield_clearing', 'biomesoplenty:coniferous_forest',
           'biomesoplenty:fir_clearing'])
OLD_GROWTH_TAIGAS = (['old_growth_pine_taiga', 'old_growth_spruce_taiga'], ['biomesoplenty:redwood_forest'])
MEADOWS = (['meadow'], ['terralith:blooming_plateau', 'terralith:temperate_highlands', 'biomesoplenty:highland'])
LUSH_CAVES = (['lush_caves'], ['terralith:cave/underground_jungle'])
CAVES = (['dripstone_caves', 'lush_caves'], ['terralith:cave/underground_jungle'])
WARM_OCEANS = (['warm_ocean'], ['terralith:deep_warm_ocean'])
OCEANS = (['cold_ocean', 'deep_cold_ocean', 'deep_lukewarm_ocean', 'deep_ocean', 'lukewarm_ocean', 'ocean', 'warm_ocean'],
          ['terralith:deep_warm_ocean'])
END = (['end_barrens', 'end_highlands', 'end_midlands', 'small_end_islands'],
       ['biomesoplenty:end_corruption', 'biomesoplenty:end_flats', 'biomesoplenty:end_reef', 'biomesoplenty:end_wilds'])

TAGS = {
    # Where each critter spawns (Spawns.inBiomeTag in its module).
    'spawns/bunbug': DESERTS,
    'spawns/balloon_rat': BADLANDS,
    'spawns/bouncelizard': JUNGLES,
    'spawns/shadelet': DARK_FORESTS,
    'spawns/snowflake_spider': SNOWY,
    'spawns/treeplet': BIRCH_FORESTS,
    'spawns/stincarp': RIVERS_AND_SWAMPS,
    'spawns/armossillo': LUSH_CAVES,
    'spawns/nauticrawl': WARM_OCEANS,
    'spawns/shimmerwing': END,
    # Where each structure may generate (its worldgen/structure file).
    'has_structure/abandoned_bunbug_farm': DESERTS,
    'has_structure/abandoned_mine': CAVES,
    'has_structure/buried_nauticrawl': WARM_OCEANS,
    'has_structure/collector_wagon_badlands': (['badlands'], BADLANDS[1]),
    'has_structure/collector_wagon_cave': CAVES,
    'has_structure/collector_wagon_desert': DESERTS,
    'has_structure/collector_wagon_jungle': JUNGLES,
    'has_structure/collector_wagon_ocean': OCEANS,
    'has_structure/collector_wagon_plains': PLAINS,
    'has_structure/collector_wagon_savanna': SAVANNAS,
    'has_structure/collector_wagon_swamp': SWAMPS,
    'has_structure/collector_wagon_taiga': TAIGAS,
    'has_structure/collector_wagon_tundra': SNOWY_PLAINS,
    'has_structure/evolutioner_tower': OLD_GROWTH_TAIGAS,
    'has_structure/freshenge': MEADOWS,
    'has_structure/ghost_ship': OCEANS,
    'has_structure/hanging_garden_of_bouncelizards': (['jungle'], JUNGLES[1]),
    'has_structure/witch_greenhouse': SWAMPS,
    # Where features generate (BiomeModifications.addFeature in the module).
    'has_feature/eerie_birch_trees': BIRCH_FORESTS,
}


def entry(biome, required):
    biome = biome if ':' in biome else 'minecraft:' + biome
    return biome if required else {'id': biome, 'required': False}


def main():
    for name, (vanilla, others) in TAGS.items():
        path = OUT / f'{name}.json'
        path.parent.mkdir(parents=True, exist_ok=True)
        values = [entry(b, True) for b in vanilla] + [entry(b, False) for b in others]
        path.write_text(json.dumps({'values': values}, indent=2) + '\n')
    print(f'{len(TAGS)} tags written to {OUT}')


if __name__ == '__main__':
    main()
