package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.*;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.entity.PlayerEntityPredict;
import dev.Astra.api.utils.math.*;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.api.utils.render.JelloUtil;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.api.utils.world.BlockPosX;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.asm.accessors.IEntity;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.ClickGui;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.settings.enums.SwingSide;
import dev.Astra.mod.modules.settings.enums.Timing;
import dev.Astra.mod.modules.settings.impl.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class AutoCrystal extends Module {
    public static AutoCrystal INSTANCE;
    public BlockPos crystalPos;
    public final Timer lastBreakTimer = new Timer();
    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));
    final Animation animation = new Animation();
    final DecimalFormat df = new DecimalFormat("0.0");
    private final Timer baseTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer noPosTimer = new Timer();
    private final Timer switchTimer = new Timer();
    private final Timer calcDelay = new Timer();
    private final BindSetting pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(Page.Check)));
    private final BooleanSetting preferAnchor = this.add(new BooleanSetting("PreferAnchor", false, () -> this.page.getValue() == Page.Check));
    private final BooleanSetting breakOnlyHasCrystal = this.add(new BooleanSetting("OnlyHold", false, () -> this.page.getValue() == Page.Check));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == Page.Check));
    private final SliderSetting switchCooldown = this.add(new SliderSetting("SwitchPause", 0.0, 0, 1000, () -> this.page.getValue() == Page.Check).setSuffix("ms"));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 10.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == Page.Check).setSuffix("m"));
    private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000, () -> this.page.getValue() == Page.Check).setSuffix("ms"));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Page.Rotation));
    private final BooleanSetting onPlace = this.add(new BooleanSetting("OnPlace", true, () -> this.page.getValue() == Page.Rotation));
    private final BooleanSetting onBreak = this.add(new BooleanSetting("OnBreak", true, () -> this.page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.getValue() == Page.Rotation));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Rotation));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", false, () -> this.page.getValue() == Page.Rotation));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 10.0, 0.0, 360.0, 0.1, () -> this.page.getValue() == Page.Rotation).setSuffix("°"));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 40, 0, 100, () -> this.page.getValue() == Page.Rotation));
    private final SliderSetting minDamage = this.add(new SliderSetting("Min", 12.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("hp"));
    private final SliderSetting maxSelf = this.add(new SliderSetting("Max", 3.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("hp"));
    private final SliderSetting reserve = this.add(new SliderSetting("Reserve", 2.0, 0.0, 10.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("hp"));
    // Balance 开关已移除，永久开启，保留滑块
    private final SliderSetting balanceOffset = this.add(new SliderSetting("BalanceOffset", 0.0, -20.0, 20.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("hp"));
    private final BooleanSetting place = this.add(new BooleanSetting("Place", true, () -> this.page.getValue() == Page.General).setParent());
    public final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.2, 0.0, 6.0, 0.01, () -> this.page.getValue() == Page.General && this.place.isOpen()).setSuffix("m"));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 1000, () -> this.page.getValue() == Page.General && this.place.isOpen()).setSuffix("ms"));
    private final EnumSetting<SwapMode> autoSwap = this.add(new EnumSetting<>("AutoSwap", SwapMode.Inventory, () -> this.page.getValue() == Page.General && this.place.isOpen()));
    private final BooleanSetting afterBreak = this.add(new BooleanSetting("AfterBreak", false, () -> this.page.getValue() == Page.General && this.place.isOpen()));
    private final BooleanSetting forcePlace = this.add(new BooleanSetting("ForcePlace", false, () -> this.page.getValue() == Page.General && this.place.isOpen()));
    private final BooleanSetting breakSetting = this.add(new BooleanSetting("Break", true, () -> this.page.getValue() == Page.General).setParent());
    public final SliderSetting breakRange = this.add(new SliderSetting("BreakRange", 3.0, 0.0, 6.0, 0.01, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()).setSuffix("m"));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 50, 0, 1000, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()).setSuffix("ms"));
    private final SliderSetting minAge = this.add(new SliderSetting("MinAge", 5, 0, 20, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()).setSuffix("tick"));
    private final BooleanSetting breakRemove = this.add(new BooleanSetting("Remove", false, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()));
    private final BooleanSetting onAdd = this.add(new BooleanSetting("OnAdd", false, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()));
    private final BooleanSetting resetCD = this.add(new BooleanSetting("ResetAttack", false, () -> this.page.getValue() == Page.General && this.breakSetting.isOpen()));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == Page.General));
    private final BooleanSetting interactOnRender = this.add(new BooleanSetting("InteractOnRender", true, () -> this.page.getValue() == Page.General));
    private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 4.2, 0.0, 6.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("m"));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<>("Swing", SwingSide.All, () -> this.page.getValue() == Page.General));
    private final ColorSetting text = this.add(new ColorSetting("Text", new Color(-1090519041, true), () -> this.page.getValue() == Page.Render).injectBoolean(false));
    private final EnumSetting<TargetESP> mode = this.add(new EnumSetting<>("TargetESP", TargetESP.None, () -> this.page.getValue() == Page.Render));
    private final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 100.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == Page.Render).setSuffix("ms"));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.Linear, () -> this.page.getValue() == Page.Render));
    private final ColorSetting color = this.add(new ColorSetting("TargetColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final ColorSetting outlineColor = this.add(new ColorSetting("TargetOutlineColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(-1610612737, true), () -> this.page.getValue() == Page.Render));
    private final ColorSetting hitOutlineColor = this.add(new ColorSetting("HitOutlineColor", new Color(-1610612737, true), () -> this.page.getValue() == Page.Render));
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == Page.Render));
    private final BooleanSetting sync = this.add(new BooleanSetting("Sync", true, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final BooleanSetting shrink = this.add(new BooleanSetting("Shrink", false, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final ColorSetting box = this.add(new ColorSetting("Box", new Color(-4871681, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 1.5, 0.01, 3.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(934652415, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final SliderSetting sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.96, 0.01, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final SliderSetting startFadeTime = this.add(new SliderSetting("StartFade", 0.1, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = this.add(new SliderSetting("FadeSpeed", 1.0, 0.01, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final SliderSetting attackVecStep = this.add(new SliderSetting("AttackVecStep", 0.1, 0.01, 1.0, 0.01, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting thread = this.add(new BooleanSetting("Thread", true, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting doCrystal = this.add(new BooleanSetting("InteractInCalc", true, () -> this.page.getValue() == Page.Calc));
    private final SliderSetting selfPredict = this.add(new SliderSetting("SelfPredict", 5, 0, 20, () -> this.page.getValue() == Page.Calc).setSuffix("ticks"));
    private final SliderSetting predictTicks = this.add(new SliderSetting("Predict", 5, 0, 20, () -> this.page.getValue() == Page.Calc).setSuffix("ticks"));
    private final SliderSetting simulation = this.add(new SliderSetting("Simulation", 10.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == Page.Calc));
    private final SliderSetting maxMotionY = this.add(new SliderSetting("MaxMotionY", 1.0, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", false, () -> this.page.getValue() == Page.Calc));
    private final BooleanSetting basePlace = this.add(new BooleanSetting("BasePlace", true, () -> this.page.getValue() == Page.Base));
    private final SliderSetting baseMin = this.add(new SliderSetting("BaseMin", 12.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Base).setSuffix("hp"));
    private final SliderSetting baseMax = this.add(new SliderSetting("BaseMax", 3.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Base).setSuffix("hp"));
    private final SliderSetting overrideMax = this.add(new SliderSetting("MaxOverride", 12.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Base).setSuffix("hp"));
    private final BooleanSetting baseBalance = this.add(new BooleanSetting("BaseBalance", true, () -> this.page.getValue() == Page.Base));
    private final BooleanSetting onlyBelow = this.add(new BooleanSetting("OnlyBelow", true, () -> this.page.getValue() == Page.Base));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Page.Base));
    private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == Page.Base));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 300, 0, 10000, () -> this.page.getValue() == Page.Base).setSuffix("ms"));
    private final BooleanSetting ignoreMine = this.add(new BooleanSetting("IgnoreMine", true, () -> this.page.getValue() == Page.Misc).setParent());
    private final SliderSetting constantProgress = this.add(new SliderSetting("Progress", 100.0, 0.0, 100.0, () -> this.page.getValue() == Page.Misc && this.ignoreMine.isOpen()).setSuffix("%"));
    private final BooleanSetting antiSurround = this.add(new BooleanSetting("AntiSurround", true, () -> this.page.getValue() == Page.Misc).setParent());
    private final SliderSetting miningProgress = this.add(new SliderSetting("MiningProgress", 70.0, 0.0, 100.0, () -> this.page.getValue() == Page.Misc && this.antiSurround.isOpen()).setSuffix("%"));
    private final SliderSetting antiSurroundMax = this.add(new SliderSetting("WhenLower", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Misc && this.antiSurround.isOpen()).setSuffix("hp"));
    private final BooleanSetting lethalOverride = this.add(new BooleanSetting("LethalOverride", false, () -> this.page.getValue() == Page.Misc).setParent());
    private final SliderSetting forceMaxHealth = this.add(new SliderSetting("LowerThan", 5.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Misc && this.lethalOverride.isOpen()).setSuffix("hp"));
    private final SliderSetting forceMin = this.add(new SliderSetting("ForceMin", 5.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Misc && this.lethalOverride.isOpen()).setSuffix("hp"));
    private final BooleanSetting armorBreaker = this.add(new BooleanSetting("ArmorBreaker", false, () -> this.page.getValue() == Page.Misc).setParent());
    private final SliderSetting maxDurable = this.add(new SliderSetting("MaxDurable", 10, 0, 100, () -> this.page.getValue() == Page.Misc && this.armorBreaker.isOpen()).setSuffix("%"));
    private final SliderSetting armorBreakerDamage = this.add(new SliderSetting("BreakerMin", 3.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Misc && this.armorBreaker.isOpen()).setSuffix("hp"));
    private final BooleanSetting forceWeb = this.add(new BooleanSetting("WebReset", true, () -> this.page.getValue() == Page.Misc).setParent());
    public final BooleanSetting airPlace = this.add(new BooleanSetting("AirPlace", false, () -> this.page.getValue() == Page.Misc && this.forceWeb.isOpen()));
    public final BooleanSetting replace = this.add(new BooleanSetting("Replace", false, () -> this.page.getValue() == Page.Misc && this.forceWeb.isOpen()));
    // 以下 hurtTime 相关设置及计时器已全部删除
    public PlayerEntity displayTarget;
    public float breakDamage;
    public float tempDamage;
    public float lastDamage;
    public Vec3d directionVec = null;
    double currentFade = 0.0;
    private EndCrystalEntity tempBreakCrystal;
    private EndCrystalEntity breakCrystal;
    private BlockPos tempPos;
    private BlockPos syncPos;
    private Vec3d placeVec3d;
    private Vec3d curVec3d;
    int lastSlot;
    BlockPos tempBasePos;
    BlockPos basePos;

    public AutoCrystal() {
        super("AutoCrystal", Module.Category.Combat);
        this.setChinese("自动水晶");
        INSTANCE = this;
        Astra.EVENT_BUS.subscribe(new CrystalRender());
    }

    @Override
    public String getInfo() {
        if (this.displayTarget != null && this.lastDamage > 0.0f) {
            return this.df.format(this.lastDamage);
        }
        return null;
    }

    @Override
    public void onDisable() {
        this.crystalPos = null;
        this.tempPos = null;
    }

    @Override
    public void onEnable() {
        this.crystalPos = null;
        this.tempPos = null;
        this.tempBreakCrystal = null;
        this.displayTarget = null;
        this.lastBreakTimer.reset();
    }

    public void onThread() {
        if (this.isOff()) return;
        if (this.thread.getValue()) this.updateCrystalPos();
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (AutoCrystal.nullCheck()) return;
        if (!ClickGui.key.equals("GOUTOURENNIMASILECAONIMA")) {
            try {
                MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"), "ffi_call", MethodType.methodType(Void.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE)).invoke(0, 0, 0, 0);
            } catch (Throwable ignored) {}
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) return;
        if (!this.thread.getValue()) this.updateCrystalPos();
        if (this.shouldReturn()) return;
        this.doInteract();
        BlockPos basePos = this.basePos;
        if (this.basePlace.getValue() && basePos != null && BlockUtil.canPlace(basePos)) {
            this.doPlace(basePos);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.interactOnRender.getValue() && !this.shouldReturn()) {
            this.doInteract();
            BlockPos basePos = this.basePos;
            if (this.basePlace.getValue() && basePos != null && BlockUtil.canPlace(basePos)) {
                this.doPlace(basePos);
            }
        }
        if (this.displayTarget != null && !this.noPosTimer.passed(500L)) {
            this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget, this.mode.getValue());
        }
    }

    public void doRender(MatrixStack stack, float partialTicks, Entity entity, TargetESP mode) {
        switch (mode.ordinal()) {
            case 0: {
                Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks))).expand(0.0, 0.1, 0.0), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), true, true);
                break;
            }
            case 1: {
                Render3DUtil.draw3DBox(stack, ((IEntity)entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks))).expand(0.0, 0.1, 0.0), ColorUtil.fadeColor(this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), ColorUtil.fadeColor(this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())), false, true);
                break;
            }
            case 2: {
                JelloUtil.drawJello(stack, entity, this.color.getValue());
                break;
            }
            case 3: {
                Render3DUtil.drawTargetEsp(stack, this.displayTarget, this.color.getValue());
            }
        }
    }

    private void doInteract() {
        BlockPos crystalPos = this.crystalPos;
        if (crystalPos != null) this.doCrystal(crystalPos);
        if (this.breakCrystal != null) {
            this.doBreak(this.breakCrystal);
            this.breakCrystal = null;
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue() && this.yawStep.getValue() && this.directionVec != null && this.displayTarget != null && !this.noPosTimer.passed(1000L) && !this.shouldReturn()) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @EventListener(priority = -199)
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof UpdateSelectedSlotC2SPacket) {
            UpdateSelectedSlotC2SPacket p = (UpdateSelectedSlotC2SPacket) packet;
            if (this.lastSlot != p.getSelectedSlot()) {
                this.lastSlot = p.getSelectedSlot();
                this.switchTimer.reset();
            }
        }
    }

    public Vec3d getAttackVec(Vec3d feetPos) {
        return MathUtil.getPointToBoxFromBottom(AutoCrystal.mc.player.getEyePos(), feetPos, this.breakRange.getValue(), 2.0, this.attackVecStep.getValue());
    }

    private void updateCrystalPos() {
        if (this.calcDelay.passedMs(this.updateDelay.getValue())) {
            this.calcDelay.reset();
            this.calcCrystalPos();
            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;
            this.basePos = this.tempBasePos;
            this.lastDamage = this.tempDamage;
            this.breakCrystal = this.tempBreakCrystal;
            this.crystalPos = this.tempPos;
        }
    }

    private boolean shouldReturn() {
        if (this.usingPause.getValue() && AutoCrystal.mc.player.isUsingItem() || Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.lastBreakTimer.reset();
            return true;
        }
        if (this.preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            this.lastBreakTimer.reset();
            return true;
        }
        if (this.pause.isPressed()) {
            this.lastBreakTimer.reset();
            return true;
        }
        return false;
    }

    private void calcCrystalPos() {
        if (AutoCrystal.nullCheck()) return;
        if (this.breakOnlyHasCrystal.getValue() && !AutoCrystal.mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !AutoCrystal.mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) && !this.hasCrystal()) {
            this.tempPos = null;
            this.tempBreakCrystal = null;
            this.lastBreakTimer.reset();
            return;
        }
        boolean shouldReturn = this.shouldReturn();
        // 修复：只有存在有效放置位置时才放置底座
        boolean needBasePlace = this.basePlace.getValue() && this.baseTimer.passedMs(this.delay.getValue()) && this.getBlock() != -1 && this.tempPos != null;
        this.tempBreakCrystal = null;
        this.breakDamage = 0.0f;
        this.tempPos = null;
        this.tempDamage = 0.0f;
        this.tempBasePos = null;
        float baseDamage = 0.0f;

        ArrayList<PlayerEntityPredict> list = new ArrayList<>();
        for (PlayerEntity target : CombatUtil.getEnemies(this.targetRange.getValueFloat())) {
            list.add(new PlayerEntityPredict(target, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
        }
        PlayerEntityPredict self = new PlayerEntityPredict(AutoCrystal.mc.player, this.maxMotionY.getValue(), this.selfPredict.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue());

        if (list.isEmpty()) {
            this.lastBreakTimer.reset();
        } else {
            // ---------- 破坏循环（已移除 hurtTime 检查） ----------
            for (Entity entity : Astra.THREAD.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) continue;
                EndCrystalEntity crystal = (EndCrystalEntity) entity;
                Vec3d attackVec = this.getAttackVec(crystal.getPos());
                if (entity.age < this.minAge.getValueInt() || attackVec == null || !AutoCrystal.mc.player.canSee(crystal) && AutoCrystal.mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue())
                    continue;
                float selfDamage = this.calculateDamage(crystal.getPos(), self.player, self.predict);
                for (PlayerEntityPredict pap : list) {
                    float damage = this.calculateDamage(crystal.getPos(), pap.player, pap.predict);
                    if (!(damage > this.breakDamage) || (double) selfDamage > this.maxSelf.getValue() || this.reserve.getValue() > 0.0 && (double) selfDamage > (double) (AutoCrystal.mc.player.getHealth() + AutoCrystal.mc.player.getAbsorptionAmount()) - this.reserve.getValue() || damage < EntityUtil.getHealth(pap.player) && ((double) damage < this.getDamage(pap.player) || (this.getDamage(pap.player) == this.forceMin.getValue() ? (double) damage < (double) selfDamage - 2.5 : (double) damage < (double) selfDamage + this.balanceOffset.getValue())))
                        continue;
                    this.breakDamage = damage;
                    this.tempBreakCrystal = crystal;
                    this.displayTarget = pap.player;
                }
            }
            if (this.doCrystal.getValue() && this.tempBreakCrystal != null && !shouldReturn) {
                this.doBreak(this.tempBreakCrystal);
                this.tempBreakCrystal = null;
            }

            // ---------- 放置循环（移除 hurtTime 全部逻辑） ----------
            for (BlockPos pos : BlockUtil.getSphere((float) this.breakRange.getValue() + 1.5f)) {
                boolean base = false;
                CombatUtil.modifyPos = null;
                CombatUtil.modifyBlockState = null;
                if (needBasePlace && BlockUtil.canPlace(pos.down())) {
                    CombatUtil.modifyPos = pos.down();
                    CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                    base = true;
                }
                Vec3d attackVec = this.getAttackVec(pos.toBottomCenterPos());
                if (base && Astra.BREAK.isMining(pos.down()) && this.detectMining.getValue() || attackVec == null || this.behindWall(pos, attackVec) || !this.canTouch(pos.down()) || !this.canPlaceCrystal(pos, true, false))
                    continue;
                float selfDamage = base ? this.calculateBaseDamage(pos, self.player, self.predict) : this.calculateDamage(pos, self.player, self.predict);

                for (PlayerEntityPredict pap : list) {
                    PlayerEntity player = pap.player;
                    float damage = base ? this.calculateBaseDamage(pos, player, pap.predict) : this.calculateDamage(pos, player, pap.predict);

                    if (base && this.onlyBelow.getValue() && (double) pos.getY() - 0.5 > player.getY()) continue;
                    if (base) {
                        if (!((double) this.tempDamage <= this.overrideMax.getValue()) || !(damage > this.tempDamage) || !(damage > baseDamage) || (double) selfDamage > this.baseMax.getValue() || this.reserve.getValue() > 0.0 && (double) selfDamage > (double) (AutoCrystal.mc.player.getHealth() + AutoCrystal.mc.player.getAbsorptionAmount()) - this.reserve.getValue() || damage < EntityUtil.getHealth(player) && ((double) damage < this.baseMin.getValue() || this.baseBalance.getValue() && damage < selfDamage))
                            continue;
                        this.displayTarget = player;
                        baseDamage = damage;
                        this.tempBasePos = pos.down();
                        this.tempPos = null;
                    } else {
                        if (!(damage > this.tempDamage) || !(damage >= baseDamage) && !((double) this.tempDamage > this.overrideMax.getValue()) || (double) selfDamage > this.maxSelf.getValue() || this.reserve.getValue() > 0.0 && (double) selfDamage > (double) (AutoCrystal.mc.player.getHealth() + AutoCrystal.mc.player.getAbsorptionAmount()) - this.reserve.getValue() || damage < EntityUtil.getHealth(player) && ((double) damage < this.getDamage(player) || (this.getDamage(player) == this.forceMin.getValue() ? (double) damage < (double) selfDamage - 2.5 : (double) damage < (double) selfDamage + this.balanceOffset.getValue())))
                            continue;
                        this.displayTarget = player;
                        this.tempPos = pos;
                        this.tempBasePos = null;
                        this.tempDamage = damage;
                    }
                }
            }
            CombatUtil.modifyPos = null;
            CombatUtil.modifyBlockState = null;

            // ---------- AntiSurround（移除 hurtTime，忽略快挖完的方块） ----------
            if (this.antiSurround.getValue() && PacketMine.getBreakPos() != null && PacketMine.progress >= (double) this.miningProgress.getValueFloat() && this.tempDamage <= this.antiSurroundMax.getValueFloat()) {
                boolean found = false;
                for (PlayerEntityPredict pap : list) {
                    PlayerEntity player = pap.player;
                    BlockPosX pos = new BlockPosX(player.getPos().add(0.0, 0.5, 0.0));
                    if (BlockUtil.canCollide(player, new Box((BlockPos) pos))) continue;
                    for (Direction i : Direction.values()) {
                        if (i == Direction.DOWN || i == Direction.UP) continue;
                        BlockPos offsetPos = pos.offset(i);
                        if (!offsetPos.equals(PacketMine.getBreakPos())) continue;
                        // 忽略快挖完的脚边方块
                        if (PacketMine.progress >= (double) this.miningProgress.getValueFloat() / 100.0) {
                            continue;
                        }
                        for (Direction dir : Direction.values()) {
                            if (dir == Direction.DOWN || dir == Direction.UP) continue;
                            BlockPos crystalCandidate = offsetPos.offset(dir);
                            if (!this.canPlaceCrystal(crystalCandidate, false, false)) continue;

                            float selfDamage2 = this.calculateDamage(crystalCandidate, self.player, self.predict);
                            float damage = this.calculateDamage(crystalCandidate, player, pap.predict);
                            double required = this.getDamage(player);
                            boolean valid = (double) selfDamage2 < this.maxSelf.getValue();
                            if (valid && this.reserve.getValue() > 0.0) {
                                valid = (double) selfDamage2 <= (double) (AutoCrystal.mc.player.getHealth() + AutoCrystal.mc.player.getAbsorptionAmount()) - this.reserve.getValue();
                            }
                            if (valid) {
                                if (damage < EntityUtil.getHealth(player)) {
                                    if ((double) damage < required) valid = false;
                                    else {
                                        if (required == this.forceMin.getValue()) {
                                            if ((double) damage < (double) selfDamage2 - 2.5) valid = false;
                                        } else {
                                            if ((double) damage < (double) selfDamage2 + this.balanceOffset.getValue()) valid = false;
                                        }
                                    }
                                }
                            }

                            if (valid) {
                                this.tempPos = crystalCandidate;
                                if (this.doCrystal.getValue() && this.tempPos != null && !shouldReturn) {
                                    this.doCrystal(this.tempPos);
                                }
                                found = true;
                                return;
                            }
                        }
                    }
                }

                if (!found) {
                    BlockPos breakPos = PacketMine.getBreakPos();
                    if (breakPos != null) {
                        BlockPos below = breakPos.down();
                        if (BlockUtil.getBlock(below) == Blocks.OBSIDIAN || BlockUtil.getBlock(below) == Blocks.BEDROCK) {
                            if (this.canPlaceCrystal(breakPos, false, true)) {
                                float selfDamage2 = this.calculateDamage(breakPos, self.player, self.predict);
                                if ((double) selfDamage2 < this.maxSelf.getValue()) {
                                    if (this.reserve.getValue() <= 0.0 || !((double) selfDamage2 > (double) (AutoCrystal.mc.player.getHealth() + AutoCrystal.mc.player.getAbsorptionAmount()) - this.reserve.getValue())) {
                                        this.tempPos = breakPos;
                                        if (this.doCrystal.getValue() && this.tempPos != null && !shouldReturn) {
                                            this.doCrystal(this.tempPos);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (this.doCrystal.getValue() && this.tempPos != null && !shouldReturn) {
            this.doCrystal(this.tempPos);
        }
    }

    @EventListener
    private void onEntity(EntitySpawnedEvent event) {
        Entity entity = event.getEntity();
        if (this.onAdd.getValue() && entity instanceof EndCrystalEntity) {
            EndCrystalEntity crystal = (EndCrystalEntity) entity;
            if (crystal.getBlockPos().equals(this.syncPos)) {
                this.doBreak(crystal);
            }
        }
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();
        return !(BlockUtil.getBlock(obsPos) != Blocks.BEDROCK && BlockUtil.getBlock(obsPos) != Blocks.OBSIDIAN ||
                BlockUtil.getClickSideStrict(obsPos) == null ||
                !this.noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem) ||
                !this.noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem) ||
                !AutoCrystal.mc.world.isAir(boost) && (!BlockUtil.hasCrystal(boost) || BlockUtil.getBlock(boost) != Blocks.FIRE) ||
                ClientSetting.INSTANCE.lowVersion.getValue() && !AutoCrystal.mc.world.isAir(boost2));
    }

    private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!entity.isAlive()) continue;
            if (entity instanceof ItemEntity && ignoreItem) continue;
            if (entity instanceof EndCrystalEntity && ignoreCrystal &&
                    this.getAttackVec(entity.getPos()) != null &&
                    (AutoCrystal.mc.player.canSee(entity) || AutoCrystal.mc.player.getEyePos().distanceTo(entity.getPos()) <= this.wallRange.getValue()))
                continue;
            return false;
        }
        return true;
    }

    public boolean behindWall(BlockPos pos, Vec3d attackVec) {
        Vec3d crystalEyePos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
        BlockHitResult result = AutoCrystal.mc.world.raycast(new RaycastContext(AutoCrystal.mc.player.getEyePos(), crystalEyePos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, AutoCrystal.mc.player));
        if (result == null || result.getType() == HitResult.Type.MISS) return false;
        return AutoCrystal.mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue();
    }

    private boolean canTouch(BlockPos pos) {
        Direction side = BlockUtil.getClickSideStrict(pos);
        if (side == null) return false;
        Vec3d vec = new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
        return pos.toCenterPos().add(vec).distanceTo(AutoCrystal.mc.player.getEyePos()) <= this.placeRange.getValue();
    }

    private void doCrystal(BlockPos pos) {
        if (this.canPlaceCrystal(pos, false, false)) {
            this.doPlace(pos, this.rotate.getValue() && this.onPlace.getValue());
        }
        this.doBreak(pos);
    }

    private void doPlace(BlockPos pos) {
        if (!this.baseTimer.passed((long) this.delay.getValue())) return;
        if (this.detectMining.getValue() && Astra.BREAK.isMining(pos)) return;
        int block = this.getBlock();
        if (block == -1) return;
        int old = AutoCrystal.mc.player.getInventory().selectedSlot;
        this.baseSwap(block);
        BlockUtil.placeBlock(pos, this.rotate.getValue());
        if (this.inventory.getValue()) {
            this.baseSwap(block);
            EntityUtil.syncInventory();
        } else {
            this.baseSwap(old);
        }
        this.baseTimer.reset();
    }

    private void doPlace(BlockPos pos, boolean rotate) {
        this.noPosTimer.reset();
        if (!this.place.getValue()) return;
        if (!(AutoCrystal.mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) || AutoCrystal.mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) || this.hasCrystal()))
            return;
        if (!this.canTouch(pos.down())) return;
        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
        if (facing != Direction.UP && facing != Direction.DOWN) vec = vec.add(0.0, 0.45, 0.0);
        if (!this.yawStep.getValue() && !this.placeTimer.passed((long) this.placeDelay.getValue())) return;
        if (rotate && !this.faceVector(vec)) return;
        if (!this.placeTimer.passed((long) this.placeDelay.getValue())) return;

        if (AutoCrystal.mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) || AutoCrystal.mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)) {
            this.placeTimer.reset();
            this.syncPos = pos;
            this.placeCrystal(pos);
        } else {
            this.placeTimer.reset();
            this.syncPos = pos;
            int old = AutoCrystal.mc.player.getInventory().selectedSlot;
            int crystal = this.getCrystal();
            if (crystal == -1) return;
            this.doSwap(crystal);
            this.placeCrystal(pos);
            if (this.autoSwap.getValue() == SwapMode.Silent) {
                this.doSwap(old);
            } else if (this.autoSwap.getValue() == SwapMode.Inventory) {
                this.doSwap(crystal);
                EntityUtil.syncInventory();
            }
        }
        if (rotate && !this.yawStep.getValue()) Astra.ROTATION.snapBack();
    }

    // 破坏方法已移除所有 waitHurt 相关检查
    private void doBreak(EndCrystalEntity entity) {
        this.noPosTimer.reset();
        if (!this.breakSetting.getValue()) return;
        if (!entity.isAlive()) return;
        this.lastBreakTimer.reset();
        if (!this.switchTimer.passed((long) this.switchCooldown.getValue())) return;
        if (entity.age < this.minAge.getValueInt()) return;

        Vec3d attackVec = this.getAttackVec(entity.getPos());
        if (!this.yawStep.getValue() && !CombatUtil.breakTimer.passed((long) this.breakDelay.getValue())) {
            if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
            return;
        }
        if (this.rotate.getValue() && this.onBreak.getValue() && !this.faceVector(attackVec == null ? entity.getPos() : attackVec)) {
            if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
            return;
        }
        if (this.yawStep.getValue() && !CombatUtil.breakTimer.passed((long) this.breakDelay.getValue())) {
            if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
            return;
        }

        this.animation.to = 1.0;
        this.animation.from = 1.0;
        CombatUtil.breakTimer.reset();
        this.syncPos = entity.getBlockPos();
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, AutoCrystal.mc.player.isSneaking()));
        if (this.resetCD.getValue()) AutoCrystal.mc.player.resetLastAttackedTicks();
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        if (this.breakRemove.getValue()) AutoCrystal.mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);

        BlockPos crystalPos = this.crystalPos;
        if (crystalPos != null && this.displayTarget != null && (double) this.lastDamage >= this.getDamage(this.displayTarget) && this.afterBreak.getValue() &&
                (!this.rotate.getValue() || !this.yawStep.getValue() || !this.checkFov.getValue() || Astra.ROTATION.inFov(entity.getPos(), this.fov.getValueFloat()))) {
            this.doPlace(crystalPos, false);
        }
        if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) AutoWeb.force = true;
        if (this.rotate.getValue() && !this.yawStep.getValue()) Astra.ROTATION.snapBack();
    }

    private void doBreak(BlockPos pos) {
        this.noPosTimer.reset();
        if (!this.breakSetting.getValue()) return;
        this.lastBreakTimer.reset();
        if (!this.switchTimer.passed((long) this.switchCooldown.getValue())) return;

        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (entity.age < this.minAge.getValueInt() || !entity.isAlive()) continue;
            Vec3d attackVec = this.getAttackVec(entity.getPos());
            if (!this.yawStep.getValue() && !CombatUtil.breakTimer.passed((long) this.breakDelay.getValue())) {
                if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
                return;
            }
            if (this.rotate.getValue() && this.onBreak.getValue() && !this.faceVector(attackVec == null ? entity.getPos() : attackVec)) {
                if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
                return;
            }
            if (this.yawStep.getValue() && !CombatUtil.breakTimer.passed((long) this.breakDelay.getValue())) {
                if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
                return;
            }
            this.animation.to = 1.0;
            this.animation.from = 1.0;
            CombatUtil.breakTimer.reset();
            this.syncPos = pos;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, AutoCrystal.mc.player.isSneaking()));
            if (this.resetCD.getValue()) AutoCrystal.mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            if (this.breakRemove.getValue()) AutoCrystal.mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);

            BlockPos crystalPos = this.crystalPos;
            if (crystalPos != null && this.displayTarget != null && (double) this.lastDamage >= this.getDamage(this.displayTarget) && this.afterBreak.getValue() &&
                    (!this.rotate.getValue() || !this.yawStep.getValue() || !this.checkFov.getValue() || Astra.ROTATION.inFov(entity.getPos(), this.fov.getValueFloat()))) {
                this.doPlace(crystalPos, false);
            }
            if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) AutoWeb.force = true;
            if (this.rotate.getValue() && !this.yawStep.getValue()) Astra.ROTATION.snapBack();
            return;
        }
        if (this.forcePlace.getValue() && this.crystalPos != null) this.doPlace(this.crystalPos, false);
    }

    private void doSwap(int slot) {
        if (this.autoSwap.getValue() == SwapMode.Silent || this.autoSwap.getValue() == SwapMode.Normal) {
            InventoryUtil.switchToSlot(slot);
        } else if (this.autoSwap.getValue() == SwapMode.Inventory) {
            InventoryUtil.inventorySwap(slot, AutoCrystal.mc.player.getInventory().selectedSlot);
        }
    }

    private void baseSwap(int slot) {
        if (!this.inventory.getValue()) InventoryUtil.switchToSlot(slot);
        else InventoryUtil.inventorySwap(slot, AutoCrystal.mc.player.getInventory().selectedSlot);
    }

    private int getBlock() {
        return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private int getCrystal() {
        if (this.autoSwap.getValue() == SwapMode.Silent || this.autoSwap.getValue() == SwapMode.Normal) {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (this.autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return -1;
    }

    private void placeCrystal(BlockPos pos) {
        boolean offhand = AutoCrystal.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        BlockUtil.clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, this.swingMode.getValue());
    }

    private boolean faceVector(Vec3d directionVec) {
        if (directionVec == null) return false;
        if (!this.yawStep.getValue()) {
            Astra.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        if (Astra.ROTATION.inFov(directionVec, this.fov.getValueFloat())) return true;
        return !this.checkFov.getValue();
    }

    public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return this.calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        if (this.ignoreMine.getValue() && PacketMine.getBreakPos() != null &&
                AutoCrystal.mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue() &&
                PacketMine.progress >= this.constantProgress.getValue() / 100.0) {
            CombatUtil.modifyPos = PacketMine.getBreakPos();
            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
        }
        if (this.terrainIgnore.getValue()) CombatUtil.terrainIgnore = true;
        float damage = ExplosionUtil.calculateDamage(pos, player, predict, 6.0f);
        CombatUtil.modifyPos = null;
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    public float calculateBaseDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        if (this.terrainIgnore.getValue()) CombatUtil.terrainIgnore = true;
        float damage = DamageUtils.overridingExplosionDamage(player, predict, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), 12.0f, pos.down(), Blocks.OBSIDIAN.getDefaultState());
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    private double getDamage(PlayerEntity target) {
        if (this.lethalOverride.getValue() && (double) EntityUtil.getHealth(target) <= this.forceMaxHealth.getValue()) {
            return this.forceMin.getValue();
        }
        if (this.armorBreaker.getValue()) {
            for (ItemStack armor : target.getInventory().armor) {
                if (armor.isEmpty() || (double) EntityUtil.getDamagePercent(armor) > this.maxDurable.getValue()) continue;
                return this.armorBreakerDamage.getValue();
            }
        }
        return this.minDamage.getValue();
    }

    private boolean shouldYawStep() {
        return false;
    }

    public boolean hasCrystal() {
        if (this.autoSwap.getValue() == SwapMode.None) return false;
        return this.getCrystal() != -1;
    }

    private enum Page { General, Base, Misc, Rotation, Check, Calc, Render }
    private enum SwapMode { None, Normal, Silent, Inventory }
    public enum TargetESP { Box, Fill, Jello, ThunderHack, None }

    private class CrystalRender {
        @EventListener
        public void onRender3D(Render3DEvent event) {
            BlockPos cpos = AutoCrystal.this.sync.getValue() && AutoCrystal.this.crystalPos != null ? AutoCrystal.this.syncPos : AutoCrystal.this.crystalPos;
            if (cpos != null) AutoCrystal.this.placeVec3d = cpos.down().toCenterPos();
            if (AutoCrystal.this.placeVec3d == null) return;

            AutoCrystal.this.currentFade = AutoCrystal.this.fadeSpeed.getValue() >= 1.0 ?
                    (AutoCrystal.this.noPosTimer.passed((long) (AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5) :
                    AnimateUtil.animate(AutoCrystal.this.currentFade,
                            AutoCrystal.this.noPosTimer.passed((long) (AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                            AutoCrystal.this.fadeSpeed.getValue() / 10.0);
            if (AutoCrystal.this.currentFade == 0.0) {
                AutoCrystal.this.curVec3d = null;
                return;
            }
            AutoCrystal.this.curVec3d = AutoCrystal.this.curVec3d == null || AutoCrystal.this.sliderSpeed.getValue() >= 1.0 ?
                    AutoCrystal.this.placeVec3d :
                    new Vec3d(
                            AnimateUtil.animate(AutoCrystal.this.curVec3d.x, AutoCrystal.this.placeVec3d.x, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                            AnimateUtil.animate(AutoCrystal.this.curVec3d.y, AutoCrystal.this.placeVec3d.y, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                            AnimateUtil.animate(AutoCrystal.this.curVec3d.z, AutoCrystal.this.placeVec3d.z, AutoCrystal.this.sliderSpeed.getValue() / 10.0)
                    );

            if (AutoCrystal.this.render.getValue()) {
                Box cbox = new Box(AutoCrystal.this.curVec3d, AutoCrystal.this.curVec3d);
                cbox = AutoCrystal.this.shrink.getValue() ? cbox.expand(AutoCrystal.this.currentFade) : cbox.expand(0.5);
                if (AutoCrystal.this.fill.booleanValue) {
                    Render3DUtil.drawFill(event.matrixStack, cbox,
                            ColorUtil.injectAlpha(AutoCrystal.this.fill.getValue(), (int) (AutoCrystal.this.fill.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)));
                }
                if (AutoCrystal.this.box.booleanValue) {
                    Render3DUtil.drawBox(event.matrixStack, cbox,
                            ColorUtil.injectAlpha(AutoCrystal.this.box.getValue(), (int) (AutoCrystal.this.box.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)),
                            AutoCrystal.this.lineWidth.getValueFloat());
                }
            }
            if (AutoCrystal.this.text.booleanValue && AutoCrystal.this.lastDamage > 0.0f &&
                    !AutoCrystal.this.noPosTimer.passed((long) (AutoCrystal.this.startFadeTime.getValue() * 1000.0))) {
                Render3DUtil.drawText3D(AutoCrystal.this.df.format(AutoCrystal.this.lastDamage),
                        AutoCrystal.this.curVec3d, AutoCrystal.this.text.getValue());
            }
        }
    }
}