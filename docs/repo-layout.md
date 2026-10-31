# 仓库结构

单仓多目标（monorepo）。每个 `platforms/<目标>/` 都是一个**独立构建**：自己的
`gradlew`、自己的 Java 与 Gradle 版本——因为这是硬约束（1.20.1 要 Java 17 + Gradle 8.8，
NeoForge 1.21.1 要 Java 21 + Gradle 9.2.1），强行统一只会让一边编不过。

```
bettercombatplus/
├── gradle.properties              身份键唯一来源（mod_id / mod_version / …）
├── gradle/identity.gradle         各平台读身份键的装配脚本
├── common/resources/              两端逐字相同的资源（语言文件、logo）
├── platforms/
│   ├── 1.20.1-forge/              ForgeGradle 6.x + Mixin 0.7 + Java 17 + Gradle 8.8
│   └── 1.21.1-neoforge/           ModDevGradle 2.0.141 + Java 21 + Gradle 9.2.1
├── docs/
├── ARCHITECTURE.md
├── CHANGELOG.md
└── NOTICE.md
```

## 为什么身份键只有一处

一次发版，所有目标出的必须是同一个版本号的 jar——玩家在不同加载器上装到的 `vX.Y.Z`
必须是同一次改动。留在各平台里它们就能各自漂移，而「某个目标悄悄落后」正是这套结构要治的病。

`gradle/identity.gradle` 读不到键就直接抛 `GradleException`，**不给默认值**：
悄悄退回默认值会让版本号静默漂移，jar 照样出得来，只是对不上同一次改动。

## 什么共享、什么不共享

| 内容 | 位置 | 理由 |
|---|---|---|
| 语言文件、logo | `common/resources/` | 两端逐字相同 |
| 三个开关的定义 | 各平台 `BcpConfig.java` | Cloth Config 的注解在两端同名，但依赖版本不同；逐字相同也不必强行合并 |
| `pack.mcmeta` | 各平台自己 | `pack_format` 不同（1.20.1 = 15，1.21.1 = 34） |
| Mixin 配置 | 各平台自己 | 注册方式不同：1.20.1 走 `MANIFEST.MF` 的 `MixinConfigs`，1.21.1 走 `neoforge.mods.toml` 的 `[[mixins]]` |
| `refmap` | **只有 1.20.1 有** | NeoForge 用官方名运行、无 SRG，不需要 refmap |
| 三处修复的算法 | 各平台各一份 | 依赖的类名/签名不同（见下） |

## 两端真正的差异

三处修复的**根因与注入点两端完全一致**，只有外部依赖的名字不同：

| 项 | 1.20.1 Forge | 1.21.1 NeoForge |
|---|---|---|
| CR 包名 | `net.combatroll.*` | `net.combat_roll.*` |
| CR 主类 | `CombatRoll` | `CombatRollMod` |
| CR 距离属性 | `EntityAttributes_CombatRoll.getAttributeValue(p, Type.DISTANCE)` | `p.getAttributeValue(CombatRoll.Attributes.DISTANCE.entry)` |
| CR 发包 | fabric shim `ClientPlayNetworking.send` | `Platform.networkC2S_Send(CustomPacketPayload)` |
| CR modId | `combatroll` | `combat_roll` |
| 盾牌标签 | `Tags.Items.TOOLS_SHIELDS`（`forge:tools/shields`） | `Tags.Items.TOOLS_SHIELD`（`c:tools/shield`） |
| 盾牌能力 | `ToolActions.SHIELD_BLOCK` | `ItemAbilities.SHIELD_BLOCK` |
| BC 副手 mixin priority | 无（默认 1000） | **899** |
| 配置界面扩展点 | `ConfigScreenHandler.ConfigScreenFactory` | `IConfigScreenFactory` |
| 映射 | official + refmap | official，无 SRG |

## 两个静默失效点（移植时最容易漏）

1. **priority**：BC 1.9.0 的 `PlayerEntityMixin` 不写 priority（默认 1000），我们写 900 抢在它前面。
   BC 2.4.0 改成 **899**，照抄 900 就会后执行——BC 先 `setReturnValue(EMPTY)`，我们的 cancel
   来得太晚，**副手修复静默失效且不报错**。NeoForge 端用 800。
2. **软依赖探针类名**：`BcpMixinPlugin` 用 `Class.forName` 探 CR 是否在。包名从
   `net.combatroll` 变成 `net.combat_roll`，照抄旧名字会让探针永远返回 false，
   **翻滚修复全程不应用且不报错**。

这两处都在代码注释里写明，且移植时以字节码核对（见 `docs/port-1.21.1-neoforge.md`）作为验证。

## 构建

```bash
# Forge 1.20.1（需要 Java 17；本机证书校验必失败，要带 flag）
cd platforms/1.20.1-forge
./gradlew build -Dnet.minecraftforge.gradle.check.certs=false

# NeoForge 1.21.1（需要 Java 21）
cd platforms/1.21.1-neoforge
./gradlew build
```

构建前把三个上游 jar 放进对应平台的 `libs/`（被 `.gitignore` 排除，见 `NOTICE.md`）。
