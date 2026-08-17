package dev.Astra.mod.modules.impl.movement;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.EntityVelocityUpdateEvent;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.TickEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.text.DecimalFormat;

public class Velocity extends Module {
    public static Velocity INSTANCE;

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.GrimV3));
    private final SliderSetting lagPause = add(new SliderSetting("LagPause", 100, 0, 500, () -> mode.is(Mode.Grim) || mode.is(Mode.GrimV3) || mode.is(Mode.Wall)).setSuffix("ms"));
    private final BooleanSetting ignorePearlLag = add(new BooleanSetting("IgnorePearlLag", true, () -> mode.is(Mode.Grim) || mode.is(Mode.GrimV3) || mode.is(Mode.Wall)));
    private final SliderSetting phaseTime = add(new SliderSetting("PhaseTime", 200, 0, 1000, () -> (mode.is(Mode.Grim) || mode.is(Mode.GrimV3) || mode.is(Mode.Wall)) && ignorePearlLag.getValue()).setSuffix("ms"));
    private final BooleanSetting cancelAll = add(new BooleanSetting("CancelAll", true, () -> mode.is(Mode.Plain) || mode.is(Mode.Wall)));
    private final SliderSetting horizontal = add(new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0, () -> !cancelAll.getValue() && (mode.is(Mode.Plain) || mode.is(Mode.Wall))).setSuffix("%"));
    private final SliderSetting vertical = add(new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0, () -> !cancelAll.getValue() && (mode.is(Mode.Plain) || mode.is(Mode.Wall))).setSuffix("%"));
    private final BooleanSetting wallsGroundOnly = add(new BooleanSetting("GroundOnly", false, () -> mode.is(Mode.Wall)));
    private final BooleanSetting wallsTrapped = add(new BooleanSetting("Trapped", false, () -> mode.is(Mode.Wall)));
    public final BooleanSetting whileLiquid = add(new BooleanSetting("WhileLiquid", true));
    public final BooleanSetting whileElytra = add(new BooleanSetting("FallFlying", true));
    public final BooleanSetting entityPush = add(new BooleanSetting("NoEntityPush", true));
    public final BooleanSetting blockPush = add(new BooleanSetting("NoBlockPush", true));
    public final BooleanSetting fishBob = add(new BooleanSetting("NoFishBob", true));
    public final BooleanSetting waterPush = add(new BooleanSetting("NoWaterPush", false));
    public final BooleanSetting noClimb = add(new BooleanSetting("NoClimb", false));

    private boolean shouldSkipNextZeroVelocity;
    private boolean cancelVelocity;
    private final Timer lagBackTimer = new Timer();
    public final Timer pearlTimer = new Timer();

    public Velocity() {
        super("Velocity", Module.Category.Movement);
        setChinese("反击退");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        if (mode.getValue() == Mode.None) return null;
        if (mode.is(Mode.Grim)) return "Grim";
        if (mode.is(Mode.GrimV3)) return "GrimV3";
        if (mode.is(Mode.Wall)) return "Wall";
        if (cancelAll.getValue()) return "Cancel";
        DecimalFormat df = new DecimalFormat("0.0");
        return String.format("H:%s%%, V:%s%%", df.format(horizontal.getValue()), df.format(vertical.getValue()));
    }

    @Override
    public void onEnable() {
        shouldSkipNextZeroVelocity = false;
        cancelVelocity = false;
    }

    @Override
    public void onDisable() {
        if (cancelVelocity && mode.getValue() == Mode.Grim) {
            sendCompensationPackets();
            cancelVelocity = false;
        }
        shouldSkipNextZeroVelocity = false;
    }

    private boolean isInsideBlock() {
        Box bb = mc.player.getBoundingBox();
        BlockPos min = new BlockPos((int) Math.floor(bb.minX), (int) Math.floor(bb.minY), (int) Math.floor(bb.minZ));
        BlockPos max = new BlockPos((int) Math.floor(bb.maxX), (int) Math.floor(bb.maxY), (int) Math.floor(bb.maxZ));
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!mc.world.getBlockState(new BlockPos(x, y, z)).isReplaceable()) return true;
                }
            }
        }
        return false;
    }

    private boolean isWallsTrapped() {
        BlockPos headPos = mc.player.getBlockPos().up(mc.player.isCrawling() ? 1 : 2);
        return !mc.world.getBlockState(headPos).isReplaceable();
    }

    private void sendCompensationPackets() {
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(),
                mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
        BlockPos pos = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    pos, mc.player.getHorizontalFacing().getOpposite()));
        }
    }

    private void applyScale(EntityVelocityUpdateEvent event) {
        double h = horizontal.getValue() / 100.0;
        double v = vertical.getValue() / 100.0;
        event.setX(event.getX() * h);
        event.setZ(event.getZ() * h);
        event.setY(event.getY() * v);
    }

    @EventListener
    public void onVelocity(EntityVelocityUpdateEvent event) {
        if (nullCheck()) return;
        if (event.getEntity() != mc.player) return;
        if (mode.getValue() == Mode.None) return;

        if (mc.player.isInFluid() && !whileLiquid.getValue()) return;
        if (mc.player.isFallFlying() && !whileElytra.getValue()) return;

        if (cancelAll.getValue() && shouldSkipNextZeroVelocity && event.getX() == 0 && event.getZ() == 0 && event.getY() == 0) {
            event.cancel();
            shouldSkipNextZeroVelocity = false;
            return;
        }

        if ((mode.is(Mode.Grim) || mode.is(Mode.GrimV3) || mode.is(Mode.Wall)) &&
                !lagBackTimer.passedMs((long) lagPause.getValue())) {
            return;
        }

        if (mode.is(Mode.Wall)) {
            if (!isInsideBlock() && (!wallsTrapped.getValue() || !isWallsTrapped())) return;
            if (wallsGroundOnly.getValue() && !mc.player.isOnGround()) return;
        } else if (mode.is(Mode.Grim) || mode.is(Mode.GrimV3)) {
            if (!isInsideBlock()) return;
        }

        if (mode.is(Mode.GrimV3)) {
            event.cancel();
            return;
        }

        if (mode.is(Mode.Grim)) {
            event.cancel();
            cancelVelocity = true;
            return;
        }

        applyScale(event);
    }

    @EventListener
    public void onReceivePacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (mc.player.isInFluid() && !whileLiquid.getValue()) return;

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (!ignorePearlLag.getValue() || pearlTimer.passed(phaseTime.getValueInt())) {
                lagBackTimer.reset();
            }
            if ((mode.is(Mode.Plain) || mode.is(Mode.Wall)) && cancelAll.getValue()) {
                shouldSkipNextZeroVelocity = true;
            }
        }

        if (fishBob.getValue() && event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 31) {
                Entity entity = packet.getEntity(mc.world);
                if (entity instanceof FishingBobberEntity hook && hook.getHookedEntity() == mc.player) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventListener
    public void onUpdate(TickEvent event) {
        if (nullCheck()) return;
        if (event.isPost()) return;
        if (mc.player.isInFluid() && !whileLiquid.getValue()) return;

        if (Blink.INSTANCE != null && Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule != null && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }

        if (cancelVelocity && mode.getValue() == Mode.Grim && lagBackTimer.passedMs((long) lagPause.getValue())) {
            sendCompensationPackets();
            cancelVelocity = false;
        }
    }

    @EventListener
    public void onPushEntity(Object event) {
        try {
            Entity pushed = (Entity) event.getClass().getMethod("getEntity").invoke(event);
            if (entityPush.getValue() && pushed == mc.player) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
            }
        } catch (Exception ignored) {}
    }

    @EventListener
    public void onPushOutOfBlocks(Object event) {
        if (blockPush.getValue()) {
            try {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
            } catch (Exception ignored) {}
        }
    }

    private BlockPos getPos() {
        if (mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.OBSIDIAN) {
            return mc.player.getBlockPos().down();
        }
        return null;
    }

    public enum Mode {
        Plain, Grim, Wall, GrimV3, None
    }
}