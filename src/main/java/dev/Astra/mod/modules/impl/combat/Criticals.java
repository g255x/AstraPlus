/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.decoration.EndCrystalEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket$InteractType
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket$PositionAndOnGround
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Direction$AxisDirection
 */
package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.MovementUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.asm.accessors.IPlayerMoveC2SPacket;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.impl.exploit.BowBomb;
import dev.Astra.mod.modules.impl.exploit.Phase;
import dev.Astra.mod.modules.impl.movement.ElytraFly;
import dev.Astra.mod.modules.impl.player.AutoPearl;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class Criticals extends Module {
    public static Criticals INSTANCE;
    public final EnumSetting<Mode> mode = this.add(new EnumSetting<Mode>("Mode", Mode.NCP));
    public final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> !this.mode.is(Mode.Ground)));
    private final BooleanSetting setOnGround = this.add(new BooleanSetting("SetNoGround", false, () -> this.mode.is(Mode.Ground)));
    private final BooleanSetting blockCheck = this.add(new BooleanSetting("BlockCheck", true, () -> this.mode.is(Mode.Ground)));
    private final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(Mode.Ground)).setParent());
    private final BooleanSetting mini = this.add(new BooleanSetting("Mini", true, () -> this.mode.is(Mode.Ground) && this.autoJump.isOpen()));
    private final SliderSetting y = this.add(new SliderSetting("MotionY", 0.05, 0.0, 1.0, 0.001, () -> this.mode.is(Mode.Ground) && this.autoJump.isOpen()).setSuffix("blocks"));
    private final BooleanSetting flight = this.add(new BooleanSetting("Flight", false, () -> this.mode.is(Mode.Ground)));

    // 所有模式通用设置
    private final BooleanSetting pauseOnMove = this.add(new BooleanSetting("PauseOnMove", true));
    private final BooleanSetting crawlPause = this.add(new BooleanSetting("CrawlPause", false));
    private final BooleanSetting onlyInBlock = this.add(new BooleanSetting("OnlyInBlock", true, () -> !this.mode.is(Mode.Ground)));
    private final BooleanSetting headBlock = this.add(new BooleanSetting("HeadBlock", false));

    // Ground 模式专用设置
    private final BooleanSetting onlyAura = this.add(new BooleanSetting("OnlyAura", true, () -> this.mode.is(Mode.Ground)));
    private final BooleanSetting pauseElytraFly = this.add(new BooleanSetting("PauseElytraFly", true, () -> this.mode.is(Mode.Ground)));

    boolean requireJump = false;
    private boolean paused = false;
    private boolean crawlingPaused = false;
    private boolean elytraPaused = false;

    public Criticals() {
        super("Criticals", Module.Category.Combat);
        this.setChinese("刀刀暴击");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    // ==================== 卡墙检测（仅用于非 Ground 模式） ====================
    private boolean isInBlock() {
        if (mc.player == null || mc.world == null) return false;
        double w = mc.player.getWidth();
        return isCornerStuck(mc.player.getX() - w * 0.35, mc.player.getZ() + w * 0.35) ||
                isCornerStuck(mc.player.getX() - w * 0.35, mc.player.getZ() - w * 0.35) ||
                isCornerStuck(mc.player.getX() + w * 0.35, mc.player.getZ() - w * 0.35) ||
                isCornerStuck(mc.player.getX() + w * 0.35, mc.player.getZ() + w * 0.35);
    }

    private boolean isCornerStuck(double x, double z) {
        BlockPos pos = BlockPos.ofFloored(x, mc.player.getY(), z);
        if (wouldCollideAt(pos)) {
            double d = x - pos.getX();
            double e = z - pos.getZ();
            for (Direction dir : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
                double g = dir.getAxis().choose(d, 0.0, e);
                double h = dir.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - g : g;
                if (h < 0.0 || wouldCollideAt(pos.offset(dir))) continue;
                return true;
            }
        }
        return false;
    }

    private boolean wouldCollideAt(BlockPos pos) {
        Box box = mc.player.getBoundingBox();
        Box box2 = new Box(pos.getX(), box.minY, pos.getZ(), pos.getX() + 1.0, box.maxY, pos.getZ() + 1.0).contract(1.0E-7);
        return mc.world.canCollide(mc.player, box2);
    }

    // ==================== 头顶方块检测（修改为检测 0.3 格上方的实体方块） ====================
    private boolean hasHeadBlock() {
        if (mc.player == null || mc.world == null) return false;
        // 取玩家头顶上方 0.3 格处的方块位置
        BlockPos headPos = BlockPos.ofFloored(
                mc.player.getX(),
                mc.player.getY() + mc.player.getHeight() + 0.3,
                mc.player.getZ()
        );
        // 检测该位置是否有非空碰撞箱（即实体方块）
        return !mc.world.getBlockState(headPos).getCollisionShape(mc.world, headPos).isEmpty();
    }

    // ==================== Aura 目标检查 ====================
    private boolean isAuraTargetingPlayer() {
        return Aura.target != null && Aura.target instanceof PlayerEntity;
    }

    // ==================== Ground 模式总条件（包含头顶检测） ====================
    private boolean shouldGroundActivate() {
        if (!this.mode.is(Mode.Ground)) return false;
        if (this.paused || this.crawlingPaused || this.elytraPaused) return false;
        if (this.onlyAura.getValue() && !isAuraTargetingPlayer()) return false;
        if (this.pauseElytraFly.getValue() && ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn()) return false;
        if (this.headBlock.getValue() && !hasHeadBlock()) return false;
        return true;
    }

    // ==================== 发包拦截 ====================
    @EventListener
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;

        if (!this.mode.is(Mode.Ground)) {
            boolean checkInBlock = this.onlyInBlock.getValue();
            boolean checkHead = this.headBlock.getValue();
            if (checkInBlock || checkHead) {
                boolean inBlock = isInBlock();
                boolean headBlocked = hasHeadBlock();
                if (!( (checkInBlock && inBlock) || (checkHead && headBlocked) )) {
                    return;
                }
            }
            if (this.paused || this.crawlingPaused) return;
        } else {
            if (!this.shouldGroundActivate()) return;
        }

        if (this.mode.is(Mode.Ground)) {
            if (BowBomb.send) return;
            if (AutoPearl.throwing || Phase.INSTANCE.isOn()) return;
            if (!this.setOnGround.getValue()) return;
            if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(false);
            }
            return;
        }

        Packet<?> packet2 = event.getPacket();
        if (packet2 instanceof PlayerInteractEntityC2SPacket) {
            PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) packet2;
            if (Criticals.getInteractType(packet) == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                Entity entity = Criticals.getEntity(packet);
                if (!(entity instanceof EndCrystalEntity) &&
                        (!this.onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying) &&
                        !mc.player.isInLava() && !mc.player.isTouchingWater() && entity != null) {
                    mc.player.addCritParticles(entity);
                    this.doCrit(entity);
                }
            }
        }
    }

    @Override
    public void onLogout() {}

    @Override
    public void onEnable() {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;
        this.requireJump = true;
        this.paused = false;
        this.crawlingPaused = false;
        this.elytraPaused = false;
        if (this.mode.is(Mode.Ground)) {
            if (nullCheck()) return;
            if (!this.shouldGroundActivate()) return;
            if (mc.player.isOnGround() && this.autoJump.getValue() &&
                    (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
                this.jump();
            }
        }
    }

    public void jump() {
        if (this.mini.getValue()) {
            MovementUtil.setMotionY(this.y.getValue());
        } else {
            mc.player.jump();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) return;

        // 1. 更新移动暂停标志
        boolean isMoving = MovementUtil.isMoving();
        if (this.pauseOnMove.getValue()) {
            if (isMoving && !this.paused) {
                this.paused = true;
                this.requireJump = false;
            } else if (!isMoving && this.paused) {
                this.paused = false;
                if (this.mode.is(Mode.Ground) && !this.crawlingPaused && !this.elytraPaused &&
                        (!this.onlyAura.getValue() || isAuraTargetingPlayer())) {
                    this.requireJump = true;
                    if (mc.player.isOnGround() && this.autoJump.getValue() &&
                            (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
                        this.jump();
                        this.requireJump = false;
                    }
                }
            }
        }

        // 2. 更新爬行暂停标志
        boolean isCrawling = mc.player.isCrawling();
        if (this.crawlPause.getValue()) {
            if (isCrawling && !this.crawlingPaused) {
                this.crawlingPaused = true;
                this.requireJump = false;
            } else if (!isCrawling && this.crawlingPaused) {
                this.crawlingPaused = false;
                if (this.mode.is(Mode.Ground) && !this.paused && !this.elytraPaused &&
                        (!this.onlyAura.getValue() || isAuraTargetingPlayer())) {
                    this.requireJump = true;
                    if (mc.player.isOnGround() && this.autoJump.getValue() &&
                            (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
                        this.jump();
                        this.requireJump = false;
                    }
                }
            }
        }

        // 3. 更新滑翔翼暂停标志
        boolean isElytraFlying = (ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn());
        if (this.pauseElytraFly.getValue()) {
            if (isElytraFlying && !this.elytraPaused) {
                this.elytraPaused = true;
                this.requireJump = false;
            } else if (!isElytraFlying && this.elytraPaused) {
                this.elytraPaused = false;
                if (this.mode.is(Mode.Ground) && !this.paused && !this.crawlingPaused &&
                        (!this.onlyAura.getValue() || isAuraTargetingPlayer())) {
                    this.requireJump = true;
                    if (mc.player.isOnGround() && this.autoJump.getValue() &&
                            (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
                        this.jump();
                        this.requireJump = false;
                    }
                }
            }
        }

        // 非 Ground 模式不需要自动跳跃
        if (!this.mode.is(Mode.Ground)) return;

        // Ground 模式自动跳跃逻辑
        if (!this.shouldGroundActivate()) return;

        if (this.flight.getValue() && mc.player.fallDistance > 0.0f) {
            MovementUtil.setMotionY(0.0);
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
            this.requireJump = false;
        } else if (this.blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2)))) {
            this.requireJump = true;
        } else if (mc.player.isOnGround() && this.autoJump.getValue() && (this.flight.getValue() || this.requireJump)) {
            this.jump();
            this.requireJump = false;
        }
    }

    public void doCrit(Entity entity) {
        if (this.mode.getValue() == Mode.NCP) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
        } else if (this.mode.getValue() == Mode.GrimV3) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch(), true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(),
                    Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch(), false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 0.04535F, mc.player.getZ(),
                    Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch(), false));
        }
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        return mc.world == null ? null : mc.world.getEntityById(((dev.Astra.asm.accessors.IPlayerInteractEntityC2SPacket)packet).getEntityId());
    }

    public static PlayerInteractEntityC2SPacket.InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        final PlayerInteractEntityC2SPacket.InteractType[] result = new PlayerInteractEntityC2SPacket.InteractType[1];
        packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override
            public void interact(net.minecraft.util.Hand hand) {
                result[0] = PlayerInteractEntityC2SPacket.InteractType.INTERACT;
            }
            @Override
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {
                result[0] = PlayerInteractEntityC2SPacket.InteractType.INTERACT_AT;
            }
            @Override
            public void attack() {
                result[0] = PlayerInteractEntityC2SPacket.InteractType.ATTACK;
            }
        });
        return result[0];
    }

    public static enum Mode {
        NCP,
        GrimV3,
        Ground
    }
}