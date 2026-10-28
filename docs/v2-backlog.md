# v2 待办

## 1. 动画时长与武器冷却解耦（同时修复「后摇举盾看不见」）

**两个症状，一个根因。**

症状 A：长冷却武器（重斧、镰刀、巨剑）前后摇明显过长，回正慢。
症状 B：攻击后摇里举盾，第一人称看不到盾牌抬起（用手感描述：像动作没回正）。

**根因**：Better Combat 把整段武器冷却当作动画长度传给动画层。

```java
// MinecraftClientInject.startUpswing (1.9.0)
int attackCooldownTicks = Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player));
this.upswingTicks = Math.max(Math.round(attackCooldownTicksFloat * upswingRate), 1);
((PlayerAttackAnimatable) player).playAttackAnimation(animationName, animatedHand, attackCooldownTicksFloat, upswingRate);
//                                                              ^^^^^^^^^^^^^^^^^^^^^^ 整段冷却
```

```java
// AbstractClientPlayerEntityMixin.playAttackAnimation
var speed = ((float)animation.endTick) / length;   // 把固定长度的动画拉伸填满整段冷却
```

实测数字（`upswing: 0.5`，全局 `upswing_multiplier: 0.5`）：

| 武器攻速 | 冷却 | 前摇 | 动画总长 |
|---|---|---|---|
| 1.6（剑） | 12.5 tick | 3 tick | 0.63s |
| 1.0（斧） | 20 tick | 5 tick | 1.00s |
| 0.8（重斧） | 25 tick | 6 tick | 1.25s |
| 0.6（镰刀类） | 33 tick | 8 tick | 1.65s |

**为什么这会让盾牌看不见**：BC 在攻击动画播放期间把第一人称渲染模式设成
`FirstPersonMode.THIRD_PERSON_MODEL`（`FirstPersonAnimationCompatibility.firstPersonMode()`），
即用玩家模型替代原版的第一人称手部渲染。原版"举盾"是 `ItemInHandRenderer` 画的，
被这条路径整个绕过；而玩家模型上 BC 又没有"格挡"姿势（`updateAnimationsOnTick` 在
`isUsingItem()` 时把所有 pose 置 null）。

只有动画接近尾声时 `CustomAnimationPlayer.getFirstPersonMode()` 才因 `isWindingDown()`
返回 `FirstPersonMode.NONE`，第一人称才交还原版——而那时后摇已经快结束了。
所以「盾看起来没举起来」和「后摇太长」是同一件事的两面。

**方案（待设计）**：

1. 让动画按自身自然长度播放，不再拉伸填满冷却：
   接管 `playAttackAnimation` 传入的 `length`，改为 `endTick` 对应的自然时长。
2. 需要同时决定：动画比冷却短时怎么办。候选：
   - 动画播完即回正（最直接，但攻击节奏会变成"打完就收"，冷却仍在后台走）；
   - 播完后接一段轻量的回正过渡；
   - 保留 `upswing` 与伤害判定时刻的同步，避免动画与伤害脱节。
3. 必须验证：伤害结算时刻不变（`upswingTicks` 与 `performAttack` 的时序不能被动画改动影响）。

**风险**：这是接管上游动画播放链路，改动面比 v1 大得多。v1 的三个修复都不碰动画时长。

**相关代码位置**

- BC `MinecraftClientInject.startUpswing:252-259`
- BC `AbstractClientPlayerEntityMixin.playAttackAnimation:126-160`
- BC `AbstractClientPlayerEntityMixin.updateAnimationsOnTick:77-85`（`isUsingItem` 清 pose）
- BC `CustomAnimationPlayer.isWindingDown:17-20`
- BC `FirstPersonAnimationCompatibility.firstPersonMode:22-31`

## 2. 双手主手 + 副手武器参与攻击

当前副手只渲染不可攻击（BC 的 `isDualWielding` 要求两手都非双手）。若要放开，
需同时改客户端动画选臂与服务端判定，否则双端不一致。

## 3. 双端配置同步

`offhand_while_two_handed` 目前双端各读本地文件，需人工保证一致。
可改为服务端权威 + 登录时同步，避免"看着在格挡、实际没挡"。

## 4. 右键派发的兼容性

后摇举盾走自有派发，绕过 `InputEvent.InteractionKeyMappingTriggered`；
也没调 `itemInHandRenderer.itemUsed()`，故播放不出装备动画。
若发现与权限/防误触 mod 冲突，再补发对应事件。
