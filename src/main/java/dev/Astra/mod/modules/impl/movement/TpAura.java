package dev.Astra.mod.modules.impl.movement;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.path.PathUtils;
import dev.Astra.api.utils.path.TPUtils;
import dev.Astra.api.utils.path.Vec3;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BindSetting;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class TpAura extends Module {
    public static TpAura INSTANCE;
    private final EnumSetting<TpMode> tpMode;
    private final BooleanSetting keyMode;
    private final BindSetting key;
    private final SliderSetting range;
    private final SliderSetting delay;
    private final SliderSetting moveDistance;
    private final SliderSetting up;
    private final EnumSetting<TPUtils.TeleportType> tpType;
    private final BooleanSetting autoDisable;
    private final SliderSetting stayTime;
    private final Timer timer;
    private final Timer stayTimer;
    private Vec3d originalPos;
    private boolean isAtTarget;

    public TpAura() {
        super("TargetTP", Module.Category.Movement);
        this.tpMode = this.add(new EnumSetting<>("Mode", TpMode.Normal));
        this.keyMode = this.add(new BooleanSetting("KeyMode", false));
        this.key = this.add(new BindSetting("TeleportKey", -1, this.keyMode::getValue));
        this.range = this.add(new SliderSetting("Range", 114.0, 1.0, 1024.0, 1.0));
        this.delay = this.add(new SliderSetting("Delay", 0.0, 0.0, 1000.0, 1.0));
        this.moveDistance = this.add(new SliderSetting("MoveDistance", 10.0, 1.0, 50.0, 1.0));
        this.up = this.add(new SliderSetting("UP", 0.0, 0.0, 50.0, 1.0));
        this.tpType = this.add(new EnumSetting<>("TPType", TPUtils.TeleportType.New));
        this.autoDisable = this.add(new BooleanSetting("AutoDisable", true));
        this.stayTime = this.add(new SliderSetting("StayTime", 500.0, 50.0, 3000.0, 50.0).setSuffix("ms"));
        this.timer = new Timer();
        this.stayTimer = new Timer();
        this.originalPos = null;
        this.isAtTarget = false;
        this.setChinese("目标传送");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
        return target == null ? null : target.getName().getString();
    }

    @Override
    public void onDisable() {
        this.originalPos = null;
        this.isAtTarget = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!nullCheck()) {
            if (!this.keyMode.getValue() || this.key.isPressed()) {
                if (this.isAtTarget) {
                    if (this.stayTimer.passed((long) this.stayTime.getValue())) {
                        this.teleportBack();
                        this.isAtTarget = false;
                        this.originalPos = null;
                        if (this.autoDisable.getValue()) {
                            this.disable();
                        }
                    }
                } else {
                    PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
                    if (target != null) {
                        if (this.timer.passed((long) this.delay.getValue())) {
                            this.timer.reset();
                            Vec3d targetPos = target.getPos().add(0.0, this.up.getValue(), 0.0);
                            if (this.tpMode.getValue() == TpMode.FakeTP) {
                                this.originalPos = mc.player.getPos();
                                this.teleportTo(targetPos);
                                this.isAtTarget = true;
                                this.stayTimer.reset();
                            } else {
                                this.teleportTo(targetPos);
                                if (this.autoDisable.getValue()) {
                                    this.disable();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void teleportTo(Vec3d targetPos) {
        switch (this.tpType.getValue()) {
            case New -> this.teleportNew(targetPos);
            case Legacy -> this.teleportLegacy(targetPos);
        }
    }

    private void teleportBack() {
        if (this.originalPos != null) {
            switch (this.tpType.getValue()) {
                case New -> this.teleportNew(this.originalPos);
                case Legacy -> this.teleportLegacy(this.originalPos);
            }
        }
    }

    private void teleportNew(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();
        double distance = playerPos.distanceTo(targetPos);
        int packetsRequired = (int) Math.ceil(distance / this.moveDistance.getValue()) - 1;
        for (int i = 0; i < packetsRequired; ++i) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y, targetPos.z, true));
        mc.player.setPosition(targetPos);
    }

    private void teleportLegacy(Vec3d targetPos) {
        ArrayList<Vec3> path = PathUtils.computePath(targetPos);
        if (!path.isEmpty()) {
            path.removeFirst();
            for (Vec3 vec3 : path) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false));
            }
            mc.player.setPosition(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    public enum TpMode {
        Normal,
        FakeTP
    }
}