# Better Combat Plus

An **unofficial** addon for [Better Combat](https://modrinth.com/mod/better-combat) and
[Combat Roll](https://modrinth.com/mod/combat-roll).
Minecraft **1.20.1** · **Forge** · Java 17 · **GPL-3.0**

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

| Mod | |
|---|---|
| Better Combat | **required** `[1.9.0,1.10)` |
| Cloth Config | **required** `[11,)` |
| Combat Roll | **optional** `[1.3.3,1.4)` — without it only the roll fix is disabled |

Install on **both** client and server. The offhand fix is a common mixin that runs on each side; with one side missing it, the shield looks raised but blocks nothing.

## Configuration

Main menu → **Mods** → Better Combat Plus → **Config**. Forge provides that entry point, so no extra menu mod is needed. All three switches default to on.

- `roll_during_attack` — allow rolling during an attack
- `offhand_while_two_handed` — keep the offhand while wielding a two-handed weapon
- `right_click_in_recovery` — allow right-click during the recovery window

`offhand_while_two_handed` is read from each side's own config, so keep both sides identical.

A shield is recognised by any of: the Forge `SHIELD_BLOCK` capability, extending `ShieldItem`, or carrying the `#forge:tools/shields` tag. Shields added by other mods therefore work without changes.

## Build

```bash
./gradlew build    # build/libs/bettercombatplus-Forge-1.20.1-<ver>.jar
```

Drop the three dependency jars into `libs/` first. They are compile-only and never shipped — see [NOTICE](NOTICE.md).

## Licence

GPL-3.0. Better Combat is All Rights Reserved: this project contains none of its code and only interoperates with it at runtime. Attribution and the exact API surface used are listed in [NOTICE](NOTICE.md).
