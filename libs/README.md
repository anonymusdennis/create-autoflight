# Runtime dependencies (compile-only)

Place these JARs in this folder before building. They are not included in the repository.

| File | Source |
|------|--------|
| `create-1.21.1-6.0.10.jar` | [Create mod](https://www.curseforge.com/minecraft/mc-mods/create) |
| `ponder.jar` | Bundled with Create / Maven |
| `flywheel.jar` | Bundled with Create / Maven |
| `sable-neoforge-1.21.1-2.0.1.jar` | Sable |
| `sable-companion.jar` | Sable |
| `simulated.jar` | Simulated mod |
| `aeronautics-1.3.0.jar` | Create Aeronautics |

```bash
./gradlew build
```

Output: `build/libs/create_autoflight-0.1.0.jar`
