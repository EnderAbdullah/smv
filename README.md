# Structure Mover (NeoForge 1.21.1, tested target: 21.1.249)

A lightweight, Sable-free way to turn a build into a drivable vehicle.
It uses no native libraries, so it avoids the JNA problem on PojavLauncher.

## Blocks
- **Vehicle Assembler**: right-click it. Every block connected to it (face to face) becomes one vehicle.
  Natural terrain (dirt, stone, sand, leaves, ...) is ignored. Blocks with inventories (chests, furnaces) stay behind.
- **Vehicle Wheel**: a vehicle needs at least 2 wheels to drive. Each wheel carries about 12 blocks of weight.
- **Steering Wheel**: the driver's seat is placed here.

## Driving
Right-click the vehicle to sit. W/S accelerate and reverse, A/D steer. Sneak to get out.
Sneak + right-click the vehicle (while not seated) to turn it back into normal blocks.
The vehicle's "front" is the direction you faced when you pressed the assembler.

## Limits (version 2.0.0)
This is NOT a real physics engine. The vehicle stays level, climbs 1 block steps, and uses a square hitbox
as wide as its longest side. Only the driver can ride. Other players cannot stand on it.

## Build
Push to GitHub, open the Actions tab, run "Build NeoForge Mod", download the JAR artifact.
Create is optional.
