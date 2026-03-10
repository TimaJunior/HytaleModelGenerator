# 🔐 Permissions

This document describes the permission system for the Cats plugin.

## Permission System Overview

The Cats plugin uses **ownership-based protection** combined with **optional permission checks** for
commands.

- **Without LuckPerms:** Basic ownership checks ensure only cat owners can control their cats
- **With LuckPerms:** Additional permission layer for fine-grained access control

## Commands and Required Permissions

### Player Commands

| Command            | Permission                               | Description               |
|--------------------|------------------------------------------|---------------------------|
| `/cat info`        | `markusbordihn.cats.command.cat.info`    | Show cat information      |
| `/cat list`        | `markusbordihn.cats.command.cat.list`    | List your tamed cats      |
| `/cat sit`         | `markusbordihn.cats.command.cat.sit`     | Make your cat sit         |
| `/cat sleep`       | `markusbordihn.cats.command.cat.sleep`   | Make your cat sleep       |
| `/cat bed`         | `markusbordihn.cats.command.cat.bed`     | Send cat to nearest bed   |
| `/cat follow`      | `markusbordihn.cats.command.cat.follow`  | Make cat follow you       |
| `/cat wait`        | `markusbordihn.cats.command.cat.wait`    | Make cat wait in place    |
| `/cat wander`      | `markusbordihn.cats.command.cat.wander`  | Allow cat to wander       |
| `/cat name <name>` | `markusbordihn.cats.command.cat.name`    | Rename your cat           |
| `/cat play`        | `markusbordihn.cats.command.cat.play`    | Make cat enter play mode  |
| `/cat search`      | `markusbordihn.cats.command.cat.search`  | Make cat search/hunt      |
| `/cat release`     | `markusbordihn.cats.command.cat.release` | Release cat to wild       |
| `/cat spawn`       | `markusbordihn.cats.command.cat.spawn`   | Respawn despawned cat     |
| `/cat despawn`     | `markusbordihn.cats.command.cat.despawn` | Despawn cat (saves state) |

### Admin Commands

| Command               | Permission                             | Description             |
|-----------------------|----------------------------------------|-------------------------|
| `/cat owner <player>` | `markusbordihn.cats.command.cat.owner` | Transfer cat ownership  |
| N/A                   | `markusbordihn.cats.admin.bypass`      | Bypass ownership checks |

### Permission Wildcards

- `markusbordihn.cats.*` - All permissions
- `markusbordihn.cats.command.cat.*` - All cat commands (recommended for players)
- `markusbordihn.cats.admin.*` - All admin permissions

## Cat Ownership Limits

Players have a **default limit of 16 cats**. This can be configured using permissions:

- `markusbordihn.cats.limit.<number>` - Set max cats (1-32, e.g., `.limit.8` for 8 cats)
- `markusbordihn.cats.limit.unlimited` - Allow unlimited cats

**If multiple limits are assigned, the highest value wins.**

### Examples with LuckPerms

```bash
# Give all players basic cat commands and 8 cat limit
/lp group default permission set markusbordihn.cats.command.cat.* true
/lp group default permission set markusbordihn.cats.limit.8 true

# VIP players get 24 cats
/lp group vip permission set markusbordihn.cats.limit.24 true

# Admins get unlimited cats and bypass
/lp group admin permission set markusbordihn.cats.limit.unlimited true
/lp group admin permission set markusbordihn.cats.admin.bypass true

# Individual player limit
/lp user <playername> permission set markusbordihn.cats.limit.16 true
```

### Examples with Hytale Permissions

```bash
# Grant command access to default group
/perm group add default markusbordihn.cats.command.cat.*

# Set cat limits
/perm group add default markusbordihn.cats.limit.8
/perm group add vip markusbordihn.cats.limit.24
/perm group add admin markusbordihn.cats.limit.unlimited

# Individual player
/perm player add <playername> markusbordihn.cats.limit.16
```

## LuckPerms Integration

If LuckPerms is installed, the plugin automatically detects it on startup and logs helpful
configuration tips to the server console.

## Important Notes

- **Ownership protection** is always active regardless of permission system
- **Cat limits** work with both LuckPerms and Hytale's built-in permissions
- **Console** always has all permissions
- **Default behavior:** Without LuckPerms, ownership checks only; with LuckPerms, additional
  permission layer
