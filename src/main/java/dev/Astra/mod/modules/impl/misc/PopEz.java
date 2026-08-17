package dev.Astra.mod.modules.impl.misc;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.TotemEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import dev.Astra.mod.modules.settings.impl.StringSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PopEz extends Module {
    public static PopEz INSTANCE;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SliderSetting randomChars = this.add(new SliderSetting("RandomChars", 3.0, 0.0, 20.0, 1.0));
    public final BooleanSetting slowSend = this.add(new BooleanSetting("SlowSend", false));
    final StringSetting customMessage = this.add(new StringSetting("CustomMessage", ""));
    Random random = new Random();
    Timer timer = new Timer();
    private final Map<Integer, Integer> popQueue = new HashMap<>();

    public PopEz() {
        super("PopEz", Module.Category.Misc);
        this.setChinese("POP嘲讽");
        INSTANCE = this;
    }

    @EventListener
    public void onTotem(TotemEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player == mc.player || Astra.FRIEND.isFriend(player)) return;

        int count = 1;
        if (Astra.POP.popContainer.containsKey(player.getName().getString())) {
            count = Astra.POP.popContainer.get(player.getName().getString());
        }

        if (this.slowSend.getValue()) {
            this.popQueue.put(player.getId(), count);
            return;
        }

        this.sendPopMessage(player, count);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.slowSend.getValue() && !this.popQueue.isEmpty() && this.timer.passedS(3.2)) {
            this.timer.reset();
            Map.Entry<Integer, Integer> entry = this.popQueue.entrySet().iterator().next();
            int playerId = entry.getKey();
            int popCount = entry.getValue();
            PlayerEntity player = null;

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof PlayerEntity p && p.getId() == playerId) {
                    player = p;
                    break;
                }
            }

            if (player != null) {
                this.sendPopMessage(player, popCount);
            }

            this.popQueue.remove(playerId);
        }
    }

    private void sendPopMessage(PlayerEntity player, int count) {
        String rawMessage = this.customMessage.getValue();
        String message;
        if (rawMessage.isEmpty()) {
            if (count == 1) {
                message = player.getName().getString() + " has popped " + count + " totem.";
            } else {
                message = player.getName().getString() + " has popped " + count + " totems.";
            }
        } else {
            message = rawMessage.replace("%player%", player.getName().getString()).replace("%pop%", String.valueOf(count));
        }
        this.sendMessage(message);
    }

    public void sendMessage(String message) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        String randomString = this.generateRandomString(this.randomChars.getValueInt());
        if (!randomString.isEmpty()) {
            message = message + " " + randomString;
        }
        mc.getNetworkHandler().sendChatMessage(message);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = this.random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}