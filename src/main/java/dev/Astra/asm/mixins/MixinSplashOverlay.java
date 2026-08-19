/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.function.Consumer;

@Mixin(value = {SplashOverlay.class})
public abstract class MixinSplashOverlay extends Overlay {

    @Shadow
    private long reloadCompleteTime;

    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private static final float SATURATION = 0.6f;
    @Unique
    private static final float BRIGHTNESS = 0.8f;

    @Unique
    private long startTime = 0;  // 记录开始时间

    public MixinSplashOverlay() {
        super();
    }

    @Inject(method = {"<init>"}, at = {@At("TAIL")})
    private void initHook(MinecraftClient var1, ResourceReload var2, Consumer<?> var3, boolean var4, CallbackInfo ci) {
        this.startTime = System.currentTimeMillis();  // 记录 Mixin 初始化时间
    }

    @Inject(method = {"render"}, at = {@At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V",
            ordinal = 0,
            shift = Shift.BEFORE,
            remap = false
    )})
    private void renderHook(DrawContext var1, int var2, int var3, float var4, CallbackInfo ci) {
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();

        int alpha = MathHelper.ceil(
                (1.0F - MathHelper.clamp(
                        (this.reloadCompleteTime > -1L
                                ? (float) (Util.getMeasuringTimeMs() - this.reloadCompleteTime) / 1000.0F
                                : -1.0F) - 1.0F,
                        0.0F, 1.0F)) * 255.0F
        );

        // 使用相对时间，从0开始
        long elapsed = System.currentTimeMillis() - this.startTime;
        float hue = (elapsed % 10000L) / 10000.0f;  // 10秒循环一次，从红色开始

        Color color = Color.getHSBColor(hue, SATURATION, BRIGHTNESS);
        int rgba = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();

        var1.fill(0, 0, width, height, rgba);
    }
}