# Titan Protection Flags

"Flag" is the user-facing name for a protected operation. Internally, Titan
uses the typed `ProtectionAction` enum and namespace-owned policies. Geometry
does not store arbitrary flags, owners, members, or inheritance.

An `ActionRuleSet` may explicitly allow or deny an action. An absent rule is
`ABSTAIN`, allowing another region at the same or lower priority, followed by
the world default, to decide.

## Delivery states

- **Core**: represented by `ProtectionAction` and resolvable by policies.
- **Paper**: translated from all relevant Bukkit/Paper events.
- **Mock**: adapter behavior covered by MockBukkit.
- **Server**: edge cases verified on the configured development server.

An action is production-ready only when every required column is complete.

## Current catalog

| User-facing flag | Protection action | Core | Paper | Mock | Server |
| --- | --- | --- | --- | --- | --- |
| block-break | `BLOCK_BREAK` | yes | yes | yes | planned |
| block-place | `BLOCK_PLACE` | yes | yes | yes | planned |
| block-interact | `BLOCK_INTERACT` | yes | yes | yes | planned |
| container-open | `CONTAINER_OPEN` | yes | future | future | future |
| entity-interact | `ENTITY_INTERACT` | yes | future | future | future |
| entity-damage | `ENTITY_DAMAGE` | yes | future | future | future |
| hanging-modify | `HANGING_MODIFY` | yes | future | future | future |
| bucket-fill | `BUCKET_FILL` | yes | future | future | future |
| bucket-empty | `BUCKET_EMPTY` | yes | future | future | future |
| explosion-block-damage | `EXPLOSION_BLOCK_DAMAGE` | yes | future | limited | required |
| piston-move | `PISTON_MOVE` | yes | future | limited | required |
| fluid-flow | `FLUID_FLOW` | yes | future | limited | required |
| fire-spread | `FIRE_SPREAD` | yes | future | limited | required |
| vehicle-modify | `VEHICLE_MODIFY` | yes | future | future | required |

## Candidate future actions

WorldGuard's catalog is used as an event-coverage checklist, not as an API to
copy. Candidates are added only when Titan has explicit semantics and tests.

| Area | Candidate actions |
| --- | --- |
| Combat | PvP, projectile damage, mob damage |
| Blocks | trampling, lighter use, item-frame rotation |
| Vehicles | place, destroy, enter, ride |
| Movement | entry, exit, ender pearl, chorus fruit, teleport |
| Items | pickup, drop |
| Environment | mob spawning, lightning, crop/growth and decay events |

Messages, greetings, teleport destinations, weather, and other non-protection
settings are not protection flags. They belong in separate typed domain
features if Titan needs them later.

## Deliberately unsupported

- Parent inheritance between regions.
- `BUILD` as a magical aggregate flag.
- `PASSTHROUGH` regions.
- A geometric global region.
- Dynamically registered untyped values.
- Undefined conflict selection for non-state values.
