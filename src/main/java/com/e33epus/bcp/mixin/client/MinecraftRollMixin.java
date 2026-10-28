package com.e33epus.bcp.mixin.client;

import com.e33epus.bcp.compat.RollTrigger;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复「Better Combat 攻击过程中无法翻滚」。
 *
 * <p>priority 400 &lt; Combat Roll 的 449：数值更小 = 更早应用 = 更早执行，
 * 所以本回调排在 CR 自己的 {@code tryRolling} 之前。
 *
 * <p>为什么必须抢在它前面：CR 自己那条路径会把起手动画 {@code cancelUpswing()} 掉，
 * 却**不重置攻击计时器**，于是几 tick 后 BC 的 {@code performAttack()} 照样砍出来——
 * 玩家看到的是「滚了一下但伤害莫名出现」。由我们统一触发，就能保证每次翻滚都走
 * {@link com.e33epus.bcp.compat.RollTrigger#voidPendingAttack}，那一刀干净作废。
 *
 * <p>不会双滚：我们滚成功后 {@code isRollAvailable} 立刻为假，随后 CR 自己的分支自然退出。
 *
 * <p>没有 Combat Roll 时这个类不会被应用（见 {@code BcpMixinPlugin}）。
 */
@Mixin(value = Minecraft.class, priority = 400)
public abstract class MinecraftRollMixin {

    @Inject(method = "handleKeybinds", at = @At("TAIL"))
    private void bcp$rollDuringAttack(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (!RollTrigger.tryRoll()) {
            return;
        }
        RollTrigger.voidPendingAttack(minecraft);
    }
}
