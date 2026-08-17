package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.*;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;

public class NoRender extends Module {
    public static NoRender INSTANCE;
    public final BooleanSetting potionsIcon = this.add(new BooleanSetting("PotionsIcon", false));
    public final BooleanSetting invisible = this.add(new BooleanSetting("Invisible", true));
    public final BooleanSetting antiTitle = this.add(new BooleanSetting("Title", true));
    public final BooleanSetting weather = this.add(new BooleanSetting("Weather", true));
    public final BooleanSetting fog = this.add(new BooleanSetting("Fog", true));
    public final BooleanSetting fireOverlay = this.add(new BooleanSetting("FireOverlay", true));
    public final BooleanSetting waterOverlay = this.add(new BooleanSetting("WaterOverlay", true));
    public final BooleanSetting blockOverlay = this.add(new BooleanSetting("BlockOverlay", true));
    public final BooleanSetting fastItem = this.add(new BooleanSetting("2DItem", false).setParent());
    public final BooleanSetting castShadow = this.add(new BooleanSetting("CastShadow", true, this.fastItem::isOpen));
    public final BooleanSetting renderSidesOfItems = this.add(new BooleanSetting("RenderSidesOfItems", true, this.fastItem::isOpen));
    public final BooleanSetting item = this.add(new BooleanSetting("Items", false));
    public final BooleanSetting armorParts = this.add(new BooleanSetting("ArmorParts", false));
    public final BooleanSetting armorTrim = this.add(new BooleanSetting("ArmorTrim", false));
    public final BooleanSetting fireEntity = this.add(new BooleanSetting("EntityFire", true));
    public final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
    public final BooleanSetting allParticles = this.add(new BooleanSetting("AllParticles", true));
    public final BooleanSetting fireworks = this.add(new BooleanSetting("Fireworks", true));
    public final BooleanSetting explosions = this.add(new BooleanSetting("Explosions", true));
    public final BooleanSetting portal = this.add(new BooleanSetting("Portal", true));
    public final BooleanSetting nausea = this.add(new BooleanSetting("Nausea", true));
    public final BooleanSetting blindness = this.add(new BooleanSetting("Blindness", true));
    public final BooleanSetting guiToast = this.add(new BooleanSetting("GuiToast", false));
    public final BooleanSetting noTerrainScreen = this.add(new BooleanSetting("NoTerrainScreen", false));
    public final BooleanSetting lightsUpdate = this.add(new BooleanSetting("LightsUpdate", false));

    public NoRender() {
        super("NoRender", Module.Category.Render);
        this.setChinese("禁用渲染");
        INSTANCE = this;
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof TitleS2CPacket && this.antiTitle.getValue()) {
            event.setCancelled(true);
        }
    }

    @EventListener
    public void onRender(TickEntityEvent event) {
        if (event.getEntity() instanceof ItemEntity && this.item.getValue()) {
            event.cancel();
        }
    }

    @EventListener
    public void onRender(RenderEntityEvent event) {
        if (event.getEntity() instanceof ItemEntity && this.item.getValue()) {
            event.cancel();
        }
    }

    @EventListener
    public void onParticle(ParticleEvent event) {
        if (this.allParticles.getValue()) {
            event.cancel();
        } else if (this.explosions.getValue() && event.particle instanceof ExplosionLargeParticle) {
            event.cancel();
        } else if (this.fireworks.getValue() && (event.particle instanceof FireworksSparkParticle.FireworkParticle || event.particle instanceof FireworksSparkParticle.Flash)) {
            event.cancel();
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent event) {
        if (NoRender.nullCheck()) return;
        if (this.noTerrainScreen.getValue() && (NoRender.mc.currentScreen instanceof DownloadingTerrainScreen || NoRender.mc.currentScreen instanceof ProgressScreen)) {
            NoRender.mc.currentScreen = null;
        }
    }
}