# Herr Shulker

Attach an item to the lid of a shulker box so you can see what it holds. This is a fork of
[MrShulker](https://github.com/mrmindor/MrShulker) by Mr.Mindor.

This repository publishes the complete source of the released versions of Herr Shulker.
Each release tag (`vX.Y.Z`) holds the source of that version. This commit is
**1.5.11** (Minecraft 26.1.2, published 2026-06-11).

## Building

Requirements: run Gradle with JDK 21 or newer (set `JAVA_HOME`). The 1.21.x targets compile for
Java 21 and the 26.x targets for Java 25. Gradle finds or downloads those through its toolchain support.
The version number normally comes from the development repository's git history, so set it
explicitly when you build from this repository:

```bash
BRECHER_VERSION_OVERRIDE=1.5.11 ./gradlew build                                          # 1.21.10 and 1.21.11
(cd modern/fabric-26.1 && BRECHER_VERSION_OVERRIDE=1.5.11 ./gradlew build)               # 26.1.x
```

Jars are written to each target's `build/libs/`.

## License

GNU General Public License v3.0 or later. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
