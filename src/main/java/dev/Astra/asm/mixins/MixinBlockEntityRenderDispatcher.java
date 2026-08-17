/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.client.render.VertexConsumerProvider
 *  net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
 *  net.minecraft.client.util.math.MatrixStack
 */
package dev.Astra.asm.mixins;

import dev.Astra.Astra;
import dev.Astra.api.events.impl.RenderBlockEntityEvent;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockEntityRenderDispatcher.class})
public abstract class MixinBlockEntityRenderDispatcher {
    @Inject(method={"render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private <E extends BlockEntity> void onRenderEntity(E blockEntity, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumerProvider, CallbackInfo info) {
        RenderBlockEntityEvent event = Astra.EVENT_BUS.post(RenderBlockEntityEvent.get(blockEntity));
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}
