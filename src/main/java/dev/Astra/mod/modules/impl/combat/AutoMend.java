package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.events.impl.UpdateRotateEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.enums.SwingSide;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class AutoMend extends Module {
    public static AutoMend INSTANCE;
    public final BooleanSetting rotation = this.add(new BooleanSetting("Rotation", true).setParent());
    private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, this.rotation::isOpen));
    public final BooleanSetting onlyBroken = this.add(new BooleanSetting("OnlyBroken", true));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 2.0, 0.0, 5.0, 0.1).setSuffix("s"));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    public final EnumSetting<SwingSide> interactSwing = this.add(new EnumSetting<SwingSide>("InteractSwing", SwingSide.None));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
    public final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", false));
    private final Timer delayTimer = new Timer();
    boolean lookDown = false;
    int exp = 0;
    private boolean throwing = false;

    public AutoMend() {
        super("AutoMend", Module.Category.Combat);
        this.setChinese("自动经验瓶");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        this.throwing = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.throwing = this.checkThrow();
        if (this.inventory.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        if (this.lookDown && this.isThrow() && this.delayTimer.passed((long)(this.delay.getValue() * 20)) && (!this.onlyGround.getValue() || AutoMend.mc.player.isOnGround())) {
            this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
            if (this.rotation.getValue() && this.instant.getValue()) {
                Astra.ROTATION.snapAt(Astra.ROTATION.rotationYaw, 88.0f);
            }
            this.throwExp();
            if (this.rotation.getValue() && this.instant.getValue()) {
                Astra.ROTATION.snapBack();
            }
        }
        if (this.autoDisable.getValue() && !this.isThrow()) {
            this.disable();
        }
    }

    @Override
    public void onEnable() {
        boolean bl = this.lookDown = !this.rotation.getValue() || this.instant.getValue();
        if (AutoMend.nullCheck()) {
            this.disable();
            return;
        }
        this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public String getInfo() {
        return String.valueOf(this.exp);
    }

    public void throwExp() {
        int newSlot;
        int oldSlot = AutoMend.mc.player.getInventory().selectedSlot;
        if (this.inventory.getValue() && (newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
            InventoryUtil.inventorySwap(newSlot, AutoMend.mc.player.getInventory().selectedSlot);
            AutoMend.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch()));
            EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
            InventoryUtil.inventorySwap(newSlot, AutoMend.mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            this.delayTimer.reset();
        } else {
            newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE);
            if (newSlot != -1) {
                InventoryUtil.switchToSlot(newSlot);
                AutoMend.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch()));
                EntityUtil.swingHand(Hand.MAIN_HAND, this.interactSwing.getValue());
                InventoryUtil.switchToSlot(oldSlot);
                this.delayTimer.reset();
            }
        }
    }

    @EventListener(priority=-200)
    public void RotateEvent(UpdateRotateEvent event) {
        if (!this.rotation.getValue() || this.instant.getValue()) {
            return;
        }
        if (this.isThrow()) {
            event.setPitch(88.0f);
            this.lookDown = true;
        }
    }

    public boolean isThrow() {
        return this.throwing;
    }

    public boolean checkThrow() {
        if (this.isOff()) {
            return false;
        }
        if (AutoMend.mc.currentScreen != null) {
            return false;
        }
        if (this.usingPause.getValue() && AutoMend.mc.player.isUsingItem()) {
            return false;
        }
        if (!(InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1 || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1)) {
            return false;
        }
        if (this.onlyBroken.getValue()) {
            DefaultedList<ItemStack> armors = AutoMend.mc.player.getInventory().armor;
            for (ItemStack armor : armors) {
                if (armor.isEmpty() || EntityUtil.getDamagePercent(armor) >= 100) continue;
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments((ItemStack)armor);
                return enchants.getEnchantments().contains(AutoMend.mc.world.getRegistryManager().get(Enchantments.MENDING.getRegistryRef()).getEntry(Enchantments.MENDING).get());
            }
        } else {
            return true;
        }
        return false;
    }
}