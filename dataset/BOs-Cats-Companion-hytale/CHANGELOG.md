# Changelog for Cats (Hytale)

## Note

This change log includes the summarized changes.
For the full changelog, please go to the [GitHub History][history] instead.

Note: Please always back up your world before updating to a new version!

### 2.4.0

- Fixed black lines in water caused by incorrect `Transparent` opacity on Cat Bed and Cat Yarn
  Ball block definitions.
- Removed `/cat attack` command (replaced by `/cat pounce`).
- Added personality system: each tamed cat receives a primary and secondary personality type
  (`LAZY`, `PLAYFUL`, `SHY`, `BRAVE`, `CURIOUS`, `CUDDLY`, `INDEPENDENT`, `MISCHIEVOUS`)
  that influences happiness gain from petting, playing, and feeding.
- Added happiness/mood system: cats have a persistent happiness value (0–100) across five levels
  (`MISERABLE` → `ECSTATIC`) with slow decay while the owner is online and active.
- Added Cat Treats item (1× Raw Fish + 1× Fibre at Farming Bench) for a bigger happiness boost.
- Added gift system: happy cats may bring the owner a personality-fitting gift when petted
  (30-minute cooldown).
- Added `/cat pounce [target]` command to send a nearby cat to pounce at a target (15-block range).
- Improved `/cat info` to display happiness level, personality types, and total gifts given.
- Extended `CatDataEntry` with new persistent fields for personality, happiness, mood timestamps,
  and gift tracking.

### 2.3.1

- Fixed missing pixels in cat textures.
- Refined shading and clean up stray and semi-transparent pixels.

### 2.3.0

- Fixed tamed cats despawning due to retained world-spawn tracking after taming. Spawn tracking is
  now cleared on taming, carrier release, and chunk load.
- Fixed Cat Carrier recipe using unobtainable `Ingredient_Leather_Soft` (changed to
  `Ingredient_Fibre` x3).
- Increased tamed cat health from 20 → 60 HP (wild cats remain at 40 HP).
- Added healing on feeding: fish restores 10 HP per feeding (owner only) with status messages.
- Improved protection config comments to clarify owner damage is blocked by default.

### 2.2.1

- Fixed cats moving around while stuck in sitting animation after being fed, petted, or playing
  with yarn ball (added animation reset before state transition).
- Fixed Cat Carrier recipe using non-existent `Ingredient_Leather` (changed to
  `Ingredient_Leather_Soft`).
- Improved cat sounds by replacing leopard/wolf sounds with more cat-appropriate vanilla sounds:
  fox breathing for sleeping/petting, fox yips for greeting/yarn ball, meerkat chirps for feeding,
  and fox alert bark for wild cat rejection.

### 2.2.0

- Fixed wild cats not reacting to players holding fish (restored direct Player sensor for food
  attraction).
- Fixed wild cats running away during feeding and rejection animations (added blocking to
  Feeding/Rejection states).
- Fixed wild cats dying too quickly in fire (MaxHealth 20 → 40).
- Fixed alerted timeout too short for food attraction to trigger (5–8 s → 15–25 s).
- Added basic Cat Carrier item to pick up and transport tamed cats (recipe: 2x Wood Planks + 1x
  Leather + 1x White Wool + 1x Red Wool at Farming Workbench).
- Added server-side config files (`config/cats/`): `general.cfg` (cat limit), `protection.cfg` (
  damage filters), `spawn.cfg` (spawn enable/weight).
- Added `/cat reload` command to reload config files without a server restart.
- Added damage filter system for tamed cats: configurable protection from owner, other players, and
  mobs (default: protected from players, vulnerable to mobs).
- Added `CatSpawnConfigSystem` to apply spawn weight multiplier and enable/disable spawning via
  config.
- Added new cat animations: Eat, Laydown, Wake.

### 2.1.1

- Fixed mod warning by using fixed ServerVersion.

### 2.1.0

- Added petting interaction for owned cats (empty hand, 5-minute cooldown).
- Added greeting behavior when cats see their owner nearby.
- Added automatic day/night cycle for tamed cats (sleep at night, active during day).
- Added fleeing behavior when hostile mobs are nearby (cats run away from danger).
- Improved wild cat AI by using vanilla animal behavior template.

### 2.0.0

#### ⚠️ MAJOR UPDATE - BREAKING CHANGES

After updating from v1.8.x or earlier, your tamed cats will despawn due to the role system changes
below.
However, all cat data (owner, name, breed) are safely stored in `CatsData.json`.

Simply use `/cat spawn` to respawn your cats, they will keep their owner, name, and breed.
Use `/cat list` to see all your cats and their status.

- Added separate NPC roles for each cat breed: `Cats_<Breed>_Wild` and `Cats_<Breed>_Tamed`.
- Added cats kitten memories category.
- Improved cats memories category icon.
- Improved NPC performance by splitting logic and behavior into separate wild and tamed roles.
- Updated all egg spawner items to spawn wild cats (untamed) instead of tamed cats.
- Optimized interaction instructions by removing redundant CatTamed sensor checks.
- Moved yarn ball item to Farming bench for better organization.

### 1.8.1

- Fixed cat despawn command error message.
- Added better taming system with increased taming chances for higher quality food.
- Smaller bug fixes for v2026.02.06

### 1.8.0

- Fixed yarn ball recipe not working due to wrong workbench type.
- Added `/cat despawn` and `/cat spawn` commands for better cat management.
- Added CatsManager as centralized data access layer for all cat operations.
- Added owner name persistence (stored alongside owner UUID in cat data).
- Added persistent cat data storage in CatsDataResource (saved to `CatsData.json`).
- Refactored all commands to use CatsManager instead of direct resource access.
- Refactored cat name storage to use Nameplate component instead of CatOwnerComponent.
- Improved data architecture with strict command → manager → resource flow.
- Optimized CatDataEntry codec with named constants and streamlined implementation.

### 1.7.0

- Fixed memory registration that was causing NPCs to not be properly tracked.
- Fixed owner command not works in specific scenarios due failing to retrieve the owners UUID.
- Added `/cat attack <target>` command for combat (experimental, no damage implementation yet).
- Added natural spawning system across all zones with environment-specific distributions and
  day/night variations.
- Added all cat breeds to memory system for proper NPC tracking and persistence.

### 1.6.0

- Fixed missing translation parameters in multiple commands (catName/name were not replaced in
  messages).
- Fixed taming help message not displaying cat name properly.
- Fixed follow re-trigger hint message not displaying cat name.
- Fixed bed command messages not displaying cat name.
- Fixed ownership error message not displaying cat name.
- Added longhaired Russian Blue cat variant based on a description by `Honest_Dragoness`.
- Added Cat Yarn Ball toy item for playing with cats.
- Added better interaction hints for owned, tamed and untamed cats.
- Improved English and German translations for various commands and messages.

### 1.5.0

- Added automatic cat naming when taming with over 90 unique names.
- Added configurable cat names list (editable in `config/cats/cat_names.txt`).
- Added security validation for custom cat names to prevent exploits.
- Added `/cat release` command to release (remove) owned cats.
- Added cat limit permission support with built-in Hytale permissions and LuckPerms.
- Added cat owner tracking system to track and limit owned cats per player.
- Added LuckPerms detection on startup with corresponding log messages and instructions.
- Fixed permission system bug that could grant unlimited cats with wildcard permissions.
- Improved code structure with centralized constants for colors and default limits.

### 1.4.0

- Fixed command exception handling when no entity is targeted.
- Added Cat Bed item (placeable block for cats to sleep in).
- Added `/cat bed` command to send cats to the nearest available cat bed.
- Added custom hitbox for Cat Bed to prevent overlapping placement.
- Added item descriptions for all cat-related items (spawn eggs and cat bed).
- Added translation entries for Cat Bed in English and German.
- Improved cat bed detection and pathfinding system.
- Improved cat state management with GOING_TO_BED state.
- Improved command error handling for CatInfoCommand, CatNameCommand, and CatOwnerCommand.

### 1.3.0

- Fixed cat following behavior when tracking is lost by retriggering search state.
- Fixed commands to show the correct cat name instead of just "Cat".
- Added experimental kitten variant (Calico breed only for now).
- Improved existing cat textures with better details and shading.

Note: The kitten variant is experimental and has limited functionality and animations issues.

### 1.2.0

- Fixed persistent component storage for cat owner and state components.
- Fixed command translation placeholders (use `{name}` / `{owner}` instead of `%name%` / `%owner%`).
- Fixed item consumption for taming and feeding (items are now consumed from the player's
  inventory).
- Fixed cats spawn eggs icons.
- Added Tuxedo cat variant based on a description by `Phosphoratorium`.
- Added cat sounds for specific actions (purring, meowing, hissing).
- Refactored commands, interactions, and systems to use the registered component types.
- Improved NPC role behavior with additional idle actions (stretching, licking, playful pouncing,
  searching).

### 1.1.0

- Fixed translation system with German and English support.
- Fixed state synchronization between component and NPC substates.
- Fixed single player world taming issue.
- Refactored interaction system with proper player messages.
- Refactored files to a separate namespace for better mod compatibility.
- Improved command structure with semantic namespaces.
- Improved animation handling and added missing animations.
- Improved taming system with proper feedback messages.

### 1.0.0 ✨

- Initial release of Cats - Tameable Cat Companions for Hytale!

[history]: https://github.com/MarkusBordihn/BOs-Cats-Companion/commits/
