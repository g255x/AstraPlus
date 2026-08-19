package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.PreRender2DEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.awt.*;

public class Ambience extends Module {
    public static Ambience INSTANCE;
    public final ColorSetting filter = this.add(new ColorSetting("Filter", new Color(16777215, true)).injectBoolean(false));
    public final ColorSetting worldColor = this.add(new ColorSetting("WorldColor", new Color(-1909249, true)).injectBoolean(true));
    public final BooleanSetting customTime = this.add(new BooleanSetting("CustomTime", true).setParent());
    public final SliderSetting time = this.add(new SliderSetting("Time", 6000, 0, 24000, this.customTime::isOpen));
    public final ColorSetting fog = this.add(new ColorSetting("FogColor", new Color(-9147222, true)).injectBoolean(true));
    public final ColorSetting sky = this.add(new ColorSetting("SkyColor", new Color(-5924609, true)).injectBoolean(true));
    public final ColorSetting cloud = this.add(new ColorSetting("CloudColor", new Color(-197380, true)).injectBoolean(false));
    public final ColorSetting dimensionColor = this.add(new ColorSetting("DimensionColor", new Color(-1, true)).injectBoolean(false));
    public final BooleanSetting fogDistance = this.add(new BooleanSetting("FogDistance", true).setParent());
    public final SliderSetting fogStart = this.add(new SliderSetting("FogStart", 50, 0, 1000, this.fogDistance::isOpen));
    public final SliderSetting fogEnd = this.add(new SliderSetting("FogEnd", 150, 0, 1000, this.fogDistance::isOpen));
    public final BooleanSetting fullBright = this.add(new BooleanSetting("FullBright", true));
    public final BooleanSetting forceOverworld = this.add(new BooleanSetting("ForceOverworld", false));
    public final BooleanSetting customLuminance = this.add(new BooleanSetting("CustomLuminance", false).setParent().injectTask(() -> {
        if (!Ambience.nullCheck()) Ambience.mc.worldRenderer.reload();
    }));
    public final SliderSetting luminance = this.add(new SliderSetting("Luminance", 15, 0, 15, this.customLuminance::isOpen).injectTask(() -> {
        if (!Ambience.nullCheck() && this.customLuminance.getValue()) Ambience.mc.worldRenderer.reload();
    }));
    long oldTime;

    public Ambience() {
        super("Ambience", "Custom ambience", Module.Category.Render);
        this.setChinese("自定义环境");
        INSTANCE = this;
    }

    @EventListener
    public void onRender2D(PreRender2DEvent event) {
        if (this.filter.booleanValue) {
            event.drawContext.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), this.filter.getValue().getRGB());
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.customTime.getValue()) {
            Ambience.mc.world.setTimeOfDay((long)this.time.getValue());
        }
    }

    @Override
    public void onEnable() {
        if (Ambience.nullCheck()) return;
        this.oldTime = Ambience.mc.world.getTimeOfDay();
        if (this.customTime.getValue()) {
            Ambience.mc.world.setTimeOfDay((long)this.time.getValue());
        }
    }

    @Override
    public void onDisable() {
        if (Ambience.nullCheck()) return;
        Ambience.mc.world.setTimeOfDay(this.oldTime);
    }

    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.getValue()) {
            this.oldTime = ((WorldTimeUpdateS2CPacket)event.getPacket()).getTime();
            event.cancel();
        }
    }
}