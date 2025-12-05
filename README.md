# HerrShulker

HerrShulker is a fork of [MrShulker](https://github.com/mrmindor/MrShulker) updated for Minecraft 1.21.10 compatibility.

The mod allows players to attach items to shulker boxes for 1 XP using an anvil. The attached item then appears on the lid of the shulker box.

HerrShulker is backward compatible with both MrShulker and Shulker+. If you are migrating from a world with either mod, the data will carry over automatically.

## Changes from MrShulker

- Updated to Minecraft 1.21.10
- Rewritten render state management for new Minecraft render API
- Added network synchronization for server config
- Package renamed to `net.tinkstav.herrshulker`

## Features

- Attach items to shulker boxes for display purposes
- Dye shulker boxes via crouch-use
- Client-configuration options for lid item scales in different display contexts
- Server-configuration options to enable or disable specific mod features
- Per-shulker lid item scaling
- Commands to manage all settings

## Requirements and Dependencies

HerrShulker is required server-side to function.
It is not required that all players have HerrShulker installed client-side, but it is required for the lid items to display.

Requires Fabric API.

## Commands

Commands are separated into two groups: server-side and client-side.

### Server Commands (mrshulker)

```
mrshulker [set|query|reset] allow_dyeing [true|false]
mrshulker [set|query|reset] allow_per_shulker_scaling [true|false]
mrshulker [set|query|reset] custom_scale #.#
```

### Client Commands (mrshulker_display)

```
mrshulker_display set scale [display_context] #.#
mrshulker_display [query|reset] scale [display_context]
mrshulker_display [set|query|reset] show_custom_scales [true|false]
```

**Display contexts:** firstperson_lefthand, firstperson_righthand, fixed, ground, gui, head, none, thirdperson_lefthand, thirdperson_righthand, block, default

## Lid Item Scaling Priority

1. Per-shulker custom_scale (if set and both allow_per_shulker_scales and show_custom_scales are true)
2. Display context scale for current ItemDisplayContext
3. The 'default' display_context value as fallback

## Credits

- Original [MrShulker](https://github.com/mrmindor/MrShulker) by mrmindor
- Crouch-use dyeing feature by tnoctua
