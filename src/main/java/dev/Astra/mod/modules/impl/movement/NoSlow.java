/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.client.gui.screen.ChatScreen
 *  net.minecraft.client.option.KeyBinding
 *  net.minecraft.client.util.InputUtil
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 *  net.minecraft.util.Hand
 */
package dev.Astra.mod.modules.impl.movement;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.InteractItemEvent;
import dev.Astra.api.events.impl.KeyboardInputEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.combat.ForceEat;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class NoSlow extends Module {
    public static NoSlow INSTANCE;
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.GrimV3));
    private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", false));
    private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
    private final BooleanSetting climb = this.add(new BooleanSetting("Climb", false));
    private final BooleanSetting gui = this.add(new BooleanSetting("Gui", true));
    private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", true, this.gui::getValue));

    boolean using = false;
    int delay = 0;

    public NoSlow() {
        super("NoSlow", Module.Category.Movement);
        this.setChinese("无减速");
        INSTANCE = this;
    }

    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) return 0.0f;
        return positive ? 1.0f : -1.0f;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    /**
     * GrimV3 条件：不骑乘，且处于使用物品的奇数 tick
     * 修改：移除对潜行和爬行的限制，使蹲下、趴下时也能生效
     */
    private boolean shouldNoSlowGrimV3() {
        if (NoSlow.mc.player == null) return false;
        return !NoSlow.mc.player.isRiding()
                && (NoSlow.mc.player.getItemUseTime() > 1 && NoSlow.mc.player.getItemUseTime() % 2 != 0);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.using = NoSlow.mc.player.isUsingItem();
        --this.delay;
        if (this.using) this.delay = 2;

        if (this.using && !NoSlow.mc.player.isRiding() && !NoSlow.mc.player.isFallFlying()) {
            if (this.mode.is(Mode.NCP)) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(NoSlow.mc.player.getInventory().selectedSlot));
            }
        }

        if (this.gui.getValue() && !(NoSlow.mc.currentScreen instanceof ChatScreen)) {
            for (KeyBinding k : new KeyBinding[]{
                    NoSlow.mc.options.backKey,
                    NoSlow.mc.options.leftKey,
                    NoSlow.mc.options.rightKey
            }) {
                k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                        InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
            }
            NoSlow.mc.options.jumpKey.setPressed(
                    (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue())
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.jumpKey.getBoundKeyTranslationKey()).getCode())
            );
            NoSlow.mc.options.forwardKey.setPressed(
                    AutoWalk.INSTANCE.forward()
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.forwardKey.getBoundKeyTranslationKey()).getCode())
            );
            NoSlow.mc.options.sprintKey.setPressed(
                    (Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater())
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.sprintKey.getBoundKeyTranslationKey()).getCode())
            );
            if (this.allowSneak.getValue()) {
                NoSlow.mc.options.sneakKey.setPressed(
                        InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                                InputUtil.fromTranslationKey(NoSlow.mc.options.sneakKey.getBoundKeyTranslationKey()).getCode())
                );
            }
        }
    }

    @EventListener(priority = 100)
    public void keyboard(KeyboardInputEvent event) {
        if (this.sneak.getValue()) event.cancel();

        if (this.gui.getValue() && !(NoSlow.mc.currentScreen instanceof ChatScreen)) {
            for (KeyBinding k : new KeyBinding[]{
                    NoSlow.mc.options.backKey,
                    NoSlow.mc.options.leftKey,
                    NoSlow.mc.options.rightKey
            }) {
                k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                        InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
            }
            NoSlow.mc.options.jumpKey.setPressed(
                    (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue())
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.jumpKey.getBoundKeyTranslationKey()).getCode())
            );
            NoSlow.mc.options.forwardKey.setPressed(
                    AutoWalk.INSTANCE.forward()
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.forwardKey.getBoundKeyTranslationKey()).getCode())
            );
            NoSlow.mc.options.sprintKey.setPressed(
                    (Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater())
                            || InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                            InputUtil.fromTranslationKey(NoSlow.mc.options.sprintKey.getBoundKeyTranslationKey()).getCode())
            );
            if (this.allowSneak.getValue()) {
                NoSlow.mc.options.sneakKey.setPressed(
                        InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                                InputUtil.fromTranslationKey(NoSlow.mc.options.sneakKey.getBoundKeyTranslationKey()).getCode())
                );
            }
            NoSlow.mc.player.input.pressingForward = NoSlow.mc.options.forwardKey.isPressed();
            NoSlow.mc.player.input.pressingBack = NoSlow.mc.options.backKey.isPressed();
            NoSlow.mc.player.input.pressingLeft = NoSlow.mc.options.leftKey.isPressed();
            NoSlow.mc.player.input.pressingRight = NoSlow.mc.options.rightKey.isPressed();
            NoSlow.mc.player.input.movementForward = getMovementMultiplier(
                    NoSlow.mc.player.input.pressingForward,
                    NoSlow.mc.player.input.pressingBack
            );
            NoSlow.mc.player.input.movementSideways = getMovementMultiplier(
                    NoSlow.mc.player.input.pressingLeft,
                    NoSlow.mc.player.input.pressingRight
            );
            NoSlow.mc.player.input.jumping = NoSlow.mc.options.jumpKey.isPressed();
            NoSlow.mc.player.input.sneaking = NoSlow.mc.options.sneakKey.isPressed();
        }
    }

    @EventListener
    public void onUse(InteractItemEvent event) {
        if (event.isPre() && this.delay > 0) {
            if (ForceEat.INSTANCE != null && ForceEat.INSTANCE.isOn()) return;
            NoSlow.mc.itemUseCooldown = 0;
            event.cancel();
        }
    }

    public boolean noSlow() {
        if (!this.isOn()) return false;
        Mode currentMode = this.mode.getValue();
        if (currentMode == Mode.None) return false;
        if (currentMode == Mode.GrimV3) {
            return shouldNoSlowGrimV3();
        }
        return true;
    }

    public boolean soulSand() {
        return this.isOn() && this.soulSand.getValue();
    }

    public boolean climb() {
        return this.isOn() && this.climb.getValue();
    }

    public static enum Mode {
        Vanilla,
        NCP,
        GrimV3,
        None;
    }
}