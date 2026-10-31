# Better Combat Plus

An **unofficial** addon for [Better Combat](https://modrinth.com/mod/better-combat) and
[Combat Roll](https://modrinth.com/mod/combat-roll).
**GPL-3.0** · Minecraft **1.20.1 Forge** and **1.21.1 NeoForge**

Not affiliated with, or endorsed by, the authors of Better Combat or Combat Roll.

## What it fixes

| Symptom | Cause |
|---|---|
| Rolling does nothing during a Better Combat attack — you have to wait for the animation to finish | Combat Roll refuses to roll while the weapon is on cooldown or the attack key is held, and Better Combat holds that key for the whole swing |
| Two-handed weapons hide the offhand: no shield model, no offhand item | Better Combat returns an empty stack for the offhand while a `two_handed` weapon is equipped |
| You cannot raise a shield during the attack recovery | The same offhand suppression, plus Better Combat cancelling right-click until the cooldown is nearly over |

## Behaviour

- Rolling interrupts the swing in progress. That hit is cancelled but still costs one weapon cooldown, so rolling is not a free attack.
- The offhand is restored for the two-handed case only. Native dual-wielding between two one-handed weapons is untouched.
- Right-click is allowed in the **recovery** window only; the windup still belongs to the attack.

## Requirements

| | 1.20.1 Forge | 1.21.1 NeoForge |
|---|---|---|
| Better Combat | **required** `[1.9.0,1.10)` | **required** `[2.4.0,2.5)` |
| Cloth Config | **required** `[11,)` | **required** `[15,)` |
| Combat Roll | **optional** `[1.3.3,1.4)` | **optional** `[2.0.6,2.1)` |
| Java | 17 | 21 |

Install on **both** client and server. The offhand fix is a common mixin that runs on each side; with one side missing it, the shield looks raised but blocks nothing.

## Configuration

Main menu → **Mods** → Better Combat Plus → **Config**. The loader provides that entry point, so no extra menu mod is needed. All three switches default to on.

- `roll_during_attack` — allow rolling during an attack
- `offhand_while_two_handed` — keep the offhand while wielding a two-handed weapon
- `right_click_in_recovery` — allow right-click during the recovery window

`offhand_while_two_handed` is read from each side's own config, so keep both sides identical.

A shield is recognised by any of: the `SHIELD_BLOCK` item ability (what vanilla `isBlocking()` checks), extending `ShieldItem`, or carrying the loader's shield tag (`#forge:tools/shields` on 1.20.1, `#c:tools/shield` on 1.21.1). Shields added by other mods therefore work without changes.

## Build

Each platform is an independent build with its own Gradle wrapper, because Java and Gradle versions are hard constraints.

```bash
cd platforms/1.20.1-forge   && ./gradlew build    # Java 17
cd platforms/1.21.1-neoforge && ./gradlew build   # Java 21
```

Drop the three dependency jars into that platform's `libs/` first. They are compile-only and never shipped — see [NOTICE](NOTICE.md). Layout details in [docs/repo-layout.md](docs/repo-layout.md).

## Licence

GPL-3.0. Better Combat is All Rights Reserved: this project contains none of its code and only interoperates with it at runtime. Attribution and the exact API surface used are listed in [NOTICE](NOTICE.md).
