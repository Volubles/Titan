# TitanMC

TitanMC is a custom Paper plugin built for a prison server. It collects the
server's mines, custom tools, menus, and region protection in one codebase.

The project is under active development. The region engine and protection core
are usable, while cells and the remaining Minecraft event coverage are still
being built.

## Current features

- Configurable mines with weighted block palettes and scheduled resets
- WorldEdit/FAWE selections for mine administration
- Custom donor pickaxes and tools
- A multi-world region engine backed by SQLite
- Overlapping regions with deterministic priorities
- Typed protection rules with fail-closed decisions
- Paper adapters for block breaking, placing, and interaction
- JUnit and MockBukkit test coverage

## Protection configuration

Protection is enabled by default, but no world is protected until it is listed:

```yaml
protection:
  enabled: true
  bypass-permission: titanmc.protection.bypass
  protected-worlds:
    - world
```

Listed worlds use default deny. Concrete region policies, such as cells and
mines, selectively allow actions inside their regions. Leave the list empty
while setting up a server that should remain open.
