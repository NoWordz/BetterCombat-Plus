package com.e33epus.bcp.mixin;

import com.e33epus.bcp.BetterCombatPlus;
import com.e33epus.bcp.compat.BetterCombatCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复「双手武器时副手消失」。
 *
 * <p>根因：Better Combat 在 {@code Player.getItemBySlot} 的 HEAD 注入，主手或副手任一带
 * {@code two_handed} 时就把 {@code OFFHAND} 查表结果换成 {@code ItemStack.EMPTY}。
 * 这是 common mixin，客户端与服务端同时生效，所以盾牌既画不出来、也举不起来。
 *
 * <p>做法：在同一个点抢在它前面返回真实副手物品并取消方法，BC 那个 HEAD 回调就没有机会执行。
 * BC 的类一行不改（它 All Rights Reserved，只允许运行时互操作）。
 *
 * <p>priority 900 &lt; BC 的默认 1000。Mixin 官方对 {@code @Mixin.priority} 的定义是
 * 「lower priority mixins being applied first」，且 inject-at-HEAD 是 first-come-first-served：
 * 先应用者的代码插在最前面、先执行。所以数值更小 = 我们先跑。
 */
@Mixin(value = Player.class, priority = 900)
public abstract class PlayerOffhandMixin {

    @Shadow
    public abstract Inventory getInventory();

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void bcp$restoreOffhandForTwoHanded(EquipmentSlot slot,
                                                CallbackInfoReturnable<ItemStack> cir) {
        if (slot != EquipmentSlot.OFFHAND || !BetterCombatPlus.offhandWhileTwoHanded()) {
            return;
        }
        // 只在 BC 本来会封锁的情形介入，其余情况完全交给原版/BC，避免改变无关行为。
        // 这里必须用能直接读背包的重载：本方法的调用点就在 getItemBySlot 内部。
        if (!BetterCombatCompat.suppressesOffhandFor(getInventory())) {
            return;
        }
        cir.setReturnValue(getInventory().offhand.get(0));
        cir.cancel();
    }
}
