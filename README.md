# Better Combat Plus

**非官方** [Better Combat](https://modrinth.com/mod/better-combat) / [Combat Roll](https://modrinth.com/mod/combat-roll) 附属，修三处手感缺陷。
Minecraft **1.20.1** · **Forge** · Java 17 · **GPL-3.0**

> 本项目与 Better Combat、Combat Roll 的作者无关，未获其背书。
> Better Combat 为 All Rights Reserved，本项目**不含**其任何代码，仅做运行时互操作。

## 修了什么

| # | 症状 | 根因 |
|---|---|---|
| 1 | Better Combat 攻击动作期间翻滚无效，必须等动作结束 | Combat Roll 的准入闸门「武器冷却未满就 return」+「按住攻击键就 return」，而 BC 默认长按攻击；且 BC 起手时把原版右键延迟写成整段武器冷却 |
| 2 | 双手武器（镰刀、大剑、重斧…）时副手物品不渲染，看不到盾牌 | BC 的 `getEquippedStack(OFFHAND)` 在主手或副手带 `two_handed` 时返回 `ItemStack.EMPTY` |
| 3 | 攻击动作期间右键举不起盾 | 同 2 的屏蔽（盾牌根本没被拿到）+ BC 在 `startUseItem` 按冷却进度直接取消 |

**行为约定**

- 翻滚硬打断正在起手的攻击：那一刀作废，但**照样吃掉一次武器冷却**，不能靠翻滚白嫖出手。
- 只放开「双手主手 + 副手」这一段；两把单手武器的双持轮流攻击**一行不动**。
- 举盾只在**后摇**放行，蓄力期仍让位给攻击，出刀手感不变。

## 依赖

| mod | 关系 |
|---|---|
| Better Combat | **必需** `[1.9.0,1.10)` |
| Cloth Config | **必需** `[11,)`（提供配置界面） |
| Combat Roll | **可选** `[1.3.3,1.4)`；不装则不启用「攻击中翻滚」，其余两项照常 |

**双端都要装**。副手放行是 common mixin，客户端和服务端各跑一半；只装单端会出现「看着能格挡、实际不挡」。

## 配置

游戏内 **主菜单 → Mods → 选中 Better Combat Plus → Config**，三个开关默认全开。
Forge 自己在主菜单留了 Mods 入口并托管右上角的 Config 按钮，**不需要装 Mod Menu 之类的入口 mod**：

- `roll_during_attack` — 攻击动作中允许翻滚
- `offhand_while_two_handed` — 双手武器时保留副手
- `right_click_in_recovery` — 后摇期允许右键举盾

副手「算不算盾」认三种信号，任一成立即可：继承 `ShieldItem`、带 `#forge:tools/shields` 标签、
或声明了 Forge 的 `SHIELD_BLOCK` 能力（`canPerformAction`，`LivingEntity.isBlocking()` 用的就是这条）。
模组自己加的盾牌只要挂了标签或实现了这个能力，不用改本 mod 就能举。
（1.20.1 原版没有 `minecraft:shields` 物品标签，标签侧只有 `#forge:tools/shields` 这一条通用路径。）

`offhand_while_two_handed` 是**双端各自读本地配置**，两端必须一致：
服务端关而客户端开时，客户端会把盾举起来、服务端不认，表现为"看着在格挡、实际没挡"。

## 构建

```bash
./gradlew build          # 产物 build/libs/bettercombatplus-Forge-1.20.1-<ver>.jar
```

`libs/` 下的三个前置 jar 只用于编译，不会打进产物。

## 许可证

GPL-3.0。对 Combat Roll（GPL-3.0）的复用与出处见 [NOTICE](NOTICE.md)。
Better Combat 为 All Rights Reserved，本项目仅通过其公开 API 与 mixin 做运行时互操作。
