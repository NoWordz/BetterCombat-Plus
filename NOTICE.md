# NOTICE

Copyright (c) 2026 E33EPUS & NoWordz.

本项目以 GPL-3.0 发布，见 [LICENSE](LICENSE)。

## 上游关系

### Better Combat — All Rights Reserved

Copyright (c) 2022 Zsolt Molnár — https://github.com/ZsoltMolnarrr/BetterCombat

**本项目不包含 Better Combat 的任何源码或资源。** 与它的全部交互限于：

- 调用其公开 API：`net.bettercombat.api.MinecraftClient_BetterCombat`、
  `net.bettercombat.api.WeaponAttributes`、`net.bettercombat.logic.WeaponRegistry`；
- 在其 mixin 目标方法上以更高优先级抢先注入（`Player.getItemBySlot`、`Minecraft.handleKeybinds`、
  `Minecraft.tick`），运行期改变行为而不修改、不复制其类。

### Combat Roll — GPL-3.0

Copyright (c) Zsolt Molnár — https://github.com/ZsoltMolnarrr/CombatRoll

本项目通过其公开 API 复用翻滚能力：`RollManager`、`Keybindings`、`RollEffect`、
`Packets.RollPublish`、`CombatRoll.Attributes`（1.20.1 上为 `EntityAttributes_CombatRoll`）。
1.21.1 端另需 `Platform.networkC2S_Send` 作为 C2S 发包入口。

`com.e33epus.bcp.compat.RollTrigger` 中的翻滚**准入条件**与**位移算法**
（方向合成、`0.475 × (DISTANCE + additional_roll_distance)`、
水面 ×0.5、岩浆 ×0.3、冰面摩擦衰减）参照其实现重新表达，未逐字复制。
按 GPL-3.0 要求，本项目同样以 GPL-3.0 发布。

## 编译期依赖

各平台 `libs/` 下的 jar 仅用于编译，不随产物分发：

| 文件 | 平台 | 许可 |
|---|---|---|
| `bettercombat-forge-1.9.0+1.20.1.jar` | 1.20.1 | All Rights Reserved，未分发 |
| `combatroll-forge-1.3.3+1.20.1.jar` | 1.20.1 | GPL-3.0，未分发 |
| `cloth-config-11.1.136-forge.jar` | 1.20.1 | LGPL-3.0，未分发 |
| `bettercombat-neoforge-2.4.0+1.21.1.jar` | 1.21.1 | All Rights Reserved，未分发 |
| `combat_roll-neoforge-2.0.6+1.21.1.jar` | 1.21.1 | GPL-3.0，未分发 |
| `cloth-config-15.0.140-neoforge.jar` | 1.21.1 | LGPL-3.0，未分发 |

## 名称

"Better Combat" 是 Zsolt Molnár 的项目名。本项目的 "Better Combat Plus" 仅用于说明
它是该 mod 的附属，不代表任何官方关系或背书。

## 构建前置 jar 的获取

`libs/` 被 `.gitignore` 排除（Better Combat 是 All Rights Reserved，不能随仓库分发）。
本地构建前把对应平台的 jar 放进 `platforms/<目标>/libs/`：

| 平台 | 文件 | Modrinth 项目 / 版本 |
|---|---|---|
| 1.20.1 | `bettercombat-forge-1.9.0+1.20.1.jar` | `better-combat` 1.9.0+1.20.1 (forge) |
| 1.20.1 | `combatroll-forge-1.3.3+1.20.1.jar` | `combat-roll` 1.3.3+1.20.1 (forge) |
| 1.20.1 | `cloth-config-11.1.136-forge.jar` | `cloth-config` 11.1.136 (forge) |
| 1.21.1 | `bettercombat-neoforge-2.4.0+1.21.1.jar` | `better-combat` 2.4.0+1.21.1 (neoforge) |
| 1.21.1 | `combat_roll-neoforge-2.0.6+1.21.1.jar` | `combat-roll` 2.0.6+1.21.1 (neoforge) |
| 1.21.1 | `cloth-config-15.0.140-neoforge.jar` | `cloth-config` 15.0.140 (neoforge) |
