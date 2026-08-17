package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.math.Animation;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.api.utils.math.MathUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.api.utils.render.JelloUtil;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.asm.accessors.IEntity;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.movement.ElytraFly;
import dev.Astra.mod.modules.settings.enums.SwingSide;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class Aura extends Module {
    public static Aura INSTANCE;
    public static Entity target;

    public enum Page { General, Weapons, Target, Render, Rotate, Swap }

    private enum TargetMode { DISTANCE, HEALTH }
    public enum TargetESP { Fill, Box, Jello, ThunderHack, None }

    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));

    // General
    private final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.1, 7.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1, 14.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.1, 7.0, () -> page.getValue() == Page.General).setSuffix("m"));
    private final SliderSetting attackInterval = this.add(new SliderSetting("Delay", 200.0, 0.0, 1000.0, 1.0, () -> page.getValue() == Page.General).setSuffix("ms"));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<>("Swing", SwingSide.All, () -> page.getValue() == Page.General));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> page.getValue() == Page.General));

    // Weapons
    private final BooleanSetting useSword = this.add(new BooleanSetting("Sword", true, () -> page.getValue() == Page.Weapons));
    private final BooleanSetting useAxe = this.add(new BooleanSetting("Axe", true, () -> page.getValue() == Page.Weapons));
    private final BooleanSetting useMace = this.add(new BooleanSetting("Mace", true, () -> page.getValue() == Page.Weapons));
    private final BooleanSetting useTrident = this.add(new BooleanSetting("Trident", true, () -> page.getValue() == Page.Weapons));
    private final BooleanSetting useOther = this.add(new BooleanSetting("Other", true, () -> page.getValue() == Page.Weapons));
    private final BooleanSetting otherOnlyPlayer = this.add(new BooleanSetting("OnlyPlayer", false, () -> page.getValue() == Page.Weapons && useOther.getValue()));

    // Target
    private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, () -> page.getValue() == Page.Target));
    private final EnumSetting<TargetMode> targetMode = this.add(new EnumSetting<>("Filter", TargetMode.DISTANCE, () -> page.getValue() == Page.Target));

    // Render
    private final EnumSetting<TargetESP> mode = this.add(new EnumSetting<>("TargetESP", TargetESP.Fill, () -> page.getValue() == Page.Render));
    private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> page.getValue() == Page.Render).setSuffix("ms"));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> page.getValue() == Page.Render));
    private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render));
    private final ColorSetting outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> page.getValue() == Page.Render));
    private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> page.getValue() == Page.Render));
    private final ColorSetting hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> page.getValue() == Page.Render));
    private final Animation animation = new Animation();

    // Rotate
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> rotate.isOpen() && page.getValue() == Page.Rotate).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> rotate.isOpen() && yawStep.isOpen() && page.getValue() == Page.Rotate));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> page.getValue() == Page.Rotate && yawStep.isOpen()));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> page.getValue() == Page.Rotate && yawStep.isOpen()));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> checkFov.getValue() && page.getValue() == Page.Rotate && yawStep.isOpen()).setSuffix("°"));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, () -> page.getValue() == Page.Rotate && yawStep.isOpen()));

    // Swap
    private final BooleanSetting swapEnabled = this.add(new BooleanSetting("Swap", false, () -> page.getValue() == Page.Swap));
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("Inventory", false, () -> page.getValue() == Page.Swap));
    private final SliderSetting durabilityThreshold = this.add(new SliderSetting("Durability", 10.0, 0.0, 100.0, 1.0, () -> page.getValue() == Page.Swap).setSuffix("%"));
    private final SliderSetting fallSpeed = this.add(new SliderSetting("FallSpeed", 40.0, 0.0, 60.0, 0.1, () -> page.getValue() == Page.Swap).setSuffix("km/h"));

    private final Timer attackTimer = new Timer();
    public Vec3d directionVec = null;

    public Aura() {
        super("Aura", Module.Category.Combat);
        this.setChinese("杀戮光环");
        INSTANCE = this;
    }

    // ================== 渲染相关 ==================
    public static void doRender(MatrixStack stack, float partialTicks, Entity entity, Color color, Color outlineColor, TargetESP mode) {
        switch (mode) {
            case Box:
                Render3DUtil.draw3DBox(stack, ((IEntity) entity).getDimensions().getBoxAt(
                        new Vec3d(
                                MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks)
                        )).expand(0.0, 0.1, 0.0), color, outlineColor, true, true);
                break;
            case Fill:
                Render3DUtil.draw3DBox(stack, ((IEntity) entity).getDimensions().getBoxAt(
                        new Vec3d(
                                MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks)
                        )).expand(0.0, 0.1, 0.0), color, outlineColor, false, true);
                break;
            case Jello:
                JelloUtil.drawJello(stack, entity, color);
                break;
            case ThunderHack:
                Render3DUtil.drawTargetEsp(stack, target, color);
                break;
            case None:
                break;
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (target != null && mode.getValue() != TargetESP.None) {
            this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), target, mode.getValue());
        }
    }

    public void doRender(MatrixStack stack, float partialTicks, Entity entity, TargetESP mode) {
        float progress = (float) animation.get(0.0, animationTime.getValueInt(), ease.getValue());
        Color currentColor = ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), progress);
        Color currentOutline = ColorUtil.fadeColor(outlineColor.getValue(), hitOutlineColor.getValue(), progress);

        switch (mode) {
            case Box:
                Render3DUtil.draw3DBox(stack, ((IEntity) entity).getDimensions().getBoxAt(
                        new Vec3d(
                                MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks)
                        )).expand(0.0, 0.1, 0.0), currentColor, currentOutline, true, true);
                break;
            case Fill:
                Render3DUtil.draw3DBox(stack, ((IEntity) entity).getDimensions().getBoxAt(
                        new Vec3d(
                                MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks),
                                MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks)
                        )).expand(0.0, 0.1, 0.0), currentColor, currentOutline, false, true);
                break;
            case Jello:
                JelloUtil.drawJello(stack, entity, color.getValue());
                break;
            case ThunderHack:
                Render3DUtil.drawTargetEsp(stack, target, color.getValue());
                break;
            case None:
                break;
        }
    }

    @Override
    public String getInfo() {
        return target == null ? null : target.getName().getString();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (Aura.nullCheck()) return;

        if (!isHoldingAllowedWeapon()) {
            target = null;
            return;
        }

        target = this.getTarget(range.getValueFloat());
        if (target == null) {
            target = this.getTarget(targetRange.getValueFloat());
            return;
        }

        this.doAura();
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (target != null && rotate.getValue() && shouldYawStep()) {
            this.directionVec = this.getAttackVec(target);
            event.setTarget(this.directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    // ================== 武器检查 ==================
    private boolean isHoldingAllowedWeapon() {
        ItemStack mainHand = mc.player.getMainHandStack();
        Item item = mainHand.getItem();

        if (item instanceof SwordItem) {
            return useSword.getValue();
        }
        if (item instanceof AxeItem) {
            return useAxe.getValue();
        }
        if (item instanceof TridentItem) {
            return useTrident.getValue();
        }
        if (item.getClass().getSimpleName().equals("MaceItem")) {
            return useMace.getValue();
        }
        return useOther.getValue();
    }

    // ================== 攻击条件 ==================
    private boolean check() {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return false;
        }

        if (attackTimer.getMs() < attackInterval.getValue()) {
            return false;
        }

        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return false;
        }

        return true;
    }

    private void doAura() {
        if (!check()) return;

        Vec3d hitVec = this.getAttackVec(target);

        if (rotate.getValue() && !this.faceVector(hitVec)) return;

        animation.to = 1.0;
        animation.from = 1.0;

        if (shouldUseMaceSpoof()) {
            doMaceSpoofAttack();
        } else {
            normalAttack();
        }

        attackTimer.reset();

        if (rotate.getValue() && !shouldYawStep()) {
            Astra.ROTATION.snapBack();
        }
    }

    private void normalAttack() {
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
    }

    // ================== 重锤切换（完全模仿 Blocker） ==================

    // 获取重锤槽位 - 模仿 Blocker.getObsidian()
    private int getMaceSlot() {
        if (inventorySwap.getValue()) {
            return InventoryUtil.findItemInventorySlot(Items.MACE);
        }
        return InventoryUtil.findItem(Items.MACE);
    }

    // 模仿 Blocker.doSwap()
    private void doSwap(int slot) {
        if (inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    // 模仿 Blocker 的条件检查
    private boolean shouldUseMaceSpoof() {
        if (!swapEnabled.getValue()) return false;
        if (!useMace.getValue()) return false;

        double fallSpeedKmh = Math.abs(mc.player.getVelocity().y) * 3.6;
        if (fallSpeedKmh < fallSpeed.getValue()) return false;

        int slot = getMaceSlot();
        if (slot == -1) return false;

        ItemStack stack = mc.player.getInventory().getStack(slot);
        if (!stack.getItem().equals(Items.MACE)) return false;

        int maxDurability = stack.getMaxDamage();
        if (maxDurability == 0) return true;
        int damage = stack.getDamage();
        float durabilityPercent = (maxDurability - damage) / (float) maxDurability * 100;
        return durabilityPercent >= durabilityThreshold.getValue();
    }

    // 完全模仿 Blocker 的切换逻辑：保存槽位 -> 切换 -> 攻击 -> 恢复
    private void doMaceSpoofAttack() {
        // 保存当前槽位（模仿 Blocker 中的 int n = mc.player.getInventory().selectedSlot;）
        int oldSlot = mc.player.getInventory().selectedSlot;

        // 获取重锤槽位（模仿 Blocker 中的 int block = this.getObsidian();）
        int maceSlot = getMaceSlot();

        // 如果没有重锤或已经在重锤槽位，直接普通攻击
        if (maceSlot == -1 || maceSlot == oldSlot) {
            normalAttack();
            return;
        }

        // 切换到重锤（模仿 Blocker 中的 this.doSwap(block);）
        doSwap(maceSlot);

        // 执行攻击（模仿 Blocker 中的 this.doPlace(defensePos);）
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());

        // 恢复原武器（完全模仿 Blocker 的恢复逻辑）
        if (inventorySwap.getValue()) {
            // 如果 inventorySwap 为 true，再次调用 doSwap(maceSlot) 交换回来
            doSwap(maceSlot);
            EntityUtil.syncInventory();
        } else {
            // 如果 inventorySwap 为 false，切换回原槽位
            doSwap(oldSlot);
        }
    }

    // ================== 辅助方法 ==================
    private Vec3d getAttackVec(Entity entity) {
        return MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }

    private boolean shouldYawStep() {
        if (!whenElytra.getValue() && (mc.player.isFallFlying() ||
                (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying()))) {
            return false;
        }
        return yawStep.getValue();
    }

    public boolean faceVector(Vec3d directionVec) {
        if (!shouldYawStep()) {
            Astra.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        if (Astra.ROTATION.inFov(directionVec, fov.getValueFloat())) {
            return true;
        }
        return !checkFov.getValue();
    }

    public Entity getTarget(double range) {
        Entity target = null;
        double getDistance = range;
        double maxHealth = 36.0;

        for (Entity entity : Astra.THREAD.getEntities()) {
            if (!this.isEnemy(entity)) continue;

            Vec3d hitVec = this.getAttackVec(entity);
            double distance = mc.player.getEyePos().distanceTo(hitVec);

            if (distance > range) continue;
            if (!mc.player.canSee(entity) && distance > wallRange.getValue()) continue;
            if (!CombatUtil.isValid(entity)) continue;

            if (target == null) {
                target = entity;
                getDistance = distance;
                maxHealth = EntityUtil.getHealth(entity);
                continue;
            }

            if (targetMode.getValue() == TargetMode.HEALTH) {
                double health = EntityUtil.getHealth(entity);
                if (health < maxHealth) {
                    target = entity;
                    maxHealth = health;
                }
            } else if (targetMode.getValue() == TargetMode.DISTANCE) {
                if (distance < getDistance) {
                    target = entity;
                    getDistance = distance;
                }
            }
        }

        return target;
    }

    private boolean isEnemy(Entity entity) {
        ItemStack mainHand = mc.player.getMainHandStack();
        Item item = mainHand.getItem();
        boolean isOther = !(item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item.getClass().getSimpleName().equals("MaceItem"));

        if (isOther && otherOnlyPlayer.getValue()) {
            return entity instanceof PlayerEntity && Players.getValue();
        }

        if (entity instanceof SlimeEntity) return Slimes.getValue();
        if (entity instanceof PlayerEntity) return Players.getValue();
        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) return Villagers.getValue();
        if (entity instanceof AnimalEntity) return Animals.getValue();
        if (entity instanceof MobEntity) return Mobs.getValue();
        return false;
    }
}