SkyRandom 1.1.0

Stable public release of SkyRandom.

This release moves the stable version line from 1.0.1 to 1.1.0 and ships as a ready-to-upload Paper plugin jar.

Added Flint and Steel to the danger loot pool with the same drop weight as TNT.

Added the new Frozen Peaks arena: a snowy floating island with tall ice-covered mountain peaks, dead trees, ice patches, and four spawn pads.

Added the new Crystal Cavern arena: a closed deepslate cave with low tunnels, vertical levels, torch lighting, amethyst patches, and crystal growths.

Added the new Trial Chamber arena: a closed multi-level chamber inspired by Minecraft trial chambers, with tuff walls, oxidized copper floors, copper bands, grates, bulbs, vaults, and trial-spawner decorations.

Added unique host menu icons for every arena to make map selection easier to scan.

Improved Frozen Peaks trees so they use real spruce logs instead of fence-like branches.

Thin snow layers on arena maps can now be broken during matches and can be cleared by arena destruction instead of acting like protected terrain.

Fixed Sudden Night mob spawning: when the host disables Sudden Night mobs, natural hostile spawns are now blocked during the night.

Added the optional Shrinking Zone match modifier.

The lobby host can enable it from Game Settings, adjust when the zone starts, and adjust how long it takes to shrink. During matches, the zone uses the per-player arena border visually and damages active players who stay outside it.

Shrinking Zone is marked as BETA in the host settings UI.

The default first zone size is 32 blocks, so the arena pressure is noticeable without forcing everyone into a tiny space too early.

Shrinking Zone now has a second stage: after the first shrink finishes, the host-selected zone delay runs again, then the final zone shrinks to 16 blocks.

Supported server software:

- Paper 1.21.11
- Paper 26.1.1
- Paper 26.1.2

Config version is now 5. Servers with a customized config can copy the new `shrinking-zone-*` settings and the Flint and Steel entry into their existing config.
