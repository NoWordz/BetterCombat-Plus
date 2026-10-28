# Changelog

本项目所有值得注意的改动记录在此。格式参考 [Keep a Changelog](https://keepachangelog.com/)，
版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

## [0.1.0] - 2026-10-28

首个版本。修 Better Combat + Combat Roll 组合下的三处手感缺陷。

### 新增

- 攻击动作期间可以翻滚。翻滚会作废正在起手的那一刀，但仍然消耗一次武器冷却。
- 双手武器（镰刀、大剑、重斧、长枪等）时副手物品正常渲染与使用，盾牌不再消失。
- 攻击后摇期可以右键举盾；蓄力期仍让位给攻击，出刀手感不变。
- Cloth Config 配置界面，三个开关默认全开。

### 说明

- Better Combat 为硬依赖，Combat Roll 为软依赖（未安装时仅关闭翻滚接管）。
- 两把单手武器的原生双持轮流攻击行为未作改动。

### 已知边界

- 双手主手 + 副手武器时，副手物品会显示但不参与攻击。Better Combat 的双持判定要求两手都非双手，
  本次刻意不改动它。
- 攻击动画时长仍与武器冷却绑定（长冷却武器的前后摇偏长）。这是独立问题，不在本次范围。

---

## 英文版

### Added

- Roll during an attack. Rolling voids the windup in progress but still consumes one weapon cooldown.
- Offhand items stay visible and usable while wielding a two-handed weapon — shields no longer disappear.
- Raise a shield during the attack recovery window; the windup phase still takes priority.
- Cloth Config screen with three switches, all enabled by default.

### Notes

- Better Combat is required; Combat Roll is optional (rolling takeover is skipped when absent).
- Native dual-wielding between two one-handed weapons is left untouched.

### Known limits

- With a two-handed main hand plus an offhand weapon, the offhand renders but does not attack —
  Better Combat's dual-wield predicate requires both hands non-two-handed, and this release deliberately leaves it alone.
- Attack animation length is still tied to weapon cooldown (slow weapons have long windup/recovery).
  That is a separate issue, out of scope here.

### 修复（审查后）

- 翻滚注入改为抢在 Combat Roll 之前（priority 400 < 449）。原先让 CR 先跑，导致它在最常见路径上
  cancel 了动画却不重置攻击计时器，那一刀仍会砍出——「能滚但刀也打了」。
- 后摇举盾增加守卫：界面/资源重载遮罩/暂停/挖方块/任意一只手在使用物品时都不派发。
  原先主手吃东西或拉弓时，副手会每 tick 被误派发（连发网络包 + 手臂连拍）。
- BC 未实现 `MinecraftClient_BetterCombat`（缺 PlayerAnimator 时它会跳过全部 mixin）时不再强转崩溃。
- 依赖版本范围收紧到实际验证过的区间：BC `[1.9.0,1.10)`、CR `[1.3.3,1.4)`。
- 补 `en_us` / `zh_cn` 语言文件，配置界面不再显示裸 key。
- jar 内打包 LICENSE 与 NOTICE；`libs/` 加入 gitignore（BC 为 All Rights Reserved，不随仓库分发）。

### 修复 0.1.1 候选（资源包）

- 补 `pack.mcmeta`：之前 jar 里有 `assets/` 却没有包元数据，Forge 报
  `Missing metadata in pack mod:bettercombatplus`，整个资源包不被加载，
  语言文件随之失效，配置界面三个开关全部显示成
  `text.autoconfig.bettercombatplus.option.xxx` 裸键名。
  一个文件同时修掉「资源包警告」和「配置界面裸键名」。
- 补 `logo.png` 与 `logoFile`，消除 Mod Menu 的 `broken icon` 警告。
