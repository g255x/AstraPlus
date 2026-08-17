/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.modules.impl.player;

import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;

public class AntiEffects
extends Module {
    public static AntiEffects INSTANCE;
    public final BooleanSetting levitation = this.add(new BooleanSetting("Levitation", true));
    public final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", true));

    public AntiEffects() {
        super("AntiEffects", Module.Category.Player);
        this.setChinese("\u53cd\u6548\u679c");
        INSTANCE = this;
    }
}

