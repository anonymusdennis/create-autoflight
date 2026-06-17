# Create AutoFlight — Reference Materials

Acquired **2026-06-17** for offline AI sessions and local development. No mod source code was changed as part of this bundle.

Repository: [anonymusdennis/create-autoflight](https://github.com/anonymusdennis/create-autoflight)  
Local clone: `/home/headadmin/Documents/projects/mods/create autoflight`  
Pulled `main` at commit `954a553` (merge of PR #1, `copilot/analyze-codebase`).

Total bundle size: ~282 MB (mostly decompiled sources + NeoForge merged JAR).

---

## Directory layout

| Path | Contents |
|------|----------|
| `jars/` | Seven compile-only dependency JARs (exact filenames required by `build.gradle`) |
| `decompiled/` | Vineflower 1.10.1 decompilation of each JAR (~4,348 `.java` files) |
| `gradle-artifacts/` | Cached Gradle plugins, NeoForge 21.1.228 artifacts, MDG build outputs, Maven metadata |
| `tooling/` | `vineflower-1.10.1.jar` (decompiler used to produce `decompiled/`) |
| `plans/` | Cursor session plans copied from `~/.cursor/plans/` (partial roadmap) |

To use the JARs for building, copy (or symlink) everything from `reference/jars/` into the project `libs/` folder.

---

## 1. Dependency JARs (`jars/`)

These match `build.gradle` `compileOnly files(...)` entries. They are **not** in git (see root `.gitignore`).

| Filename | Size | SHA-256 | Origin |
|----------|------|---------|--------|
| `create-1.21.1-6.0.10.jar` | 19 MB | `ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a` | [Create on CurseForge](https://www.curseforge.com/minecraft/mc-mods/create) (MC 1.21.1, Create 6.0.10) |
| `ponder.jar` | 801 KB | `0cf4611ad853042b689ac386184c5bbe02950efcffddb49e5f604e82baddb0dc` | Bundled with Create / Create Maven |
| `flywheel.jar` | 854 KB | `31dda15c205eb596d3b3449ef03f6af7363a6cd35b3da4bfe916b304f9e5337e` | Bundled with Create / Create Maven |
| `sable-neoforge-1.21.1-2.0.1.jar` | 13 MB | `f0513d490dc099a7271b5e29b5040a1ff556219dee1007df3fe29355fd7ff68d` | [Sable Maven](https://maven.ryanhcode.dev/releases/dev/ryanhcode/sable/sable-neoforge-1.21.1/) (also copied from local `libs/`) |
| `sable-companion.jar` | 35 KB | `873633e35046e3761b277ff8a1ecad0d55d9a3014fa81a0b084c9aecba1f3bed` | Sable companion / local modpack |
| `simulated.jar` | 6.1 MB | `9cbfdaf421b450727232d7a2de5f9e2ab826ac2113a25fb1286ba431a1a8a403` | Simulated mod (Create ecosystem) |
| `aeronautics-1.3.0.jar` | 25 MB | `2059443a3f601e167e6090af0a44ae8b18ca67e46f3a234a4c231f0d46c3979c` | Create Aeronautics 1.3.0 |

**Also present locally (not referenced by `build.gradle`):** `aeronautics-bundled.jar`, `create-aeronautics-bundled-*.jar` in `libs/` and Prism instance mods folder.

**Prism test instance mods folder:**  
`~/.local/share/PrismLauncher/instances/robin automopdpac/.minecraft/mods/`

---

## 2. Decompiled sources (`decompiled/`)

| Source JAR | Output directory | Approx. size |
|------------|------------------|--------------|
| `create-1.21.1-6.0.10.jar` | `decompiled/create-1.21.1-6.0.10/` | 72 MB |
| `ponder.jar` | `decompiled/ponder/` | 2.4 MB |
| `flywheel.jar` | `decompiled/flywheel/` | 2.8 MB |
| `sable-neoforge-1.21.1-2.0.1.jar` | `decompiled/sable-neoforge-1.21.1-2.0.1/` | 18 MB |
| `sable-companion.jar` | `decompiled/sable-companion/` | 144 KB |
| `simulated.jar` | `decompiled/simulated/` | 16 MB |
| `aeronautics-1.3.0.jar` | `decompiled/aeronautics-1.3.0/` | 29 MB |

**Tool:** [Vineflower 1.10.1](https://github.com/Vineflower/vineflower/releases/tag/1.10.1)  
**Command used:**

```bash
java -jar reference/tooling/vineflower-1.10.1.jar -dgs=1 reference/jars/<jar> reference/decompiled/<name>/
```

Decompiled output is for **reference only** — names may differ from original sources; prefer official sources/repos when available.

---

## 3. Gradle / NeoForge / toolchain (`gradle-artifacts/`)

Resolved online on 2026-06-17 via `./gradlew --refresh-dependencies dependencies` and `./gradlew createMinecraftArtifacts`.

### Plugins

| Artifact | Version | Cached copy | Upstream |
|----------|---------|-------------|----------|
| `net.neoforged.moddev` (moddev-gradle) | **2.0.141** | `gradle-artifacts/plugins/moddev-gradle-2.0.141.jar` | [NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/moddev-gradle/) |
| `org.gradle.toolchains.foojay-resolver-convention` | **0.8.0** | `gradle-artifacts/plugins/foojay-resolver-0.8.0.jar` + POMs in `metadata/` | [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention) |

Declared in project:

- `build.gradle` → `id 'net.neoforged.moddev' version '2.0.141'`
- `settings.gradle` → `id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'`
- `gradle.properties` → `neoforge_version=21.1.228`, `java_version=21`

### NeoForge 21.1.228

| File | Location |
|------|----------|
| `neoforge-21.1.228-userdev.jar` | `gradle-artifacts/neoforge/` |
| `neoforge-21.1.228-universal.jar` | `gradle-artifacts/neoforge/` |
| `neoforge-21.1.228-sources.jar` | `gradle-artifacts/neoforge/` |
| `neoforge-21.1.228.jar` (merged) | `gradle-artifacts/moddev-build/` |
| `neoforge-21.1.228-merged.jar` | `gradle-artifacts/moddev-build/` |
| `neoforge-21.1.228-client-extra-aka-minecraft-resources.jar` | `gradle-artifacts/moddev-build/` |
| `neoforge-21.1.228-sources.jar` (MDG copy) | `gradle-artifacts/moddev-build/` |

Upstream: [https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.228/](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.228/)

### Sable Maven (`maven.ryanhcode.dev`)

Repository URL in `build.gradle`: `https://maven.ryanhcode.dev/releases` (group `dev.ryanhcode.sable`).

This project uses **local JARs** for Sable, not a Gradle Maven dependency. Metadata fetched for reference:

- `gradle-artifacts/metadata/sable-neoforge-1.21.1-maven-metadata.xml` — latest published: **2.0.3** (project pins **2.0.1** in `libs/`).

Index: [https://maven.ryanhcode.dev/releases/dev/ryanhcode/sable/](https://maven.ryanhcode.dev/releases/dev/ryanhcode/sable/)

### JDK 21 toolchain

| Property | Value |
|----------|-------|
| Version | OpenJDK **21.0.2** (2024-01-16) |
| Path | `/home/headadmin/.sdkman/candidates/java/21.0.2-open` |
| `JAVA_HOME` for builds | `$HOME/.sdkman/candidates/java/current` → same 21.0.2 |

Gradle resolves this via the foojay toolchain plugin when online. For fully offline builds, set `JAVA_HOME` to the path above.

### Offline Gradle note

The copies in `gradle-artifacts/` are a **snapshot** of resolved artifacts. A sandbox with no network still needs either:

- A populated `~/.gradle/caches` (copy from this machine), or
- A custom `init.gradle` / local Maven repo wiring — not set up in this repo.

---

## 4. Roadmap & Phase E backlog

### What is in git (Copilot “Phase E”)

Only items **18** and **19** appear in commit messages on `copilot/analyze-codebase` (merged to `main` as PR #1):

| Item | Commit | Summary |
|------|--------|---------|
| **18** | `0ed06ca` | Body-relative attitude kinematics in `GyroscopeBlockEntity.measure()` |
| **19** | `dfd7f22` | Feed nav attitude to gyro in **all** nav modes (not only helicopter) |

**Phase E item 20 and beyond are not in the repository, PR body, or recoverable session plans.** The maintainer must supply the next item.

### Recovered plans (`plans/`)

| File | Pending work |
|------|----------------|
| `navigation_block_eb67c483.plan.md` | **`phase3-approach`** — approach-mode 1-block hitbox shrink + speed/arrival polish |
| `create_autoflight_addon_d0e53d3c.plan.md` | **`simple-scanners`** (later); gyro/thruster scaffold marked done |
| `thrust_vectoring_system_1fb3128a.plan.md` | All items **completed** |

Suggested next engineering tasks (inferred, not official Phase E numbering):

1. Complete navigation **phase3-approach** (hitbox shrink + SpeedControl polish).
2. Playtest item **19** gyro attitude-following in waypoint/cruise/hold vs helicopter modes.
3. Decide whether non-heli modes need **max pitch limits** (see §8 below).

---

## 5. PR status (`copilot/analyze-codebase`)

- **PR #1** — *“Exploring and analyzing broken features in navigator and thruster”* — **merged** into `main` (`954a553`).
- Branch `copilot/analyze-codebase` still exists at `dfd7f22` (same tip as item 19).
- **No open PR** at time of acquisition. Further work can go directly on `main` or a new branch.

---

## 6. In-game test results (item 19) — **not available**

The sandbox cannot run Minecraft. **No playtest feedback** was recorded for item 19 (gyro following `desiredOrientation()` in all nav modes).

**Maintainer action needed:** Test in Prism instance `robin automopdpac` with latest `create_autoflight-0.1.0.jar` and report:

- Waypoint / cruise / hold / approach behavior vs helicopter mode
- Unwanted pitch/roll in non-heli modes
- Regressions from earlier gyro or thruster fixes

---

## 7. Persisted roadmap recommendation

The full Phase E numbered backlog was **never committed**. To avoid losing it again:

- Commit `reference/plans/*.plan.md` and/or a root `ROADMAP.md` with explicit phase/item checklist.
- Or keep using Cursor plans and periodically export to `reference/plans/`.

---

## 8. Gyro semantics after item 19 (open design question)

**What changed (item 19):** `AssemblyFlightController.buildCommand()` now always publishes `GyroTargetAngles` derived from the flight `desiredOrientation` quaternion, so gyros receive real targets in every nav mode instead of zero/neutral commands.

**Mode-specific behavior still in code:**

- **Helicopter mode:** Uses `helicopterTargetAngles` / `desiredHelicopterOrientation` with `helicopterMaxPitchDeg` and pitch scaling near arrival.
- **Non-helicopter modes:** `desiredOrientationToward()` (or hold snapshot / docked orientation) — **no separate max-pitch clamp** beyond what that function implies.
- **Hold / redstone hold:** May lock to `holdSnapshot.orientation()` instead of toward-target attitude.

**Open question for maintainer:** Should waypoint/cruise modes follow `desiredOrientation()` **identically** to helicopter mode, or should non-heli modes keep **stricter attitude limits** (e.g. capped pitch)? That determines whether item 19 is “done” or needs a follow-up.

---

## 9. Build verification

With network + `libs/` populated, this environment successfully ran:

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
./gradlew --refresh-dependencies dependencies
./gradlew createMinecraftArtifacts   # UP-TO-DATE
```

Output mod JAR (when built): `build/libs/create_autoflight-0.1.0.jar`

---

## 10. Licenses (summary)

| Component | License (typical) |
|-----------|-------------------|
| Create AutoFlight | MIT (`gradle.properties`) |
| Create | Custom / mod license — see Create distribution |
| Sable | See Sable project / Maven |
| Simulated, Aeronautics | Respective mod licenses |
| NeoForge / MDG | LGPL / project terms |
| Vineflower | Apache 2.0 |

Do not redistribute decompiled third-party sources as if they were official source releases.
