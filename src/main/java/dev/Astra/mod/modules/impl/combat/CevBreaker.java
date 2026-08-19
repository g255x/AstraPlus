package dev.Astra.mod.modules.impl.combat;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class CevBreaker extends Module {
    public static CevBreaker INSTANCE;

    // 目标与距离设置
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting mineRange = this.add(new SliderSetting("MineRange", 4.5, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting breakRange = this.add(new SliderSetting("BreakRange", 3.0, 0.0, 8.0, 0.1).setSuffix("m"));

    // 操作延迟
    private final SliderSetting obsidianDelay = this.add(new SliderSetting("ObsidianDelay", 550.0, 0, 600).setSuffix("ms"));
    private final SliderSetting crystalDelay = this.add(new SliderSetting("CrystalDelay", 500.0, 0, 600).setSuffix("ms"));

    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting ground = this.add(new BooleanSetting("Ground", false));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting top = this.add(new BooleanSetting("Top", true));
    private final BooleanSetting bevel = this.add(new BooleanSetting("Bevel", true));

    // 水晶伤害与破坏延迟
    private final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true));
    private final BooleanSetting checkDamage = this.add(new BooleanSetting("DetectProgress", true));
    private final SliderSetting crystalDamage = this.add(new SliderSetting("Progress", 0.8, 0.0, 1.0, 0.01, () -> crystal.isOpen() && checkDamage.getValue()));
    private final BooleanSetting placeRotate = this.add(new BooleanSetting("PlaceRotate", true));
    private final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", 4.0, 0.0, 20.0, 0.1, () -> crystal.isOpen()).setSuffix("hp"));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 150.0, 0, 1000, () -> crystal.isOpen()).setSuffix("ms"));
    private final Timer obsidianPlaceTimer = new Timer();
    private final Timer crystalPlaceTimer = new Timer();
    private final Timer breakTimer = new Timer();

    private PlayerEntity target;
    private BlockPos placedCrystalPos = null; // 记录自己放置的水晶

    public CevBreaker() {
        super("CevBreaker", Module.Category.Combat);
        this.setChinese("自动炸头");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (inventory.getValue() && !EntityUtil.inInventory()) return;
        if (ground.getValue() && !mc.player.isOnGround()) return;
        if (ForceEat.INSTANCE.isEating()) return;

        target = CombatUtil.getClosestEnemy(targetRange.getValue());
        if (target == null) {
            placedCrystalPos = null;
            return;
        }

        // 1. 攻击自己放置的水晶（伤害检查 + 破坏延迟）
        if (crystal.getValue() && placedCrystalPos != null && breakTimer.passedMs(breakDelay.getValueInt())) {
            if (isCrystalAt(placedCrystalPos)) {
                if (mc.player.squaredDistanceTo(placedCrystalPos.getX() + 0.5, placedCrystalPos.getY() + 0.5, placedCrystalPos.getZ() + 0.5) <= breakRange.getValue() * breakRange.getValue()) {
                    float damage = AutoCrystal.INSTANCE.calculateDamage(placedCrystalPos, target, target);
                    if (damage >= minDamage.getValueFloat()) {
                        CombatUtil.attackCrystal(placedCrystalPos, placeRotate.getValue(), true);
                        breakTimer.reset();
                        placedCrystalPos = null;
                    }
                }
            } else {
                placedCrystalPos = null;
            }
        }

        // 2. 确定黑曜石位置
        BlockPos targetPos = EntityUtil.getEntityPos(target);
        BlockPos breakPos = PacketMine.getBreakPos();
        BlockPos obsidianPos = null;
        for (Direction facing : Direction.values()) {
            if (facing == Direction.DOWN) continue;
            if (facing != Direction.UP && !bevel.getValue()) continue;
            if (facing == Direction.UP && !top.getValue()) continue;
            BlockPos pos = targetPos.up(1).offset(facing);
            if (pos.up().toCenterPos().distanceTo(mc.player.getPos()) <= mineRange.getValue()) {
                obsidianPos = pos;
                break;
            }
        }
        if (obsidianPos == null) return;

        Block block = getBlock(obsidianPos);
        boolean isAirAbove = mc.world.isAir(obsidianPos.up());

        // 3. 黑曜石处理
        if (block == Blocks.OBSIDIAN) {
            if (isAirAbove) {
                if (breakPos == null || !breakPos.equals(obsidianPos)) {
                    // 直接开始挖掘，无延迟
                    PacketMine.INSTANCE.mine(obsidianPos);
                }
            }
        } else if (block == Blocks.BEDROCK) {
            // 忽略基岩
        } else if (mc.world.isAir(obsidianPos)) {
            if (BlockUtil.canPlace(obsidianPos)) {
                if (obsidianPlaceTimer.passedMs(obsidianDelay.getValueInt())) {
                    placeObsidian(obsidianPos);
                    obsidianPlaceTimer.reset();
                }
            }
        } else {
            if (isAirAbove) {
                if (breakPos == null || !breakPos.equals(obsidianPos)) {
                    PacketMine.INSTANCE.mine(obsidianPos);
                }
            }
        }

        // 4. 放置水晶（仅在正在挖掘的黑曜石上，且进度达标）
        if (crystal.getValue() && block == Blocks.OBSIDIAN && breakPos != null && breakPos.equals(obsidianPos) && isAirAbove) {
            double currentProgress = PacketMine.INSTANCE.mineTimer.getMs() / PacketMine.INSTANCE.breakFinalTime;
            if (canPlaceCrystal(obsidianPos.up())) {
                boolean shouldPlace = !checkDamage.getValue() || currentProgress >= crystalDamage.getValue();
                if (shouldPlace && crystalPlaceTimer.passedMs(crystalDelay.getValueInt())) {
                    placeCrystal(obsidianPos);
                    crystalPlaceTimer.reset();
                }
            }
        }
    }

    // ========== 辅助方法（不变） ==========
    private boolean canPlaceCrystal(BlockPos crystalPos) {
        BlockPos obsPos = crystalPos.down();
        Block block = getBlock(obsPos);
        if (block != Blocks.BEDROCK && block != Blocks.OBSIDIAN) return false;
        if (BlockUtil.getClickSideStrict(obsPos) == null) return false;
        if (!noEntity(crystalPos) || !noEntity(crystalPos.up())) return false;
        if (ClientSetting.INSTANCE.lowVersion.getValue() && !mc.world.isAir(crystalPos.up())) return false;
        return true;
    }

    private boolean noEntity(BlockPos pos) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (entity instanceof ItemEntity) continue;
            if (entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.ignoreArmorStand.getValue()) continue;
            return false;
        }
        return true;
    }

    private boolean isCrystalAt(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && entity.getBlockPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private void placeCrystal(BlockPos obsidianPos) {
        int crystalSlot = findCrystal();
        if (crystalSlot == -1) return;
        BlockPos crystalPos = obsidianPos.up();
        if (!canPlaceCrystal(crystalPos)) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        doSwap(crystalSlot, crystalSlot);
        BlockUtil.placeCrystal(crystalPos, placeRotate.getValue());
        doSwap(oldSlot, crystalSlot);
        placedCrystalPos = crystalPos;
        breakTimer.reset();
    }

    private void placeObsidian(BlockPos pos) {
        int obsidianSlot = findObsidian();
        if (obsidianSlot == -1) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        doSwap(obsidianSlot, obsidianSlot);
        if (BlockUtil.canPlace(pos)) {
            if (BlockUtil.allowAirPlace()) {
                BlockUtil.airPlace(pos, rotate.getValue());
            } else {
                Direction side = BlockUtil.getPlaceSide(pos);
                if (side != null) {
                    BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate.getValue());
                }
            }
        }
        doSwap(oldSlot, obsidianSlot);
    }

    private int findObsidian() {
        if (inventory.getValue()) return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        else return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private int findCrystal() {
        if (inventory.getValue()) return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        else return InventoryUtil.findItem(Items.END_CRYSTAL);
    }

    private void doSwap(int slot, int inv) {
        if (!inventory.getValue()) InventoryUtil.switchToSlot(slot);
        else InventoryUtil.inventorySwap(inv, mc.player.getInventory().selectedSlot);
    }

    private Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    @Override
    public String getInfo() {
        return target != null ? target.getName().getString() : null;
    }
}