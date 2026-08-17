package dev.Astra.mod.modules.impl.combat;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class AntiWeak extends Module {
    private final EnumSetting<SwapMode> swapMode = this.add(new EnumSetting<SwapMode>("SwapMode", SwapMode.Normal));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 200.0, 0, 500).setSuffix("ms"));
    private final BooleanSetting onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
    private final Timer delayTimer = new Timer();
    boolean ignore = false;
    private PlayerInteractEntityC2SPacket lastPacket = null;

    public AntiWeak() {
        super("AntiWeak", Module.Category.Combat);
        this.setChinese("反虚弱");
    }

    @Override
    public String getInfo() {
        return this.swapMode.getValue().name();
    }

    @EventListener(priority=200)
    public void onPacketSend(PacketEvent.Send event) {
        PlayerInteractEntityC2SPacket packet;
        if (AntiWeak.nullCheck()) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (this.ignore) {
            return;
        }
        if (AntiWeak.mc.player.getStatusEffect(StatusEffects.WEAKNESS) == null) {
            return;
        }
        if (AntiWeak.mc.player.getMainHandStack().getItem() instanceof SwordItem) {
            return;
        }
        if (!this.delayTimer.passedMs(this.delay.getValue())) {
            return;
        }
        Packet<?> packet2 = event.getPacket();
        if (packet2 instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
                return;
            }
            this.lastPacket = packet;
            this.delayTimer.reset();
            this.ignore = true;
            this.doAnti();
            this.ignore = false;
            event.cancel();
        }
    }

    private void doAnti() {
        if (this.lastPacket == null) {
            return;
        }
        int strong = this.swapMode.getValue() != SwapMode.Inventory ? InventoryUtil.findClass(SwordItem.class) : InventoryUtil.findClassInventorySlot(SwordItem.class);
        if (strong == -1) {
            return;
        }
        int old = AntiWeak.mc.player.getInventory().selectedSlot;
        if (this.swapMode.getValue() != SwapMode.Inventory) {
            InventoryUtil.switchToSlot(strong);
        } else {
            InventoryUtil.inventorySwap(strong, AntiWeak.mc.player.getInventory().selectedSlot);
        }
        mc.getNetworkHandler().sendPacket((Packet)this.lastPacket);
        if (this.swapMode.getValue() != SwapMode.Inventory) {
            if (this.swapMode.getValue() != SwapMode.Normal) {
                InventoryUtil.switchToSlot(old);
            }
        } else {
            InventoryUtil.inventorySwap(strong, AntiWeak.mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        }
    }

    public static enum SwapMode {
        Normal,
        Silent,
        Inventory;
    }
}