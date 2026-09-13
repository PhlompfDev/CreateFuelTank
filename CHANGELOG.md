# Changelog

## 0.2.0 — Create Propulsion support

**Added**
- The Fuel Tank now feeds **Create Propulsion: Simulated** liquid thrusters: the Thruster and the Liquid Vector Thruster, on any side, with the same redstone stop. Propulsion is optional; nothing changes without it.
- Each machine decides for itself what counts as fuel, so thrusters also take fuels Diesel Generators does not know (the ones Propulsion's own fuel list allows).

**Changed**
- Goggle readout now counts "machines" (engines and thrusters) instead of engines.

**Requires**
- As before. Create Propulsion: Simulated 1.1.5+ is optional.

## 0.1.0 — Initial release

**Added**
- **Fuel Tank** block: a Create Fluid Tank that feeds fuel directly into any Create Diesel Generators engine touching it (normal, modular and huge engines), on any side of the engine.
- Only fluids Diesel Generators recognises as fuel are pushed; other fluids stay in the tank.
- Redstone control: powering any block of the tank stops the feed until the signal drops.
- Multiblock support up to 3x3 wide, same as the Fluid Tank, including the place-a-whole-layer shortcut.
- Engineer's Goggles readout showing feed status.
- Recipe: Brass Sheet / Wooden Barrel / Brass Sheet in a column.

**Requires**
- NeoForge 1.21.1 (Java 21), Create 6.0.10+, Create Diesel Generators 1.3.15+.
