package com.e33epus.bcp;

import com.e33epus.bcp.config.BcpConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Better Combat Plus — 非官方附属，修 Better Combat + Combat Roll 的三处手感缺陷。
 *
 * <p>本 mod 不复制上游任何实现：对 Better Combat（All Rights Reserved）只做运行时互操作，
 * 通过它的公开 API（{@code MinecraftClient_BetterCombat}、{@code WeaponRegistry}）读取状态；
 * 对 Combat Roll（GPL-3.0）只调用其公开 API 并复用它的翻滚网络包与视觉，未搬其代码。
 */
@Mod(BetterCombatPlus.MOD_ID)
public final class BetterCombatPlus {
    public static final String MOD_ID = "bettercombatplus";
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterCombatPlus");

    // mixin 会在渲染线程读它，构造期之后只读，volatile 保证可见性。
    private static volatile BcpConfig config;

    public BetterCombatPlus() {
        AutoConfig.register(BcpConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BcpConfig.class).getConfig();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Forge 1.20.1 没有自动配置界面（ConfigurationScreen 是 1.20.2+），
            // 所以和上游一样借 Cloth Config 生成 - 一行界面代码都不用自己写。
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> AutoConfig.getConfigScreen(BcpConfig.class, parent).get()));
        }

        LOGGER.info("Better Combat Plus loaded (roll={}, offhand={}, recoveryUse={})",
                rollDuringAttack(), offhandWhileTwoHanded(), rightClickInRecovery());
    }

    public static BcpConfig config() {
        return config;
    }

    /** 攻击动作未结束时也能翻滚。默认开。 */
    public static boolean rollDuringAttack() {
        return config == null || config.roll_during_attack;
    }

    /** 主手是双手武器时，副手仍然正常渲染与可用。默认开。 */
    public static boolean offhandWhileTwoHanded() {
        return config == null || config.offhand_while_two_handed;
    }

    /** 后摇期允许右键（举盾），蓄力期仍然让位给攻击。默认开。 */
    public static boolean rightClickInRecovery() {
        return config == null || config.right_click_in_recovery;
    }
}
