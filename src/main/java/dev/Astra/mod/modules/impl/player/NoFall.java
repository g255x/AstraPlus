/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket$Full
 */
package dev.Astra.mod.modules.impl.player;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.asm.accessors.IPlayerMoveC2SPacket;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.BowBomb;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall
        extends Module {
    private final EnumSetting<NoFallMode> mode = this.add(new EnumSetting<NoFallMode>("Mode", NoFallMode.Packet));
    private final SliderSetting getDistance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));

    public NoFall() {
        super("NoFall", Module.Category.Player);
        this.setChinese("没有摔落伤害");
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (NoFall.nullCheck()) {
            return;
        }
        for (ItemStack is : NoFall.mc.player.getArmorItems()) {
            if (is.getItem() != Items.ELYTRA) continue;
            return;
        }
        if (!this.mode.is(NoFallMode.Packet)) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet2 = (PlayerMoveC2SPacket)packet;
            if (NoFall.mc.player.fallDistance >= (float)this.getDistance.getValue() && !BowBomb.send) {
                ((IPlayerMoveC2SPacket)packet2).setOnGround(true);
            }
        }
    }

    public static enum NoFallMode {
        Packet;
    }
}