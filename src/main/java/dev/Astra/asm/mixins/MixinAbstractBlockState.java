/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.block.AbstractBlock$AbstractBlockState
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.BlockView
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.Xray;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {AbstractBlock.AbstractBlockState.class})
public class MixinAbstractBlockState {
    @Inject(method = "getAmbientOcclusionLightLevel", at = @At(value = "HEAD"), cancellable = true)
    public void getAmbientOcclusionLightLevelHook(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
        Xray xray = Xray.INSTANCE;
        if (xray != null && xray.isOn()) {
            info.setReturnValue(1.0f);
        }
    }
}
