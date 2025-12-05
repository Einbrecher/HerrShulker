# Changelog

All notable changes to Herr Shulker will be documented in this file.

This project is a fork of [MrShulker](https://github.com/mrmindor/MrShulker) by Mr.Mindor, licensed under GPL-3.0.

## [Unreleased] - Herr Shulker Fork

### Fork Information

- **Forked From**: MrShulker v1.4.2-1.21.10
- **Original Author**: Mr.Mindor (mrmindor)
- **Original Contributors**: N (tnoctua)
- **Fork Date**: December 2025
- **Fork Author**: Einbrecher

### Changed (Rename/Rebrand)

- Renamed mod from "MrShulker" to "Herr Shulker"
- Changed mod ID from `mrshulker` to `herrshulker`
- Changed package from `io.github.mrmindor.mrshulker` to `net.tinkstav.herrshulker`
- Renamed main classes: `MrShulker` → `HerrShulker`, `MrShulkerClient` → `HerrShulkerClient`

### Added

- `IShulkerRendererLidItem` interface for managing renderer state data
  - Provides `getBlockEntityData()` for TypedEntityData access from ItemStack
  - Provides `getStack()`/`setStack()` for current ItemStack being rendered
- `MixinLayerRenderState` for per-layer ItemStack capture
  - Implements "capture/restore" pattern for ShulkerBoxSpecialRenderer
  - Stores ItemStack in `@Unique` field since NoDataSpecialModelRenderer loses it
- `MixinSpecialModelWrapper` for passing block entity data through model system
- `ShulkerBoxLidItemRenderState` interface for render state data on ShulkerBoxRenderState

### Changed

- **Render State Management**: Complete overhaul for Minecraft 1.21.10's new caching behavior
  - Each `ShulkerBoxRenderState` now has its own `ItemStackRenderState` via mixin
  - Prevents state corruption when multiple shulker boxes render in the same frame
  - Shared renderer state is restored from per-render-state data before each submit
- `MixinShulkerBoxSpecialRenderer`: Refactored for new render API
  - Updated to work with queued render nodes that execute later
  - Implements proper state isolation per shulker box instance
- `MixinShulkerBoxRenderer`: Updated for 1.21.10 render changes
- `ClientConfig`: Enhanced configuration handling
- `ServerConfig`: Improved server-side configuration
- `MixinAnvilMenu`: Updated anvil interaction logic
- `MixinDyeItem`: Refined dye interaction handling
- `MixinShulkerBoxBlock`: Updated block behavior
- `MixinShulkerBoxBlockEntity`: Enhanced block entity data handling
- `ModComponents`: Updated component registration

### Removed

- `MixinItemRenderer` - Deprecated, replaced by new render state management system

### Technical Details

The primary motivation for this fork was to address render state corruption issues in Minecraft 1.21.10. The new Minecraft render API queues render nodes that execute later, which caused issues when multiple shulker boxes with lid items rendered in the same frame. The original MrShulker's approach of using shared renderer state was incompatible with this new behavior.

The solution implements per-render-state isolation:
1. Each `ShulkerBoxRenderState` maintains its own `ItemStackRenderState`
2. `MixinLayerRenderState` captures the ItemStack per-layer before it's lost
3. State is restored from per-render-state data before each submit call

---

## Original MrShulker Changelog

### [1.4.2] - 1.21.10

- Updated to Minecraft 1.21.10

### [1.4.0] - 1.21.5

- Added client-side configuration for lid item scales in different display contexts
- Added server-side configuration options for mod features
- Added per-shulker lid item scaling
- Added commands to manage settings
- Added Permissions API to server commands (contributor: tnoctua)

### [1.3.0]

- Added crouch-use dyeing of shulker boxes (contributor: tnoctua)

### [1.2.0]

- Fixed rendering of filled maps
- Maps now render correctly on placed shulkers
- Filled map items now attempt to render map contents when available

### [1.1.0]

- Added backward compatibility for migrating from ShulkerPlus mod
- Cleaned up anvil menu behavior for consistent lid item/rename operations
- Cleaned up component tags

### [1.0.0]

- Initial release
- Items can be attached to shulker boxes via anvil for 1 XP
- Lid items display on placed shulker boxes and item forms
