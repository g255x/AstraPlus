/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.render.VertexConsumerProvider
 *  net.minecraft.client.render.entity.LivingEntityRenderer
 *  net.minecraft.client.render.entity.model.EntityModel
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package dev.Astra.asm.mixins;

import dev.Astra.core.impl.RotationManager;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
    @Unique
    private LivingEntity lastEntity;
    @Unique
    private float originalYaw;
    @Unique
    private float originalHeadYaw;
    @Unique
    private float originalBodyYaw;
    @Unique
    private float originalPitch;
    @Unique
    private float originalPrevYaw;
    @Unique
    private float originalPrevHeadYaw;
    @Unique
    private float originalPrevBodyYaw;

    @Inject(method={"render*"}, at={@At(value="HEAD")})
    public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
            this.originalYaw = livingEntity.getYaw();
            this.originalHeadYaw = ((LivingEntity)livingEntity).headYaw;
            this.originalBodyYaw = ((LivingEntity)livingEntity).bodyYaw;
            this.originalPitch = livingEntity.getPitch();
            this.originalPrevYaw = ((LivingEntity)livingEntity).prevYaw;
            this.originalPrevHeadYaw = ((LivingEntity)livingEntity).prevHeadYaw;
            this.originalPrevBodyYaw = ((LivingEntity)livingEntity).prevBodyYaw;
            livingEntity.setYaw(RotationManager.getRenderYawOffset());
            ((LivingEntity)livingEntity).headYaw = RotationManager.getRotationYawHead();
            ((LivingEntity)livingEntity).bodyYaw = RotationManager.getRenderYawOffset();
            livingEntity.setPitch(RotationManager.getRenderPitch());
            ((LivingEntity)livingEntity).prevYaw = RotationManager.getPrevRenderYawOffset();
            ((LivingEntity)livingEntity).prevHeadYaw = RotationManager.getPrevRotationYawHead();
            ((LivingEntity)livingEntity).prevBodyYaw = RotationManager.getPrevRenderYawOffset();
            ((LivingEntity)livingEntity).prevPitch = RotationManager.getPrevRenderPitch();
        }
        this.lastEntity = livingEntity;
    }

    @Inject(method={"render*"}, at={@At(value="TAIL")})
    public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
            livingEntity.setYaw(this.originalYaw);
            ((LivingEntity)livingEntity).headYaw = this.originalHeadYaw;
            ((LivingEntity)livingEntity).bodyYaw = this.originalBodyYaw;
            livingEntity.setPitch(this.originalPitch);
            ((LivingEntity)livingEntity).prevYaw = this.originalPrevYaw;
            ((LivingEntity)livingEntity).prevHeadYaw = this.originalPrevHeadYaw;
            ((LivingEntity)livingEntity).prevBodyYaw = this.originalPrevBodyYaw;
            ((LivingEntity)livingEntity).prevPitch = this.originalPitch;
        }
    }
}

