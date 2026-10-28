package com.e33epus.bcp.mixin.client;

import com.e33epus.bcp.BetterCombatPlus;
import com.e33epus.bcp.compat.BetterCombatCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复「攻击后摇中举不起盾」。
 *
 * <p>根因是两层叠加，只在 {@code startUseItem} 上做手脚是不够的：
 * <ol>
 *   <li>Better Combat 起手时把原版私有的 {@code rightClickDelay} 写成整段武器冷却，
 *       而原版 {@code handleKeybinds} 只有在它为 0 时才调 {@code startUseItem}——
 *       于是在整个后摇期，右键路径根本不会被执行到；</li>
 *   <li>即使走到了，Better Combat 又在 {@code startUseItem} 的 HEAD 按「冷却进度不足」直接取消。</li>
 * </ol>
 *
 * <p>做法：绕开这两层，在 tick 末尾自行判断并派发一次副手使用。{@code gameMode.useItem}
 * 是公开方法，自己会发 {@code ServerboundUseItemPacket} 并在本地执行 {@code ItemStack.use}，
 * 盾牌因此立刻进入格挡状态（已用字节码核实）。
 */
@Mixin(value = Minecraft.class, priority = 1500)
public abstract class MinecraftRecoveryUseMixin {

    @Shadow
    public LocalPlayer player;

    @Shadow
    public net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode;

    @Shadow
    @Final
    public net.minecraft.client.Options options;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bcp$raiseShieldInRecovery(CallbackInfo ci) {
        if (!BetterCombatPlus.rightClickInRecovery() || player == null || gameMode == null) {
            return;
        }
        Minecraft minecraft = (Minecraft) (Object) this;
        // 开着界面（背包/聊天/菜单）时右键是拿来点 UI 的，绝不能在这里举手。
        if (minecraft.screen != null || minecraft.getOverlay() != null || minecraft.isPaused()) {
            return;
        }
        // 挖方块期间原版会整体跳过右键，我们也跟着跳过，别抢它的输入。
        if (gameMode.isDestroying()) {
            return;
        }
        if (!options.keyUse.isDown()) {
            return;
        }
        // 蓄力期让位给攻击：这一刀该由 BC 决定要不要出手。
        if (BetterCombatCompat.upswingTicks(minecraft) > 0) {
            return;
        }
        // 只在后摇窗口介入。冷却已满时没有后摇，交给原版正常路径。
        float progress = player.getAttackStrengthScale(0.0F);
        if (progress >= 1.0F) {
            return;
        }
        // 任何一只手在使用物品时都不派发。
        // 不区分手：主手吃东西/拉弓时若继续派发，ShieldItem.use 会照旧返回 consume，
        // 于是每 tick 发一个 ServerboundUseItemPacket 并挥一次副手手臂（实测可复现）。
        if (player.isUsingItem()) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!bcp$isShield(offhand)) {
            return;
        }

        // 不动原版的 rightClickDelay：我们自己派发，动它会让原版下一 tick 又走回
        // 「主手/顺手派发」那条路，可能把右键交给主手物品。
        InteractionResult result = gameMode.useItem(player, InteractionHand.OFF_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.OFF_HAND);
        }
    }

    /** @Unique：会被合并进 Minecraft，不带前缀有可能和别的 mod 撞名。 */
    @Unique
    private static boolean bcp$isShield(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ShieldItem || stack.is(Tags.Items.TOOLS_SHIELDS));
    }
}
