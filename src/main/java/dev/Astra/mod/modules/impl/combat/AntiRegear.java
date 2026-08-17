package dev.Astra.mod.modules.impl.combat;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.OpenScreenEvent;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.misc.AutoRegear;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class AntiRegear extends Module {
    public static AntiRegear INSTANCE;

    private final SliderSetting range = this.add(new SliderSetting("Range", 4.5, 1.0, 8.0, 0.1).setSuffix("m"));
    private final BooleanSetting pauseRegear = this.add(new BooleanSetting("PauseRegear", true));

    private final List<BlockPos> processed = new ArrayList<>();
    private final List<BlockPos> ignored = new ArrayList<>();
    private enum State { IDLE, OPENING, PROCESSING }
    private State state = State.IDLE;
    private BlockPos currentTarget = null;
    private int progress = 0;
    private long openTime = 0;

    public AntiRegear() {
        super("AntiRegear", Category.Combat);
        this.setChinese("反补给");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        processed.clear();
        ignored.clear();
        state = State.IDLE;
        currentTarget = null;
        progress = 0;
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            mc.player.closeScreen();
        }
    }

    public void addIgnored(BlockPos pos) {
        if (pos != null && !ignored.contains(pos)) {
            ignored.add(pos);
        }
    }

    private void cleanIgnored() {
        ignored.removeIf(pos -> {
            BlockState state = mc.world.getBlockState(pos);
            return !(state.getBlock() instanceof ShulkerBoxBlock);
        });
    }

    @EventListener(priority = 1000)
    public void onOpenScreen(OpenScreenEvent event) {
        if (state == State.OPENING || state == State.PROCESSING) {
            if (event.screen instanceof ShulkerBoxScreen) {
                event.cancel();
                event.screen = null;
                if (mc.currentScreen instanceof ShulkerBoxScreen) {
                    mc.currentScreen.close();
                }
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        if (pauseRegear.getValue() && AutoRegear.INSTANCE != null && AutoRegear.INSTANCE.isOn()) return;

        cleanIgnored();

        switch (state) {
            case IDLE:
                BlockPos target = findNearestUnprocessedShulker();
                if (target != null) {
                    currentTarget = target;
                    openShulker(target);
                    state = State.OPENING;
                    openTime = System.currentTimeMillis();
                }
                break;

            case OPENING:
                if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
                    state = State.PROCESSING;
                    progress = 0;
                } else if (System.currentTimeMillis() - openTime > 2000) {
                    if (currentTarget != null) {
                        processed.add(currentTarget);
                    }
                    state = State.IDLE;
                    currentTarget = null;
                }
                break;

            case PROCESSING:
                boolean done = processShulker();
                if (done) {
                    if (currentTarget != null) {
                        processed.add(currentTarget);
                    }
                    mc.player.closeScreen();
                    state = State.IDLE;
                    currentTarget = null;
                    progress = 0;
                }
                break;
        }
    }

    private BlockPos findNearestUnprocessedShulker() {
        BlockPos playerPos = mc.player.getBlockPos();
        double rangeSq = range.getValue() * range.getValue();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1.0f)) {
            if (processed.contains(pos)) continue;
            if (ignored.contains(pos)) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (!(state.getBlock() instanceof ShulkerBoxBlock)) continue;
            if (!canOpen(pos)) continue;
            double dist = pos.getSquaredDistance(playerPos);
            if (dist <= rangeSq && dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }
        return best;
    }

    private boolean canOpen(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) return false;
        if (state.contains(Properties.FACING)) {
            Direction facing = state.get(Properties.FACING);
            BlockPos openDir = pos.offset(facing);
            BlockState openState = mc.world.getBlockState(openDir);
            return openState.isAir() || openState.getBlock() == Blocks.WATER;
        }
        return mc.world.isAir(pos.up()) || mc.world.getBlockState(pos.up()).getBlock() == Blocks.WATER;
    }

    private void openShulker(BlockPos pos) {
        Direction side = BlockUtil.getClickSide(pos);
        if (side == null) return;
        BlockUtil.clickBlock(pos, side, false, Hand.MAIN_HAND, AntiCheat.INSTANCE.packetPlace.getValue());
    }

    private boolean processShulker() {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            return true;
        }
        ShulkerBoxScreenHandler handler = (ShulkerBoxScreenHandler) mc.player.currentScreenHandler;
        int totalSlots = 27;

        for (int i = progress; i < totalSlots; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) {
                continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            if (!handler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, -999, 0, SlotActionType.PICKUP, mc.player);
            }
            progress = i + 1;
            return false;
        }

        return true;
    }
}