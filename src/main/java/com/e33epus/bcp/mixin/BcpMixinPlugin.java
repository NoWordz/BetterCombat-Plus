package com.e33epus.bcp.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Combat Roll 是软依赖：没装它时，引用它类的 mixin 必须整体不应用，
 * 否则类加载期就会 NoClassDefFoundError（而不是等到调用时）。
 *
 * <p>用 Class.forName 而不是 ModList 判断，是为了不依赖 FML 的加载时序。
 */
public final class BcpMixinPlugin implements IMixinConfigPlugin {
    private static final String COMBAT_ROLL_PROBE = "net.combatroll.client.Keybindings";

    private static Boolean combatRollPresent;

    static boolean isCombatRollPresent() {
        if (combatRollPresent == null) {
            try {
                Class.forName(COMBAT_ROLL_PROBE, false, BcpMixinPlugin.class.getClassLoader());
                combatRollPresent = Boolean.TRUE;
            } catch (Throwable ignored) {
                combatRollPresent = Boolean.FALSE;
            }
        }
        return combatRollPresent;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MinecraftRollMixin")) {
            return isCombatRollPresent();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
