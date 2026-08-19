/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.modules.impl.movement;

import dev.Astra.mod.modules.Module;

public class EntityControl
        extends Module {
    public static EntityControl INSTANCE;

    public EntityControl() {
        super("EntityControl", Module.Category.Movement);
        this.setChinese("无鞍骑行");
        INSTANCE = this;
    }
}