package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.AmbientOcclusionEvent;
import dev.Astra.api.events.impl.ChunkOcclusionEvent;
import dev.Astra.api.events.impl.RenderBlockEntityEvent;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class Xray extends Module {
    public static Xray INSTANCE;
    private static final ThreadLocal<BlockPos.Mutable> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

    // ----- 四个类别开关（使用 .getValue() 取值） -----
    private final BooleanSetting ores = this.add(new BooleanSetting("Ores", true));
    private final BooleanSetting mineralBlocks = this.add(new BooleanSetting("Mineral Blocks", true));
    private final BooleanSetting storageBlocks = this.add(new BooleanSetting("Storage", true));
    private final BooleanSetting liquids = this.add(new BooleanSetting("Liquids", true));

    public Xray() {
        super("Xray", Module.Category.Render);
        this.setChinese("矿物透视");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        mc.worldRenderer.reload();
    }

    // ----- 判断是否应该渲染该方块（Xray 开启时） -----
    public boolean shouldRender(BlockState state) {
        if (!isOn()) return false;
        Block block = state.getBlock();

        if (ores.getValue() && isOre(block)) return true;
        if (mineralBlocks.getValue() && isMineralBlock(block)) return true;
        if (storageBlocks.getValue() && isStorageBlock(block)) return true;
        if (liquids.getValue() && isLiquid(block)) return true;

        return false;
    }

    // ----- 各分类判断（纯字符串，无标签/注册表） -----
    private boolean isOre(Block block) {
        String name = block.getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", "");
        return name.contains("_ore") || name.equals("ancient_debris") || name.equals("nether_quartz_ore");
    }

    private boolean isMineralBlock(Block block) {
        String name = block.getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", "");
        if (!name.endsWith("_block")) return false;
        String[] minerals = {"diamond", "gold", "iron", "netherite", "quartz", "redstone", "lapis", "coal", "emerald"};
        for (String m : minerals) {
            if (name.contains(m)) return true;
        }
        return false;
    }

    private boolean isStorageBlock(Block block) {
        String name = block.getTranslationKey().replace("block.minecraft.", "").replace("item.minecraft.", "");
        return name.contains("chest") ||
                name.contains("shulker") ||
                name.contains("ender_chest") ||
                name.contains("barrel") ||
                name.contains("hopper") ||
                name.contains("dropper") ||
                name.contains("dispenser") ||
                name.contains("furnace") ||
                name.contains("smoker") ||
                name.contains("blast_furnace");
    }

    private boolean isLiquid(Block block) {
        return block instanceof FluidBlock;
    }

    // ----- 原有工具方法（保持兼容） -----
    public boolean isBlocked(Block block) {
        return isOn() && !shouldRender(block.getDefaultState());
    }

    public static boolean shouldBlock(BlockState state) {
        return INSTANCE.isOn() && !INSTANCE.shouldRender(state);
    }

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
        if (!returns) {
            BlockPos adjPos = pos.offset(facing);
            BlockState adjState = view.getBlockState(adjPos);
            return adjState.getCullingFace(view, adjPos, facing.getOpposite()) != VoxelShapes.fullCube() ||
                    adjState.getBlock() != state.getBlock() ||
                    isExposed(adjPos);
        }
        return returns;
    }

    private static boolean isExposed(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (!mc.world.getBlockState(EXPOSED_POS.get().set(pos, dir)).isOpaque()) {
                return true;
            }
        }
        return false;
    }

    // ----- 事件监听（兼容旧逻辑，新 mixin 已接管主要渲染控制） -----
    @EventListener
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        if (isBlocked(event.blockEntity.getCachedState().getBlock())) {
            event.cancel();
        }
    }

    @EventListener
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventListener
    private void onAmbientOcclusion(AmbientOcclusionEvent event) {
        event.lightLevel = 1.0f;
    }
}
