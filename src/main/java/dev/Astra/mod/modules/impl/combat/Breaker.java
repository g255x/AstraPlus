package dev.Astra.mod.modules.impl.combat;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.world.BlockPosX;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Breaker extends Module {
    public static Breaker INSTANCE;
    public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    public final SliderSetting range = this.add(new SliderSetting("Range", 4.5, 0.0, 8.0, 0.1).setSuffix("m"));
    private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
    private final BooleanSetting head = this.add(new BooleanSetting("Head", false));
    private final BooleanSetting face = this.add(new BooleanSetting("Face", false));
    private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
    private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
    private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", false));
    public final BooleanSetting avoidSelf = this.add(new BooleanSetting("AvoidSelf", true));
    // 🔧 新增 ForceDouble 开关
    private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));

    public static final List<Block> hard;

    public Breaker() {
        super("Breaker", Module.Category.Combat);
        this.setChinese("自动挖掘");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (CevBreaker.INSTANCE.isOn() && this.cevPause.getValue()) {
            return;
        }
        if (AntiCrawl.INSTANCE.work) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
        if (player == null) {
            return;
        }
        this.doBreak(player);
    }

    private void doBreak(PlayerEntity player) {
        BlockPos pos = EntityUtil.getEntityPos((Entity) player, true);
        boolean avoid = this.avoidSelf.getValue();

        // 🛑 ForceDouble 保护：如果双挖目标未完成，则不再发起新挖掘
        if (this.forceDouble.getValue() &&
                PacketMine.getBreakPos() != null &&
                !PacketMine.getBreakPos().equals(PacketMine.secondPos) &&
                PacketMine.secondPos != null &&
                !Breaker.mc.world.isAir(PacketMine.secondPos)) {
            return;
        }

        double[] dArray = new double[]{-0.8, 0.3, 1.1};
        double[] xzOffset = new double[]{0.3, -0.3};

        // 检查是否正在挖掘同一个目标（包括双挖）
        for (PlayerEntity playerEntity : CombatUtil.getEnemies(this.targetRange.getValue())) {
            for (double y : dArray) {
                for (double x : xzOffset) {
                    for (double z : xzOffset) {
                        BlockPosX offsetPos = new BlockPosX(playerEntity.getX() + x, playerEntity.getY() + y, playerEntity.getZ() + z);
                        if (this.canBreak(offsetPos, Math.abs(y - 0.3) < 0.01) &&
                                (offsetPos.equals(PacketMine.getBreakPos()) || offsetPos.equals(PacketMine.secondPos))) {
                            return; // 已经在挖，不重复提交
                        }
                    }
                }
            }
        }

        // 依次尝试各个身体部位的高度
        ArrayList<Float> yList = new ArrayList<>();
        if (this.down.getValue()) yList.add(-0.8f);
        if (this.head.getValue()) yList.add(2.3f);
        if (this.burrow.getValue()) yList.add(0.3f);
        if (this.face.getValue()) yList.add(1.1f);

        // 单轴偏移
        for (double y : yList) {
            boolean isBurrow = Math.abs(y - 0.3) < 0.01;
            for (double offset : xzOffset) {
                BlockPosX offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
                if (this.canBreak(offsetPos, isBurrow)) {
                    if (avoid && isSelfBlock(offsetPos)) continue;
                    PacketMine.INSTANCE.mine(offsetPos);
                    return;
                }
            }
        }
        // 双轴偏移
        for (double y : yList) {
            boolean isBurrow = Math.abs(y - 0.3) < 0.01;
            for (double offset : xzOffset) {
                for (double offset2 : xzOffset) {
                    BlockPosX offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
                    if (this.canBreak(offsetPos, isBurrow)) {
                        if (avoid && isSelfBlock(offsetPos)) continue;
                        PacketMine.INSTANCE.mine(offsetPos);
                        return;
                    }
                }
            }
        }

        // Surround 模式
        if (this.surround.getValue()) {
            // 先检查是否有空着的 surround 方块（可能是水晶放置位），如果有则退出不挖
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockPos surroundPos = pos.offset(direction);
                if (Math.sqrt(Breaker.mc.player.getEyePos().squaredDistanceTo(surroundPos.toCenterPos())) > this.range.getValue()) continue;
                if (!Breaker.mc.world.isAir(surroundPos) && !surroundPos.equals(PacketMine.getBreakPos()) && !surroundPos.equals(PacketMine.secondPos)) continue;
                if (!this.canPlaceCrystal(surroundPos, false)) continue;
                return; // 该位置可放水晶，不挖
            }

            ArrayList<BlockPos> candidates = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockPos checkPos = pos.offset(direction);
                if (Math.sqrt(Breaker.mc.player.getEyePos().squaredDistanceTo(checkPos.toCenterPos())) > this.range.getValue()) continue;
                if (this.canBreak(checkPos, false) && this.canPlaceCrystal(checkPos, true) && !this.isSurroundPos(checkPos)) {
                    if (avoid && isSelfBlock(checkPos)) continue;
                    candidates.add(checkPos);
                }
            }

            if (!candidates.isEmpty()) {
                PacketMine.INSTANCE.mine(candidates.stream().min(Comparator.comparingDouble(e -> e.getSquaredDistance((Position) Breaker.mc.player.getEyePos()))).get());
            } else {
                candidates.clear();
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.UP || direction == Direction.DOWN) continue;
                    BlockPos checkPos = pos.offset(direction);
                    if (Math.sqrt(Breaker.mc.player.getEyePos().squaredDistanceTo(checkPos.toCenterPos())) > this.range.getValue()) continue;
                    if (this.canBreak(checkPos, false) && this.canPlaceCrystal(checkPos, false)) {
                        if (avoid && isSelfBlock(checkPos)) continue;
                        candidates.add(checkPos);
                    }
                }
                if (!candidates.isEmpty()) {
                    PacketMine.INSTANCE.mine(candidates.stream().min(Comparator.comparingDouble(e -> e.getSquaredDistance((Position) Breaker.mc.player.getEyePos()))).get());
                }
            }
        }
    }

    /**
     * 判断指定方块是否属于玩家自身的 Down / Burrow / Surround
     */
    private boolean isSelfBlock(BlockPos pos) {
        BlockPos selfPos = EntityUtil.getPlayerPos(true);
        if (pos.equals(selfPos)) return true;
        if (pos.equals(selfPos.down())) return true;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            if (pos.equals(selfPos.offset(dir))) return true;
        }
        return false;
    }

    private boolean isSurroundPos(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) continue;
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block) &&
                BlockUtil.noEntityBlockCrystal(boost, true, true) && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
    }

    /**
     * 增强的 canBreak，兼容双挖和自动切换
     */
    private boolean canBreak(BlockPos pos, boolean isBurrow) {
        // 距离检查
        if (Breaker.mc.player.getEyePos().distanceTo(pos.toCenterPos()) > PacketMine.INSTANCE.range.getValue()) {
            return false;
        }

        // 可点击性检查（除非正在被双挖）
        if (!pos.equals(PacketMine.getBreakPos()) && !pos.equals(PacketMine.secondPos) && BlockUtil.getClickSideStrict(pos) == null) {
            return false;
        }

        Block block = BlockUtil.getBlock(pos);
        if (block == null || block == Blocks.BEDROCK || block == Blocks.BARRIER ||
                Breaker.mc.world.isAir(pos) || Breaker.mc.world.getBlockState(pos).isLiquid()) {
            return false;
        }

        // Burrow 必须为硬方块
        if (isBurrow && !hard.contains(block)) {
            return false;
        }

        // 🔧 适配自动切换变量：硬方块需要镐子（除非自动切换开启）
        if (hard.contains(block) &&
                !(Breaker.mc.player.getMainHandStack().getItem() instanceof PickaxeItem) &&
                !PacketMine.INSTANCE.autoSwitch.getValue()) {
            return false;
        }

        return true;
    }

    static {
        hard = Arrays.asList(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN,
                Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL);
    }
}