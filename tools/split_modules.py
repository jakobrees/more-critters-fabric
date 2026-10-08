#!/usr/bin/env python3
"""One-off: splits the four largest modules into sub-modules small enough for one agent each.
Edits tools/inventory/ids.json and reference-files.json; run tools/generate_modules.py afterwards."""
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ids = json.loads((ROOT / 'tools/inventory/ids.json').read_text())
files = json.loads((ROOT / 'tools/inventory/reference-files.json').read_text())

def snake(s): return re.sub(r'(?<!^)(?=[A-Z])', '_', s).lower()

# module -> list of (new module, [keywords matched against id names and snake_case file stems]); first match wins
SPLITS = {
    'critterlings': [
        ('critterlings_a', ['cubefrog', 'plainswyrm', 'dunger', 'snek']),
        ('critterlings_b', ['expy', 'scowl', 'rollball', 'opalcrab']),
        ('critterlings_c', ['mothkid', 'gillmunch', 'dominic', 'olmer']),
        ('critterlings_d', ['stalk', 'flarg', 'piranheed', 'mangotrice']),
        ('critterlings_e', ['fresnoid', 'lightfly', 'cobble']),
        ('critterling_system', ['']),   # everything else: sacks, evolution table, evolite, trophy, confetti, evolutioner, maw, eater, fossils
    ],
    'mightshroom': [
        ('nightshroom', ['nightshroom', 'frightshroom', 'fungal_zombie', 'rot_', 'rot$', 'ancient_skeleton', 'mori_roots', 'spawn_doll', 'ancient_bone', 'fungal_flesh', 'regenerative_flesh', 'fright', 'zoglin', 'rotsclale']),
        ('mightshroom', ['']),
    ],
    'corpse_crew': [
        ('corpse_gear', ['flying_pearl', 'pearl', 'rum', 'hardtack', 'cutlass', 'pirate', 'virus', 'purgatorial', 'stunned', 'infused_cannon', 'cannon_ball']),
        ('corpse_crew', ['']),
    ],
    'ghost_ship': [
        ('ship_fittings', ['cannon', 'ship_wheel', 'jolly_roger', 'treasure', 'ectometal_screw$']),
        ('ghostly_wood', ['']),
    ],
}

def assign(old, name):
    for new, kws in SPLITS[old]:
        if any(k == '' or re.search(k, name) for k in kws):
            return new
    return old

changed = 0
for key, module in list(ids.items()):
    if module in SPLITS:
        kind, id_ = key.split(':', 1)
        name = id_.replace('.', '_')
        new = assign(module, name)
        if new != module: changed += 1
        ids[key] = new
for rel, module in list(files.items()):
    if module in SPLITS:
        stem = snake(Path(rel).stem)
        files[rel] = assign(module, stem)
json.dump(ids, open(ROOT / 'tools/inventory/ids.json', 'w'), indent=0, sort_keys=True)
json.dump(files, open(ROOT / 'tools/inventory/reference-files.json', 'w'), indent=0, sort_keys=True)
from collections import Counter
print(Counter(ids.values()).most_common())
print(Counter(files.values()).most_common())
