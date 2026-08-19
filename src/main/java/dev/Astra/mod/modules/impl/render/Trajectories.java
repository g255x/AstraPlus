package dev.Astra.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.Astra;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.player.AutoPearl;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Trajectories extends Module {
    private static MatrixStack matrixStack;
    // 原有设置
    private final SliderSetting maxEntities = this.add(new SliderSetting("MaxEntities", 5, 1, 20, 1));
    private final SliderSetting maxSteps = this.add(new SliderSetting("MaxSteps", 200, 50, 500, 10));
    private final ColorSetting pearl = this.add(new ColorSetting("Pearl", new Color(255, 255, 255, 255)).injectBoolean(true));
    private final ColorSetting arrow = this.add(new ColorSetting("Arrow", new Color(255, 255, 255, 255)).injectBoolean(true));
    private final ColorSetting xp = this.add(new ColorSetting("Xp", new Color(255, 255, 255, 255)).injectBoolean(true));
    private final ColorSetting snowball = this.add(new ColorSetting("Snowball", new Color(255, 255, 255, 255)).injectBoolean(false));
    private final ColorSetting egg = this.add(new ColorSetting("Egg", new Color(255, 255, 255, 255)).injectBoolean(false));
    private final ColorSetting potion = this.add(new ColorSetting("Potion", new Color(255, 255, 255, 255)).injectBoolean(false));
    private final ColorSetting trident = this.add(new ColorSetting("Trident", new Color(255, 255, 255, 255)).injectBoolean(false));
    // 速度设置（默认值为原版速度数值，步进0.01）
    private final SliderSetting pearlSpeed = this.add(new SliderSetting("PearlSpeed", 1.5, 0.0, 10.0, 0.01));
    private final SliderSetting arrowSpeed = this.add(new SliderSetting("ArrowSpeed", 3.0, 0.0, 10.0, 0.01));
    private final SliderSetting xpSpeed = this.add(new SliderSetting("XpSpeed", 0.7, 0.0, 10.0, 0.01));
    private final SliderSetting snowballSpeed = this.add(new SliderSetting("SnowballSpeed", 1.5, 0.0, 10.0, 0.01));
    private final SliderSetting eggSpeed = this.add(new SliderSetting("EggSpeed", 1.5, 0.0, 10.0, 0.01));
    private final SliderSetting potionSpeed = this.add(new SliderSetting("PotionSpeed", 0.5, 0.0, 10.0, 0.01));
    private final SliderSetting tridentSpeed = this.add(new SliderSetting("TridentSpeed", 2.5, 0.0, 10.0, 0.01));
    // 阻力系数
    private final SliderSetting airResistance = this.add(new SliderSetting("AirResistance", 0.99, 0.5, 0.99, 0.01));
    private final SliderSetting waterResistance = this.add(new SliderSetting("WaterResistance", 0.8, 0.5, 0.99, 0.01));
    // 高亮颜色（无开关）
    private final ColorSetting hitFill = this.add(new ColorSetting("HitFill", new Color(255, 0, 0, 50)));
    private final ColorSetting hitOutline = this.add(new ColorSetting("HitOutline", new Color(255, 0, 0, 255)));

    public Trajectories() {
        super("Trajectories", Module.Category.Render);
        this.setChinese("投掷物轨迹预测");
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (nullCheck()) return;
        matrixStack = stack;
        RenderSystem.disableDepthTest();
        // 空中弹射物
        List<Entity> entitiesToRender = new ArrayList<>();
        for (Entity e : Astra.THREAD.getEntities()) if (getEntityColor(e) != null) entitiesToRender.add(e);
        entitiesToRender.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));
        int max = maxEntities.getValueInt();
        for (int i = 0; i < Math.min(entitiesToRender.size(), max); i++) calcTrajectory(entitiesToRender.get(i), getEntityColor(entitiesToRender.get(i)));
        // 手持预览 - 根据物品获取对应颜色和开关
        if (mc.options.getPerspective().isFirstPerson()) {
            ItemStack main = mc.player.getMainHandStack();
            ItemStack off = mc.player.getOffHandStack();
            Item item = null;
            Hand activeHand = null;
            if (isThrowable(main.getItem()) || main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem || AutoPearl.INSTANCE.isOn()) {
                item = main.getItem();
                activeHand = Hand.MAIN_HAND;
            } else if (isThrowable(off.getItem()) || off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) {
                item = off.getItem();
                activeHand = Hand.OFF_HAND;
            }
            if (item != null) {
                // 获取物品对应的颜色和开关
                ColorItemPair pair = getColorForItem(item);
                if (pair != null && pair.enabled) {
                    boolean oldBob = mc.options.getBobView().getValue();
                    mc.options.getBobView().setValue(false);
                    double x = mc.player.getX();
                    double y = mc.player.getEyeY();
                    double z = mc.player.getZ();
                    boolean multishot = (activeHand == Hand.MAIN_HAND && hasMultishot(main)) || (activeHand == Hand.OFF_HAND && hasMultishot(off));
                    if (multishot && item instanceof CrossbowItem) {
                        calcPreview(item, mc.player.getYaw(), x, y, z, pair.color);
                        calcPreview(item, mc.player.getYaw(), x, y, z, pair.color);
                        calcPreview(item, mc.player.getYaw(), x, y, z, pair.color);
                    } else calcPreview(item, mc.player.getYaw(), x, y, z, pair.color);
                    mc.options.getBobView().setValue(oldBob);
                }
            }
        }
        RenderSystem.enableDepthTest();
    }

    // 内部类用于存储颜色和开关状态
    private static class ColorItemPair {
        Color color;
        boolean enabled;
        ColorItemPair(Color color, boolean enabled) {
            this.color = color;
            this.enabled = enabled;
        }
    }

    private ColorItemPair getColorForItem(Item item) {
        if (item instanceof EnderPearlItem) return new ColorItemPair(pearl.getValue(), pearl.booleanValue);
        if (item instanceof BowItem || item instanceof CrossbowItem) return new ColorItemPair(arrow.getValue(), arrow.booleanValue);
        if (item instanceof TridentItem) return new ColorItemPair(trident.getValue(), trident.booleanValue);
        if (item instanceof ExperienceBottleItem) return new ColorItemPair(xp.getValue(), xp.booleanValue);
        if (item instanceof SnowballItem) return new ColorItemPair(snowball.getValue(), snowball.booleanValue);
        if (item instanceof EggItem) return new ColorItemPair(egg.getValue(), egg.booleanValue);
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return new ColorItemPair(potion.getValue(), potion.booleanValue);
        return null;
    }

    private Color getEntityColor(Entity e) {
        if (e instanceof EnderPearlEntity && pearl.booleanValue) return pearl.getValue();
        if (e instanceof ArrowEntity && arrow.booleanValue) return arrow.getValue();
        if (e instanceof ExperienceBottleEntity && xp.booleanValue) return xp.getValue();
        if (e instanceof SnowballEntity && snowball.booleanValue) return snowball.getValue();
        if (e instanceof EggEntity && egg.booleanValue) return egg.getValue();
        if (e instanceof PotionEntity && potion.booleanValue) return potion.getValue();
        if (e instanceof TridentEntity && trident.booleanValue) return trident.getValue();
        return null;
    }

    private double getSpeedValue(Entity e) {
        if (e instanceof EnderPearlEntity) return pearlSpeed.getValue();
        if (e instanceof ArrowEntity) return arrowSpeed.getValue();
        if (e instanceof ExperienceBottleEntity) return xpSpeed.getValue();
        if (e instanceof SnowballEntity) return snowballSpeed.getValue();
        if (e instanceof EggEntity) return eggSpeed.getValue();
        if (e instanceof PotionEntity) return potionSpeed.getValue();
        if (e instanceof TridentEntity) return tridentSpeed.getValue();
        return 0.0;
    }

    private double getPreviewSpeedValue(Item item) {
        if (item instanceof EnderPearlItem) return pearlSpeed.getValue();
        if (item instanceof BowItem || item instanceof CrossbowItem) return arrowSpeed.getValue();
        if (item instanceof TridentItem) return tridentSpeed.getValue();
        if (item instanceof ExperienceBottleItem) return xpSpeed.getValue();
        if (item instanceof SnowballItem) return snowballSpeed.getValue();
        if (item instanceof EggItem) return eggSpeed.getValue();
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return potionSpeed.getValue();
        return 1.5;
    }

    private void calcTrajectory(Entity e, Color color) {
        Vec3d vel = e.getVelocity();
        double motionX = vel.x, motionY = vel.y, motionZ = vel.z;
        if (motionX == 0 && motionY == 0 && motionZ == 0) return;
        double speed = getSpeedValue(e);
        if (speed > 0) {
            double len = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (len > 0) {
                motionX = motionX / len * speed;
                motionY = motionY / len * speed;
                motionZ = motionZ / len * speed;
            }
        }
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double gravity = (e instanceof ArrowEntity || e instanceof TridentEntity) ? 0.05 : 0.03;
        int maxStep = maxSteps.getValueInt();
        double airRes = airResistance.getValue();
        double waterRes = waterResistance.getValue();
        for (int step = 0; step < maxStep; step++) {
            Vec3d last = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (mc.world.getBlockState(new BlockPos((int)x, (int)y, (int)z)).getBlock() == Blocks.WATER) {
                motionX *= waterRes;
                motionY *= waterRes;
                motionZ *= waterRes;
            } else {
                motionX *= airRes;
                motionY *= airRes;
                motionZ *= airRes;
            }
            motionY -= gravity;
            Vec3d now = new Vec3d(x, y, z);

            Vec3d hitPoint = null;
            Entity hitEntity = null;
            // 实体碰撞
            for (Entity other : mc.world.getEntities()) {
                if (other == e) continue;
                if (other instanceof ProjectileEntity) continue;
                Optional<Vec3d> hit = other.getBoundingBox().raycast(last, now);
                if (hit.isPresent()) {
                    hitPoint = hit.get();
                    hitEntity = other;
                    break;
                }
            }
            if (hitPoint != null) {
                Render3DUtil.drawLine(last, hitPoint, color);
                if (hitEntity != null) {
                    Box box = hitEntity.getBoundingBox();
                    Render3DUtil.draw3DBox(matrixStack, box, hitFill.getValue(), hitOutline.getValue(), true, true);
                }
                break;
            }
            // 方块碰撞
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(last, now, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                Render3DUtil.drawLine(last, bhr.getPos(), color);
                BlockPos pos = bhr.getBlockPos();
                var shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                if (shape != null && !shape.isEmpty()) {
                    Box box = shape.getBoundingBox().offset(pos);
                    Render3DUtil.draw3DBox(matrixStack, box, hitFill.getValue(), hitOutline.getValue(), true, true);
                }
                break;
            }
            if (y <= -65) break;
            Render3DUtil.drawLine(last, now, color);
        }
    }

    private void calcPreview(Item item, float yaw, double x, double y, double z, Color color) {
        float maxDist = getDistance(item);
        float pitch = mc.player.getPitch();
        // 删除所有偏移，直接使用视角方向
        double motionX = -MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE) * maxDist;
        double motionY = -MathHelper.sin(pitch * MathHelper.RADIANS_PER_DEGREE) * maxDist;
        double motionZ = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE) * maxDist;
        float power = (float) mc.player.getItemUseTime() / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;
        if (power > 1.0f) power = 1.0f;
        float len = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= len; motionY /= len; motionZ /= len;
        float pow = (item instanceof BowItem ? power * 2.0f : (item instanceof CrossbowItem ? 2.2f : 1.0f)) * getThrowVelocity(item);
        motionX *= pow; motionY *= pow; motionZ *= pow;
        double speed = getPreviewSpeedValue(item);
        if (speed > 0) {
            double currentLen = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (currentLen > 0) {
                motionX = motionX / currentLen * speed;
                motionY = motionY / currentLen * speed;
                motionZ = motionZ / currentLen * speed;
            }
        }
        // 移除玩家速度叠加，使轨迹仅基于视角方向
        double gravity = (item instanceof BowItem || item instanceof CrossbowItem) ? 0.05 : 0.03;
        int maxStep = 300;
        Vec3d lastPos = new Vec3d(x, y, z);
        double airRes = airResistance.getValue();
        double waterRes = waterResistance.getValue();
        for (int step = 0; step < maxStep; step++) {
            double newX = x + motionX;
            double newY = y + motionY;
            double newZ = z + motionZ;
            Vec3d newPos = new Vec3d(newX, newY, newZ);
            if (mc.world.getBlockState(new BlockPos((int)newX, (int)newY, (int)newZ)).getBlock() == Blocks.WATER) {
                motionX *= waterRes; motionY *= waterRes; motionZ *= waterRes;
            } else {
                motionX *= airRes; motionY *= airRes; motionZ *= airRes;
            }
            motionY -= gravity;
            Vec3d hitPoint = null;
            Entity hitEntity = null;
            // 实体碰撞
            for (Entity other : mc.world.getEntities()) {
                if (other == mc.player) continue;
                if (other instanceof ProjectileEntity) continue;
                Optional<Vec3d> hit = other.getBoundingBox().raycast(lastPos, newPos);
                if (hit.isPresent()) {
                    hitPoint = hit.get();
                    hitEntity = other;
                    break;
                }
            }
            BlockHitResult bhr = null;
            if (hitPoint == null) {
                bhr = mc.world.raycast(new RaycastContext(lastPos, newPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
                if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) hitPoint = bhr.getPos();
            }
            if (hitPoint != null) {
                Render3DUtil.drawLine(lastPos, hitPoint, color);
                if (hitEntity != null) {
                    Box box = hitEntity.getBoundingBox();
                    Render3DUtil.draw3DBox(matrixStack, box, hitFill.getValue(), hitOutline.getValue(), true, true);
                } else if (bhr != null) {
                    BlockPos pos = bhr.getBlockPos();
                    var shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                    if (shape != null && !shape.isEmpty()) {
                        Box box = shape.getBoundingBox().offset(pos);
                        Render3DUtil.draw3DBox(matrixStack, box, hitFill.getValue(), hitOutline.getValue(), true, true);
                    }
                }
                break;
            }
            Render3DUtil.drawLine(lastPos, newPos, color);
            x = newX; y = newY; z = newZ;
            lastPos = newPos;
            if (y <= -65) break;
        }
    }

    private boolean isThrowable(Item item) {
        return item instanceof EnderPearlItem || item instanceof TridentItem || item instanceof ExperienceBottleItem ||
                item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
    }

    private float getDistance(Item item) { return (item instanceof BowItem) ? 1.0f : 0.4f; }
    private float getThrowVelocity(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.59f;
        if (item instanceof TridentItem) return 2.0f;
        return 1.5f;
    }

    // 删除所有偏移，所有物品都返回0
    private int getThrowPitch(Item item) {
        return 0;
    }

    private boolean hasMultishot(ItemStack stack) {
        if (!(stack.getItem() instanceof CrossbowItem)) return false;
        var multishot = mc.world.getRegistryManager().get(Enchantments.MULTISHOT.getRegistryRef()).getEntry(Enchantments.MULTISHOT).get();
        return EnchantmentHelper.getLevel(multishot, stack) != 0;
    }
}