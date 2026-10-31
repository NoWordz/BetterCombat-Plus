# NeoForge 1.21.1 移植方案与实施记录

**状态**：已实施（构建通过 + 字节码核对完成）；**未实机验收**，等待用户在有 NeoForge 1.21.1 的环境测试。

本文的每条 API 名字都对着 1.21.1 实物核过，核对方式写在表里。

---

## 一、验收环境

`D:/Myworld/.minecraft/versions/Mechanomania-航空学` 是 **NeoForge 21.1.228 + MC 1.21.1**
（`mainClass = cpw.mods.bootstraplauncher.BootstrapLauncher`，库里有
`net.neoforged.fancymodloader:loader:4.0.42` 与 `net.fabricmc:sponge-mixin:0.15.2+mixin.0.8.7`），
已装 `cloth-config-15.0.140-neoforge.jar`。

**缺 BC 2.4.0 与 CR 2.0.6**，装上前两项修复无法验收。三个 jar 已在本地
`platforms/1.21.1-neoforge/libs/`（只在编译期用，不进仓库）。

---

## 二、为什么不能照抄

三处修复的**根因与注入点全部不变**，但**每一个外部依赖的名字都变了**。
下面每一条都可能导致「能编译、跑起来什么都不做」，而且不报错——这是本次移植最大的风险类别。

| 我们依赖的东西 | 1.20.1 Forge | 1.21.1 NeoForge | 核实方式 |
|---|---|---|---|
| Combat Roll 包名 | `net.combatroll.*` | **`net.combat_roll.*`** | 解包 2.0.6 目录树 |
| CR 主类 | `CombatRoll` | **`CombatRollMod`** | `javap -p`，常量池里 `ID = "combat_roll"` |
| CR 配置字段 | `CombatRoll.config` | `CombatRollMod.config`（`ServerConfig` 字段名全部不变） | `javap -p ServerConfig` |
| CR 距离属性 | `EntityAttributes_CombatRoll.getAttributeValue(p, Type.DISTANCE)` | **`p.getAttributeValue(CombatRoll.Attributes.DISTANCE.entry)`** | `javap -p 'CombatRoll$Attributes$Entry'` 有 `Holder<Attribute> entry` 字段 |
| CR 发包 | fabric shim `ClientPlayNetworking.send(ID, Buf)` | **`Platform.networkC2S_Send(CustomPacketPayload)`** | `javap -p Platform` + CR 自己 mixin 偏移 575 就是这么调的 |
| CR 翻滚包 | `Packets.RollPublish(...)` + `.write()` | 同构造签名，实现 `CustomPacketPayload` | `javap -p 'Packets$RollPublish'` |
| 盾牌标签 | `Tags.Items.TOOLS_SHIELDS` | **`Tags.Items.TOOLS_SHIELD`**（单数） | `javap -p 'Tags$Items'` |
| 盾牌能力 | `ToolActions.SHIELD_BLOCK` | **`ItemAbilities.SHIELD_BLOCK`**（`ItemAbility` 类型） | `javap -p ItemAbilities` |
| 标签命名空间 | `forge:tools/shields` | **`c:tools/shield`** | `Tags$Items.tag()` 偏移 0 是 `ldc "c"` |
| BC 的 `getEquippedStack` 注入 | `PlayerEntityMixin`，无 priority（默认 1000） | **`player.PlayerEntityMixin`，`priority=899`** | `javap -v` Mixin 注解 |
| BC 的 `MinecraftClientInject` | `tick` HEAD + TAIL，无 priority | 同（`tick()V` HEAD + TAIL） | `javap -v` 注解表 |
| BC mixin plugin 的守卫 | `Class.forName("dev.kosmx.playerAnim...IAnimation")` | 同 | `javap -p -c BetterCombatMixinPlugin` |
| 映射 | official + **必须有 refmap** | official，**无 SRG，不需要 refmap** | 产物里零 `f_`/`m_` 残留（已验证） |
| Mixin 注册 | `MANIFEST.MF` 的 `MixinConfigs` | **`neoforge.mods.toml` 的 `[[mixins]] config=`** | CR 2.0.6 的 toml 实样 |
| 配置界面 | `ConfigScreenHandler.ConfigScreenFactory` | **`IConfigScreenFactory`**（`createScreen(ModContainer, Screen)`） | `javap -p IConfigScreenFactory` |
| Java | 17 | 21 | MC 1.21.1 |

---

## 三、已实施的内容

### 结构

按 monorepo 重组，详见 [repo-layout](repo-layout.md)：

```
gradle.properties              身份键唯一来源
gradle/identity.gradle         各平台读身份键（读不到就失败）
common/resources/              语言文件 + logo（两端逐字相同）
platforms/1.20.1-forge/
platforms/1.21.1-neoforge/
```

1.20.1 目标只是搬家 + 接上身份装配脚本，源码逻辑一行未改，**搬完重新构建验证通过**
（产物 `bettercombatplus-Forge-1.20.1-0.1.0.jar`，内容与搬家前一致）。

### 缺陷 2a（副手）—— priority 必须改

```java
// 1.20.1
@Mixin(value = Player.class, priority = 900)
// 1.21.1：BC 2.4.0 把它自己的改成 899 了
@Mixin(value = Player.class, priority = 800)
```

`Player.getItemBySlot` / `Inventory.getSelected()` / `Inventory.offhand` 在 1.21.1 全部同名同形。

### 缺陷 1（翻滚）—— 名字全换

`RollTrigger.java` 是两端差异最大的文件，全部改动集中在它里面：

```java
// 距离属性
player.getAttributeValue(CombatRoll.Attributes.DISTANCE.entry)
// 配置
CombatRollMod.config.allow_rolling_while_airborn
// 发包
Platform.networkC2S_Send(new Packets.RollPublish(player.getId(), visuals, direction));
```

`RollManager.isRollAvailable/isRolling/onRoll`、`((RollingEntity) player).getRollManager()`、
`Keybindings.roll`、`RollEffect.playVisuals`、`Input.forwardImpulse/leftImpulse` 全部不变。

`BcpMixinPlugin` 的软依赖探针改成 `net.combat_roll.client.Keybindings`。

### 缺陷 2b（后摇举盾）—— 主体不变

`Minecraft.tick()` / `gameMode.useItem(player, OFF_HAND)` / `player.getCooldowns().isOnCooldown(...)` /
`result.shouldSwing()` / `gameRenderer.itemInHandRenderer.itemUsed(hand)` / `player.isHandsBusy()`
在 1.21.1 全部存在（逐个在官方映射表里查到）。只有盾牌判据换名字：

```java
stack.is(Tags.Items.TOOLS_SHIELD)             // c:tools/shield
stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)
```

### 构建

```groovy
plugins { id 'net.neoforged.moddev' version '2.0.141' }
neoForge {
    version = '21.1.228'
    parchment { minecraftVersion = '1.21.1'; mappingsVersion = '2024.11.17' }
}
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
```

- Gradle **9.2.1**（8.8 解析不到 ModDevGradle 插件）
- **不声明** mixin annotation processor：无 SRG 就不需要 refmap，MDK 的 NeoForge 目标同样只 `compileOnly`
- `compatibilityLevel` = `JAVA_21`

---

## 四、验证阶梯

| 级别 | 内容 | 状态 |
|---|---|---|
| 1. 编译 | `cd platforms/1.21.1-neoforge && ./gradlew build` 出 jar | ✅ 通过 |
| 2. 字节码核对 | 三个 mixin 进包、priority 正确、零 SRG 残留 | ✅ 通过 |
| 3. 静态核对 | 目标方法与字段在 1.21.1 官方映射表逐个存在 | ✅ 通过 |
| 4. 依赖类存在性 | 引用的 12 个 CR 类 + 3 个 BC 类在 jar 里都在 | ✅ 通过 |
| 5. 实机 smoke | 启动 NeoForge 1.21.1，确认 mixin 全应用、无 `MixinApplyError` | ❌ **未做** |
| 6. 验收 | 三项修复逐一手测 | ❌ **未做** |

### 已执行的核对命令与结果

```
priority:  PlayerOffhandMixin=800 (< BC 2.4.0 的 899) ✓
           MinecraftRollMixin=400 (< CR 2.0.6 的 449) ✓
SRG 残留:  javap 全类扫描无 f_/m_ ✓
注入点:    getItemBySlot HEAD / handleKeybinds TAIL / tick TAIL ✓
探针:      ldc "net.combat_roll.client.Keybindings" ✓
CR 类:     12/12 存在 ✓
BC 类:     3/3 存在 ✓
jar 内容:  3 个 mixin 类 + 2 个 lang + logo + pack.mcmeta + neoforge.mods.toml + LICENSE + NOTICE ✓
```

---

## 五、要实机验收需要装的东西

到 `1.21.1-航空学` 实例的 `mods/` 下放入：

| 文件 | 来源 | 说明 |
|---|---|---|
| `bettercombatplus-NeoForge-1.21.1-0.1.0.jar` | 本仓构建产物 | |
| `bettercombat-neoforge-2.4.0+1.21.1.jar` | Modrinth `better-combat` | **缺**，必须装 |
| `combat_roll-neoforge-2.0.6+1.21.1.jar` | Modrinth `combat-roll` | **缺**，只验前两项修复可不装 |
| `playeranimator`（1.21.1 neoforge） | Modrinth | BC/CR 都硬依赖它，**缺了它们会静默跳过全部 mixin** |
| `cloth-config-15.0.140-neoforge.jar` | Modrinth | 已在实例里 |

### 实机验收要点

看 `latest.log`，按顺序确认：

1. 没有 `MixinApplyError` / `InvalidInjectionException`
2. 有 `Better Combat Plus loaded (roll=true, offhand=true, recoveryUse=true)`
3. 三处手测：攻击中能翻滚且那一刀作废；拿镰刀时副手盾牌可见；后摇期右键能举盾

## 六、已知风险

| # | 风险 | 处置 |
|---|---|---|
| 1 | priority 照抄 900 → 副手修复静默失效 | 已改 800，字节码核对过 |
| 2 | CR 包名探针写错 → 翻滚修复静默失效 | 已改 `net.combat_roll`，字节码核对过 |
| 3 | BC/CR 缺 PlayerAnimator 时静默跳过 mixin | `BetterCombatCompat` 的 `instanceof` 守卫已覆盖，不用改 |
| 4 | 1.21.1 的 `Minecraft.tick` 是否有多个 return（TAIL 偏移歧义） | 未逐条核实；若注入报 `InvalidInjectionException` 需换 `RETURN` + ordinal |
| 5 | 无实机环境 | 见第四节，第 5/6 级未做 |
