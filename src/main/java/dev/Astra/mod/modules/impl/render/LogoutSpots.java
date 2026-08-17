package dev.Astra.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.render.ModelPlayer;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.asm.accessors.IEntity;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.hud.TextRadar;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LogoutSpots extends Module {
    private final SliderSetting maxSpots = this.add(new SliderSetting("MaxSpots", 5, 1, 20, 1));
    private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
    private final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
    private final BooleanSetting message = this.add(new BooleanSetting("Message", true));
    private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(1570936063, true)).injectBoolean(true));
    private final ColorSetting box = this.add(new ColorSetting("Box", new Color(-6122241, true)).injectBoolean(true));
    private final ColorSetting text = this.add(new ColorSetting("Text", new Color(-1, true)).injectBoolean(true));
    private final ColorSetting chamsFill = this.add(new ColorSetting("ChamsFill", new Color(-1214081793, true)).injectBoolean(true));
    private final ColorSetting chamsLine = this.add(new ColorSetting("ChamsLine", new Color(-56453889, true)).injectBoolean(true));
    final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
    final Map<UUID, ModelPlayer> logoutCache = Maps.newConcurrentMap();

    public LogoutSpots() {
        super("LogoutSpots", Module.Category.Render);
        this.setChinese("退出记录");
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (LogoutSpots.nullCheck()) {
            return;
        }
        Object object = event.getPacket();
        if (object instanceof PlayerListS2CPacket) {
            PlayerListS2CPacket packet = (PlayerListS2CPacket)object;
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                    if (addedPlayer.gameMode() == GameMode.SPECTATOR) continue;
                    for (UUID uuid : this.logoutCache.keySet()) {
                        if (!uuid.equals(addedPlayer.profile().getId())) continue;
                        PlayerEntity player = this.logoutCache.get((Object)uuid).player;
                        if (this.message.getValue()) {
                            mc.execute(() -> this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rLogged back at \u00a7f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ()));
                        }
                        this.logoutCache.remove(uuid);
                    }
                }
            }
        } else {
            object = event.getPacket();
            if (object instanceof PlayerRemoveS2CPacket) {
                List<UUID> profileIds;
                PlayerRemoveS2CPacket playerRemoveS2CPacket = (PlayerRemoveS2CPacket)object;
                try {
                    List<UUID> addedPlayer;
                    profileIds = addedPlayer = playerRemoveS2CPacket.profileIds();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
                for (UUID uuid2 : profileIds) {
                    for (UUID uuid : this.playerCache.keySet()) {
                        if (!uuid.equals(uuid2)) continue;
                        PlayerEntity player = this.playerCache.get(uuid);
                        if (this.logoutCache.containsKey(uuid) || player == null) continue;
                        ModelPlayer modelPlayer = new ModelPlayer(player);
                        if (this.message.getValue()) {
                            mc.execute(() -> this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rLogged out at \u00a7f" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ()));
                        }
                        this.logoutCache.put(uuid, modelPlayer);
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.playerCache.clear();
        this.logoutCache.clear();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.playerCache.clear();
        for (AbstractClientPlayerEntity player : Astra.THREAD.getPlayers()) {
            if (player == null || player.equals((Object)LogoutSpots.mc.player)) continue;
            this.playerCache.put(player.getGameProfile().getId(), (PlayerEntity)player);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.logoutCache.isEmpty()) return;

        int max = (int) this.maxSpots.getValue();
        if (max <= 0) return;

        Vec3d playerPos = mc.player.getPos();

        List<ModelPlayer> sortedSpots = this.logoutCache.values().stream()
                .sorted(Comparator.comparingDouble(spot -> spot.player.getPos().squaredDistanceTo(playerPos)))
                .limit(max)
                .collect(Collectors.toList());

        for (ModelPlayer data : sortedSpots) {
            PlayerEntity player = data.player;
            Box box = ((IEntity)player).getDimensions().getBoxAt(player.getPos());
            if (this.box.booleanValue) {
                Render3DUtil.drawBox(matrixStack, box, this.box.getValue());
            }
            if (this.fill.booleanValue) {
                Render3DUtil.drawFill(matrixStack, box, this.fill.getValue());
            }
            if (this.chamsFill.booleanValue || this.chamsLine.booleanValue) {
                data.render(matrixStack, this.chamsFill, this.chamsLine);
            }
            if (!this.text.booleanValue) continue;
            Render3DUtil.drawText3D(player.getName().getString() + (String)(this.health.getValue() ? String.valueOf(TextRadar.getHealthColor(player)) + " " + LogoutSpots.round2(player.getHealth() + player.getAbsorptionAmount()) : "") + (String)(this.totem.getValue() && Astra.POP.getPop(player) > 0 ? String.valueOf(TextRadar.getPopColor(Astra.POP.getPop(player))) + " -" + Astra.POP.getPop(player) : ""), new Vec3d(player.getX(), ((IEntity)player).getDimensions().getBoxAt((Vec3d)player.getPos()).maxY + 0.5, player.getZ()), this.text.getValue());
        }
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}