package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.Render3DEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.entity.PlayerEntityPredict;
import dev.Astra.api.utils.math.AnimateUtil;
import dev.Astra.api.utils.math.ExplosionUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.impl.player.AirPlace;
import dev.Astra.mod.modules.settings.enums.SwingSide;
import dev.Astra.mod.modules.settings.enums.Timing;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
 import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoAnchor extends Module {
    public static AutoAnchor INSTANCE;
    static Vec3d placeVec3d;
    static Vec3d curVec3d;

    public final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.General));

    public final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("m"));
    public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1, 12.0, 0.1, () -> this.page.getValue() == Page.General).setSuffix("m"));
    private final BooleanSetting preferCrystal = this.add(new BooleanSetting("PreferCrystal", false, () -> this.page.getValue() == Page.General));
    private final BooleanSetting thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == Page.General));
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == Page.General));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<SwingSide>("Swing", SwingSide.All, () -> this.page.getValue() == Page.General));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<Timing>("Timing", Timing.All, () -> this.page.getValue() == Page.General));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting fillDelay = this.add(new SliderSetting("FillDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting breakDelay = this.add(new SliderSetting("BreakDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting spamDelay = this.add(new SliderSetting("SpamDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == Page.General).setSuffix("ms"));
    private final BooleanSetting multiPlaceUse = this.add(new BooleanSetting("Use", false, () -> this.page.getValue() == Page.MultiPlace));
    private final SliderSetting multiPlaceRange = this.add(new SliderSetting("Range", 3, 1, 6, 1, () -> this.page.getValue() == Page.MultiPlace).setSuffix("m"));
    private final SliderSetting multiPlaceMaxNumber = this.add(new SliderSetting("MaxNumber", 8, 1, 36, 1, () -> this.page.getValue() == Page.MultiPlace));

    private final BooleanSetting burstMode = this.add(new BooleanSetting("BurstMode", false, () -> this.page.getValue() == Page.Burst));
    private final SliderSetting burstPrePlace = this.add(new SliderSetting("PrePlace", 0.85, 0.5, 1.0, 0.01, () -> this.page.getValue() == Page.Burst && this.burstMode.getValue()));
    private final SliderSetting burstLatency = this.add(new SliderSetting("Latency", 50, 0, 500, 1, () -> this.page.getValue() == Page.Burst && this.burstMode.getValue()).setSuffix("ms"));
    private final BooleanSetting burstSmart = this.add(new BooleanSetting("Smart", false, () -> this.page.getValue() == Page.Burst && this.burstMode.getValue()));

    private final BooleanSetting spam = this.add(new BooleanSetting("Spam", true, () -> this.page.getValue() == Page.General).setParent());
    private final BooleanSetting mineSpam = this.add(new BooleanSetting("OnlyMining", true, () -> this.page.getValue() == Page.General && this.spam.isOpen()));
    private final BooleanSetting spamPlace = this.add(new BooleanSetting("Fast", true, () -> this.page.getValue() == Page.General).setParent());
    private final BooleanSetting inSpam = this.add(new BooleanSetting("WhenSpamming", true, () -> this.page.getValue() == Page.General && this.spamPlace.isOpen()));

    public final SliderSetting minDamage = this.add(new SliderSetting("Min", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    public final SliderSetting breakMin = this.add(new SliderSetting("ExplosionMin", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    public final SliderSetting headDamage = this.add(new SliderSetting("ForceHead", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    private final BooleanSetting noSuicide = this.add(new BooleanSetting("NoSuicide", true, () -> this.page.getValue() == Page.Interact));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.getValue() == Page.Interact));
    private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == Page.Interact));
    private final SliderSetting minPrefer = this.add(new SliderSetting("Prefer", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));
    private final SliderSetting maxSelfDamage = this.add(new SliderSetting("MaxSelf", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Interact).setSuffix("hp"));

    private final SliderSetting selfPredict = this.add(new SliderSetting("SelfPredict", 4, 0, 10, () -> this.page.getValue() == Page.Predict).setSuffix("ticks"));
    private final SliderSetting predictTicks = this.add(new SliderSetting("Predict", 4, 0, 10, () -> this.page.getValue() == Page.Predict).setSuffix("ticks"));
    private final SliderSetting simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == Page.Predict));
    private final SliderSetting maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == Page.Predict));
    private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == Page.Predict));

    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Page.Rotate));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.getValue() == Page.Rotate));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Rotate));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", false, () -> this.page.getValue() == Page.Rotate));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 10.0, 0.0, 360.0, 0.1, () -> this.page.getValue() == Page.Rotate).setSuffix("°"));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 30, 0, 100, () -> this.page.getValue() == Page.Rotate));

    private final BooleanSetting assist = this.add(new BooleanSetting("Assist", true, () -> this.page.getValue() == Page.Assist));
    private final BooleanSetting obsidian = this.add(new BooleanSetting("Obsidian", true, () -> this.page.getValue() == Page.Assist));
    private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == Page.Assist));
    private final SliderSetting assistRange = this.add(new SliderSetting("AssistRange", 10.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == Page.Assist).setSuffix("m"));
    private final SliderSetting assistDamage = this.add(new SliderSetting("AssistDamage", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == Page.Assist).setSuffix("hp"));
    private final SliderSetting assistDelay = this.add(new SliderSetting("AssistDelay", 0.1, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Assist).setSuffix("s"));

    private final EnumSetting<Aura.TargetESP> mode = this.add(new EnumSetting<Aura.TargetESP>("TargetESP", Aura.TargetESP.None, () -> this.page.getValue() == Page.Render));
    private final ColorSetting color = this.add(new ColorSetting("TargetColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final ColorSetting outlineColor = this.add(new ColorSetting("TargetOutlineColor", new Color(419430399, true), () -> this.page.getValue() == Page.Render));
    private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == Page.Render));
    private final BooleanSetting shrink = this.add(new BooleanSetting("Shrink", false, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final ColorSetting box = this.add(new ColorSetting("Box", new Color(-1049473294, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(1282823407, true), () -> this.page.getValue() == Page.Render && this.render.getValue()).injectBoolean(true));
    private final SliderSetting sliderSpeed = this.add(new SliderSetting("SliderSpeed", 0.92, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));
    private final SliderSetting startFadeTime = this.add(new SliderSetting("StartFade", 0.1, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = this.add(new SliderSetting("FadeSpeed", 1.0, 0.01, 1.0, 0.01, () -> this.page.getValue() == Page.Render && this.render.getValue()));

    private final BooleanSetting multiRender = this.add(new BooleanSetting("MultiRender", true, () -> this.page.getValue() == Page.Render));
    private final BooleanSetting multiShrink = this.add(new BooleanSetting("MultiShrink", false, () -> this.page.getValue() == Page.Render && this.multiRender.getValue()));
    private final ColorSetting multiBox = this.add(new ColorSetting("MultiBox", new Color(-1049473294, true), () -> this.page.getValue() == Page.Render && this.multiRender.getValue()).injectBoolean(true));
    private final ColorSetting multiFill = this.add(new ColorSetting("MultiFill", new Color(1282823407, true), () -> this.page.getValue() == Page.Render && this.multiRender.getValue()).injectBoolean(true));
    private final SliderSetting multiDuration = this.add(new SliderSetting("MultiDuration", 1.0, 0.1, 5.0, 0.1, () -> this.page.getValue() == Page.Render && this.multiRender.getValue()).setSuffix("s"));
    private final SliderSetting multiStartTime = this.add(new SliderSetting("MultiStartTime", 0.1, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Render && this.multiRender.getValue()).setSuffix("s"));
    private final SliderSetting multiEndTime = this.add(new SliderSetting("MultiEndTime", 0.3, 0.0, 2.0, 0.01, () -> this.page.getValue() == Page.Render && this.multiRender.getValue()).setSuffix("s"));

    final ArrayList<BlockPos> chargeList = new ArrayList();
    private final Timer delayTimer = new Timer();
    private final Timer calcTimer = new Timer();
    private final Timer noPosTimer = new Timer();
    private final Timer assistTimer = new Timer();
    public Vec3d directionVec = null;
    public PlayerEntity displayTarget;
    public BlockPos currentPos;
    public BlockPos tempPos;
    double fade = 0.0;
    BlockPos assistPos;
    private Vec3d lastTargetPos = null;
    private final Set<BlockPos> placedPositions = new HashSet<>();
    private int placedCount = 0;
    private boolean multiPlaceActive = false;
    private final List<BlockPos> multiPlaceQueue = new ArrayList<>();
    private int multiPlaceIndex = 0;
    private int multiPlaceStableTicks = 0;
    private PlayerEntity multiPlaceTarget = null;
    private final Map<BlockPos, Long> multiRenderTimes = new HashMap<>();
    private final Set<BlockPos> multiRenderRemoved = new HashSet<>();
    private int swapSlot = -1;
    private boolean swapBypass = false;
    private int originalSlot = -1;

    public AutoAnchor() {
        super("AutoAnchor", Module.Category.Combat);
        this.setChinese("重生锚光环");
        INSTANCE = this;
        Astra.EVENT_BUS.subscribe(new AnchorRender());
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.originalSlot = mc.player.getInventory().selectedSlot;
        }
        this.swapSlot = -1;
        this.swapBypass = false;
        placeVec3d = null;
        curVec3d = null;
    }

    public static boolean canSee(Vec3d from, Vec3d to) {
        BlockHitResult result = AutoAnchor.mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)AutoAnchor.mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    @Override
    public String getInfo() {
        if (this.displayTarget != null && this.currentPos != null) {
            return this.displayTarget.getName().getString();
        }
        return null;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.displayTarget != null && this.currentPos != null) {
            Aura.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), (Entity)this.displayTarget, this.color.getValue(), this.outlineColor.getValue(), this.mode.getValue());
        }
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.currentPos != null && this.rotate.getValue() && this.yawStep.getValue() && this.directionVec != null) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && this.inventorySwap.getValue() && this.swapSlot != -1) {
            if (!this.swapBypass && EntityUtil.inInventory()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.swapSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                mc.player.getInventory().updateItems();
            }
            if (this.originalSlot != -1) {
                InventoryUtil.switchToSlot(this.originalSlot);
            }
        }
        this.swapSlot = -1;
        this.swapBypass = false;
        this.originalSlot = -1;
        this.tempPos = null;
        this.currentPos = null;
        this.multiPlaceQueue.clear();
        this.placedPositions.clear();
        this.placedCount = 0;
        this.multiPlaceActive = false;
        this.multiPlaceIndex = 0;
        this.multiPlaceStableTicks = 0;
        this.lastTargetPos = null;
        this.multiPlaceTarget = null;
        this.multiRenderTimes.clear();
        this.multiRenderRemoved.clear();
        placeVec3d = null;
        curVec3d = null;
    }

    public void onThread() {
        if (this.isOff() || AutoAnchor.nullCheck()) {
            return;
        }
        if (mc.world != null && mc.world.getRegistryKey() == World.NETHER) {
            this.currentPos = null;
            return;
        }
        if (this.thread.getValue()) {
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
                this.currentPos = null;
                return;
            }
            if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
                this.currentPos = null;
                return;
            }
            int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = InventoryUtil.findUnBlock();
            if (anchor == -1) {
                this.currentPos = null;
                return;
            }
            if (glowstone == -1) {
                this.currentPos = null;
                return;
            }
            if (unBlock == -1) {
                this.currentPos = null;
                return;
            }
            if (AutoAnchor.mc.player.isSneaking()) {
                this.currentPos = null;
                return;
            }
            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
                this.currentPos = null;
                return;
            }
            this.calc();
            if (this.multiPlaceUse.getValue() && this.multiPlaceActive && !this.multiPlaceQueue.isEmpty()) {
                this.tryMultiPlace(anchor);
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        BlockPos pos;
        if (AutoAnchor.nullCheck()) {
            return;
        }
        if (ForceEat.INSTANCE.isEating()) {
            return;
        }
        if (mc.world != null && mc.world.getRegistryKey() == World.NETHER) {
            this.currentPos = null;
            return;
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) {
            return;
        }
        int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int unBlock = InventoryUtil.findUnBlock();
        int old = AutoAnchor.mc.player.getInventory().selectedSlot;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            this.currentPos = null;
            return;
        }
        if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
            this.currentPos = null;
            return;
        }
        if (anchor == -1) {
            this.currentPos = null;
            return;
        }
        if (glowstone == -1) {
            this.currentPos = null;
            return;
        }
        if (unBlock == -1) {
            this.currentPos = null;
            return;
        }
        if (AutoAnchor.mc.player.isSneaking()) {
            this.currentPos = null;
            return;
        }
        if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            this.currentPos = null;
            return;
        }
        if (this.inventorySwap.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        if (this.assist.getValue()) {
            this.onAssist();
        }
        if (!this.thread.getValue()) {
            this.calc();
        }
        if ((pos = this.currentPos) != null) {
            if (this.breakCrystal.getValue()) {
                CombatUtil.attackCrystal(new BlockPos((Vec3i)pos), this.rotate.getValue(), false);
            }
            if (!this.thread.getValue() && this.multiPlaceUse.getValue() && this.multiPlaceActive && !this.multiPlaceQueue.isEmpty()) {
                this.tryMultiPlace(anchor);
            }
            if (this.burstMode.getValue()) {
                Block block = BlockUtil.getBlock(pos);
                if (block != null && block != Blocks.AIR && block != Blocks.RESPAWN_ANCHOR && block != Blocks.BEDROCK) {
                    if (PacketMine.INSTANCE != null) {
                        BlockPos minePos = PacketMine.getBreakPos();
                        if (minePos == null || !minePos.equals(pos)) {
                            PacketMine.INSTANCE.mine(pos);
                        }
                        if (minePos != null && minePos.equals(pos)) {
                            double progress = PacketMine.progress;
                            double breakTime = PacketMine.INSTANCE.breakFinalTime;
                            int ping = 0;
                            if (mc.getNetworkHandler() != null && mc.player != null) {
                                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                                ping = entry != null ? entry.getLatency() : 0;
                            }
                            double prePlace = this.burstSmart.getValue() ? this.getSmartPrePlace(ping, breakTime) : this.burstPrePlace.getValue();
                            double latency = this.burstSmart.getValue() ? this.getSmartLatency(ping, breakTime) : this.burstLatency.getValue();
                            if (progress >= prePlace) {
                                double remainingMs = breakTime * (1.0 - progress);
                                if (remainingMs <= ping + latency) {
                                    if (BlockUtil.getClickSideStrict(pos) != null) {
                                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                                    }
                                }
                            }
                        }
                    }
                    if (!this.inventorySwap.getValue()) {
                        this.doSwap(old);
                    }
                    return;
                }
            }
            boolean shouldSpam;
            boolean bl = shouldSpam = this.spam.getValue() && (!this.mineSpam.getValue() || Astra.BREAK.isMining(pos));
            if (shouldSpam) {
                if (!this.delayTimer.passed((long)this.spamDelay.getValueFloat())) {
                    return;
                }
                this.delayTimer.reset();
                if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                    this.placeBlock(pos, this.rotate.getValue(), anchor);
                }
                if (!this.chargeList.contains(pos)) {
                    this.delayTimer.reset();
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                    this.chargeList.add(pos);
                }
                this.chargeList.remove(pos);
                this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                this.nextPosition();
                if (this.spamPlace.getValue() && this.inSpam.getValue()) {
                    if (this.yawStep.getValue() && this.checkFov.getValue()) {
                        Direction side = BlockUtil.getClickSide(pos);
                        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
                        if (Astra.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            this.placeBlock(pos, this.rotate.getValue(), anchor);
                            CombatUtil.modifyPos = null;
                        }
                    } else {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                        CombatUtil.modifyPos = null;
                    }
                }
            } else if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                if (!this.delayTimer.passed((long)this.placeDelay.getValueFloat())) {
                    return;
                }
                this.delayTimer.reset();
                this.placeBlock(pos, this.rotate.getValue(), anchor);
            } else if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                if (!this.chargeList.contains(pos)) {
                    if (!this.delayTimer.passed((long)this.fillDelay.getValueFloat())) {
                        return;
                    }
                    this.delayTimer.reset();
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                    this.chargeList.add(pos);
                } else {
                    if (!this.delayTimer.passed((long)this.breakDelay.getValueFloat())) {
                        return;
                    }
                    this.delayTimer.reset();
                    this.chargeList.remove(pos);
                    this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                    this.nextPosition();
                    if (this.spamPlace.getValue()) {
                        if (this.yawStep.getValue() && this.checkFov.getValue()) {
                            Direction side = BlockUtil.getClickSide(pos);
                            Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
                            if (Astra.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                                CombatUtil.modifyPos = pos;
                                CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                this.placeBlock(pos, this.rotate.getValue(), anchor);
                                CombatUtil.modifyPos = null;
                            }
                        } else {
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                            this.placeBlock(pos, this.rotate.getValue(), anchor);
                            CombatUtil.modifyPos = null;
                        }
                    }
                }
            }
            if (!this.inventorySwap.getValue()) {
                this.doSwap(old);
            }
        }
    }

    private void calc() {
        if (AutoAnchor.nullCheck()) {
            return;
        }
        if (mc.world != null && mc.world.getRegistryKey() == World.NETHER) {
            this.currentPos = null;
            return;
        }
        if (this.calcTimer.passed((long)this.updateDelay.getValueFloat())) {
            this.calcTimer.reset();
            PlayerEntityPredict selfPredict = new PlayerEntityPredict((PlayerEntity)AutoAnchor.mc.player, this.maxMotionY.getValue(), this.selfPredict.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue());
            this.tempPos = null;
            List<PlayerEntity> enemies = CombatUtil.getEnemies(this.targetRange.getValue());
            ArrayList<PlayerEntityPredict> list = new ArrayList<PlayerEntityPredict>();
            for (PlayerEntity player : enemies) {
                list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
            }

            if (this.multiPlaceUse.getValue()) {
                if (this.multiPlaceActive) {
                    if (this.multiPlaceTarget != null) {
                        Vec3d targetPos = this.multiPlaceTarget.getPos();
                        if (this.lastTargetPos != null && targetPos.distanceTo(this.lastTargetPos) > 0.5) {
                            this.lastTargetPos = targetPos;
                            this.placedPositions.clear();
                            this.placedCount = 0;
                            this.multiPlaceQueue.clear();
                            this.multiPlaceIndex = 0;
                            this.multiPlaceStableTicks = 0;
                            this.multiRenderTimes.clear();
                            this.multiRenderRemoved.clear();
                            this.calcMultiPlaceQueue(list, selfPredict, this.multiPlaceTarget, null);
                        } else {
                            this.multiPlaceStableTicks++;
                            if (this.multiPlaceStableTicks > 40) {
                                this.multiPlaceActive = false;
                            }
                        }
                    }
                }
                if (!this.multiPlaceActive) {
                    this.calcMultiPlace(list, selfPredict);
                }
            }

            double damage;
            double placeDamage = this.minDamage.getValue();
            double breakDamage = this.breakMin.getValue();
            boolean anchorFound = false;
            for (PlayerEntityPredict pap : list) {
                double selfDamage;
                BlockPos pos = EntityUtil.getEntityPos((Entity)pap.player, true).up(2);
                if (!BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue()) && (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR || BlockUtil.getClickSideStrict(pos) == null) || (selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue() || this.noSuicide.getValue() && selfDamage > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) continue;
                damage = this.getAnchorDamage(pos, pap.player, pap.predict);
                if (!(damage > (double)this.headDamage.getValueFloat()) || this.smart.getValue() && selfDamage > damage) continue;
                this.displayTarget = pap.player;
                this.tempPos = pos;
                break;
            }
            if (this.tempPos == null) {
                Map<BlockPos, Double> positionScores = new HashMap<>();
                for (PlayerEntityPredict pap : list) {
                    Vec3d targetEye = pap.player.getEyePos();
                    Vec3d moveDir = pap.player.getVelocity();
                    if (moveDir.lengthSquared() < 0.01 && pap.predict != pap.player) {
                        moveDir = pap.predict.getPos().subtract(pap.player.getPos());
                    }
                    boolean hasMoveDir = moveDir.lengthSquared() > 0.01;
                    if (hasMoveDir) moveDir = moveDir.normalize();

                    for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat() + 1.0f, targetEye)) {
                        double selfDamage;
                        if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                            double selfDamage2;
                            if (anchorFound || !BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) continue;
                            CombatUtil.modifyPos = pos;
                            CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                            boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                            CombatUtil.modifyPos = null;
                            if (skip || !((damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= placeDamage) || (selfDamage2 = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue() || this.noSuicide.getValue() && selfDamage2 > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount()) || this.smart.getValue() && selfDamage2 > damage) continue;
                            double score = Math.sqrt(pos.getSquaredDistance(pap.player.getBlockPos()));
                            if (hasMoveDir) {
                                Vec3d dirToPos = pos.toCenterPos().subtract(targetEye).normalize();
                                score -= dirToPos.dotProduct(moveDir) * 2.0;
                            }
                            if (!positionScores.containsKey(pos) || score < positionScores.get(pos)) {
                                positionScores.put(pos, score);
                            }
                            continue;
                        }
                        double damage2 = this.getAnchorDamage(pos, pap.player, pap.predict);
                        if (BlockUtil.getClickSideStrict(pos) == null || !(damage2 >= breakDamage)) continue;
                        if (damage2 >= this.minPrefer.getValue()) {
                            anchorFound = true;
                        }
                        if (!anchorFound && damage2 < placeDamage || (selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue() || this.noSuicide.getValue() && selfDamage > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount()) || this.smart.getValue() && selfDamage > damage2) continue;
                        double score = Math.sqrt(pos.getSquaredDistance(pap.player.getBlockPos()));
                        if (hasMoveDir) {
                            Vec3d dirToPos = pos.toCenterPos().subtract(targetEye).normalize();
                            score -= dirToPos.dotProduct(moveDir) * 2.0;
                        }
                        if (!positionScores.containsKey(pos) || score < positionScores.get(pos)) {
                            positionScores.put(pos, score);
                        }
                    }
                }
                if (!positionScores.isEmpty()) {
                    BlockPos bestPos = null;
                    double bestScore = Double.MAX_VALUE;
                    for (Map.Entry<BlockPos, Double> entry : positionScores.entrySet()) {
                        if (entry.getValue() < bestScore) {
                            bestScore = entry.getValue();
                            bestPos = entry.getKey();
                        }
                    }
                    this.tempPos = bestPos;
                }
            }
        }
        this.currentPos = this.tempPos;
    }

    private boolean calcMultiPlace(ArrayList<PlayerEntityPredict> list, PlayerEntityPredict selfPredict) {
        this.tempPos = null;

        PlayerEntity target = null;
        double bestDist = Double.MAX_VALUE;
        PlayerEntityPredict targetPredict = null;
        for (PlayerEntityPredict pap : list) {
            double dist = pap.player.distanceTo(AutoAnchor.mc.player);
            if (dist < bestDist) {
                bestDist = dist;
                target = pap.player;
                targetPredict = pap;
            }
        }

        if (target == null) {
            this.multiPlaceActive = false;
            return false;
        }

        double speed = target.getVelocity().length();
        Vec3d targetPos = target.getPos();
        double moved = this.lastTargetPos != null ? targetPos.distanceTo(this.lastTargetPos) : Double.MAX_VALUE;

        if (moved > 0.5) {
            this.lastTargetPos = targetPos;
            this.placedPositions.clear();
            this.placedCount = 0;
            this.multiPlaceQueue.clear();
            this.multiPlaceIndex = 0;
            this.multiPlaceStableTicks = 0;
            this.multiRenderTimes.clear();
            this.multiRenderRemoved.clear();
            this.multiPlaceTarget = target;
        }

        if (speed <= 0.5) {
            this.calcMultiPlaceQueue(list, selfPredict, target, targetPredict);
            this.multiPlaceActive = !this.multiPlaceQueue.isEmpty();
            return this.multiPlaceActive;
        }

        this.multiPlaceActive = false;
        return false;
    }

    private void calcMultiPlaceQueue(ArrayList<PlayerEntityPredict> list, PlayerEntityPredict selfPredict, PlayerEntity target, PlayerEntityPredict targetPredict) {
        Map<BlockPos, Double> positionScores = new HashMap<>();
        BlockPos targetBlockPos = EntityUtil.getEntityPos(target, true);
        int range = this.multiPlaceRange.getValueInt();

        Vec3d moveDir = getTargetMoveDirection(target, targetPredict);

        for (int yOff = 0; yOff <= 2; yOff++) {
            Vec3d center = new Vec3d(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + yOff, targetBlockPos.getZ() + 0.5);
            for (BlockPos pos : BlockUtil.getSphere(range + 2.0f, center)) {
                if (this.placedPositions.contains(pos)) continue;

                double dist = Math.sqrt(pos.getSquaredDistance(targetBlockPos));
                if (dist > range + 2.0) continue;
                if (pos.getY() < targetBlockPos.getY() - 1 || pos.getY() > targetBlockPos.getY() + 3) continue;

                for (PlayerEntityPredict pap : list) {
                    double selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict);
                    double targetDamage = this.getAnchorDamage(pos, pap.player, pap.predict);

                    if (targetDamage < this.minDamage.getValue()) continue;
                    if (selfDamage > this.maxSelfDamage.getValue()) continue;
                    if (this.noSuicide.getValue() && selfDamage > (double)(AutoAnchor.mc.player.getHealth() + AutoAnchor.mc.player.getAbsorptionAmount())) continue;
                    if (this.smart.getValue() && selfDamage > targetDamage) continue;

                    if (!BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                        if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) continue;
                        if (BlockUtil.getClickSideStrict(pos) == null) continue;
                    } else {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                        boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                        CombatUtil.modifyPos = null;
                        if (skip) continue;
                    }

                    double score = dist;
                    if (pos.getY() > targetBlockPos.getY()) {
                        score -= 0.5;
                    }
                    if (moveDir != null) {
                        Vec3d posVec = pos.toCenterPos();
                        Vec3d targetVec = targetBlockPos.toCenterPos();
                        Vec3d dirToPos = posVec.subtract(targetVec).normalize();
                        double dot = dirToPos.dotProduct(moveDir);
                        score -= dot * 2.0;
                    }

                    if (!positionScores.containsKey(pos) || score < positionScores.get(pos)) {
                        positionScores.put(pos, score);
                    }
                }
            }
        }

        if (!positionScores.isEmpty()) {
            this.multiPlaceQueue.clear();
            this.multiPlaceQueue.addAll(positionScores.keySet());
            this.multiPlaceQueue.sort((a, b) -> Double.compare(positionScores.get(a), positionScores.get(b)));
        }
    }

    private Vec3d getTargetMoveDirection(PlayerEntity target, PlayerEntityPredict targetPredict) {
        Vec3d vel = target.getVelocity();
        if (vel.lengthSquared() > 0.01) {
            return vel.normalize();
        }
        if (targetPredict != null && targetPredict.predict != target) {
            Vec3d predictedPos = targetPredict.predict.getPos();
            Vec3d dir = predictedPos.subtract(target.getPos());
            if (dir.lengthSquared() > 0.01) {
                return dir.normalize();
            }
        }
        return null;
    }

    private void nextPosition() {
    }

    private void tryMultiPlace(int anchor) {
        if (this.placedCount >= this.multiPlaceMaxNumber.getValueInt()) {
            this.multiPlaceActive = false;
            return;
        }
        if (this.multiPlaceIndex >= this.multiPlaceQueue.size()) {
            this.multiPlaceIndex = 0;
        }
        BlockPos mpPos = this.multiPlaceQueue.get(this.multiPlaceIndex);
        if (BlockUtil.canPlace(mpPos, this.range.getValue(), this.breakCrystal.getValue())) {
            int oldSlot = AutoAnchor.mc.player.getInventory().selectedSlot;
            this.placeBlock(mpPos, this.rotate.getValue(), anchor);
            if (!this.inventorySwap.getValue()) {
                InventoryUtil.switchToSlot(oldSlot);
            }
            this.placedPositions.add(mpPos);
            this.placedCount++;
            this.multiRenderTimes.put(mpPos, System.currentTimeMillis());
            this.multiRenderRemoved.add(mpPos);
            this.multiPlaceIndex++;
            if (this.multiPlaceIndex >= this.multiPlaceQueue.size()) {
                this.multiPlaceIndex = 0;
            }
        } else {
            this.multiPlaceIndex++;
            if (this.multiPlaceIndex >= this.multiPlaceQueue.size()) {
                this.multiPlaceIndex = 0;
            }
        }
    }

    public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
        if (this.terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }
        double damage = ExplosionUtil.anchorDamage(anchorPos, (LivingEntity)target, (LivingEntity)predict);
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    public void placeBlock(BlockPos pos, boolean rotate, int slot) {
        if (BlockUtil.allowAirPlace()) {
            this.airPlace(pos, rotate, slot);
            return;
        }
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) {
            return;
        }
        this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
    }

    public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        if (pos == null) {
            return;
        }
        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
        if (rotate && !this.faceVector(directionVec)) {
            return;
        }
        this.doSwap(slot);
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (this.inventorySwap.getValue()) {
            this.doSwap(slot);
        }
        if (rotate && !this.yawStep.getValue()) {
            Astra.ROTATION.snapBack();
        }
    }

    public void airPlace(BlockPos pos, boolean rotate, int slot) {
        if (pos == null) {
            return;
        }
        Direction side = BlockUtil.getClickSide(pos);
        Vec3d directionVec = new Vec3d((double)pos.getX() + 0.5 + (double)side.getVector().getX() * 0.5, (double)pos.getY() + 0.5 + (double)side.getVector().getY() * 0.5, (double)pos.getZ() + 0.5 + (double)side.getVector().getZ() * 0.5);
        if (rotate && !this.faceVector(directionVec)) {
            return;
        }
        this.doSwap(slot);
        boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
        if (bypass) {
            mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
        if (bypass) {
            mc.getNetworkHandler().sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
        }
        if (this.inventorySwap.getValue()) {
            this.doSwap(slot);
        }
        if (rotate && !this.yawStep.getValue()) {
            Astra.ROTATION.snapBack();
        }
    }

    private void doSwap(int slot) {
        if (this.inventorySwap.getValue()) {
            if (this.swapSlot == -1) {
                this.swapSlot = slot;
                this.swapBypass = AntiCheat.INSTANCE.invSwapBypass.getValue() && slot - 36 >= 0;
            }
            InventoryUtil.inventorySwap(slot, AutoAnchor.mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    public boolean faceVector(Vec3d directionVec) {
        if (!this.yawStep.getValue()) {
            Astra.ROTATION.lookAt(directionVec);
            return true;
        }
        this.directionVec = directionVec;
        if (Astra.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
        }
        return !this.checkFov.getValue();
    }

    public void onAssist() {
        if (mc.world != null && mc.world.getRegistryKey() == World.NETHER) {
            return;
        }
        BlockPos placePos;
        this.assistPos = null;
        int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
        int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
        int old = AutoAnchor.mc.player.getInventory().selectedSlot;
        if (anchor == -1) {
            return;
        }
        if (this.obsidian.getValue()) {
            int n = anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
                return;
            }
        }
        if (glowstone == -1) {
            return;
        }
        if (AutoAnchor.mc.player.isSneaking()) {
            return;
        }
        if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (!this.assistTimer.passed((long)(this.assistDelay.getValueFloat() * 1000.0f))) {
            return;
        }
        this.assistTimer.reset();
        ArrayList<PlayerEntityPredict> list = new ArrayList<PlayerEntityPredict>();
        for (PlayerEntity player : CombatUtil.getEnemies(this.assistRange.getValue())) {
            list.add(new PlayerEntityPredict(player, this.maxMotionY.getValue(), this.predictTicks.getValueInt(), this.simulation.getValueInt(), this.step.getValue(), this.doubleStep.getValue(), this.jump.getValue(), this.inBlockPause.getValue()));
        }
        double bestDamage = this.assistDamage.getValue();
        for (PlayerEntityPredict pap : list) {
            double damage;
            BlockPos pos = EntityUtil.getEntityPos((Entity)pap.player, true).up(2);
            if (AutoAnchor.mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                return;
            }
            if (BlockUtil.clientCanPlace(pos, false) && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) >= bestDamage) {
                bestDamage = damage;
                this.assistPos = pos;
            }
            for (Direction i : Direction.values()) {
                double damage2;
                if (i == Direction.UP || i == Direction.DOWN || !BlockUtil.clientCanPlace(pos.offset(i), false) || !((damage2 = this.getAnchorDamage(pos.offset(i), pap.player, pap.predict)) >= bestDamage)) continue;
                bestDamage = damage2;
                this.assistPos = pos.offset(i);
            }
        }
        if (this.assistPos != null && BlockUtil.getPlaceSide(this.assistPos, this.range.getValue()) == null && (placePos = this.getHelper(this.assistPos)) != null) {
            this.doSwap(anchor);
            BlockUtil.placeBlock(placePos, this.rotate.getValue());
            if (this.inventorySwap.getValue()) {
                this.doSwap(anchor);
            } else {
                this.doSwap(old);
            }
        }
    }

    public BlockPos getHelper(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (this.checkMine.getValue() && Astra.BREAK.isMining(pos.offset(i)) || !BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite()) || !BlockUtil.canPlace(pos.offset(i))) continue;
            return pos.offset(i);
        }
        return null;
    }

    private double getSmartPrePlace(int ping, double breakTime) {
        double base = 0.85;
        double pingAdjust = (ping / 1000.0) * 0.20;
        double breakAdjust = Math.max(0, (1.0 - breakTime / 1000.0) * 0.05);
        double result = base - pingAdjust - breakAdjust;
        return Math.max(0.6, Math.min(0.95, result));
    }

    private double getSmartLatency(int ping, double breakTime) {
        double result = ping * 0.6 + 20;
        return Math.max(0, Math.min(300, result));
    }

    public static enum Page {
        General,
        Interact,
        Predict,
        Rotate,
        Assist,
        Burst,
        MultiPlace,
        Render;
    }

    public class AnchorRender {
        @EventListener
        public void onRender3D(Render3DEvent event) {
            if (AutoAnchor.mc.world == null) return;
            BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
            if (currentPos != null) {
                AutoAnchor.this.noPosTimer.reset();
                placeVec3d = currentPos.toCenterPos();
            }
            if (placeVec3d != null) {
                AutoAnchor.this.fade = AutoAnchor.this.fadeSpeed.getValue() >= 1.0 ? (AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5) : AnimateUtil.animate(AutoAnchor.this.fade, AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5, AutoAnchor.this.fadeSpeed.getValue() / 10.0);
                if (AutoAnchor.this.fade == 0.0) {
                    curVec3d = null;
                } else {
                    curVec3d = curVec3d == null || AutoAnchor.this.sliderSpeed.getValue() >= 1.0 ? placeVec3d : new Vec3d(AnimateUtil.animate(AutoAnchor.curVec3d.x, AutoAnchor.placeVec3d.x, AutoAnchor.this.sliderSpeed.getValue() / 10.0), AnimateUtil.animate(AutoAnchor.curVec3d.y, AutoAnchor.placeVec3d.y, AutoAnchor.this.sliderSpeed.getValue() / 10.0), AnimateUtil.animate(AutoAnchor.curVec3d.z, AutoAnchor.placeVec3d.z, AutoAnchor.this.sliderSpeed.getValue() / 10.0));
                    if (AutoAnchor.this.render.getValue() && AutoAnchor.this.fade > 0.005) {
                        Box cbox = new Box(curVec3d, curVec3d);
                        cbox = AutoAnchor.this.shrink.getValue() ? cbox.expand(Math.max(0.001, AutoAnchor.this.fade)) : cbox.expand(0.5);
                        if (AutoAnchor.this.fill.booleanValue) {
                            event.drawFill(cbox, ColorUtil.injectAlpha(AutoAnchor.this.fill.getValue(), (int)((double)AutoAnchor.this.fill.getValue().getAlpha() * AutoAnchor.this.fade * 2.0)));
                        }
                        if (AutoAnchor.this.box.booleanValue) {
                            event.drawBox(cbox, ColorUtil.injectAlpha(AutoAnchor.this.box.getValue(), (int)((double)AutoAnchor.this.box.getValue().getAlpha() * AutoAnchor.this.fade * 2.0)));
                        }
                    }
                }
            }
            if (AutoAnchor.this.multiRender.getValue() && !AutoAnchor.this.multiRenderTimes.isEmpty()) {
                long now = System.currentTimeMillis();
                float startTime = AutoAnchor.this.multiStartTime.getValueFloat();
                float duration = AutoAnchor.this.multiDuration.getValueFloat();
                float endTime = AutoAnchor.this.multiEndTime.getValueFloat();
                Iterator<Map.Entry<BlockPos, Long>> it = AutoAnchor.this.multiRenderTimes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<BlockPos, Long> entry = it.next();
                    BlockPos pos = entry.getKey();
                    long addedTime = entry.getValue();
                    if (!AutoAnchor.mc.world.isAir(pos) && BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                        if (AutoAnchor.this.multiRenderRemoved.contains(pos)) {
                            it.remove();
                            AutoAnchor.this.multiRenderRemoved.remove(pos);
                        }
                        continue;
                    }
                    float elapsed = (now - addedTime) / 1000.0f;
                    boolean removed = AutoAnchor.this.multiRenderRemoved.contains(pos);
                    float fade;
                    if (removed) {
                        float removeElapsed = Math.max(0.0f, elapsed - startTime - duration);
                        fade = 1.0f - removeElapsed / Math.max(0.01f, endTime);
                        if (fade <= 0.0f) {
                            it.remove();
                            AutoAnchor.this.multiRenderRemoved.remove(pos);
                            continue;
                        }
                    } else {
                        fade = Math.min(1.0f, elapsed / Math.max(0.01f, startTime));
                    }
                    fade = Math.max(0.0f, Math.min(1.0f, fade));
                    if (fade <= 0.005f) continue;
                    Vec3d vec = pos.toCenterPos();
                    Box cbox = new Box(vec, vec);
                    cbox = AutoAnchor.this.multiShrink.getValue() ? cbox.expand(Math.max(0.001f, fade * 0.5f)) : cbox.expand(0.5f);
                    if (AutoAnchor.this.multiFill.booleanValue) {
                        event.drawFill(cbox, ColorUtil.injectAlpha(AutoAnchor.this.multiFill.getValue(), (int)((double)AutoAnchor.this.multiFill.getValue().getAlpha() * fade)));
                    }
                    if (AutoAnchor.this.multiBox.booleanValue) {
                        event.drawBox(cbox, ColorUtil.injectAlpha(AutoAnchor.this.multiBox.getValue(), (int)((double)AutoAnchor.this.multiBox.getValue().getAlpha() * fade)));
                    }
                }
            }
        }
    }
}