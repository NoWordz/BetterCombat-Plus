# NOTICE

本项目以 GPL-3.0 发布，见 [LICENSE](LICENSE)。

## 上游关系

### Better Combat — All Rights Reserved

Copyright (c) 2022 Zsolt Molnár — https://github.com/ZsoltMolnarrr/BetterCombat

**本项目不包含 Better Combat 的任何源码或资源。** 与它的全部交互限于：

- 调用其公开 API：`net.bettercombat.api.MinecraftClient_BetterCombat`、
  `net.bettercombat.api.WeaponAttributes`、`net.bettercombat.logic.WeaponRegistry`；
- 在其 mixin 目标方法上以更高优先级抢先注入（`Player.getItemBySlot`、`Minecraft.startUseItem`），
  运行期改变行为而不修改、不复制其类。

### Combat Roll — GPL-3.0

Copyright (c) Zsolt Molnár — https://github.com/ZsoltMolnarrr/CombatRoll

本项目通过其公开 API 复用翻滚能力：`RollManager`、`Keybindings`、`RollEffect`、
`Packets.RollPublish`、`EntityAttributes_CombatRoll`。

`com.e33epus.bcp.compat.RollTrigger` 中的翻滚**准入条件**与**位移算法**
（方向合成、`0.475 × (DISTANCE + additional_roll_distance)`、
水面 ×0.5、岩浆 ×0.3、冰面摩擦衰减）参照其实现重新表达，未逐字复制。
按 GPL-3.0 要求，本项目同样以 GPL-3.0 发布。

## 编译期依赖

`libs/` 下的 jar 仅用于编译，不随产物分发：

- `bettercombat-forge-1.9.0+1.20.1.jar`（All Rights Reserved，未分发）
- `combatroll-forge-1.3.3+1.20.1.jar`（GPL-3.0，未分发）
- `cloth-config-11.1.136-forge.jar`（LGPL-3.0，未分发）

## 名称

"Better Combat" 是 Zsolt Molnár 的项目名。本项目的 "Better Combat Plus" 仅用于说明
它是该 mod 的附属，不代表任何官方关系或背书。

## 构建前置 jar 的获取

`libs/` 被 `.gitignore` 排除（Better Combat 是 All Rights Reserved，不能随仓库分发）。
本地构建前放入以下三个文件：

| 文件 | 来源 |
|---|---|
| `bettercombat-forge-1.9.0+1.20.1.jar` | Modrinth `better-combat` 1.9.0+1.20.1 forge |
| `combatroll-forge-1.3.3+1.20.1.jar` | Modrinth `combat-roll` 1.3.3+1.20.1 forge |
| `cloth-config-11.1.136-forge.jar` | Modrinth `cloth-config` 11.1.136 forge |
