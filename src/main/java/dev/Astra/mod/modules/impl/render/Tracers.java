/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.client.network.AbstractClientPlayerEntity
 *  net.minecraft.client.render.Camera
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  org.lwjgl.opengl.GL32C
 */
package dev.Astra.mod.modules.impl.render;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.Render3DEvent;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.math.MathUtil;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL32C;

import java.awt.*;

public class Tracers
        extends Module {
    public static Tracers INSTANCE;
    public final EnumSetting<Point> point = this.add(new EnumSetting<Point>("Point", Point.Feet));
    public final SliderSetting range = this.add(new SliderSetting("Range", 200.0, 0.0, 500.0, 0.1));
    public final SliderSetting width = this.add(new SliderSetting("Width", 1.5, 0.5, 5.0, 0.1, () -> this.fade.getValue()));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
    public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true));
    public final BooleanSetting box = this.add(new BooleanSetting("Box", false));
    public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true));
    public final BooleanSetting players = this.add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = this.add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = this.add(new BooleanSetting("Animals", true));

    public Tracers() {
        super("Tracers", Module.Category.Render);
        this.setChinese("追踪线");
        INSTANCE = this;
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (Tracers.nullCheck()) return;
        Camera camera = Wrapper.mc.gameRenderer.getCamera();
        Vec3d crosshairPos = this.getCrosshairPos(camera);

        GL32C.glLineWidth(this.width.getValueFloat());

        for (Entity entity : Wrapper.mc.world.getEntities()) {
            if (!this.shouldRender(entity)) continue;

            double dist = camera.getPos().distanceTo(entity.getPos());
            if (dist > this.range.getValue()) continue;

            Vec3d target = this.getTargetPos(entity);
            Color lineColor = this.color.getValue();

            if (this.fade.getValue()) {
                double minDist = 0.0;
                double maxDist = this.range.getValue();
                double t = MathUtil.clamp((dist - minDist) / (maxDist - minDist), 0.0, 1.0);
                float alpha = (float) ((1.0 - t) * lineColor.getAlpha());
                lineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), (int) alpha);
            }

            // 主追踪线：从准星到目标点
            Render3DUtil.drawLine(crosshairPos, target, lineColor);

            // 可选：绘制实体包围盒
            if (this.box.getValue()) {
                Vec3d eyes = entity.getLerpedPos(Wrapper.mc.getRenderTickCounter().getTickDelta(true));
                float eyeH = entity.getEyeHeight(entity.getPose());
                Vec3d top = eyes.add(0.0, eyeH, 0.0);
                Vec3d bottom = eyes;
                Render3DUtil.drawLine(eyes, top, lineColor);
            }
        }

        GL32C.glLineWidth(1.0f);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == Wrapper.mc.player) return false;
        if (entity instanceof EndCrystalEntity) return this.crystal.getValue();
        if (entity instanceof PlayerEntity) return this.players.getValue();
        if (entity instanceof MobEntity) return this.mobs.getValue();
        if (entity instanceof AnimalEntity) return this.animals.getValue();
        return false;
    }

    private Vec3d getTargetPos(Entity entity) {
        Vec3d pos = entity.getLerpedPos(Wrapper.mc.getRenderTickCounter().getTickDelta(true));
        switch (this.point.getValue()) {
            case Feet -> {
                return pos;
            }
            case Hand -> {
                float eyeH = entity.getEyeHeight(entity.getPose());
                return pos.add(0.0, eyeH, 0.0);
            }
            case Torso -> {
                float eyeH = entity.getEyeHeight(entity.getPose());
                Vec3d feet = pos;
                Vec3d eyes = pos.add(0.0, eyeH, 0.0);
                return feet.add(eyes).multiply(0.5);
            }
        }

        return pos.add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
    }

    private Vec3d getCrosshairPos(Camera camera) {
        Vec3d camPos = camera.getPos();
        float pitchRad = (float) Math.toRadians(camera.getPitch());
        float yawRad = (float) Math.toRadians(camera.getYaw());
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return camPos.add(x, y, z);
    }

    public static enum Point {
        Feet,
        Hand,
        Torso;
    }
}