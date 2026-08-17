package dev.Astra.mod.modules.impl.misc;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.core.impl.CommandManager;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.combat.AntiRegear;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.settings.impl.BindSetting;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

public class AutoRegear extends Module {
    public static AutoRegear INSTANCE;

    public final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));

    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.page.is(Page.General)));
    private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 800.0, 0, 1000, () -> this.page.is(Page.General)).setSuffix("ms"));
    private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1, () -> this.page.is(Page.General)));

    private final BooleanSetting place = this.add(new BooleanSetting("Place", true, () -> this.page.is(Page.General)));
    private final BooleanSetting open = this.add(new BooleanSetting("Open", true, () -> this.page.is(Page.General)));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true, () -> this.page.is(Page.General)));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(Page.General)));
    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 0.0, 6.0, 0.1, () -> this.page.is(Page.General)).setSuffix("m"));
    private final SliderSetting mineRange = this.add(new SliderSetting("MineRange", 2.0, 0.0, 8.0, 0.1, () -> this.page.is(Page.General) && mine.getValue()).setSuffix("m"));
    private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.is(Page.General)));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.is(Page.General)));

    private final BooleanSetting take = this.add(new BooleanSetting("Take", true, () -> this.page.is(Page.Take)));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(Page.Take) && this.take.getValue()).setParent());
    private final BooleanSetting forceMove = this.add(new BooleanSetting("ForceQuickMove", true, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting crystal = this.add(new SliderSetting("Crystal", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting exp = this.add(new SliderSetting("Exp", 128, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting totem = this.add(new SliderSetting("Totem", 8, 0, 36, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting gapple = this.add(new SliderSetting("Gapple", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting obsidian = this.add(new SliderSetting("Obsidian", 128, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting web = this.add(new SliderSetting("Web", 0, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting glowstone = this.add(new SliderSetting("Glowstone", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting anchor = this.add(new SliderSetting("Anchor", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting pearl = this.add(new SliderSetting("Pearl", 32, 0, 64, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting redstone = this.add(new SliderSetting("RedStone", 64, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting bed = this.add(new SliderSetting("Bed", 0, 0, 512, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 0, 0, 8, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting resistance = this.add(new SliderSetting("Resistance", 8, 0, 8, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting strength = this.add(new SliderSetting("Strength", 2, 0, 8, () -> this.page.is(Page.Take) && this.take.getValue() && this.smart.isOpen()));

    public final Timer timeoutTimer = new Timer();
    private final int[] stealCountList = new int[15];
    private final List<BlockPos> openList = new ArrayList<>();
    public BlockPos placePos = null;
    private BlockPos openPos;
    private boolean opend = false;
    private boolean on = false;
    private Vec3d directionVec = null;

    public AutoRegear() {
        super("AutoRegear", Category.Misc);
        this.setChinese("自动补给");
        INSTANCE = this;
    }

    private int findShulker() {
        if (this.inventory.getValue()) {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof ShulkerBoxBlock) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof ShulkerBoxBlock) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void update() {
        stealCountList[0] = (int) (crystal.getValue() - InventoryUtil.getItemCount(Items.END_CRYSTAL));
        stealCountList[1] = (int) (exp.getValue() - InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
        stealCountList[2] = (int) (totem.getValue() - InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING));
        stealCountList[3] = (int) (gapple.getValue() - InventoryUtil.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
        stealCountList[4] = (int) (obsidian.getValue() - InventoryUtil.getItemCount(Blocks.OBSIDIAN.asItem()));
        stealCountList[5] = (int) (web.getValue() - InventoryUtil.getItemCount(Blocks.COBWEB.asItem()));
        stealCountList[6] = (int) (glowstone.getValue() - InventoryUtil.getItemCount(Blocks.GLOWSTONE.asItem()));
        stealCountList[7] = (int) (anchor.getValue() - InventoryUtil.getItemCount(Blocks.RESPAWN_ANCHOR.asItem()));
        stealCountList[8] = (int) (pearl.getValue() - InventoryUtil.getItemCount(Items.ENDER_PEARL));
        stealCountList[9] = (int) (piston.getValue() - InventoryUtil.getItemCount(Blocks.PISTON.asItem()) - InventoryUtil.getItemCount(Blocks.STICKY_PISTON.asItem()));
        stealCountList[10] = (int) (redstone.getValue() - InventoryUtil.getItemCount(Blocks.REDSTONE_BLOCK.asItem()));
        stealCountList[11] = (int) (bed.getValue() - InventoryUtil.getItemCount(BedBlock.class));
        stealCountList[12] = (int) (speed.getValue() - InventoryUtil.getPotionCount(StatusEffects.SPEED.value()));
        stealCountList[13] = (int) (resistance.getValue() - InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value()));
        stealCountList[14] = (int) (strength.getValue() - InventoryUtil.getPotionCount(StatusEffects.STRENGTH.value()));
    }

    private boolean faceVector(Vec3d vec) {
        Astra.ROTATION.lookAt(vec);
        return true;
    }

    private boolean canOpenShulker(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) return false;
        if (state.contains(Properties.FACING)) {
            Direction facing = state.get(Properties.FACING);
            BlockPos openDirPos = pos.offset(facing);
            BlockState openState = mc.world.getBlockState(openDirPos);
            return openState.isAir() || openState.getBlock() == Blocks.WATER;
        }
        return mc.world.isAir(pos.up()) || mc.world.getBlockState(pos.up()).getBlock() == Blocks.WATER;
    }

    private boolean tryOpenNearbyShulker() {
        if (!open.getValue()) return false;

        if (placePos != null && mc.player.squaredDistanceTo(placePos.toCenterPos()) <= placeRange.getValue() * placeRange.getValue()
                && mc.world.getBlockState(placePos).getBlock() instanceof ShulkerBoxBlock) {
            if (!openList.contains(placePos) && canOpenShulker(placePos)) {
                openPos = placePos;
                BlockUtil.clickBlock(placePos, BlockUtil.getClickSide(placePos), rotate.getValue());
                return true;
            }
        }

        for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue())) {
            if (openList.contains(pos)) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (!(state.getBlock() instanceof ShulkerBoxBlock)) continue;
            if (canOpenShulker(pos)) {
                openPos = pos;
                BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), rotate.getValue());
                return true;
            }
        }
        return false;
    }

    private void tryMineOpenedShulkers() {
        if (!mine.getValue()) return;
        for (BlockPos pos : openList) {
            if (!(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) {
                openList.remove(pos);
                continue;
            }
            double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
            if (dist <= mineRange.getValue() * mineRange.getValue()) {
                if (detectMining.getValue() && Astra.BREAK.isMining(pos)) continue;
                PacketMine.INSTANCE.mine(pos);
            }
        }
    }

    private void doPlace() {
        BlockPos bestPos = null;
        Direction bestDir = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockUtil.getSphere((float) placeRange.getValue())) {
            double dist = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));

            if (detectMining.getValue() && Astra.BREAK.isMining(pos)) continue;
            if (!BlockUtil.canPlace(pos, placeRange.getValue(), true)) continue;

            Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            if (!mc.world.getOtherEntities(null, box, e -> !(e instanceof ItemEntity)).isEmpty()) continue;

            Direction validDir = null;
            for (Direction dir : Direction.values()) {
                BlockPos solidPos = pos.offset(dir);
                BlockPos airPos = pos.offset(dir.getOpposite());
                BlockState solidState = mc.world.getBlockState(solidPos);
                if (!solidState.isSolid() || solidState.getBlock() instanceof ShulkerBoxBlock) continue;
                BlockState airState = mc.world.getBlockState(airPos);
                if (!airState.isAir() && airState.getBlock() != Blocks.WATER) continue;
                validDir = dir;
                break;
            }
            if (validDir == null) continue;

            if (dist < bestDistance) {
                bestDistance = dist;
                bestPos = pos;
                bestDir = validDir;
            }
        }

        if (bestPos == null) {
            CommandManager.sendMessageId("§4No suitable placement position found.", this.hashCode() - 1);
            return;
        }

        int shulkerSlot = findShulker();
        if (shulkerSlot == -1) {
            CommandManager.sendMessageId("§4No shulker box found.", this.hashCode() - 1);
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;

        if (inventory.getValue()) {
            if (shulkerSlot < 9) {
                InventoryUtil.switchToSlot(shulkerSlot);
            } else {
                InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
            }
        } else {
            InventoryUtil.switchToSlot(shulkerSlot);
        }

        ItemStack mainHand = mc.player.getInventory().getMainHandStack();
        if (!(mainHand.getItem() instanceof BlockItem) || !(((BlockItem) mainHand.getItem()).getBlock() instanceof ShulkerBoxBlock)) {
            if (inventory.getValue() && shulkerSlot >= 9) {
                InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
                EntityUtil.syncInventory();
            } else {
                InventoryUtil.switchToSlot(oldSlot);
            }
            CommandManager.sendMessageId("§cFailed to hold shulker box, aborting.", this.hashCode() - 1);
            return;
        }

        BlockPos targetBlock = bestPos.offset(bestDir);
        Direction clickFace = bestDir.getOpposite();
        boolean packet = AntiCheat.INSTANCE.packetPlace.getValue();

        if (rotate.getValue()) {
            Vec3d targetVec = new Vec3d(targetBlock.getX() + 0.5 + clickFace.getOffsetX() * 0.5,
                    targetBlock.getY() + 0.5 + clickFace.getOffsetY() * 0.5,
                    targetBlock.getZ() + 0.5 + clickFace.getOffsetZ() * 0.5);
            if (!faceVector(targetVec)) {
                if (inventory.getValue() && shulkerSlot >= 9) {
                    InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
                    EntityUtil.syncInventory();
                } else {
                    InventoryUtil.switchToSlot(oldSlot);
                }
                return;
            }
        }

        BlockUtil.clickBlock(targetBlock, clickFace, rotate.getValue(), Hand.MAIN_HAND, packet);

        if (AntiRegear.INSTANCE != null) {
            AntiRegear.INSTANCE.addIgnored(bestPos);
        }

        if (inventory.getValue() && shulkerSlot >= 9) {
            InventoryUtil.inventorySwap(shulkerSlot, oldSlot);
            EntityUtil.syncInventory();
        } else {
            InventoryUtil.switchToSlot(oldSlot);
        }

        if (rotate.getValue()) {
            Astra.ROTATION.snapBack();
        }

        this.placePos = bestPos;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;

        if (smart.getValue()) update();

        if (placeKey.isPressed() && mc.currentScreen == null) {
            if (!on) {
                opend = false;
                openPos = null;
                timeoutTimer.reset();
                placePos = null;
                if (open.getValue() && tryOpenNearbyShulker()) {
                } else if (place.getValue()) {
                    doPlace();
                }
            }
            on = true;
        } else {
            on = false;
        }

        openList.removeIf(pos -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));

        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            if (opend) {
                opend = false;
                if (openPos != null && !openList.contains(openPos)) {
                    openList.add(openPos);
                }
                openPos = null;
                if (autoDisable.getValue()) timeoutTimer.reset();
            }

            if (open.getValue()) {
                tryOpenNearbyShulker();
            }
            if (mine.getValue()) {
                tryMineOpenedShulkers();
            }

            if (autoDisable.getValue()) {
                if (timeoutTimer.passed(disableTime.getValueInt())) {
                    disable();
                    return;
                }
            }
            return;
        }

        opend = true;
        if (openPos != null && !openList.contains(openPos)) {
            openList.add(openPos);
        }
        if (autoDisable.getValue()) {
            timeoutTimer.reset();
        }

        if (!take.getValue()) {
            return;
        }

        boolean took = false;
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (screenHandler instanceof ShulkerBoxScreenHandler) {
            ShulkerBoxScreenHandler shulker = (ShulkerBoxScreenHandler) screenHandler;
            for (Slot slot : shulker.slots) {
                if (slot.id >= 27 || slot.getStack().isEmpty()) continue;
                Type type = needSteal(slot.getStack());
                if (!smart.getValue() || type == Type.QuickMove || (type == Type.Stack && forceMove.getValue())) {
                    mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                    took = true;
                } else if (type == Type.Stack) {
                    for (int slot1 = 0; slot1 < 36; ++slot1) {
                        ItemStack stack = mc.player.getInventory().getStack(slot1);
                        if (stack.isEmpty() || !stack.isStackable() || stack.getItem() != slot.getStack().getItem() || stack.getCount() >= stack.getMaxCount()) continue;
                        int i = (slot1 < 9 ? slot1 + 36 : slot1) + 18;
                        mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(shulker.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        took = true;
                        break;
                    }
                }
            }
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (directionVec != null && rotate.getValue()) {
            event.setTarget(directionVec, 1.0f, 100.0f);
        }
    }

    private Type needSteal(ItemStack i) {
        if (i.getItem().equals(Items.END_CRYSTAL) && stealCountList[0] > 0) {
            stealCountList[0] -= i.getCount();
            return stealCountList[0] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.EXPERIENCE_BOTTLE) && stealCountList[1] > 0) {
            stealCountList[1] -= i.getCount();
            return stealCountList[1] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.TOTEM_OF_UNDYING) && stealCountList[2] > 0) {
            stealCountList[2] -= i.getCount();
            return stealCountList[2] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && stealCountList[3] > 0) {
            stealCountList[3] -= i.getCount();
            return stealCountList[3] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Blocks.OBSIDIAN.asItem()) && stealCountList[4] > 0) {
            stealCountList[4] -= i.getCount();
            return stealCountList[4] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Blocks.COBWEB.asItem()) && stealCountList[5] > 0) {
            stealCountList[5] -= i.getCount();
            return stealCountList[5] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Blocks.GLOWSTONE.asItem()) && stealCountList[6] > 0) {
            stealCountList[6] -= i.getCount();
            return stealCountList[6] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem()) && stealCountList[7] > 0) {
            stealCountList[7] -= i.getCount();
            return stealCountList[7] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.ENDER_PEARL) && stealCountList[8] > 0) {
            stealCountList[8] -= i.getCount();
            return stealCountList[8] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem) i.getItem()).getBlock() instanceof PistonBlock && stealCountList[9] > 0) {
            stealCountList[9] -= i.getCount();
            return stealCountList[9] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem()) && stealCountList[10] > 0) {
            stealCountList[10] -= i.getCount();
            return stealCountList[10] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem) i.getItem()).getBlock() instanceof BedBlock && stealCountList[11] > 0) {
            stealCountList[11] -= i.getCount();
            return stealCountList[11] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() == Items.SPLASH_POTION) {
            PotionContentsComponent potion = i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potion.getEffects()) {
                if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                    if (stealCountList[12] <= 0) continue;
                    stealCountList[12] -= i.getCount();
                    return stealCountList[12] < 0 ? Type.Stack : Type.QuickMove;
                }
                if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                    if (stealCountList[13] <= 0) continue;
                    stealCountList[13] -= i.getCount();
                    return stealCountList[13] < 0 ? Type.Stack : Type.QuickMove;
                }
                if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                    if (stealCountList[14] <= 0) continue;
                    stealCountList[14] -= i.getCount();
                    return stealCountList[14] < 0 ? Type.Stack : Type.QuickMove;
                }
            }
        }
        return Type.None;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        opend = false;
        openPos = null;
        timeoutTimer.reset();
        placePos = null;
        openList.clear();

        if (open.getValue()) {
            boolean opened = tryOpenNearbyShulker();
            if (opened) return;
        }
        if (place.getValue()) {
            doPlace();
        }
    }

    @Override
    public void onDisable() {
        opend = false;
        directionVec = null;
    }

    private enum Type { None, Stack, QuickMove }
    public enum Page { General, Take }
}