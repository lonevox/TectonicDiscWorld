# Tectonic Disk World — Implementation Notes

## Overview

Tectonic Disk World is a Minecraft 1.20.1 Forge/Fabric mod (using Architectury Loom) that confines Tectonic's world generation to a configurable circular disk. Outside the disk, the world becomes a deep ocean. The mod achieves this by generating a **runtime datapack** at world load that overrides Tectonic's density functions.

---

## Architecture

### Multiplatform (Architectury)

- `common/` — shared logic, including all world gen code
- `forge/` — Forge-specific config (`TectonicDiscWorldForgeConfig.java`)
- `fabric/` — Fabric-specific config (`TectonicDiscWorldFabricConfig.java`)

### Runtime Datapack

`GeneratedPackWriter.java` is the core file. It:
1. Reads Tectonic's original density function JSON from inside the Tectonic JAR (`resourcepacks/tectonic/...`)
2. Wraps those density functions in lerp expressions that blend from the original value (inside the disk) to a target value (outside the disk)
3. Writes the resulting JSON files into a temporary datapack directory that is registered at world load

---

## Density Function Pipeline

Minecraft 1.20.1 world generation uses a pipeline of JSON-defined density functions. Tectonic overrides the vanilla ones. The relevant chain is:

```
raw_continents
    └─► island_selector  (range_choice: 1 if raw_continents ∈ [-1, -0.5), else 0)
    └─► full_continents  (spline on raw_continents → drives biome placement)
            └─► depth_additive  (spline: 0 for raw_continents ≤ 0, positive on land)
                    └─► sloped_cheese / final_density  (terrain shape)
```

### Key Signals

| Signal | Role |
|---|---|
| `raw_continents` | Raw noise driving everything downstream |
| `full_continents` | Processed continentalness; controls biome selection |
| `island_selector` | 1 when `raw_continents ∈ [-1, -0.5)`, else 0 |
| `depth_additive` | Adds terrain height for land areas (0 for ocean) |
| `terrain_spline/factor/islands` | Constant 5.6 — the high terrain factor used when `island_selector=1` |

### Critical Insight: `full_continents` Extrapolation

Tectonic's `full_continents` is a spline with control points only at `raw_continents = 0.05` and `0.175`, both with `derivative=1`. Below 0.05, it extrapolates linearly with slope 1:

```
full_continents ≈ raw_continents  (for all negative values)
```

This means overriding only `raw_continents` automatically keeps `full_continents` in sync — no biome/terrain mismatch.

---

## How the Disk Works

### Computed Density Functions

Three intermediate density functions are written to the datapack:

1. **`distance_to_origin`** — Euclidean distance from the world center (using `moredfs:x`, `moredfs:z`, `moredfs:subtract`, `moredfs:sqrt`)
2. **`band_progress`** — `clamp((distance - unaffected_radius) / falloff_radius, 0, 1)`
   - 0 inside the disk, 1 outside, linearly interpolated across the falloff band
3. **`fast_curve`** — Applied to `band_progress`: `f(t) = 2√t − t`
   - Monotone increasing [0,1]→[0,1], very steep near t=0
   - The transition from full terrain to full ocean happens within ~125 blocks of the falloff start, even for the most continental terrain

### Overridden Tectonic Density Functions

Uses the lerp pattern: `original * (1 - fast_curve) + target * fast_curve`

**`raw_continents`** — target: `-0.499`
- Keeps `raw_continents` just above the `-0.5` threshold, so `island_selector = 0` always
- This prevents island landmasses from forming in the outer ocean (which would occur if `island_selector = 1` activated `full_continents = raw_islands`)
- Because `full_continents ≈ raw_continents` for negative values (spline extrapolates with slope 1), biomes and terrain automatically stay in sync — no separate `full_continents` override is needed

---

## Configuration

Defined in `TectonicDiscWorldConfig.java`:

| Field | Default | Description |
|---|---|---|
| `unaffectedRadius` | 5000 | Radius (blocks) of full Tectonic terrain |
| `falloffRadius` | 2500 | Width of the transition band |
| `offsetX` | 0 | X offset of disk center from world origin |
| `offsetZ` | 0 | Z offset of disk center from world origin |
| `forceEnablePack` | true | Always register the datapack, even without Tectonic |

---

## External Dependencies

- **Tectonic** — provides the density functions being overridden; its JAR is read at runtime via `Class.getResourceAsStream`
- **MoreDensityFunctions** — provides math operations not in vanilla:
  - `moredfs:x`, `moredfs:z` — current block X/Z coordinate
  - `moredfs:sqrt` — square root
  - `moredfs:subtract` — subtraction (`argument1 - argument2`)
  - `moredfs:div` — division (`numerator / denominator`)

---

## Known Behaviors and Trade-offs

- **Transition sharpness** is governed by the fast curve `2√t − t`. At `t ≈ 0.17`, the curve reaches 0.5, meaning even the most continental terrain (`raw_continents ≈ 0.35`) crosses zero within ~5% of the falloff band width (~125 blocks for a 2500-block falloff).
- **No island landmasses** outside the disk: `raw_continents` is clamped to `-0.499`, keeping `island_selector = 0` and preventing Tectonic's island path from activating.
- **Deep ocean outside the disk**: the outer ocean is a uniform deep ocean with no terrain above sea level, since `island_selector = 0` uses the continent path with a small terrain factor and the negative `raw_continents` value zeros out `depth_additive`.
- **`depth_additive`** — a spline that returns 0 for `raw_continents ≤ 0` and positive for land. Once `raw_continents` goes negative (which happens quickly due to the fast curve), this returns 0 and prevents terrain from being pushed above sea level by the continent factor alone.
