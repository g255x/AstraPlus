/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder
 *  net.minecraft.util.math.BlockPos
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.Xray;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ChunkOcclusionDataBuilder.class})
public abstract class MixinChunkOcclusionDataBuilder {
    @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
    private void onMarkClosed(BlockPos pos, CallbackInfo info) {
        Xray xray = Xray.INSTANCE;
        if (xray != null && xray.isOn()) {
            info.cancel();
        }
    }
}
