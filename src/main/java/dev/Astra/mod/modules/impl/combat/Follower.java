package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.math.Animation;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.api.utils.math.MathUtil;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.api.utils.render.JelloUtil;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.movement.ElytraFly;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class Follower extends Module {
    public static Follower INSTANCE;

    private Entity target;
    private Vec3d directionVec = null;

    // 原有设置
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.LowestDistance));
    private final SliderSetting range = this.add(new SliderSetting("Range", 60.0, 5.0, 200.0, 0.1).setSuffix("m"));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 1.0, 0.0, 1.0, 0.1));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 0, 0, 100));
    private final EnumSetting<TargetESP> renderMode = this.add(new EnumSetting<>("RenderMode", TargetESP.Fill));
    private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0).setSuffix("ms"));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.Linear));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(1291648252, true)));
    private final ColorSetting outlineColor = this.add(new ColorSetting("OutlineColor", new Color(-1275068417, true)));
    private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(-1761607681, true)));
    private final ColorSetting hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(-1442840577, true)));
    private final Animation animation = new Animation();
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true));

    public Follower() {
        super("Follower", Module.Category.Combat);
        setChinese("自动追人");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return target == null ? null : target.getName().getString();
    }

    @Override
    public void onDisable() {
        target = null;
        directionVec = null;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (event.isPost()) return;
        if (!nullCheck()) {
            // 自动禁用逻辑：若 AutoDisable 开启且 ElytraFly 未激活，则关闭本模块
            if (autoDisable.getValue() && !ElytraFly.INSTANCE.isOn()) {
                disable();  // 使用 disable() 而非 setEnabled(false)
                return;
            }
            target = findTarget();
            directionVec = (target != null) ? getHitVec(target) : null;
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (directionVec != null) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        // 如果渲染模式为 None，则不绘制任何内容
        if (target != null && render.getValue() && renderMode.getValue() != TargetESP.None) {
            float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
            doRender(matrixStack, partialTicks, target);
        }
    }

    private void doRender(MatrixStack stack, float partialTicks, Entity entity) {
        Box bb = entity.getBoundingBox();
        Vec3d interpolated = new Vec3d(
                MathUtil.interpolate(entity.prevX, entity.getX(), partialTicks),
                MathUtil.interpolate(entity.prevY, entity.getY(), partialTicks),
                MathUtil.interpolate(entity.prevZ, entity.getZ(), partialTicks)
        );
        Box renderBox = bb.offset(interpolated.subtract(entity.getPos()));
        renderBox = renderBox.expand(0.0, 0.1, 0.0);

        switch (renderMode.getValue()) {
            case Fill:
                Render3DUtil.draw3DBox(stack, renderBox,
                        ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), animation.get(0.0, animationTime.getValueInt(), ease.getValue())),
                        ColorUtil.fadeColor(outlineColor.getValue(), hitOutlineColor.getValue(), animation.get(0.0, animationTime.getValueInt(), ease.getValue())),
                        false, true);
                break;
            case Box:
                Render3DUtil.draw3DBox(stack, renderBox,
                        ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), animation.get(0.0, animationTime.getValueInt(), ease.getValue())),
                        ColorUtil.fadeColor(outlineColor.getValue(), hitOutlineColor.getValue(), animation.get(0.0, animationTime.getValueInt(), ease.getValue())),
                        true, true);
                break;
            case Jello:
                JelloUtil.drawJello(stack, entity, color.getValue());
                break;
            case ThunderHack:
                Render3DUtil.drawTargetEsp(stack, target, color.getValue());
                break;
            case None:
                // 不绘制任何内容
                break;
        }
    }

    private Entity findTarget() {
        Entity bestTarget = null;
        for (Entity entity : Astra.THREAD.getEntities()) {
            if (!isValidTarget(entity)) continue;
            if (bestTarget == null) {
                bestTarget = entity;
                continue;
            }
            double distance = mc.player.getEyePos().distanceTo(entity.getPos());
            float health = EntityUtil.getHealth(entity);
            switch (mode.getValue()) {
                case LowestDistance:
                    double bestDistance = mc.player.getEyePos().distanceTo(bestTarget.getPos());
                    if (distance < bestDistance) bestTarget = entity;
                    break;
                case LowestHealth:
                    float bestHealth = EntityUtil.getHealth(bestTarget);
                    if (health < bestHealth) {
                        bestTarget = entity;
                        break;
                    }
                    if (health == bestHealth) {
                        double currentTargetDistance = mc.player.getEyePos().distanceTo(bestTarget.getPos());
                        if (distance < currentTargetDistance) bestTarget = entity;
                    }
                    break;
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        if (player == mc.player) return false;
        if (Astra.FRIEND.isFriend(player)) return false;
        if (!entity.isAlive()) return false;
        double distance = mc.player.getEyePos().distanceTo(entity.getPos());
        return distance <= range.getValue();
    }

    private Vec3d getHitVec(Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        double heightOffset = (entity instanceof PlayerEntity) ? entity.getHeight() * 0.5 : mc.player.getEyeHeight(mc.player.getPose()) * 0.5;
        double targetY = box.minY + heightOffset;
        return new Vec3d(closestX, targetY, closestZ);
    }

    public enum Mode {
        LowestDistance,
        LowestHealth
    }

    public enum TargetESP {
        Fill,
        Box,
        Jello,
        ThunderHack,
        None   // 新增 None 模式
    }
}