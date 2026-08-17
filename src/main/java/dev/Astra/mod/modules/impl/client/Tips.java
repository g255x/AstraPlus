package dev.Astra.mod.modules.impl.client;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.*;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.core.impl.CommandManager;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Tips extends Module {
    public static Tips INSTANCE;
    public final BooleanSetting visualRange = this.add(new BooleanSetting("VisualRange", true).setParent());
    public final BooleanSetting friends = this.add(new BooleanSetting("Friends", true, this.visualRange::isOpen));
    public final BooleanSetting visualRangeMessage = this.add(new BooleanSetting("Message", false, this.visualRange::isOpen));
    public final BooleanSetting visualRangeSound = this.add(new BooleanSetting("Sound", true, this.visualRange::isOpen));
    public final BooleanSetting popCounter = this.add(new BooleanSetting("PopCounter", true));
    public final BooleanSetting deathCoords = this.add(new BooleanSetting("DeathCoords", true));
    public final BooleanSetting serverLag = this.add(new BooleanSetting("ServerLag", true).setParent());
    public final SliderSetting serverLagYOffset = this.add(new SliderSetting("ServerLagYOffset", 140, -200, 200, this.serverLag::isOpen));
    public final BooleanSetting lagBack = this.add(new BooleanSetting("LagBack", true).setParent());
    public final SliderSetting lagBackYOffset = this.add(new SliderSetting("LagBackYOffset", 160, -200, 200, this.lagBack::isOpen));
    public final BooleanSetting potion = this.add(new BooleanSetting("Potion", true).setParent());
    public final BooleanSetting resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, this.potion::isOpen));
    public final SliderSetting potionYOffset = this.add(new SliderSetting("PotionYOffset", -40, -200, 200, this.potion::isOpen));
    public final BooleanSetting armorDurability = this.add(new BooleanSetting("ArmorDurability", true).setParent());
    public final SliderSetting armorThreshold = this.add(new SliderSetting("ArmorThreshold", 20, 0, 100, 1, this.armorDurability::isOpen));
    public final SliderSetting armorYOffset = this.add(new SliderSetting("ArmorYOffset", 30, -200, 200, this.armorDurability::isOpen));

    final DecimalFormat df = new DecimalFormat("0.0");
    final int color = new Color(190, 0, 0).getRGB();
    private final Timer lagTimer = new Timer();
    private final Timer lagBackTimer = new Timer();
    int turtles = 0;

    private static class TextSegment {
        String text;
        int color;
        TextSegment(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    public Tips() {
        super("Tips", Category.Client);
        this.setChinese("提示");
        INSTANCE = this;
    }

    @EventListener
    public void onAddEntity(EntitySpawnEvent event) {
        if (!this.visualRange.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Astra.FRIEND.isFriend(playerName);
        if (isFriend && !this.friends.getValue() || event.getEntity() == Tips.mc.player) {
            return;
        }
        if (this.visualRangeMessage.getValue()) {
            CommandManager.sendMessageId((isFriend ? String.valueOf(Formatting.AQUA) + playerName : String.valueOf(Formatting.WHITE) + playerName) + "\u00a7f entered your visual range.", event.getEntity().getId() + 777);
        }
        if (this.visualRangeSound.getValue()) {
            Tips.mc.world.playSound((PlayerEntity)Tips.mc.player, Tips.mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0f, 1.9f);
        }
    }

    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (!this.visualRange.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Astra.FRIEND.isFriend(playerName);
        if (isFriend && !this.friends.getValue() || event.getEntity() == Tips.mc.player) {
            return;
        }
        if (this.visualRangeMessage.getValue()) {
            CommandManager.sendMessageId((isFriend ? String.valueOf(Formatting.AQUA) + playerName : String.valueOf(Formatting.WHITE) + playerName) + "\u00a7f left your visual range.", event.getEntity().getId() + 777);
        }
        if (this.visualRangeSound.getValue()) {
            Tips.mc.world.playSound((PlayerEntity)Tips.mc.player, Tips.mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0f, 1.9f);
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.potion.getValue()) {
            this.turtles = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
        }
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        this.lagTimer.reset();
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagBackTimer.reset();
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int centerY = screenHeight / 2 + 9;

        if (this.serverLag.getValue() && this.lagTimer.passedS(1.0)) {
            String text = "Server not responding (" + this.df.format((double)this.lagTimer.getMs() / 1000.0) + "s)";
            int x = screenWidth / 2 - Tips.mc.textRenderer.getWidth(text) / 2;
            int y = centerY - this.serverLagYOffset.getValueInt();
            drawContext.drawText(Tips.mc.textRenderer, text, x, y, this.color, true);
        }

        if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.0)) {
            String text = "Lagback (" + this.df.format((double)(1000L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
            int x = screenWidth / 2 - Tips.mc.textRenderer.getWidth(text) / 2;
            int y = centerY - this.lagBackYOffset.getValueInt();
            drawContext.drawText(Tips.mc.textRenderer, text, x, y, this.color, true);
        }

        if (this.potion.getValue()) {
            List<TextSegment> segments = new ArrayList<>();

            if (this.turtles > 0) {
                segments.add(new TextSegment(String.valueOf(this.turtles), 0xFFFFFF00));
            }

            if (Tips.mc.player.hasStatusEffect(StatusEffects.RESISTANCE) && (!this.resistanceLevelCheck.getValue() || Tips.mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
                if (!segments.isEmpty()) {
                    segments.add(new TextSegment(" ", 0xFFFFFFFF));
                }
                String time = String.valueOf(Tips.mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                segments.add(new TextSegment(time, 0xFFA88EC8)); // 柔紫灰
            }

            if (Tips.mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                if (!segments.isEmpty()) {
                    segments.add(new TextSegment(" ", 0xFFFFFFFF));
                }
                String time = String.valueOf(Tips.mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
                segments.add(new TextSegment(time, 0xFFFFC107)); // 琥珀黄
            }

            if (!segments.isEmpty()) {
                float totalWidth = 0;
                for (TextSegment seg : segments) {
                    totalWidth += Tips.mc.textRenderer.getWidth(seg.text);
                }
                float startX = screenWidth / 2 - totalWidth / 2;
                float currentX = startX;
                int y = centerY - this.potionYOffset.getValueInt();
                for (TextSegment seg : segments) {
                    drawContext.drawText(Tips.mc.textRenderer, seg.text, (int)currentX, y, seg.color, true);
                    currentX += Tips.mc.textRenderer.getWidth(seg.text);
                }
            }
        }

        if (this.armorDurability.getValue() && Tips.mc.player != null) {
            this.renderArmorWarning(drawContext, centerY);
        }
    }

    private void renderArmorWarning(DrawContext drawContext, int centerY) {
        PlayerEntity player = Tips.mc.player;
        if (player == null) return;

        int threshold = this.armorThreshold.getValueInt();
        boolean lowDurability = false;

        int[] slots = {3, 2, 1, 0};
        for (int slot : slots) {
            net.minecraft.item.ItemStack stack = player.getInventory().armor.get(slot);
            if (!stack.isEmpty() && stack.isDamageable()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamage();
                int remaining = maxDamage - currentDamage;
                int percent = (int)((float)remaining / maxDamage * 100);
                if (percent < threshold) {
                    lowDurability = true;
                    break;
                }
            }
        }

        if (lowDurability) {
            String text = "Armor durability too low!";
            int textColor = 0xFF5555;
            int x = mc.getWindow().getScaledWidth() / 2 - Tips.mc.textRenderer.getWidth(text) / 2;
            int y = centerY - this.armorYOffset.getValueInt();
            drawContext.drawText(Tips.mc.textRenderer, text, x, y, textColor, true);
        }
    }

    @EventListener
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player = event.getPlayer();
        if (this.popCounter.getValue()) {
            if (Astra.POP.popContainer.containsKey(player.getName().getString())) {
                int l_Count = Astra.POP.popContainer.get(player.getName().getString());
                if (l_Count == 1) {
                    if (player.equals((Object)Tips.mc.player)) {
                        this.sendMessage("\u00a7fYou\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
                    } else {
                        this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
                    }
                } else if (player.equals((Object)Tips.mc.player)) {
                    this.sendMessage("\u00a7fYou\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
                } else {
                    this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died after popping \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
                }
            } else if (player.equals((Object)Tips.mc.player)) {
                this.sendMessage("\u00a7fYou\u00a7r died.", player.getId());
            } else {
                this.sendMessage("\u00a7f" + player.getName().getString() + "\u00a7r died.", player.getId());
            }
        }
        if (this.deathCoords.getValue() && player == Tips.mc.player) {
            this.sendMessage("\u00a74You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
        }
    }

    @EventListener
    public void onTotem(TotemEvent event) {
        if (this.popCounter.getValue()) {
            PlayerEntity player = event.getPlayer();
            int l_Count = 1;
            if (Astra.POP.popContainer.containsKey(player.getName().getString())) {
                l_Count = Astra.POP.popContainer.get(player.getName().getString());
            }
            if (l_Count == 1) {
                if (player.equals((Object)Tips.mc.player)) {
                    this.sendMessage("\u00a7fYou\u00a7r popped \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
                } else {
                    this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rpopped \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
                }
            } else if (player.equals((Object)Tips.mc.player)) {
                this.sendMessage("\u00a7fYou\u00a7r popped \u00a7f" + l_Count + "\u00a7r totem.", player.getId());
            } else {
                this.sendMessage("\u00a7f" + player.getName().getString() + " \u00a7rhas popped \u00a7f" + l_Count + "\u00a7r totems.", player.getId());
            }
        }
    }

    public void sendMessage(String message, int id) {
        if (!Tips.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.sendMessageId("\u00a7f[\u00a73" + this.getName() + "\u00a7f] " + message, id);
                return;
            }
            CommandManager.sendMessageId(message, id);
        }
    }
}