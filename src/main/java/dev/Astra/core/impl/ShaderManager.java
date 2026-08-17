/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.GlStateManager$DstFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SrcFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gl.Framebuffer
 *  net.minecraft.client.gl.PostEffectProcessor
 *  net.minecraft.util.Identifier
 *  org.jetbrains.annotations.NotNull
 */
package dev.Astra.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.api.interfaces.IShaderEffectHook;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.mod.modules.impl.render.ShaderModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShaderManager
implements Wrapper {
    static final Timer timer = new Timer();
    private static final List<RenderTask> tasks = new ArrayList<RenderTask>();
    public static ManagedShaderEffect DEFAULT_OUTLINE;
    public static ManagedShaderEffect SMOKE_OUTLINE;
    public static ManagedShaderEffect GRADIENT_OUTLINE;
    public static ManagedShaderEffect SNOW_OUTLINE;
    public static ManagedShaderEffect FLOW_OUTLINE;
    public static ManagedShaderEffect RAINBOW_OUTLINE;
    public static ManagedShaderEffect DEFAULT;
    public static ManagedShaderEffect SMOKE;
    public static ManagedShaderEffect GRADIENT;
    public static ManagedShaderEffect SNOW;
    public static ManagedShaderEffect FLOW;
    public static ManagedShaderEffect RAINBOW;
    public static ManagedShaderEffect SKYBOX;
    public float time = 0.0f;
    private boolean shaderDisabled = false;
    private MyFramebuffer shaderBuffer;

    public void renderShader(Runnable runnable, Shader mode) {
        if (this.shaderDisabled) return;
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        if (this.shaderDisabled) return;
        try {
            tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
            tasks.clear();
        } catch (Exception e) {
            this.shaderDisabled = true;
            tasks.clear();
        }
    }

    public void applyShader(Runnable runnable, Shader mode) {
        if (this.shaderDisabled || this.fullNullCheck()) {
            return;
        }
        try {
        RenderSystem.assertOnRenderThreadOrInit();
        Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
        if (this.shaderBuffer.textureWidth != MCBuffer.textureWidth || this.shaderBuffer.textureHeight != MCBuffer.textureHeight) {
            this.shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
        }
        GlStateManager._glBindFramebuffer((int)36009, (int)this.shaderBuffer.fbo);
        this.shaderBuffer.beginWrite(true);
        runnable.run();
        this.shaderBuffer.endWrite();
        GlStateManager._glBindFramebuffer((int)36009, (int)MCBuffer.fbo);
        MCBuffer.beginWrite(false);
        ManagedShaderEffect shader = this.getShader(mode);
        if (shader == null) return;
        PostEffectProcessor effect = shader.getShaderEffect();
        if (effect != null) {
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", this.shaderBuffer);
        } else {
            return;
        }
        Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
        if (outBuffer == null) return;
        this.setupShader(mode, shader);
        this.shaderBuffer.clear(false);
        MCBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.disableBlend();
        } catch (Exception e) {
            this.shaderDisabled = true;
        }
    }

    public ManagedShaderEffect getShader(@NotNull Shader mode) {
        return switch (mode.ordinal()) {
            case 2 -> GRADIENT;
            case 1 -> SMOKE;
            case 3 -> SNOW;
            case 4 -> FLOW;
            case 5 -> RAINBOW;
            default -> DEFAULT;
        };
    }

    public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
        return switch (mode.ordinal()) {
            case 2 -> GRADIENT_OUTLINE;
            case 1 -> SMOKE_OUTLINE;
            case 3 -> SNOW_OUTLINE;
            case 4 -> FLOW_OUTLINE;
            case 5 -> RAINBOW_OUTLINE;
            default -> DEFAULT_OUTLINE;
        };
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect) {
        if (this.shaderDisabled) return;
        ShaderModule module = ShaderModule.INSTANCE;
        Color color = module.fill.getValue();
        this.time = (float)timer.getMs() / 5.0f * module.speed.getValueFloat() * 0.004f;
        try {
        if (shader == Shader.Rainbow) {
            effect.setUniformValue("alpha2", (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Gradient) {
            effect.setUniformValue("alpha2", (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("rgb", (float)module.smoke1.getValue().getRed() / 255.0f, (float)module.smoke1.getValue().getGreen() / 255.0f, (float)module.smoke1.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb1", (float)module.smoke2.getValue().getRed() / 255.0f, (float)module.smoke2.getValue().getGreen() / 255.0f, (float)module.smoke2.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb2", (float)module.smoke3.getValue().getRed() / 255.0f, (float)module.smoke3.getValue().getGreen() / 255.0f, (float)module.smoke3.getValue().getBlue() / 255.0f);
            effect.setUniformValue("rgb3", (float)module.smoke4.getValue().getRed() / 255.0f, (float)module.smoke4.getValue().getGreen() / 255.0f, (float)module.smoke4.getValue().getBlue() / 255.0f);
            effect.setUniformValue("step", module.step.getValueFloat() * 300.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time * 300.0f);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Smoke) {
            effect.setUniformValue("alpha1", (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("first", (float)module.smoke1.getValue().getRed() / 255.0f, (float)module.smoke1.getValue().getGreen() / 255.0f, (float)module.smoke1.getValue().getBlue() / 255.0f, (float)module.smoke1.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("second", (float)module.smoke2.getValue().getRed() / 255.0f, (float)module.smoke2.getValue().getGreen() / 255.0f, (float)module.smoke2.getValue().getBlue() / 255.0f);
            effect.setUniformValue("third", (float)module.smoke3.getValue().getRed() / 255.0f, (float)module.smoke3.getValue().getGreen() / 255.0f, (float)module.smoke3.getValue().getBlue() / 255.0f);
            effect.setUniformValue("oct", (int)module.octaves.getValue());
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Solid) {
            effect.setUniformValue("mixFactor", (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("color", (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Snow) {
            effect.setUniformValue("color", (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        } else if (shader == Shader.Flow) {
            effect.setUniformValue("mixFactor", (float)color.getAlpha() / 255.0f);
            effect.setUniformValue("radius", module.radius.getValueFloat());
            effect.setUniformValue("quality", module.smoothness.getValueFloat());
            effect.setUniformValue("divider", module.divider.getValueFloat());
            effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", this.time);
            effect.render(mc.getRenderTickCounter().getTickDelta(true));
        }
        } catch (Exception ignored) {
        }
    }

    public void reloadShaders() {
        DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/outline.json"));
        SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/smoke.json"));
        GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/gradient.json"));
        SNOW = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/snow.json"));
        FLOW = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/flow.json"));
        RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/rainbow.json"));
        SKYBOX = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/skybox.json"));
        DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/outline.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/smoke.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/gradient.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/snow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/flow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of((String)"shaders/post/rainbow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) {
                return;
            }
            ((IShaderEffectHook)effect).frogClient$addHook("bufIn", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).frogClient$addHook("bufOut", ShaderManager.mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
    }

    // keep track of the last time we attempted to reload shaders so we don't spam the GPU
    private static long lastReloadAttempt = 0L;

    public void renderSkyboxFullscreen(Color color, Color backgroundColor, float scale, float speed, float opacity) {
        if (this.shaderDisabled || this.fullNullCheck()) {
            return;
        }
        try {
        if (!this.fullNullCheck()) {
            RenderSystem.assertOnRenderThreadOrInit();
            ManagedShaderEffect shader = SKYBOX;
            if (shader != null && shader.getShaderEffect() != null) {
                Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
                PostEffectProcessor effect = shader.getShaderEffect();
                if (effect != null) {
                    try {
                        ((IShaderEffectHook)effect).frogClient$addHook("bufIn", MCBuffer);
                    } catch (Exception ignored) {
                    }
                }
                Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
                if (outBuffer != null) {
                    float t = (float)timer.getMs() / 5.0f * speed * 0.004f;
                    this.safeSetUniformValue(shader, "mixFactor", opacity);
                    this.safeSetUniformValue(shader, "u_Color", (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
                    this.safeSetUniformValue(shader, "u_BackgroundColor", (float)backgroundColor.getRed() / 255.0f, (float)backgroundColor.getGreen() / 255.0f, (float)backgroundColor.getBlue() / 255.0f, (float)backgroundColor.getAlpha() / 255.0f);
                    this.safeSetUniformValue(shader, "u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
                    this.safeSetUniformValue(shader, "u_Mouse", 0.0f, 0.0f);
                    this.safeSetUniformValue(shader, "u_Scale", scale);
                    this.safeSetUniformValue(shader, "u_Time", t);
                    try {
                        shader.render(mc.getRenderTickCounter().getTickDelta(true));
                    } catch (Exception ignored) {
                    }
                    MCBuffer.beginWrite(false);
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
                    RenderSystem.backupProjectionMatrix();
                    outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
                    RenderSystem.restoreProjectionMatrix();
                    RenderSystem.disableBlend();
                }
            }
        }
        } catch (Exception e) {
            this.shaderDisabled = true;
        }
    }

    private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1) {
        try {
            shader.setUniformValue(name, v1);
        } catch (Exception ignored) {
        }
    }

    private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1, float v2) {
        try {
            shader.setUniformValue(name, v1, v2);
        } catch (Exception ignored) {
        }
    }

    private void safeSetUniformValue(ManagedShaderEffect shader, String name, float v1, float v2, float v3, float v4) {
        try {
            shader.setUniformValue(name, v1, v2, v3, v4);
        } catch (Exception ignored) {
        }
    }

    public boolean fullNullCheck() {
        if (GRADIENT == null || SMOKE == null || DEFAULT == null || FLOW == null || RAINBOW == null || SNOW == null || SKYBOX == null || GRADIENT_OUTLINE == null || SMOKE_OUTLINE == null || DEFAULT_OUTLINE == null || FLOW_OUTLINE == null || RAINBOW_OUTLINE == null || SNOW_OUTLINE == null || this.shaderBuffer == null) {
            if (mc.getFramebuffer() == null) {
                return true;
            }
            long now = System.currentTimeMillis();
            // if we tried less than 5 seconds ago, skip to avoid reload storm
            if (now - lastReloadAttempt < 5000L) {
                return true;
            }
            lastReloadAttempt = now;
            if (this.shaderBuffer != null) {
                this.shaderBuffer.delete();
            }
            this.shaderBuffer = new MyFramebuffer(ShaderManager.mc.getFramebuffer().textureWidth, ShaderManager.mc.getFramebuffer().textureHeight);
            this.reloadShaders();
            return true;
        }
        return false;
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public static enum Shader {
        Solid,
        Smoke,
        Gradient,
        Snow,
        Flow,
        Rainbow;

    }

    public static class MyFramebuffer
    extends Framebuffer {
        public MyFramebuffer(int width, int height) {
            super(false);
            RenderSystem.assertOnRenderThreadOrInit();
            this.resize(width, height, true);
            this.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }
    }
}