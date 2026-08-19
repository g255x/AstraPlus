package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PhaseESP extends Module {
    public static PhaseESP INSTANCE;
    private final SliderSetting getDistance = this.add(new SliderSetting("Distance", 0.1, 0.0, 1.0, 0.1).setSuffix("m"));
    private final SliderSetting bevelDistance = this.add(new SliderSetting("BevelDistance", 0.2, 0.0, 1.0, 0.1).setSuffix("m"));
    private final ColorSetting safeFill = this.add(new ColorSetting("SafeFill", new Color(1327692800, true)).injectBoolean(true));
    private final ColorSetting safeBox = this.add(new ColorSetting("SafeBox", new Color(-14484480, true)).injectBoolean(false));
    private final ColorSetting semiSafeFill = this.add(new ColorSetting("SemiSafeFill", new Color(1341977856, true)).injectBoolean(true));
    private final ColorSetting semiSafeBox = this.add(new ColorSetting("SemiSafeBox", new Color(-199424, true)).injectBoolean(false));
    private final ColorSetting unsafeFill = this.add(new ColorSetting("UnsafeFill", new Color(1341915136, true)).injectBoolean(true));
    private final ColorSetting unsafeBox = this.add(new ColorSetting("UnsafeBox", new Color(-50593792, true)).injectBoolean(false));
    List<BlockPos> safe = new ArrayList<BlockPos>();
    List<BlockPos> semiSafe = new ArrayList<BlockPos>();
    List<BlockPos> unsafe = new ArrayList<BlockPos>();
    int[] offsets = new int[]{1, 0, -1};

    public PhaseESP() {
        super("PhaseESP", Module.Category.Render);
        this.setChinese("穿墙显示");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.safe.clear();
        this.semiSafe.clear();
        this.unsafe.clear();
        for (int x : this.offsets) {
            for (int z : this.offsets) {
                Block downBlock;
                BlockPos pos = PhaseESP.mc.player.getBlockPos().add(x, 0, z);
                double d = PhaseESP.mc.player.getPos().distanceTo(pos.toBottomCenterPos());
                double d2 = x != 0 && z != 0 ? this.bevelDistance.getValue() + 1.0 : this.getDistance.getValue() + 0.8;
                if (!(d <= d2)) continue;
                BlockState blockState = PhaseESP.mc.world.getBlockState(pos);
                BlockPos downPos = pos.down();
                if (blockState.getBlock() == Blocks.BEDROCK) {
                    downBlock = PhaseESP.mc.world.getBlockState(downPos).getBlock();
                    if (downBlock == Blocks.BEDROCK) {
                        this.safe.add(pos);
                        continue;
                    }
                    this.unsafe.add(pos);
                    continue;
                }
                if (!blockState.isFullCube((BlockView)PhaseESP.mc.world, pos)) continue;
                downBlock = PhaseESP.mc.world.getBlockState(downPos).getBlock();
                if (downBlock == Blocks.BEDROCK) {
                    this.semiSafe.add(pos);
                    continue;
                }
                this.unsafe.add(pos);
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        this.draw(matrixStack, this.safe, this.safeFill, this.safeBox);
        this.draw(matrixStack, this.unsafe, this.unsafeFill, this.unsafeBox);
        this.draw(matrixStack, this.semiSafe, this.semiSafeFill, this.semiSafeBox);
    }

    private void draw(MatrixStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting box) {
        for (BlockPos pos : list) {
            Box espBox = new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)pos.getY(), (double)(pos.getZ() + 1));
            if (fill.booleanValue) {
                Render3DUtil.drawFill(matrixStack, espBox, fill.getValue());
            }
            if (!box.booleanValue) continue;
            Render3DUtil.drawBox(matrixStack, espBox, box.getValue());
        }
    }

    public static enum Type {
        None,
        Air,
        Normal,
        Bedrock;
    }
}