/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.block.BlockState
 *  net.minecraft.client.render.VertexConsumer
 *  net.minecraft.client.render.block.BlockModelRenderer
 *  net.minecraft.client.render.model.BakedModel
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.random.Random
 *  net.minecraft.world.BlockRenderView
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {BlockModelRenderer.class})
public abstract class MixinBlockModelRenderer {
    @Inject(method = {"renderSmooth", "renderFlat"}, at = {@At("HEAD")}, cancellable = true)
    private void onRenderSmooth(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay, CallbackInfo info) {
        Xray xray = Xray.INSTANCE;
        if (xray != null && xray.isOn() && !xray.shouldRender(state)) {
            info.cancel();
        }
    }
}
