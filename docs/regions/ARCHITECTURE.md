# Titan Region Engine

## Invariants

- SQLite region definitions are the source of truth; the spatial index is derived.
- Every region and world has a stable UUID.
- A `(world, namespace, name)` key is unique.
- Boxes use half-open bounds: minimum inclusive, maximum exclusive.
- Queries may return multiple regions ordered by priority, key, then UUID.
- Published index snapshots are immutable and queries never acquire a write lock.
- Mutations are serialized and publish a complete snapshot atomically.
- Index limits reject pathological geometry before it can exhaust server memory.

## Boundaries

The model and index packages do not depend on Bukkit or Paper. Protection rules,
FAWE selection, commands, and cell rental behavior belong in higher layers.

The v0.1 engine intentionally does not enforce protection. It supplies durable,
deterministic region data for the later protection and cells modules.
