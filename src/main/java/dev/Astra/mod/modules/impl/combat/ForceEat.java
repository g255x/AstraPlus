package dev.Astra.mod.modules.impl.combat;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class ForceEat extends Module {
    public static ForceEat INSTANCE;

    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Both));
    private final EnumSetting<HandMode> choose = this.add(new EnumSetting<>("Choose", HandMode.MainHand));
    private final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true));
    private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", true));

    private boolean active = false;
    private boolean eating = false;
    private int eatProgress = 0;
    private int totalEatTicks = 32;
    private int delay = 0;
    private int prevSlot = -1;
    private int eatingSlot = -1;

    public static boolean switchingSlot = false;
    public static boolean movingOffhand = false;

    public ForceEat() {
        super("ForceEat", Category.Combat);
        this.setChinese("吃苹果");
        INSTANCE = this;
    }

    public boolean isEating() {
        return eating;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        prevSlot = mc.player.getInventory().selectedSlot;
        int appleSlot = findAppleSlot();
        if (appleSlot == -1) {
            disable();
            return;
        }
        if (autoSwitch.getValue() && getHand() == Hand.MAIN_HAND) {
            switchingSlot = true;
            InventoryUtil.switchToSlot(appleSlot);
            switchingSlot = false;
        } else if (getHand() == Hand.OFF_HAND) {
            movingOffhand = true;
            moveAppleToOffhand(appleSlot);
            movingOffhand = false;
        }
        active = false;
        eating = false;
        eatProgress = 0;
        delay = 1;
        eatingSlot = mc.player.getInventory().selectedSlot;
        updateTotalEatTicks();
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            mc.options.useKey.setPressed(false);
        }
        if (nullCheck()) return;
        if (prevSlot != -1 && prevSlot < 9 && autoSwitch.getValue() && getHand() == Hand.MAIN_HAND) {
            switchingSlot = true;
            InventoryUtil.switchToSlot(prevSlot);
            switchingSlot = false;
        }
        active = false;
        eating = false;
        eatProgress = 0;
        delay = 0;
        prevSlot = -1;
        eatingSlot = -1;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!isOn() || nullCheck()) return;

        if (delay > 0) {
            delay--;
            return;
        }

        if (!active && isAppleInHand() && mc.options.useKey.isPressed()) {
            eating = true;
            return;
        }

        if (getHand() == Hand.MAIN_HAND && autoSwitch.getValue() && !isAppleInHand()) {
            int slot = findAppleSlot();
            if (slot != -1) {
                switchingSlot = true;
                InventoryUtil.switchToSlot(slot);
                switchingSlot = false;
                eatingSlot = slot;
                active = false;
                eating = false;
                eatProgress = 0;
            } else {
                disable();
                return;
            }
        }

        if (getHand() == Hand.OFF_HAND && !isAppleInHand()) {
            if (autoSwitch.getValue()) {
                int slot = findAppleSlot();
                if (slot != -1) {
                    movingOffhand = true;
                    moveAppleToOffhand(slot);
                    movingOffhand = false;
                    active = false;
                    eating = false;
                    eatProgress = 0;
                } else {
                    disable();
                    return;
                }
            } else {
                disable();
                return;
            }
        }

        if (!active) {
            mc.options.useKey.setPressed(true);
            startEating();
            active = true;
            eating = true;
            eatProgress = 0;
            return;
        }

        mc.options.useKey.setPressed(true);
        eating = true;
        eatProgress++;

        if (eatProgress >= totalEatTicks) {
            eating = false;
            if (!isAppleInHand()) {
                if (autoSwitch.getValue()) {
                    if (getHand() == Hand.MAIN_HAND) {
                        int nextSlot = findAppleSlot();
                        if (nextSlot != -1) {
                            switchingSlot = true;
                            InventoryUtil.switchToSlot(nextSlot);
                            switchingSlot = false;
                            eatingSlot = nextSlot;
                            eatProgress = 0;
                            startEating();
                            eating = true;
                        } else {
                            disable();
                        }
                    } else {
                        int nextSlot = findAppleSlot();
                        if (nextSlot != -1) {
                            movingOffhand = true;
                            moveAppleToOffhand(nextSlot);
                            movingOffhand = false;
                            eatProgress = 0;
                            startEating();
                            eating = true;
                        } else {
                            disable();
                        }
                    }
                } else {
                    disable();
                }
            } else {
                eatProgress = 0;
                startEating();
                eating = true;
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (!isOn() || nullCheck()) return;
        if (!event.isPost()) return;

        boolean manualEating = !active && isAppleInHand() && mc.options.useKey.isPressed();
        boolean autoEating = active;

        if (!manualEating && !autoEating) {
            eating = false;
            return;
        }

        eating = true;
        mc.options.useKey.setPressed(true);
    }

    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (!isOn() || nullCheck()) return;
        Packet<?> packet = event.getPacket();

        if (noInteract.getValue()) {
            if (packet instanceof PlayerInteractItemC2SPacket && ((PlayerInteractItemC2SPacket) packet).getHand() != getHand()) {
                event.cancel();
            }
            if (packet instanceof PlayerActionC2SPacket) {
                event.cancel();
            }
        }
    }

    private void startEating() {
        mc.itemUseCooldown = 0;
        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(getHand(), id, mc.player.getYaw(), mc.player.getPitch()));
    }

    private void moveAppleToOffhand(int appleSlot) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, appleSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, appleSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private Hand getHand() {
        return this.choose.getValue() == HandMode.OffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    private int findAppleSlot() {
        return switch (mode.getValue()) {
            case GoldenApple -> InventoryUtil.findItem(Items.GOLDEN_APPLE);
            case EnchantedGoldenApple -> InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            case Both -> {
                int slot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
                if (slot == -1) slot = InventoryUtil.findItem(Items.GOLDEN_APPLE);
                yield slot;
            }
        };
    }

    private boolean isAppleInHand() {
        ItemStack stack = this.choose.getValue() == HandMode.OffHand ? mc.player.getOffHandStack() : mc.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        return switch (mode.getValue()) {
            case GoldenApple -> stack.getItem() == Items.GOLDEN_APPLE;
            case EnchantedGoldenApple -> stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            case Both -> stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
        };
    }

    private void updateTotalEatTicks() {
        ItemStack stack = getHand() == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
        totalEatTicks = stack.isEmpty() ? 32 : stack.getMaxUseTime(mc.player);
    }

    public enum HandMode {
        MainHand("MainHand"),
        OffHand("OffHand");
        private final String name;
        HandMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    public enum Mode {
        GoldenApple("Gapple"),
        EnchantedGoldenApple("EnchantedGapple"),
        Both("Both");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
}