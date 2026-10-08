# More Critters for Fabric

An unofficial Fabric port of [More Critters](https://modrinth.com/mod/more-critters) by Portakal Cevheri:
fantasy creatures in vanilla style. Published with the original author's permission.

This port matches More Critters **1.4.5** and runs on **Minecraft 26.3** with Fabric. Please report bugs
in this port here, not to the original mod.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [GeckoLib](https://modrinth.com/mod/geckolib) 5.5 or newer
- Java 25 or newer

Install it on both the server and every client.

## Differences from the original

- MCreator's bucket fluids are replaced by vanilla-style mob buckets.
- The Critter Atlas is a single client-side book screen.

## Compatibility

- **Terralith** and **Biomes O' Plenty**: critters, structures and features also appear in their matching
  biomes (bunbugs in Terralith's deserts, collector wagons in BoP's prairies, and so on). Neither mod is
  required (nor is Incendium, below); the extra biomes are simply skipped when they are not installed.
- **Incendium**: warptraps also spawn in its Inverted Forest, the warped forest of Incendium's Nether.
- **Geophilic** and other mods that reshape vanilla biomes without renaming them work as they are.
- The biome choices are tags in `data/more_critters/tags/worldgen/biome/` (written by `tools/biome_tags.py`),
  so datapacks can add or remove biomes.

## Building

```
./gradlew build          # jar in build/libs
./gradlew runGameTest    # behaviour tests (src/gametest)
```

## Credits

- **Portakal Cevheri**: More Critters (textures, models, animations, sounds and design)
- Natsirt, OrangeeApple, MistyJam, MSF and Skipster112: contributors to the original
- **Nergan**: the NeoForge port this port was written from

## How this port was made

More Critters is released for Forge 1.20.1 only, and its source code is not published. Nergan decompiled it
and turned it into a NeoForge 1.21.1 port. That decompiled code was the starting point here, as a reference
for how everything behaves.

The Fabric version is a full rewrite, not a conversion: every critter, block, item, structure and
screen was written again from scratch against Fabric and Minecraft 26.3. Where the decompiled code was
unclear, the original Forge jar was the final word. The textures, models, animations, sounds and data
files are the original mod's own.

## License

The code of this port is licensed under the MPL-2.0 (`LICENSE`). The original More Critters, including
its assets, is MIT-licensed by Portakal Cevheri (`LICENSE-MORE-CRITTERS`). Both licences ship inside the jar.
