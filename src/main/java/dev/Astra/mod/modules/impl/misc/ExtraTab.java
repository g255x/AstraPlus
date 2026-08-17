/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.modules.impl.misc;

import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.SliderSetting;

public class ExtraTab
extends Module {
    public static ExtraTab INSTANCE;
    public final SliderSetting size = this.add(new SliderSetting("Size", 200.0, 80.0, 1000.0, 1.0));
    public final SliderSetting columns = this.add(new SliderSetting("Columns", 20.0, 1.0, 100.0, 1.0));

    public ExtraTab() {
        super("ExtraTab", Module.Category.Misc);
        this.setChinese("玩家列表");
        INSTANCE = this;
    }
}

