package com.e33epus.bcp.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 三个开关，默认全开。修 bug 的 mod 每多一个开关就多一份「开着却没生效」的排查面，
 * 所以只留「能不能关掉某条修正」，不提供调数值的旋钮。
 */
@Config(name = "bettercombatplus")
public class BcpConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean roll_during_attack = true;

    @ConfigEntry.Gui.Tooltip
    public boolean offhand_while_two_handed = true;

    @ConfigEntry.Gui.Tooltip
    public boolean right_click_in_recovery = true;
}
