# Changelog

## v0.1.0

**新增**

- 攻击动作期间可以翻滚，翻滚会作废正在起手的那一刀但照旧消耗一次武器冷却。
- 双手武器（镰刀、大剑、重斧、长枪等）时副手物品正常渲染与使用，盾牌不再消失。
- 攻击后摇期可以右键举盾，蓄力期仍让位给攻击。
- Cloth Config 配置界面，三个开关默认全开。

**修复**

- 翻滚注入抢在 Combat Roll 之前（priority 400），否则 CR 取消动画却不重置攻击计时器，那一刀仍会砍出。
- 后摇举盾加守卫：界面、资源重载遮罩、暂停、挖方块、任一只手在使用物品时都不派发。
- BC 未实现 `MinecraftClient_BetterCombat` 时不再强转崩溃。
- 补 `en_us` / `zh_cn` 语言文件，配置界面不再显示裸键名。
- 补 `pack.mcmeta`，修掉资源包不加载导致的配置界面裸键名与 `ResourcePackInfo` 报错。
- 后摇举盾的盾牌判定扩为三条并集：`ShieldItem`、`#forge:tools/shields`、`SHIELD_BLOCK` 能力，模组盾也能举。
- 盾牌破防冷却期间不再重复派发；举盾不再挥手，且补上了举起的装备动画；派发限速到每 4 tick 一次。
- jar 内打包 LICENSE 与 NOTICE。

**说明**

- Better Combat 硬依赖 `[1.9.0,1.10)`，Cloth Config 硬依赖 `[11,)`，Combat Roll 软依赖 `[1.3.3,1.4)`（未安装时仅关闭翻滚接管）。
- 两把单手武器的原生双持轮流攻击行为未作改动。
- 配置界面从主菜单 Mods 进入，不需要入口 mod。

**已知边界**

- 双手主手 + 副手武器时副手会显示但不参与攻击，BC 的双持判定要求两手都非双手，本次刻意不动。
- 攻击动画时长仍与武器冷却绑定，长冷却武器的前后摇偏长，不在本次范围。
- 后摇前段只派发副手，主手拿末影珍珠/雪球/喷溅药水时这些物品用不出来（那段窗口原版右键本来就不执行）。
- `libs/` 为编译期前置，不进仓库也不进产物（BC 为 All Rights Reserved）。

----

**Added**

- Roll during an attack; the windup in progress is voided but still costs one weapon cooldown.
- Offhand items render and work while wielding a two-handed weapon, shields no longer disappear.
- Raise a shield during the attack recovery window; the windup phase still takes priority.
- Cloth Config screen with three switches, all enabled by default.

**Fixed**

- Roll injection now pre-empts Combat Roll (priority 400); otherwise CR cancels the animation without resetting the attack ticker and the hit still lands.
- Guarded recovery shield dispatch against screens, resource-reload overlay, pause, block breaking, and any hand already using an item.
- No more cast crash when Better Combat skips its mixins and `MinecraftClient_BetterCombat` is absent.
- Added `en_us` / `zh_cn` language files; the config screen no longer shows raw keys.
- Added `pack.mcmeta`; the resource pack now loads, fixing both the raw config keys and the `ResourcePackInfo` error.
- Shield detection widened to `ShieldItem`, `#forge:tools/shields`, or the `SHIELD_BLOCK` capability, so modded shields work too.
- No repeat dispatch while a shield is on its disable cooldown; raising a shield no longer swings the arm, now plays the equip animation, and dispatch is throttled to once per 4 ticks.
- LICENSE and NOTICE are now packaged into the jar.

**Notes**

- Better Combat required `[1.9.0,1.10)`, Cloth Config required `[11,)`, Combat Roll optional `[1.3.3,1.4)` (rolling takeover is skipped when absent).
- Native dual-wielding between two one-handed weapons is untouched.
- The config screen is reachable from the main menu Mods list; no entry-point mod needed.

**Known limits**

- With a two-handed main hand plus an offhand weapon, the offhand renders but does not attack; Better Combat's dual-wield predicate requires both hands non-two-handed and this release leaves it alone.
- Attack animation length is still tied to weapon cooldown, so slow weapons have long windup/recovery; out of scope here.
- Early in the recovery window only the offhand is dispatched, so an ender pearl / snowball / splash potion in the main hand cannot be used there (vanilla right-click did not run in that window either).
- `libs/` holds compile-time dependencies only and is neither committed nor shipped (Better Combat is All Rights Reserved).
