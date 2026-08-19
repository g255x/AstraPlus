/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.TotemParticleEvent;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;

import java.awt.*;
import java.util.Random;

public class TotemParticle
extends Module {
    public static TotemParticle INSTANCE;
    public final SliderSetting velocityXZ = this.add(new SliderSetting("VelocityXZ", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
    public final SliderSetting velocityY = this.add(new SliderSetting("VelocityY", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
    final Random random = new Random();
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
    private final ColorSetting color2 = this.add(new ColorSetting("Color2", new Color(0, 0, 0, 255)));

    public TotemParticle() {
        super("TotemParticle", Module.Category.Render);
        this.setChinese("\u81ea\u5b9a\u4e49\u56fe\u817e\u7c92\u5b50");
        INSTANCE = this;
    }

    @EventListener
    public void idk(TotemParticleEvent event) {
        event.cancel();
        event.velocityZ *= this.velocityXZ.getValue() / 100.0;
        event.velocityX *= this.velocityXZ.getValue() / 100.0;
        event.velocityY *= this.velocityY.getValue() / 100.0;
        event.color = ColorUtil.fadeColor(this.color.getValue(), this.color2.getValue(), this.random.nextDouble());
    }
}

