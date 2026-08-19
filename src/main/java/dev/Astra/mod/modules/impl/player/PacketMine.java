package dev.Astra.mod.modules.impl.player;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClickBlockEvent;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.api.utils.math.FadeUtils;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.asm.accessors.IPlayerMoveC2SPacket;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import dev.Astra.mod.modules.impl.combat.AutoAnchor;
import dev.Astra.mod.modules.impl.combat.AutoCrystal;
import dev.Astra.mod.modules.impl.combat.CevBreaker;
import dev.Astra.mod.modules.impl.combat.Criticals;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.movement.ElytraFly;
import dev.Astra.mod.modules.impl.movement.Velocity;
import dev.Astra.mod.modules.settings.enums.SwingSide;
import dev.Astra.mod.modules.settings.enums.Timing;
import dev.Astra.mod.modules.settings.impl.BindSetting;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PacketMine extends Module {
    public static PacketMine INSTANCE;
    public static BlockPos secondPos;
    public static double progress;
    private final FadeUtils animationTime = new FadeUtils(1000L);
    private final FadeUtils secondAnim = new FadeUtils(1000L);
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.General));
    private final SliderSetting stopDelay = this.add(new SliderSetting("StopDelay", 50.0, 0.0, 500.0, 1.0, () -> this.page.is(Page.General)));
    private final SliderSetting startDelay = this.add(new SliderSetting("StartDelay", 200.0, 0.0, 500.0, 1.0, () -> this.page.is(Page.General)));
    private final SliderSetting damage = this.add(new SliderSetting("Damage", 0.7f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General)));
    private final SliderSetting maxBreak = this.add(new SliderSetting("MaxBreak", 3.0, 0.0, 20.0, 1.0, () -> this.page.is(Page.General)));
    private final EnumSetting<Timing> timing = this.add(new EnumSetting<Timing>("Timing", Timing.All, () -> this.page.getValue() == Page.General));
    private final BooleanSetting fastBypass = this.add(new BooleanSetting("FastBypass", false, () -> this.page.is(Page.General)));
    private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(Page.General)));
    private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(Page.General)));
    private final BooleanSetting mineAir = this.add(new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(Page.General)));
    private final BooleanSetting hotBar = this.add(new BooleanSetting("HotbarSwap", false, () -> this.page.is(Page.General)));
    private final BooleanSetting doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(Page.General))).setParent();
    public final BooleanSetting autoSwitch = this.add(new BooleanSetting("AutoSwitch", true, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final SliderSetting start = this.add(new SliderSetting("Start", 0.9f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final SliderSetting timeOut = this.add(new SliderSetting("TimeOut", 1.2f, 0.0, 2.0, 0.01, () -> this.page.is(Page.General) && this.doubleBreak.isOpen()));
    private final BooleanSetting setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(Page.General)));
    private final BooleanSetting swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(Page.General)));
    private final BooleanSetting endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(Page.General)));
    public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1, () -> this.page.is(Page.General)));
    private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<SwingSide>("SwingSide", SwingSide.All, () -> this.page.is(Page.General)));
    private final BooleanSetting unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(Page.Check)));
    private final BooleanSetting checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(Page.Check)));
    // Smart 宸插垹闄?
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false, () -> this.page.is(Page.Check)).setParent());
    private final BooleanSetting allowOffhand = this.add(new BooleanSetting("AllowOffhand", true, () -> this.page.is(Page.Check) && this.usingPause.isOpen()));
    private final BooleanSetting bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(Page.Check)));
    private final SliderSetting bypassTime = this.add(new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(Page.Check)));
    private final BooleanSetting rotate = this.add(new BooleanSetting("StartRotate", true, () -> this.page.is(Page.Rotation)));
    private final BooleanSetting endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(Page.Rotation)));
    private final SliderSetting syncTime = this.add(new SliderSetting("Sync", 300, 0, 1000, () -> this.page.is(Page.Rotation)));
    private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.is(Page.Rotation)).setParent());
    private final BooleanSetting whenElytra = this.add(new BooleanSetting("FallFlying", true, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final BooleanSetting checkFov = this.add(new BooleanSetting("OnlyLooking", true, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting fov = this.add(new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    private final SliderSetting priority = this.add(new SliderSetting("Priority", 100, 0, 100, () -> this.page.is(Page.Rotation) && this.yawStep.isOpen()));
    // 宸插垹闄ゆ墍鏈?Place 鐩稿叧璁剧疆
    private final BooleanSetting checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(Page.Render)));
    private final EnumSetting<Animation> animation = this.add(new EnumSetting<Animation>("Animation", Animation.Up, () -> this.page.is(Page.Render)));
    private final EnumSetting<Easing> ease = this.add(new EnumSetting<Easing>("Ease", Easing.CubicInOut, () -> this.page.is(Page.Render)));
    private final EnumSetting<Easing> fadeEase = this.add(new EnumSetting<Easing>("FadeEase", Easing.CubicInOut, () -> this.page.is(Page.Render)));
    private final SliderSetting expandLine = this.add(new SliderSetting("ExpandLine", 0.0, 0.0, 1.0, () -> this.page.is(Page.Render)));
    private final ColorSetting startColor = this.add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting startOutlineColor = this.add(new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting endColor = this.add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting endOutlineColor = this.add(new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> this.page.is(Page.Render)));
    private final ColorSetting doubleColor = this.add(new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(Page.Render)));
    private final ColorSetting doubleOutlineColor = this.add(new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(Page.Render)));
    private final BooleanSetting text = this.add(new BooleanSetting("Text", true, () -> this.page.is(Page.Render)));
    private final BooleanSetting box = this.add(new BooleanSetting("Box", true, () -> this.page.is(Page.Render)));
    private final BooleanSetting outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(Page.Render)));
    public final Timer mineTimer = new Timer();
    private final Timer sync = new Timer();
    private final Timer secondTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer startTime = new Timer(); // startDelay 璁℃椂鍣?
    public static boolean ghost;
    public static boolean complete;
    int lastSlot = -1;
    Vec3d directionVec = null;
    Runnable switchBack;
    BlockPos breakPos;
    boolean startPacket = false;
    int breakNumber = 0;
    public double breakFinalTime;
    double secondFinalTime;
    boolean sendGroundPacket = false;
    boolean swapped = false;
    int mainSlot = 0;

    public PacketMine() {
        super("PacketMine", Module.Category.Player);
        this.setChinese("\u53d1\u5305\u6316\u6398");
        INSTANCE = this;
    }

    public static BlockPos getBreakPos() {
        if (INSTANCE.isOn()) {
            return PacketMine.INSTANCE.breakPos;
        }
        return null;
    }

    @Override
    public String getInfo() {
        if (progress >= 1.0) {
            return "Done";
        }
        return this.df.format(progress * 100.0) + "%";
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
            event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
        }
    }

    @Override
    public void onLogin() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
        secondPos = null;
    }

    @Override
    public void onDisable() {
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.breakPos = null;
    }

    private void autoSwitch() {
        if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
            int index = -1;
            if (secondPos != null) {
                float CurrentFastest = 1.0f;
                for (int i = 0; i < 9; ++i) {
                    float destroySpeed;
                    float digSpeed;
                    ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                    if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(secondPos))) > CurrentFastest)) continue;
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
            if (index == -1 || PacketMine.mc.options.useKey.isPressed() || PacketMine.mc.options.attackKey.isPressed() || PacketMine.mc.player.isUsingItem() || !this.secondTimer.passedMs(this.getBreakTime(secondPos, index, this.start.getValue()))) {
                if (this.swapped) {
                    InventoryUtil.switchToSlot(this.mainSlot);
                    this.swapped = false;
                }
            } else if (index != PacketMine.mc.player.getInventory().selectedSlot) {
                this.mainSlot = PacketMine.mc.player.getInventory().selectedSlot;
                InventoryUtil.switchToSlot(index);
                this.swapped = true;
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (PacketMine.nullCheck()) {
            return;
        }
        if (this.breakPos != null && PacketMine.mc.world.isAir(this.breakPos)) {
            complete = true;
        }
        if (secondPos != null) {
            int secondSlot = this.getTool(secondPos);
            if (secondSlot == -1) {
                secondSlot = PacketMine.mc.player.getInventory().selectedSlot;
            }
            this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, 1.0);
            if (this.isAir(secondPos) || PacketMine.unbreakable(secondPos)) {
                secondPos = null;
            } else {
                double time = this.getBreakTime(secondPos, PacketMine.mc.player.getInventory().selectedSlot, 1.0);
                if (this.secondTimer.passedMs(time * this.timeOut.getValue())) {
                    secondPos = null;
                }
            }
        }
        if (this.switchBack != null && event.isPre()) {
            this.switchBack.run();
            this.switchBack = null;
        }
        if (this.timing.is(Timing.Pre) && event.isPost() || this.timing.is(Timing.Post) && event.isPre()) {
            return;
        }
        if (PacketMine.mc.player.isDead()) {
            secondPos = null;
        }
        this.autoSwitch();
        if (PacketMine.mc.player.isCreative()) {
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.breakNumber = 0;
            this.breakPos = null;
            progress = 0.0;
            return;
        }
        if (this.breakPos == null) {
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            progress = 0.0;
            return;
        }
        int slot = this.getTool(this.breakPos);
        if (slot == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
        progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
        if (this.isAir(this.breakPos)) {
            this.breakNumber = 0;
        }
        if ((double)this.breakNumber > this.maxBreak.getValue() - 1.0 && this.maxBreak.getValue() > 0.0 && !complete || !this.wait.getValue() && this.isAir(this.breakPos) && !this.instant.getValue()) {
            if (this.breakPos.equals((Object)secondPos)) {
                secondPos = null;
            }
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.breakNumber = 0;
            this.breakPos = null;
            return;
        }
        if (PacketMine.unbreakable(this.breakPos)) {
            if (this.unbreakableCancel.getValue()) {
                this.breakPos = null;
                this.startPacket = false;
                ghost = false;
                complete = false;
            }
            this.breakNumber = 0;
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos()))) > this.range.getValue()) {
            return;
        }
        if (this.usingPause.getValue() && mc.player.isUsingItem() && (!this.allowOffhand.getValue() || PacketMine.mc.player.getActiveHand() == Hand.MAIN_HAND)) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (this.breakPos.equals((Object)AutoAnchor.INSTANCE.currentPos) && BlockUtil.getBlock(PacketMine.getBreakPos()) instanceof RespawnAnchorBlock) {
            return;
        }
        if (!this.hotBar.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        // 宸插垹闄ゆ墍鏈?Place 閫昏緫
        if (!this.delayTimer.passed((long)this.stopDelay.getValue())) {
            return;
        }
        if (this.startPacket) {
            if (this.isAir(this.breakPos)) {
                return;
            }
            if (this.mineTimer.passed((long)this.breakFinalTime)) {
                boolean shouldSwitch;
                if (this.endRotate.getValue() && this.shouldYawStep() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                    return;
                }
                int old = PacketMine.mc.player.getInventory().selectedSlot;
                if (this.hotBar.getValue()) {
                    shouldSwitch = slot != old;
                } else {
                    if (slot < 9) {
                        slot += 36;
                    }
                    boolean bl = shouldSwitch = old + 36 != slot;
                }
                if (shouldSwitch) {
                    if (this.hotBar.getValue()) {
                        InventoryUtil.switchToSlot(slot);
                    } else {
                        PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, slot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                    }
                }
                int finalSlot = slot;
                this.switchBack = () -> {
                    if (this.endRotate.getValue() && !this.faceVector(this.breakPos.toCenterPos().offset(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                        if (shouldSwitch) {
                            if (this.hotBar.getValue()) {
                                InventoryUtil.switchToSlot(old);
                            } else {
                                PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                                EntityUtil.syncInventory();
                            }
                        }
                        return;
                    }
                    PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, BlockUtil.getClickSide(this.breakPos), id));
                    if (this.endSwing.getValue()) {
                        EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
                    }
                    if (shouldSwitch) {
                        if (this.hotBar.getValue()) {
                            InventoryUtil.switchToSlot(old);
                        } else {
                            PacketMine.mc.interactionManager.clickSlot(PacketMine.mc.player.currentScreenHandler.syncId, finalSlot, old, SlotActionType.SWAP, (PlayerEntity)PacketMine.mc.player);
                            EntityUtil.syncInventory();
                        }
                    }
                    ++this.breakNumber;
                    this.delayTimer.reset();
                    this.startTime.reset();
                    // 宸插垹闄?afterBreak 鏅朵綋鏀诲嚮
                    if (this.setAir.getValue()) {
                        PacketMine.mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                    }
                    if (this.endRotate.getValue() && !this.shouldYawStep()) {
                        Astra.ROTATION.snapBack();
                    }
                    ghost = true;
                };
                this.switchBack.run();
                this.switchBack = null;
            }
        } else {
            // startDelay 閫昏緫
            if (!this.startTime.passed(this.startDelay.getValueInt())) {
                return;
            }
            if (!this.mineAir.getValue() && this.isAir(this.breakPos)) {
                return;
            }
            Direction side = BlockUtil.getClickSide(this.breakPos);
            if (this.rotate.getValue()) {
                Vec3i vec3i = side.getVector();
                if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * 0.5, (double)vec3i.getY() * 0.5, (double)vec3i.getZ() * 0.5)))) {
                    return;
                }
            }
            this.mineTimer.reset();
            this.animationTime.reset();
            if (this.swing.getValue()) {
                EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
            }
            if (this.doubleBreak.getValue()) {
                if (secondPos == null || this.isAir(secondPos)) {
                    double breakTime = this.getBreakTime(this.breakPos, slot, 1.0);
                    this.secondAnim.reset();
                    this.secondAnim.setLength((long)breakTime);
                    this.secondTimer.reset();
                    secondPos = this.breakPos;
                }
                // doDoubleBreak 宸蹭笉鍐嶈皟鐢?
            }
            PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.breakPos, side, id));
            if (this.fastBypass.getValue()) {
                this.sendFastBypassPackets();
            }
            if (this.rotate.getValue() && !this.shouldYawStep()) {
                Astra.ROTATION.snapBack();
            }
            this.startTime.reset();
        }
    }

    private void breakBlock(BlockPos breakPos) {
        PacketMine.mc.world.getBlockState(breakPos).getBlock().onBreak((World)PacketMine.mc.world, breakPos, PacketMine.mc.world.getBlockState(breakPos), (PlayerEntity)PacketMine.mc.player);
    }

    void doDoubleBreak(Direction side) {
        // 淇濈暀浣嗕笉璋冪敤
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.breakPos, side, id));
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, side, id));
    }

    @EventListener
    public void onAttackBlock(ClickBlockEvent event) {
        int slot;
        if (PacketMine.nullCheck()) {
            return;
        }
        if (PacketMine.mc.player.isCreative()) {
            return;
        }
        event.cancel();
        BlockPos pos = event.getPos();
        if (pos.equals((Object)this.breakPos)) {
            return;
        }
        if (PacketMine.unbreakable(pos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()))) > this.range.getValue()) {
            return;
        }

        // 鍒囨崲鍓嶅彂閫佸畬鎴愬寘
        if (this.breakPos != null && this.startPacket) {
            Direction oldSide = BlockUtil.getClickSide(this.breakPos);
            if (oldSide != null) {
                PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, oldSide, id));
            }
            this.startPacket = false;
            ghost = false;
            complete = false;
        }

        this.breakPos = pos;
        this.breakNumber = 0;
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.mineTimer.reset();
        this.animationTime.reset();
        this.startTime.reset();

        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        Direction side = BlockUtil.getClickSide(this.breakPos);
        if (this.rotate.getValue()) {
            Vec3i vec3i = side.getVector();
            if (!this.faceVector(this.breakPos.toCenterPos().add(new Vec3d((double)vec3i.getX() * 0.5, (double)vec3i.getY() * 0.5, (double)vec3i.getZ() * 0.5)))) {
                return;
            }
        }
        // startDelay 妫€鏌?
        if (!this.startTime.passed(this.startDelay.getValueInt())) {
            return;
        }
        if (this.swing.getValue()) {
            EntityUtil.swingHand(Hand.MAIN_HAND, this.swingMode.getValue());
        }
        if (this.doubleBreak.getValue()) {
            if (secondPos == null || this.isAir(secondPos)) {
                int slot2 = this.getTool(this.breakPos);
                if (slot2 == -1) {
                    slot2 = PacketMine.mc.player.getInventory().selectedSlot;
                }
                this.secondFinalTime = this.getBreakTime(this.breakPos, slot2, 1.0);
                this.secondAnim.reset();
                this.secondAnim.setLength((long)this.secondFinalTime);
                this.secondTimer.reset();
                secondPos = this.breakPos;
            }
            // doDoubleBreak 宸插垹闄よ皟鐢?
        }
        if ((slot = this.getTool(this.breakPos)) == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
        PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.breakPos, side, id));
        if (this.fastBypass.getValue()) {
            this.sendFastBypassPackets();
        }
        if (this.rotate.getValue() && !this.shouldYawStep()) {
            Astra.ROTATION.snapBack();
        }
        this.startTime.reset();
    }

    public void mine(BlockPos pos) {
        if (PacketMine.nullCheck()) {
            return;
        }
        if (PacketMine.mc.player.isCreative()) {
            PacketMine.mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (this.isOff()) {
            PacketMine.mc.interactionManager.attackBlock(pos, BlockUtil.getClickSide(pos));
            return;
        }
        if (pos.equals((Object)this.breakPos)) {
            return;
        }
        if (PacketMine.unbreakable(pos)) {
            return;
        }
        if (BlockUtil.getClickSideStrict(pos) == null) {
            return;
        }
        if ((double)MathHelper.sqrt((float)((float)PacketMine.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()))) > this.range.getValue()) {
            return;
        }

        if (this.breakPos != null && this.startPacket) {
            Direction oldSide = BlockUtil.getClickSide(this.breakPos);
            if (oldSide != null) {
                PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, oldSide, id));
            }
            this.startPacket = false;
            ghost = false;
            complete = false;
        }

        this.breakPos = pos;
        this.breakNumber = 0;
        this.startPacket = false;
        ghost = false;
        complete = false;
        this.mineTimer.reset();
        this.animationTime.reset();
        this.startTime.reset();
    }

    public void stopMining() {
        if (this.breakPos != null && this.startPacket) {
            Direction oldSide = BlockUtil.getClickSide(this.breakPos);
            if (oldSide != null) {
                sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.breakPos, oldSide, id));
            }
            this.startPacket = false;
            ghost = false;
            complete = false;
        }
        this.breakPos = null;
    }

    private void sendFastBypassPackets() {
        BlockPos highPos = new BlockPos(this.breakPos.getX(), 400, this.breakPos.getZ());
        for (int i = 0; i < 3; i++) {
            final int heightOffset = i * 10;
            Direction[] directions = Direction.values();
            Direction randomDir = directions[(int) (Math.random() * directions.length)];
            PacketMine.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, highPos.up(heightOffset), randomDir, id));
        }
    }

    boolean faceVector(Vec3d directionVec) {
        if (!this.shouldYawStep()) {
            Astra.ROTATION.lookAt(directionVec);
            return true;
        }
        this.sync.reset();
        this.directionVec = directionVec;
        if (Astra.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
            return true;
        }
        return !this.checkFov.getValue();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.breakPos != null && PacketMine.mc.world.isAir(this.breakPos)) {
            complete = true;
        }
        if (!PacketMine.mc.player.isCreative()) {
            double ease;
            if (secondPos != null) {
                if (this.isAir(secondPos)) {
                    secondPos = null;
                    return;
                }
                if (!this.checkDouble.getValue() || !secondPos.equals((Object)this.breakPos)) {
                    this.secondAnim.setLength((long)this.secondFinalTime);
                    ease = this.secondAnim.ease(this.ease.getValue());
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, this.getFillBox(secondPos, ease), this.doubleColor.getValue());
                    }
                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, this.getOutlineBox(secondPos, ease), this.doubleOutlineColor.getValue());
                    }
                }
            }
            if (this.breakPos != null) {
                progress = (double)this.mineTimer.getMs() / this.breakFinalTime;
                this.animationTime.setLength((long)this.breakFinalTime);
                ease = this.animationTime.ease(this.ease.getValue());
                if (PacketMine.unbreakable(this.breakPos)) {
                    if (this.box.getValue()) {
                        Render3DUtil.drawFill(matrixStack, new Box(this.breakPos), this.startColor.getValue());
                    }
                    if (this.outline.getValue()) {
                        Render3DUtil.drawBox(matrixStack, new Box(this.breakPos), this.startOutlineColor.getValue());
                    }
                    return;
                }
                if (this.box.getValue()) {
                    Render3DUtil.drawFill(matrixStack, this.getFillBox(this.breakPos, ease), this.getColor(this.animationTime.ease(this.fadeEase.getValue())));
                }
                if (this.outline.getValue()) {
                    Render3DUtil.drawBox(matrixStack, this.getOutlineBox(this.breakPos, ease), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue())));
                }
                if (this.text.getValue()) {
                    if (this.isAir(this.breakPos)) {
                        Render3DUtil.drawText3D("Waiting", this.breakPos.toCenterPos(), -1);
                    } else if ((double)((int)this.mineTimer.getMs()) < this.breakFinalTime) {
                        Render3DUtil.drawText3D(this.df.format(progress * 100.0) + "%", this.breakPos.toCenterPos(), -1);
                    } else {
                        Render3DUtil.drawText3D("100.0%", this.breakPos.toCenterPos(), -1);
                    }
                }
            } else {
                progress = 0.0;
            }
        } else {
            progress = 0.0;
        }
    }

    private Box getFillBox(BlockPos pos, double ease) {
        switch (this.animation.getValue().ordinal()) {
            case 0: // Center
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            case 1: // Grow
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            case 2: // Down
                return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(),
                        (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
            case 3: // Up
                return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(),
                        (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
            case 4: // Oscillation
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            default: // None (ordinal=5) 鎴栧叾瀹冿紝鏃犲姩鐢?
                return new Box(pos);
        }
    }

    private Box getOutlineBox(BlockPos pos, double ease) {
        ease = Math.min(ease + this.expandLine.getValue(), 1.0);
        switch (this.animation.getValue().ordinal()) {
            case 0: // Center
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            case 1: // Grow
                ease = (1.0 - ease) / 2.0;
                return new Box(pos).shrink(ease, 0.0, ease).shrink(-ease, 0.0, -ease);
            case 2: // Down
                return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(),
                        (double)(pos.getX() + 1), (double)pos.getY() + ease, (double)(pos.getZ() + 1));
            case 3: // Up
                return new Box((double)pos.getX(), (double)(pos.getY() + 1) - ease, (double)pos.getZ(),
                        (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
            case 4: // Oscillation
                return new Box(pos).shrink(ease, ease, ease).shrink(-ease, -ease, -ease);
            default: // None
                return new Box(pos);
        }
    }

    @EventListener(priority=-200)
    public void onPacketSend(PacketEvent.Send event) {
        if (PacketMine.nullCheck() || PacketMine.mc.player.isCreative()) {
            return;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.bypassGround.getValue() && !PacketMine.mc.player.isFallFlying() && this.breakPos != null && !this.isAir(this.breakPos) && this.bypassTime.getValue() > 0.0 && MathHelper.sqrt((float)((float)this.breakPos.toCenterPos().squaredDistanceTo(PacketMine.mc.player.getEyePos()))) <= this.range.getValueFloat() + 2.0f) {
                double breakTime = this.breakFinalTime - this.bypassTime.getValue();
                if (breakTime <= 0.0 || this.mineTimer.passed((long)breakTime)) {
                    this.sendGroundPacket = true;
                    ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(true);
                }
            } else {
                this.sendGroundPacket = false;
            }
        } else {
            Packet<?> packet = event.getPacket();
            if (packet instanceof UpdateSelectedSlotC2SPacket) {
                UpdateSelectedSlotC2SPacket packet2 = (UpdateSelectedSlotC2SPacket)packet;
                if (packet2.getSelectedSlot() != this.lastSlot) {
                    this.lastSlot = packet2.getSelectedSlot();
                }
            } else {
                packet = event.getPacket();
                if (packet instanceof PlayerActionC2SPacket) {
                    PlayerActionC2SPacket packet3 = (PlayerActionC2SPacket)packet;
                    if (packet3.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                        if (this.breakPos == null || !packet3.getPos().equals((Object)this.breakPos)) {
                            return;
                        }
                        this.startPacket = true;
                    } else if (packet3.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                        if (this.breakPos == null || !packet3.getPos().equals((Object)this.breakPos)) {
                            return;
                        }
                        if (!this.instant.getValue()) {
                            this.startPacket = false;
                            ghost = false;
                            complete = false;
                        }
                    }
                }
            }
        }
    }

    public static double getBreakTime(BlockPos pos) {
        int slot = INSTANCE.getTool(pos);
        if (slot == -1) {
            slot = PacketMine.mc.player.getInventory().selectedSlot;
        }
        return INSTANCE.getBreakTime(pos, slot);
    }

    double getBreakTime(BlockPos pos, int slot) {
        return this.getBreakTime(pos, slot, this.damage.getValue());
    }

    double getBreakTime(BlockPos pos, int slot, double damage) {
        return (double)(1.0f / this.getBlockStrength(pos, PacketMine.mc.player.getInventory().getStack(slot)) / 20.0f * 1000.0f) * damage;
    }

    float getBlockStrength(BlockPos position, ItemStack itemStack) {
        BlockState state = PacketMine.mc.world.getBlockState(position);
        float hardness = state.getHardness((BlockView)PacketMine.mc.world, position);
        if (hardness < 0.0f) {
            return 0.0f;
        }
        float i = !state.isToolRequired() || itemStack.isSuitableFor(state) ? 30.0f : 100.0f;
        return this.getDigSpeed(state, itemStack) / hardness / i;
    }

    float getDigSpeed(BlockState state, ItemStack itemStack) {
        boolean inWeb;
        int efficiencyModifier;
        float digSpeed = this.getDestroySpeed(state, itemStack);
        if (digSpeed > 1.0f && (efficiencyModifier = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)itemStack)) > 0 && !itemStack.isEmpty()) {
            digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1.0f + (float)(PacketMine.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2f;
        }
        if (PacketMine.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            digSpeed *= (switch (PacketMine.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            });
        }
        if (PacketMine.mc.player.isSubmergedInWater()) {
            digSpeed *= (float)PacketMine.mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();
        }
        boolean bl = inWeb = this.checkWeb.getValue() && Astra.PLAYER.isInWeb((PlayerEntity)PacketMine.mc.player) && PacketMine.mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
        // Smart 宸插垹闄わ紝淇濈暀妫€鏌?
        if ((!PacketMine.mc.player.isOnGround() || inWeb) && PacketMine.INSTANCE.checkGround.getValue() && (Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || PacketMine.mc.player.isFallFlying() || inWeb)) {
            digSpeed /= 5.0f;
        }
        return digSpeed < 0.0f ? 0.0f : digSpeed;
    }

    float getDestroySpeed(BlockState state, ItemStack itemStack) {
        float destroySpeed = 1.0f;
        if (itemStack != null && !itemStack.isEmpty()) {
            destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
        }
        return destroySpeed;
    }

    Color getColor(double quad) {
        int sR = this.startColor.getValue().getRed();
        int sG = this.startColor.getValue().getGreen();
        int sB = this.startColor.getValue().getBlue();
        int sA = this.startColor.getValue().getAlpha();
        int eR = this.endColor.getValue().getRed();
        int eG = this.endColor.getValue().getGreen();
        int eB = this.endColor.getValue().getBlue();
        int eA = this.endColor.getValue().getAlpha();
        return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
    }

    Color getOutlineColor(double quad) {
        int sR = this.startOutlineColor.getValue().getRed();
        int sG = this.startOutlineColor.getValue().getGreen();
        int sB = this.startOutlineColor.getValue().getBlue();
        int sA = this.startOutlineColor.getValue().getAlpha();
        int eR = this.endOutlineColor.getValue().getRed();
        int eG = this.endOutlineColor.getValue().getGreen();
        int eB = this.endOutlineColor.getValue().getBlue();
        int eA = this.endOutlineColor.getValue().getAlpha();
        return new Color((int)((double)sR + (double)(eR - sR) * quad), (int)((double)sG + (double)(eG - sG) * quad), (int)((double)sB + (double)(eB - sB) * quad), (int)((double)sA + (double)(eA - sA) * quad));
    }

    int getTool(BlockPos pos) {
        if (this.hotBar.getValue()) {
            int index = -1;
            float CurrentFastest = 1.0f;
            for (int i = 0; i < 9; ++i) {
                float destroySpeed;
                float digSpeed;
                ItemStack stack = PacketMine.mc.player.getInventory().getStack(i);
                if (stack == ItemStack.EMPTY || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)stack)) + (destroySpeed = stack.getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
                CurrentFastest = digSpeed + destroySpeed;
                index = i;
            }
            return index;
        }
        AtomicInteger slot = new AtomicInteger();
        slot.set(-1);
        float CurrentFastest = 1.0f;
        for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            float destroySpeed;
            float digSpeed;
            if (entry.getValue().getItem() instanceof AirBlockItem || !((digSpeed = (float)EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)PacketMine.mc.world.getRegistryManager().get(Enchantments.EFFICIENCY.getRegistryRef()).getEntry(Enchantments.EFFICIENCY).get()), (ItemStack)entry.getValue())) + (destroySpeed = entry.getValue().getMiningSpeedMultiplier(PacketMine.mc.world.getBlockState(pos))) > CurrentFastest)) continue;
            CurrentFastest = digSpeed + destroySpeed;
            slot.set(entry.getKey());
        }
        return slot.get();
    }

    private boolean shouldYawStep() {
        if (!this.whenElytra.getValue() && (PacketMine.mc.player.isFallFlying() || ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.isFallFlying())) {
            return false;
        }
        return this.yawStep.getValue();
    }

    boolean isAir(BlockPos breakPos) {
        return PacketMine.mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
    }

    public static boolean unbreakable(BlockPos blockPos) {
        Block block = PacketMine.mc.world.getBlockState(blockPos).getBlock();
        return !(block instanceof AirBlock) && (block.getHardness() == -1.0f || block.getHardness() == 100.0f);
    }

    static {
        progress = 0.0;
        ghost = false;
        complete = false;
    }

    public static enum Page {
        General,
        Check,
        Rotation,
        Render;
    }

    private static enum Animation {
        Center,
        Grow,
        Up,
        Down,
        Oscillation,
        None;
    }
}