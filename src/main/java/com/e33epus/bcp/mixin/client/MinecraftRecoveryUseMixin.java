package com.e33epus.bcp.mixin.client;

import com.e33epus.bcp.BetterCombatPlus;
import com.e33epus.bcp.compat.BetterCombatCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
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
 *       于是在后摇前段，右键路径根本不会被执行到；</li>
 *   <li>即使走到了，Better Combat 又在 {@code startUseItem} 的 HEAD 按「冷却进度不足」直接取消。</li>
 * </ol>
 *
 * <p>做法：绕开这两层，在 tick 末尾自行判断并派发一次副手使用。{@code gameMode.useItem}
 * 是公开方法，自己会发 {@code ServerboundUseItemPacket} 并在本地执行 {@code ItemStack.use}
 * （已用字节码核实）。注意 {@code LivingEntity.isBlocking()} 还要求使用已持续 5 tick，
 * 所以派发后要按住几 tick 才真正格挡，这不是即时生效的。
 *
 * <p><b>priority 1500 只是「排在 BC 之后」的保守选值，不是正确性依赖。</b>
 * BC 的 {@code upswingTicks} 是在它自己的 {@code pre_Tick}（注入 {@code tick} 的 HEAD）
 * 里递减的，任何 TAIL 注入都晚于它，所以这里读到的必然是当 tick 的最新值。
 */
@Mixin(value = Minecraft.class, priority = 1500)
public abstract class MinecraftRecoveryUseMixin {

    @Shadow
    public LocalPlayer player;

    @Shadow
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Final
    public Options options;

    /**
     * 派发节流计数。原版 {@code startUseItem} 结尾把私有的 {@code rightClickDelay} 写成 4，
     * 于是最快每 4 tick 派发一次；我们绕过了那条路径，得自己维持同样的节奏。
     * 用自减计数而不是比对 {@code player.tickCount}：{@code tickCount} 在换世界/重生时归零，
     * 跨世界比对会得到负差值，节流就整局失效。
     *
     * <p>不给初值：{@code @Unique} 字段带初值要靠构造器注入，没必要引入这个风险。
     * 默认 0 正好等于「可以立刻派发」，不会拖慢窗口里的第一下。
     */
    @Unique
    private int bcp$dispatchCooldown;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bcp$raiseShieldInRecovery(CallbackInfo ci) {
        if (bcp$dispatchCooldown > 0) {
            bcp$dispatchCooldown--;
        }
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
        // 划船时原版会跳过整个右键（startUseItem 的 isHandsBusy 守卫），跟着跳过。
        if (player.isHandsBusy()) {
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
        // 盾牌被破防后有 100 tick 物品冷却。冷却中 gameMode.useItem 的预测 lambda
        // 直接返回 PASS 不调 ItemStack.use，但包照样发；这里提前拦掉，别白发。
        if (player.getCooldowns().isOnCooldown(offhand.getItem())) {
            return;
        }
        if (bcp$dispatchCooldown > 0) {
            return;
        }

        // 不动原版的 rightClickDelay：我们自己派发，动它会让原版下一 tick 又走回
        // 「主手/顺手派发」那条路，可能把右键交给主手物品。
        bcp$dispatchCooldown = 4;
        InteractionResult result = gameMode.useItem(player, InteractionHand.OFF_HAND);
        if (result.consumesAction()) {
            // 跟原版一致：只有 shouldSwing 为真才挥手臂。盾的 use() 返回 CONSUME，
            // shouldSwing 为假，原版举盾也不挥手。
            if (result.shouldSwing()) {
                player.swing(InteractionHand.OFF_HAND);
            }
            // 原版成功使用物品后都会重置手上动画，少了这一步盾的「举起」动作不播。
            minecraft.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);
        }
    }

    /**
     * @Unique：会被合并进 Minecraft，不带前缀有可能和别的 mod 撞名。
     *
     * <p>三条任一成立即认，覆盖原版与模组盾：
     * <ul>
     *   <li>{@code canPerformAction(SHIELD_BLOCK)} — 功能判据，正是
     *       {@code LivingEntity.isBlocking()} 用的那一条；能真正格挡的物品必然过这条；</li>
     *   <li>{@code instanceof ShieldItem} — 冗余但便宜，防御子类覆写能力判据的情况；</li>
     *   <li>{@code #forge:tools/shields} — 标签判据。严格说比能力弱（挂了标签却没实现能力
     *       的物品服务端本来也不认格挡），保留是因为「有盾牌标签就该能举」是对外承诺的语义，
     *       模组作者按标签声明时不该再被要求改代码。</li>
     * </ul>
     *
     * <p>刻意<b>不</b>用 {@code getUseAnimation() == UseAnim.BLOCK}：使用动画是纯表现层，
     * 跟能不能格挡无关。实测整合包里 Simply Swords 的 {@code RunicSwordItem}
     * 无条件返回 {@code UseAnim.BLOCK}（{@code m_6164_} 只有一行 {@code getstatic UseAnim.BLOCK}），
     * 它连 {@code SHIELD_BLOCK} 能力都没有，认它会让副手的符文剑被每 tick 派发 use()。
     */
    @Unique
    private static boolean bcp$isShield(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ShieldItem
                || stack.is(Tags.Items.TOOLS_SHIELDS)
                || stack.canPerformAction(ToolActions.SHIELD_BLOCK));
    }
}
