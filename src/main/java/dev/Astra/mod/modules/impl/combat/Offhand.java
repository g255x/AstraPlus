package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.TotemEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.enums.Timing;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Offhand extends Module {
    private final EnumSetting<OffhandItem> item = this.add(new EnumSetting<>("Item", OffhandItem.Totem));
    private final BooleanSetting safe = this.add(new BooleanSetting("Safe", true).setParent());
    private final SliderSetting safeHealth = this.add(new SliderSetting("Health", 0.2, 0.0, 36.0, 0.1, this.safe::isOpen).setSuffix("hp"));
    private final BooleanSetting lethalCrystal = this.add(new BooleanSetting("LethalCrystal", true, this.safe::isOpen));
    private final BooleanSetting gapSwitch = this.add(new BooleanSetting("GapSwitch", false).setParent());
    private final BooleanSetting always = this.add(new BooleanSetting("Always", false, this.gapSwitch::isOpen));
    private final BooleanSetting gapOnTotem = this.add(new BooleanSetting("Gap-Totem", true, this.gapSwitch::isOpen));
    private final BooleanSetting gapOnSword = this.add(new BooleanSetting("Gap-Sword", true, this.gapSwitch::isOpen));
    private final BooleanSetting gapOnPick = this.add(new BooleanSetting("Gap-Pickaxe", true, this.gapSwitch::isOpen));
    private final BooleanSetting mainHandTotem = this.add(new BooleanSetting("MainHandTotem", true).setParent());
    private final SliderSetting slot = this.add(new SliderSetting("Slot", 9, 1, 9, 1, this.mainHandTotem::isOpen));
    private final BooleanSetting forceUpdate = this.add(new BooleanSetting("ForceUpdate", true, this.mainHandTotem::isOpen));
    private final BooleanSetting withOffhand = this.add(new BooleanSetting("WithOffhand", true, this.mainHandTotem::isOpen));
    private final BooleanSetting onlyPlayer = this.add(new BooleanSetting("OnlyPlayer", true, this.mainHandTotem::isOpen));
    private final SliderSetting playerDistance = this.add(new SliderSetting("PlayerDistance", 14.0, 1.0, 30.0, 0.5, () -> this.mainHandTotem.isOpen() && this.onlyPlayer.getValue()).setSuffix("m"));

    // 自动切回原槽位
    private final BooleanSetting autoRestore = this.add(new BooleanSetting("AutoRestore", true, this.mainHandTotem::isOpen));

    private final EnumSetting<SwapMode> swapMode = this.add(new EnumSetting<>("SwapMode", SwapMode.OffhandSwap));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 50.0, 0.0, 500.0, 1.0).setSuffix("ms"));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<>("Timing", Timing.All));
    private final Timer timer = new Timer();

    private int previousMainHandSlot = -1;
    private boolean holdingTotemDueToDanger = false;

    public Offhand() {
        super("Offhand", Module.Category.Combat);
        this.setChinese("副手物品");
    }

    @EventListener
    public void totem(TotemEvent event) {
        if (event.getPlayer() == Offhand.mc.player) {
            if (Offhand.mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                Offhand.mc.player.getInventory().removeStack(0);
            } else if (Offhand.mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                Offhand.mc.player.getInventory().offHand.set(0, ItemStack.EMPTY);
            }
        }
    }

    private boolean lethalCrystal() {
        if (!this.lethalCrystal.getValue()) {
            return false;
        }
        for (Entity entity : Astra.THREAD.getEntities()) {
            if (!(entity instanceof EndCrystalEntity) || !(Offhand.mc.player.distanceTo(entity) <= 12.0f)) continue;
            Vec3d vec3d = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            if (!(AutoCrystal.INSTANCE.calculateDamage(vec3d, (PlayerEntity)Offhand.mc.player, (PlayerEntity)Offhand.mc.player) >= EntityUtil.getHealth((Entity)Offhand.mc.player))) continue;
            return true;
        }
        return false;
    }

    private boolean hasNearbyPlayers() {
        if (!this.onlyPlayer.getValue()) return true;
        double dist = this.playerDistance.getValue();
        for (PlayerEntity player : Offhand.mc.world.getPlayers()) {
            if (player == Offhand.mc.player) continue;
            if (Offhand.mc.player.distanceTo(player) <= dist) return true;
        }
        return false;
    }

    private void ensureTotemInSlot() {
        int targetSlot = this.slot.getValueInt() - 1; // 0-indexed
        if (Offhand.mc.player.getInventory().getStack(targetSlot).getItem() == Items.TOTEM_OF_UNDYING) {
            return; // 已经是图腾，无需操作
        }
        int totemSlot = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
        if (totemSlot == -1) return;
        // 将图腾移动到目标槽位
        switch (this.swapMode.getValue().ordinal()) {
            case 0: // ClickSlot
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, Offhand.mc.player);
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, targetSlot + 36, 0, SlotActionType.PICKUP, Offhand.mc.player);
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, Offhand.mc.player);
                break;
            case 1: // OffhandSwap
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, totemSlot, targetSlot, SlotActionType.SWAP, Offhand.mc.player);
                break;
            case 2: // Pick
                int old = Offhand.mc.player.getInventory().selectedSlot;
                InventoryUtil.switchToSlot(targetSlot);
                mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(totemSlot));
                InventoryUtil.switchToSlot(old);
                break;
        }
        EntityUtil.syncInventory();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (Offhand.nullCheck()) return;

        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) return;
        if (!this.timer.passed(this.delay.getValueInt())) return;
        if (!EntityUtil.inInventory()) return;

        boolean unsafe = ((double) EntityUtil.getHealth(Offhand.mc.player) < this.safeHealth.getValue() || this.lethalCrystal());
        boolean switchMainHandTotem = (Offhand.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) || this.withOffhand.getValue();

        // 1. 无条件填充指定 slot 里的图腾
        if (this.mainHandTotem.getValue()) {
            this.ensureTotemInSlot();
        }

        // 2. 恢复逻辑：如果 autoRestore 开启且危险解除，则切回原槽位
        if (this.autoRestore.getValue() && !unsafe && this.holdingTotemDueToDanger && this.previousMainHandSlot != -1) {
            InventoryUtil.switchToSlot(this.previousMainHandSlot);
            this.previousMainHandSlot = -1;
            this.holdingTotemDueToDanger = false;
            this.timer.reset();
            return;
        }

        // 3. 安全模式：危险时切图腾（主手或副手）
        if (this.safe.getValue() && unsafe) {
            if (this.mainHandTotem.getValue() && switchMainHandTotem && this.hasNearbyPlayers()) {
                int targetSlot = this.slot.getValueInt() - 1;
                if (targetSlot != Offhand.mc.player.getInventory().selectedSlot || this.forceUpdate.getValue()) {
                    if (!this.holdingTotemDueToDanger) {
                        this.previousMainHandSlot = Offhand.mc.player.getInventory().selectedSlot;
                        this.holdingTotemDueToDanger = true;
                    }
                    InventoryUtil.switchToSlot(targetSlot);
                }
            } else {
                this.swap(Items.TOTEM_OF_UNDYING);
                this.timer.reset();
                return;
            }
        }

        // 4. 金苹果右键切换
        if (this.gapSwitch.getValue() && Offhand.mc.options.useKey.isPressed()) {
            boolean canGap = false;
            Item mainItem = Offhand.mc.player.getMainHandStack().getItem();
            if (this.gapOnSword.getValue() && mainItem instanceof SwordItem) canGap = true;
            if (this.always.getValue() && mainItem != Items.GOLDEN_APPLE && mainItem != Items.ENCHANTED_GOLDEN_APPLE) canGap = true;
            if (this.gapOnPick.getValue() && mainItem instanceof PickaxeItem) canGap = true;
            if (this.gapOnTotem.getValue() && mainItem == Items.TOTEM_OF_UNDYING) canGap = true;
            if (canGap) {
                this.swap(Items.GOLDEN_APPLE);
                this.timer.reset();
                return;
            }
        }

        // 5. 默认副手物品
        OffhandItem i = this.item.getValue();
        if (i == OffhandItem.Shield) {
            this.swap(Items.SHIELD);
            this.timer.reset();
        } else if (i == OffhandItem.Chorus) {
            this.swap(Items.CHORUS_FRUIT);
            this.timer.reset();
        } else if (i == OffhandItem.Crystal) {
            this.swap(Items.END_CRYSTAL);
            this.timer.reset();
        } else if (i == OffhandItem.Totem) {
            this.swap(Items.TOTEM_OF_UNDYING);
            this.timer.reset();
        } else if (i == OffhandItem.Gapple) {
            this.swap(Items.GOLDEN_APPLE);
            this.timer.reset();
        }
    }

    private void swap(Item item) {
        int itemSlot = (item == Items.GOLDEN_APPLE) ? this.getGAppleSlot() : this.findItemInventorySlot(item);
        if (itemSlot == -1) return;

        switch (this.swapMode.getValue().ordinal()) {
            case 1: // OffhandSwap
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, itemSlot, 40, SlotActionType.SWAP, Offhand.mc.player);
                EntityUtil.syncInventory();
                break;
            case 2: // Pick
                mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN, 0));
                mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
                break;
            case 0: // ClickSlot
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, Offhand.mc.player);
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, Offhand.mc.player);
                Offhand.mc.interactionManager.clickSlot(Offhand.mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, Offhand.mc.player);
                EntityUtil.syncInventory();
                break;
        }
    }

    private int getGAppleSlot() {
        return this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1 ? this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) : this.findItemInventorySlot(Items.GOLDEN_APPLE);
    }

    @Override
    public String getInfo() {
        return this.item.getValue().name();
    }

    public int findItemInventorySlot(Item item) {
        if (Offhand.mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE && item == Items.GOLDEN_APPLE) return -1;
        if (Offhand.mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE)) return -1;
        if (item == Offhand.mc.player.getOffHandStack().getItem()) return -1;

        switch (this.swapMode.getValue().ordinal()) {
            case 2: // Pick
                for (int i = 9; i < Offhand.mc.player.getInventory().size() + 1; i++) {
                    if (Offhand.mc.player.getInventory().getStack(i).getItem() != item) continue;
                    return i;
                }
                for (int i = 0; i < 9; i++) {
                    if (Offhand.mc.player.getInventory().getStack(i).getItem() != item) continue;
                    return i;
                }
                break;
            case 0:
            case 1:
                for (int i = 44; i >= 0; i--) {
                    if (Offhand.mc.player.getInventory().getStack(i).getItem() != item) continue;
                    return i < 9 ? i + 36 : i;
                }
                break;
        }
        return -1;
    }

    public static enum OffhandItem {
        None,
        Totem,
        Crystal,
        Gapple,
        Shield,
        Chorus;
    }

    public static enum SwapMode {
        ClickSlot,
        OffhandSwap,
        Pick;
    }
}