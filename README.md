# RollBall

RollBall is a Fabric mod based on the open-source KatamariIO project.

## Credits and license

The gameplay and rendering foundation is derived from **KatamariIO by TACOWASA059**:
https://github.com/tacowasa059/KatamariIO

The upstream project is distributed under the MIT License. The original `LICENSE` file is retained in this repository. Changes in this fork are intended to preserve the upstream license and attribution. Review the upstream repository's license and notices before redistribution.

## Build

- Minecraft: 1.21.11
- Java: 21
- Loader: Fabric
- Run `./gradlew :fabric:build` (Windows: `gradlew.bat :fabric:build`).

This source package has not been compiled in this environment. If Gradle reports dependency, mapping, or Minecraft API errors, those need to be fixed against the exact 1.21.11 Fabric toolchain.

## Features

- RollBall identity (`rollball`)
- On-screen size meter
- Goal countdown when size reaches 50 blocks
- Win banner and circular firework particle effect
- No custom inventory item is registered
