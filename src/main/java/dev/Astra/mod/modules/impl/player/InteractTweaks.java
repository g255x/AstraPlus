/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.block.AnvilBlock
 *  net.minecraft.block.Block
 *  net.minecraft.block.ChestBlock
 *  net.minecraft.block.EnderChestBlock
 *  net.minecraft.client.gui.screen.DeathScreen
 *  net.minecraft.item.PickaxeItem
 *  net.minecraft.item.SwordItem
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
 */
package dev.Astra.mod.modules.impl.player;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.asm.accessors.ILivingEntity;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class InteractTweaks
        extends Module {
    public static InteractTweaks INSTANCE;

    private final BooleanSetting noJumpDelay = this.add(new BooleanSetting("NoJumpDelay", true));
    public final BooleanSetting noEntityTrace = this.add(new BooleanSetting("NoEntityTrace", true).setParent());
    public final BooleanSetting onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, this.noEntityTrace::isOpen));
    public final BooleanSetting multiTask = this.add(new BooleanSetting("MultiTask", true));
    public final BooleanSetting respawn = this.add(new BooleanSetting("Respawn", true));
    private final BooleanSetting noDelay = this.add(new BooleanSetting("NoMineDelay", false));
    private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", false));
    private final BooleanSetting reach = this.add(new BooleanSetting("Reach", false));
    public final SliderSetting blockRange = this.add(new SliderSetting("BlockRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
    public final SliderSetting entityRange = this.add(new SliderSetting("EntityRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
    private final SliderSetting delay = this.add(new SliderSetting("UseDelay", 4.0, 0.0, 4.0, 1.0));

    public boolean isActive;

    public InteractTweaks() {
        super("InteractTweaks", Module.Category.Player);
        this.setChinese("\u4ea4\u4e92\u8c03\u6574");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        // 自动重生
        if (this.respawn.getValue() && InteractTweaks.mc.currentScreen instanceof DeathScreen) {
            InteractTweaks.mc.player.requestRespawn();
            mc.setScreen(null);
        }

        // 使用冷却调整
        if (InteractTweaks.mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
            InteractTweaks.mc.itemUseCooldown = 0;
        }

        // 无跳跃冷却
        if (this.noJumpDelay.getValue() && InteractTweaks.mc.player != null) {
            ((ILivingEntity) InteractTweaks.mc.player).setLastJumpCooldown(0);
        }
    }

    @EventListener
    public void onPacket(PacketEvent.Send event) {
        Packet<?> packet;
        if (InteractTweaks.nullCheck() || !this.noInteract.getValue() || !((packet = event.getPacket()) instanceof PlayerInteractBlockC2SPacket)) {
            return;
        }
        PlayerInteractBlockC2SPacket packet2 = (PlayerInteractBlockC2SPacket) packet;
        Block block = InteractTweaks.mc.world.getBlockState(packet2.getBlockHitResult().getBlockPos()).getBlock();
        if (!InteractTweaks.mc.player.isSneaking() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
        }
    }

    @Override
    public void onDisable() {
        this.isActive = false;
    }

    public boolean reach() {
        return this.isOn() && this.reach.getValue();
    }

    public boolean noDelay() {
        return this.isOn() && this.noDelay.getValue();
    }

    public boolean multiTask() {
        return this.isOn() && this.multiTask.getValue();
    }

    public boolean noEntityTrace() {
        if (this.isOff() || !this.noEntityTrace.getValue()) {
            return false;
        }
        if (this.onlyPickaxe.getValue()) {
            return InteractTweaks.mc.player.getMainHandStack().getItem() instanceof PickaxeItem ||
                    InteractTweaks.mc.player.isUsingItem() && !(InteractTweaks.mc.player.getMainHandStack().getItem() instanceof SwordItem);
        }
        return true;
    }
}