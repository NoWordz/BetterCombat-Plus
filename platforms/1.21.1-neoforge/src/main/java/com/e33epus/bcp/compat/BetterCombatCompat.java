package com.e33epus.bcp.compat;

import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Better Combat 的只读门面。全部走它的公开 API，不碰内部字段。
 *
 * <p>为什么需要这一层：BC 是 All Rights Reserved，唯一合法做法是运行时互操作。
 * 这里的每个方法都能在 BC 发布 jar 里找到对应公开签名（2.4.0+1.21.1 已用 javap 核实）。
 */
public final class BetterCombatCompat {
    private BetterCombatCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("bettercombat");
    }

    /** 起手（前摇）剩余 tick；0 表示不在蓄力期。 */
    public static int upswingTicks(Minecraft minecraft) {
        if (!isLoaded() || !(minecraft instanceof MinecraftClient_BetterCombat betterCombat)) {
            // BC 在缺 PlayerAnimator 时会把自己全部 mixin 静默跳过（2.4.0 的
            // BetterCombatMixinPlugin 仍以 dev.kosmx.playerAnim.api.layered.IAnimation 探类），
            // 此时它不会实现这个接口，直接强转会 ClassCastException。降级为「不在蓄力期」。
            return 0;
        }
        return betterCombat.getUpswingTicks();
    }

    /** 作废当前这一刀：停动画。调用方负责同时重置攻击进度。 */
    public static void cancelUpswing(Minecraft minecraft) {
        if (!isLoaded() || !(minecraft instanceof MinecraftClient_BetterCombat betterCombat)) {
            return;
        }
        betterCombat.cancelUpswing();
    }

    public static WeaponAttributes attributesOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return WeaponRegistry.getAttributes(stack);
    }

    public static boolean isTwoHanded(ItemStack stack) {
        WeaponAttributes attributes = attributesOf(stack);
        return attributes != null && attributes.isTwoHanded();
    }

    /**
     * BC 会不会因为双手武器而隐藏副手：主手或副手任一带 {@code two_handed} 它就会。
     * 与 BC 自己的判定保持同一口径，避免我们放行和它封锁的范围不一致。
     *
     * <p>刻意从 {@link Inventory} 直读，不走 {@code getOffhandItem()} / {@code getMainHandItem()}：
     * 后两者内部会回调 {@code getItemBySlot}，而本方法的调用点就在 {@code getItemBySlot} 的注入里，
     * 走它们会立刻自我递归（BC 自己也因此直接读背包）。
     */
    public static boolean suppressesOffhandFor(Inventory inventory) {
        return isTwoHanded(inventory.getSelected()) || isTwoHanded(inventory.offhand.get(0));
    }

    public static boolean suppressesOffhand(Player player) {
        return suppressesOffhandFor(player.getInventory());
    }
}
