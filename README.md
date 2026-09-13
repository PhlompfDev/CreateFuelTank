# Create: Vehicle Surplus

<p align="center"><img src="thumbnail.png" width="256" alt="Fuel Tank"></p>

A tiny [Create](https://modrinth.com/mod/create) addon for NeoForge 1.21.1 with vehicle parts:
the **Fuel Tank**, the **Long Fuel Tank** and the **Differential**.

Example: a Create Aeronautics vehicle with a Diesel Engine driving the propellers. Put a Fuel Tank beside the engine and wire a lever to the tank as an engine cutoff. That replaces the mechanical pump, pipe run and hand crank needed before.

- **Automatic feeding**: the tank pushes its fluid into any Diesel Engine (normal, modular or
  huge) adjacent to any block of the multiblock, through the engine's own fluid handler, so
  the engine's port orientation does not matter.
- **Create Propulsion: Simulated** (optional): the Thruster and Liquid Vector Thruster are fed
  the same way.
- **Fuel only**: each machine decides what it burns. Only fluids Diesel Generators recognises
  as a fuel go into engines, and only fluids on Propulsion's fuel list go into thrusters. Water
  in a Fuel Tank stays in the Fuel Tank.
- **Redstone stop**: power any block of the tank and it stops feeding. Fluid stays inside.
- **Goggles**: engineer's goggles show how many machines are being fed, or why not.
- **Long Fuel Tank**: the same tank lying down. A 1x1 tube along X or Z that chains end to end
  up to 8 blocks, for thin airframes with the thruster at the end of the row. Craft it from a
  Fuel Tank alone in the grid (and back).
- **Differential**: a Gearbox whose outputs all turn the input's way. Drive it from the front
  and the wheel mounts on both sides of the axle turn forward together, instead of mirrored
  like a Gearbox. Four shafts, one free axis (sneak-place against a wall for vertical), no
  stress impact. Recipe: Cogwheel / Shaft, Brass Casing, Shaft / Cogwheel.

## Recipe

<img width="376" height="193" alt="image" src="https://github.com/user-attachments/assets/da4acd0b-738d-4d4d-b41c-a2f814aaec16" />


## Requirements

| Mod | Version |
|---|---|
| NeoForge | 21.1.219+ (built against 21.1.248) |
| Create | 6.0.10 – 6.0.x |
| Create Diesel Generators | 1.21.1-1.3.15+ |
| Create Propulsion: Simulated | 1.1.5+ (optional) |

Create Aeronautics / Simulated are not required; the tank works in the regular world too.
