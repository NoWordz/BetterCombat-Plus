# Better Combat Plus — 架构

非官方附属，修 **Better Combat**（下称 BC）与 **Combat Roll**（下称 CR）组合下的三处手感缺陷。
Forge 1.20.1 / Java 17。

## 一、为什么是附属而不是 fork

| 上游 | 实际许可 | 我们能做什么 |
|---|---|---|
| Better Combat | **All Rights Reserved**（LICENSE 正文与 Modrinth 项目页一致） | 只能运行时互操作：读它的公开 API、在它之外抢先注入。**不能**复制代码、不能分发改过的 jar |
| Combat Roll | GPL-3.0 | 可复用，但衍生作品必须同为 GPL-3.0 |

因此本 mod 的整体策略是：**一行上游代码都不搬**。

- 对 BC：只调用 `MinecraftClient_BetterCombat`、`WeaponRegistry`、`WeaponAttributes` 这几个公开类型；
  需要改变 BC 行为的地方，用**比 BC 更高优先级的 mixin 抢先返回**，让 BC 自己的注入没有机会执行。
- 对 CR：调用其公开 API（`RollManager`、`Keybindings`、`EntityAttributes_CombatRoll`、`Packets`、`RollEffect`）。
  唯一「重写」的是翻滚触发条件的**准入判断**，距离/方向/衰减算法照其实现重新表达；
  本 mod 同为 GPL-3.0，来源在 `NOTICE` 标注。

## 二、三个缺陷的根因与修法

### 缺陷 1：攻击动作期间无法翻滚

**根因**：CR 的 `tryRolling()`（`MinecraftClientMixin`）里有两道闸门，都在「攻击中」必然为真：

```java
if (!config.allow_rolling_while_weapon_cooldown && player.getAttackCooldownProgress(0) < 0.95) return;
...
if (BetterCombatHelper.isDoingUpswing()) { cancelUpswing(); }
else { if (client.options.attackKey.isPressed()) return; }   // BC 默认「按住攻击」→ 恒为真
if (itemUseCooldown > 0) return;                              // BC 起手时把它设成整段冷却
```

`itemUseCooldown` 被 BC 写成整段武器冷却，所以即使把配置开关打开也仍被挡。而且即便放行，
BC 的 `cancelWeaponSwing()` 不清 `upswingTicks`，下一 tick 归零时 `performAttack()` 照旧出手。

**修法**：`MinecraftRollMixin` 以 priority 400（< CR 的 449）在 `handleKeybinds` 的 TAIL **抢在 CR 前面**接管触发：

- 必须抢在它前面，而不是"跟它走"。CR 自己那条路径会 `cancelUpswing()` 停掉动画，
  却**不重置攻击计时器**——几 tick 后 BC 的 `performAttack()` 照样砍出来，
  玩家看到"滚了一下但伤害莫名出现"。由我们统一触发，每次翻滚都能走完
  `voidPendingAttack()`，那一刀干净作废。
- 不会双滚：我们滚成功后 `isRollAvailable` 立刻为假，CR 随后自然退出。

### 缺陷 2a：双手武器时副手物品不渲染

**根因**：BC 的 `PlayerEntityMixin.getEquippedStack_Pre` 在 HEAD 注入，主手或副手任一带
`two_handed` 时把 `getEquippedStack(OFFHAND)` 换成 `ItemStack.EMPTY`。这是 common mixin，
**双端同时生效**，所以盾牌既画不出、也举不起（`getOffhandItem()` → `getItemBySlot(OFFHAND)`）。

**修法**：`PlayerOffhandMixin` 以 priority 900（< BC 的 1000）在同一个 HEAD 抢先返回真实副手物品。
Mixin 官方对 `@Mixin.priority` 的定义是「lower priority mixins being applied first」，
配合 inject-at-HEAD 的 first-come-first-served，数值更小者先执行。

> 注意：判定「BC 会不会封锁」必须直读 `Inventory`，不能用 `getOffhandItem()` ——
> 后者的内部实现就是 `getItemBySlot(OFFHAND)`，而我们的代码就在 `getItemBySlot` 里，会无限递归。

### 缺陷 2b：攻击后摇中举不起盾

**根因是两层**：

1. BC 起手时把原版私有的 `rightClickDelay` 写成整段武器冷却，而原版 `handleKeybinds` 只在
   它为 0 时才调 `startUseItem()` → 后摇前段右键路径根本不会被执行到；
   （后摇尾段 `rightClickDelay` 会先归零而攻击进度仍 < 1，那几 tick 原版路径是通的。）
2. 即使走到，BC 又在 `startUseItem` 的 HEAD 按「起手未结束 / 冷却进度不足」直接取消。

**修法**：`MinecraftRecoveryUseMixin` 在 `tick` 的 TAIL 自行判断并派发：
起手已结束（`upswingTicks == 0`）、冷却未满（`getAttackStrengthScale(0) < 1`）、
副手握着盾、界面/遮罩未打开、未暂停、不在挖方块、未划船、任意一只手不在使用物品 ——
满足则调 `gameMode.useItem(player, OFF_HAND)`。
该方法是公开的，自己会发 `ServerboundUseItemPacket` 并在本地执行 `ItemStack.use`
（已用字节码核实）。注意 `LivingEntity.isBlocking()` 还要求使用已持续 5 tick，
所以是按住几 tick 才真正格挡，不是按下即格挡。蓄力期不介入，攻击手感不变。

「是不是盾」认三种信号，任一成立即可，覆盖原版盾与模组盾：

| 判据 | 覆盖范围 | 核实依据 |
|---|---|---|
| `canPerformAction(ToolActions.SHIELD_BLOCK)` | 能真正格挡的物品，功能判据 | `LivingEntity.isBlocking()` 用的正是这条；`IForgeItem.canPerformAction` 默认 `false`，`ShieldItem` 覆盖为 `DEFAULT_SHIELD_ACTIONS.contains(...)` |
| `instanceof ShieldItem` | 原版盾，及覆写了能力判据的子类 | `ShieldItem` 类层级 |
| `is(Tags.Items.TOOLS_SHIELDS)` | 挂了 `#forge:tools/shields` 的模组盾 | forge universal jar 内 `data/forge/tags/items/tools/shields.json`（值含 `minecraft:shield`）；`Tags.Items.tag()` 内部固定 `forge` 命名空间 |

> 标签这条严格说比能力判据弱（挂标签但没实现能力的物品，服务端 `isBlocking()` 也不认），
> 保留是因为「有盾牌标签就该能举」是对外承诺的语义 —— 模组作者按标签声明时不该被要求改代码。

> **刻意不用 `getUseAnimation() == UseAnim.BLOCK`。** 使用动画是纯表现层，和能不能格挡无关。
> 反例就在本机整合包里：Simply Swords 的 `RunicSwordItem.m_6164_` 只有一行
> `getstatic UseAnim.BLOCK; areturn`，无条件返回格挡动画，却没有任何 `SHIELD_BLOCK` 能力。
> 用它当判据会让副手的符文剑被每 tick 派发 `use()`（它的 `use()` 不调 `startUsingItem`，
> 所以 `isUsingItem()` 守卫也拦不住），触发投掷与 `hurt(1)`。

对应地还补了几处与原版对齐的行为：

- 盾牌被破防后有 100 tick 物品冷却。冷却中 `gameMode.useItem` 的预测 lambda
  （`MultiPlayerGameMode.lambda$useItem$5`）在 `isOnCooldown` 为真时直接 `setValue(PASS)`
  并返回、**不调 `ItemStack.use`**，但网络包照样发出；所以提前用
  `player.getCooldowns().isOnCooldown(...)` 拦掉，别白发。
- 派发节流到每 4 tick 最多一次，对齐原版 `startUseItem` 把 `rightClickDelay` 写成 4 的节奏。
  计数用自减字段而不是比对 `player.tickCount`：后者在换世界/重生时归零，跨世界比对会得负差值、
  节流整局失效。
- 成功派发后只有 `shouldSwing()` 为真才挥手臂（原版举盾不挥手），并调
  `gameRenderer.itemInHandRenderer.itemUsed(OFF_HAND)` 重置手上动画 —— 这与原版
  `startUseItem` 使用物品那条分支完全同构。

> 守卫为什么这么多：这是每 tick 运行的注入。少一个就会在非攻击场景误触发。
> 特别是「任意一只手在使用物品」——主手吃东西/拉弓时若继续派发，`ShieldItem.use`
> 仍返回 consume，于是每 tick 发一个网络包并挥一次副手手臂。

## 三、模块划分

```
com.e33epus.bcp
├── BetterCombatPlus      入口：注册 Cloth Config、配置界面、日志
├── config/BcpConfig      三个开关（默认全开）
├── compat/
│   ├── BetterCombatCompat  BC 只读门面（全部公开 API）
│   └── RollTrigger         翻滚准入判断 + 作废待发攻击 + 派发
└── mixin/
    ├── BcpMixinPlugin      软依赖守卫：CR 不在时整体跳过 MinecraftRollMixin
    ├── PlayerOffhandMixin          缺陷 2a
    └── client/
        ├── MinecraftRollMixin      缺陷 1
        └── MinecraftRecoveryUseMixin 缺陷 2b
```

## 四、依赖与前置

- **硬依赖**：Better Combat（行为主体）、Cloth Config（配置界面）。
  Forge 1.20.1 没有**自动生成**配置界面（`ConfigurationScreen` 是 1.20.2+ 才有），
  所以和上游一样借 Cloth Config 生成，不手写 GUI。
- **配置界面入口**：不需要任何入口 mod。Forge 打补丁后的 `TitleScreen` 自己建了
  `fml.menu.mods` 按钮（`TitleScreen.lambda$init$1` → `new ModListScreen(this)`），
  `ModListScreen.updateCache()` 用 `ConfigScreenHandler.getScreenFactoryFor(modInfo).isPresent()`
  决定 Config 按钮是否可点。我们注册的 `ConfigScreenHandler.ConfigScreenFactory`
  落在本 mod 自己的 `ModContainer.extensionPoints` 里（`ModLoadingContext.registerExtensionPoint`
  → `getActiveContainer().registerExtensionPoint`），所以主菜单 Mods 里选中本 mod 即可打开。
- **软依赖**：Combat Roll。没装时 `BcpMixinPlugin` 让 `MinecraftRollMixin` 整体不应用
  （用 `Class.forName` 探类，避免类加载期 `NoClassDefFoundError`），其余两项修复照常工作。
- **部署面**：设计为双端都装。不做握手，也不写网络包。

## 五、验证

- `./gradlew build` 通过；refmap 三个 SRG 目标已与 MCP 映射表、BC/CR 各自 refmap 交叉核对。
- 产物部署至 `D:/Myworld/.minecraft/versions/1.20.1-main/mods`，哈希比对一致。
- 实机验收已通过：双手武器时盾牌可见、后摇期右键能架盾、攻击动作中能翻滚；
  配置界面文字正常（`pack.mcmeta` 修好后语言文件加载）。

## 六、已知边界

- 双手主手 + 副手武器时副手会显示出来，但不参与攻击：BC 的双持判定要求两手都非双手，
  本 mod 刻意不动这条，以免连带重写其动画选臂与服务端判定。
- 「动画时长被拉伸填满整段武器冷却」是独立的手感问题（`AbstractClientPlayerEntityMixin` 里
  `speed = animation.endTick / length`），**不在 v1 范围**。
- `offhand_while_two_handed` 是双端各自读本地配置，**两端必须一致**。服务端关而客户端开时，
  客户端会把盾举起来、服务端不认，表现为"看着在格挡、实际没挡"。
- 后摇举盾走的是自己派发，绕过了 Forge 的 `InputEvent.InteractionKeyMappingTriggered`、
  `LocalPlayer.isHandsBusy()` 与 `ItemStack.isItemEnabled()` 这三道 `startUseItem` 内的安全门
  （前者自己补了 `isHandsBusy` 判断，后两者未补）。依赖 `InteractionKeyMappingTriggered`
  做右键拦截的权限/防误触 mod 在本路径上会失效。
- 本 mixin 在 TAIL 派发，而原版 `startUseItem` 在同一 tick 的 `handleKeybinds` 里先跑。
  后摇尾段（BC 写的 `rightClickDelay` 已归零、但攻击进度仍 < 1）原版路径是通的，
  此时若主手物品消费了这次点击，本 mixin 仍会再派发一次副手 —— 表现为同一 tick 多发一个
  `ServerboundUseItemPacket`，不会双倍物品（只用 OFF_HAND），第二次也会被 `isUsingItem()` 挡掉。
- 后摇前段只派发副手，所以这段窗口里主手拿末影珍珠/雪球/喷溅药水时这些物品用不出来。
  这不是本 mod 引入的回退：那段窗口 BC 本来就把 `rightClickDelay` 钉住，原版右键路径不执行，
  区别只是「以前什么都不发生」变成「现在举盾」。
- 派发后 `LivingEntity.isBlocking()` 还要求使用已持续 5 tick，所以不是按下即格挡。
