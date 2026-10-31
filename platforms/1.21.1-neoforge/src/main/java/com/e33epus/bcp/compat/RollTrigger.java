package com.e33epus.bcp.compat;

import com.e33epus.bcp.BetterCombatPlus;
import net.combat_roll.CombatRollMod;
import net.combat_roll.Platform;
import net.combat_roll.api.CombatRoll;
import net.combat_roll.client.Keybindings;
import net.combat_roll.client.RollEffect;
import net.combat_roll.internals.RollManager;
import net.combat_roll.internals.RollingEntity;
import net.combat_roll.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 让翻滚在整段攻击动作里都能用。
 *
 * <p>Combat Roll 自己的触发点（{@code tryRolling}）里有两道闸门会让这里失效：
 * 「武器冷却未到 95% 就 return」和「按住攻击键就 return」——后者在 Better Combat
 * 默认开启的长按攻击下每时每刻都成立。这两道闸门在 Combat Roll 的 mixin 类内部，
 * 无法从外部改写，所以这里用它的公开 API 重新实现一次触发。
 *
 * <p>复用而重写的部分只限「什么时候允许滚」；距离、方向、水面/冰面衰减的算法照
 * Combat Roll（GPL-3.0）的实现重新表达，本 mod 同为 GPL-3.0，来源已在 NOTICE 标注。
 *
 * <p><b>1.21.1 与 1.20.1 的差异全在这个文件里</b>：CR 包名 {@code net.combatroll} →
 * {@code net.combat_roll}，主类 → {@code CombatRollMod}，距离属性改走
 * {@code CombatRoll.Attributes.DISTANCE.entry}（{@code Holder}），发包改走
 * {@code Platform.networkC2S_Send(CustomPacketPayload)}。名字逐条对 2.0.6 jar 核实过。
 */
public final class RollTrigger {
    private RollTrigger() {
    }

    /**
     * 尝试翻滚。返回 true 表示这一 tick 已经滚出去了。
     *
     * <p>本回调排在 Combat Roll 自己的 {@code tryRolling} 之前（priority 400 &lt; 449）。
     * 我们滚成功时 {@code isRollAvailable} 立即为假，它随后自然退出，不会双滚。
     */
    public static boolean tryRoll() {
        if (!BetterCombatPlus.rollDuringAttack() || !BetterCombatCompat.isLoaded()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || minecraft.isPaused()) {
            return false;
        }
        if (!Keybindings.roll.isDown()) {
            return false;
        }

        RollManager rollManager = ((RollingEntity) player).getRollManager();
        if (!rollManager.isRollAvailable(player)) {
            return false;
        }
        // 原版条件下的准入判断保持与 Combat Roll 一致，只放开「攻击中」这一条。
        if (!CombatRollMod.config.allow_rolling_while_airborn && !player.onGround()) {
            return false;
        }
        if (player.getFoodData().getFoodLevel() <= CombatRollMod.config.food_level_required) {
            return false;
        }
        if (player.isSwimming() || player.isVisuallyCrawling()) {
            return false;
        }
        if (player.getVehicle() != null) {
            return false;
        }
        // 保持 Combat Roll 的这两条：正在吃东西/拉弓/举盾时不滚。
        // 我们只放开「攻击动作期间」，不顺手改动与攻击无关的手感。
        if (player.isUsingItem() || player.isBlocking()) {
            return false;
        }

        Vec3 direction = rollDirection(player);
        player.push(direction.x, direction.y, direction.z);
        rollManager.onRoll(player);
        publish(player, direction);
        return true;
    }

    /**
     * 作废正在进行的那一刀：停动画 + 重置攻击进度，让待发的那次攻击在冷却检查处自我放弃。
     *
     * <p>只在确实还停在起手期时动作。后摇期没有待发的攻击可作废，也就不该白白重置冷却 ——
     * 翻滚本身不该额外惩罚已经在收招的人。
     */
    public static void voidPendingAttack(Minecraft minecraft) {
        if (!BetterCombatCompat.isLoaded() || BetterCombatCompat.upswingTicks(minecraft) <= 0) {
            return;
        }
        BetterCombatCompat.cancelUpswing(minecraft);
        LocalPlayer player = minecraft.player;
        if (player != null) {
            // Better Combat 自己的假动作（feint）就是这么作废的：停摆 + 重置冷却计时。
            // 重置后冷却检查必然不通过，那一刀既不会出手、也照样吃掉一次武器冷却。
            player.resetAttackStrengthTicker();
        }
    }

    private static Vec3 rollDirection(LocalPlayer player) {
        float forward = player.input.forwardImpulse;
        float sideways = player.input.leftImpulse;
        Vec3 direction = (forward == 0.0F && sideways == 0.0F)
                ? new Vec3(0.0, 0.0, 1.0)
                : new Vec3(sideways, 0.0, forward).normalize();
        direction = direction.yRot((float) Math.toRadians(-player.getYRot()));

        double distance = 0.475 * (player.getAttributeValue(CombatRoll.Attributes.DISTANCE.entry)
                + CombatRollMod.config.additional_roll_distance);
        direction = direction.scale(distance);

        if (player.isInWater()) {
            // Entity.getFluidHeight(TagKey) 在 1.21.1 标了 @Deprecated（1.20.1 未标），
            // 但它仍是 Entity 上唯一的水位重载，CR 2.0.6 自己也在用；弃用警告留置。
            double liquidHeight = Math.min(player.getFluidHeight(FluidTags.WATER), 1.0);
            direction = direction.scale(liquidHeight * 0.5);
        }
        if (player.isInLava()) {
            direction = direction.scale(0.3);
        }

        Block below = player.level().getBlockState(player.blockPosition().below()).getBlock();
        float slipperiness = below.getFriction();
        float vanillaFriction = Blocks.GRASS_BLOCK.getFriction();
        if (slipperiness > vanillaFriction) {
            double multiplier = vanillaFriction / slipperiness;
            direction = direction.scale(multiplier * multiplier);
        }
        return direction;
    }

    private static void publish(LocalPlayer player, Vec3 direction) {
        RollEffect.Visuals visuals = new RollEffect.Visuals(
                CombatRollMod.ID + ":roll", RollEffect.Particles.PUFF);
        Platform.networkC2S_Send(
                new Packets.RollPublish(player.getId(), visuals, direction));
        RollEffect.playVisuals(visuals, player, direction);
    }
}
