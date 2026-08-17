package dev.Astra.api.utils.player;

import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

public class InventoryUtil implements Wrapper {
    private static int lastSlot = -1;
    private static int lastSelect = -1;

    public static void inventorySwap(int slot, int selectedSlot) {
        if (slot == lastSlot) {
            switchToSlot(lastSelect);
            lastSlot = -1;
            lastSelect = -1;
            return;
        }
        if (slot - 36 == selectedSlot) return;
        if (!EntityUtil.inInventory()) return;

        if (AntiCheat.INSTANCE.invSwapBypass.getValue()) {
            if (slot - 36 >= 0) {
                lastSlot = slot;
                lastSelect = selectedSlot;
                switchToSlot(slot - 36);
            }
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, mc.player);
            mc.player.getInventory().updateItems();
        }
    }

    public static void switchToSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static int findItem(Item input) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == input) return i;
        }
        return -1;
    }

    public static int getFood() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).contains(DataComponentTypes.FOOD)) return i;
        }
        return -1;
    }

    public static int getPotionCount(StatusEffect targetEffect) {
        int count = 0;
        for (int i = 35; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potion == null) continue;
            for (StatusEffectInstance effect : potion.getEffects()) {
                if (effect.getEffectType().value() != targetEffect) continue;
                count += stack.getCount();
                break;
            }
        }
        return count;
    }

    public static int getItemCount(Class<?> clazz) {
        int count = 0;
        for (ItemStack stack : getInventoryAndHotbarSlots().values()) {
            if (!(stack.getItem() instanceof BlockItem) || !clazz.isInstance(((BlockItem) stack.getItem()).getBlock())) continue;
            count += stack.getCount();
        }
        return count;
    }

    public static int getItemCount(Item item) {
        int count = 0;
        for (ItemStack stack : getInventoryAndHotbarSlots().values()) {
            if (stack.getItem() == item) count += stack.getCount();
        }
        if (mc.player.getOffHandStack().getItem() == item) count += mc.player.getOffHandStack().getCount();
        return count;
    }

    public static int findClass(Class<?> clazz) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (clazz.isInstance(item)) return i;
            if (item instanceof BlockItem && clazz.isInstance(((BlockItem) item).getBlock())) return i;
        }
        return -1;
    }

    public static int findClassInventorySlot(Class<?> clazz) {
        boolean fromZero = AntiCheat.INSTANCE.priorHotbar.getValue();
        for (int i = fromZero ? 0 : 35; fromZero ? i < 36 : i >= 0; i += fromZero ? 1 : -1) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (clazz.isInstance(item) || (item instanceof BlockItem && clazz.isInstance(((BlockItem) item).getBlock()))) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static int findBlock(Block blockIn) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() == blockIn) return i;
        }
        return -1;
    }

    public static int findUnBlock() {
        for (int i = 0; i < 9; i++) {
            if (!(mc.player.getInventory().getStack(i).getItem() instanceof BlockItem)) return i;
        }
        return -1;
    }

    public static int findBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof BlockItem)) continue;
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (BlockUtil.isClickable(block) || block == Blocks.COBWEB) continue;
            return i;
        }
        return -1;
    }

    public static int findBlockInventorySlot(Block block) {
        return findItemInventorySlot(block.asItem());
    }

    public static int findItemInventorySlot(Item item) {
        boolean fromZero = AntiCheat.INSTANCE.priorHotbar.getValue();
        for (int i = fromZero ? 0 : 35; fromZero ? i < 36 : i >= 0; i += fromZero ? 1 : -1) {
            if (mc.player.getInventory().getStack(i).getItem() != item) continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    public static int findItemInventorySlotFromZero(Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() != item) continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
        Map<Integer, ItemStack> map = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            map.put(i, mc.player.getInventory().getStack(i));
        }
        return map;
    }
}