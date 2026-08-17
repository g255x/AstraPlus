package dev.Astra.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.Render3DEvent;
import dev.Astra.api.utils.combat.PotionTracker;
import dev.Astra.api.utils.math.MathUtil;
import dev.Astra.api.utils.render.ColorUtil;
import dev.Astra.api.utils.render.Render2DUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.hud.TextRadar;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NameTags extends Module {
    public static NameTags INSTANCE;

    private final SliderSetting maxPlayers = this.add(new SliderSetting("MaxPlayers", 12, 1, 50, 1));
    final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
    final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
    final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
    final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", false));
    final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", false).setParent());
    final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
    final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
    final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
    final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
    final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
    final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", true));
    final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
    final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
    final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
    final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen).setSuffix("m"));
    final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01).setSuffix("m"));

    final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(-197380, true)));
    final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(-16712452, true)).injectBoolean(true));
    final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(-3618616, true)).injectBoolean(true));
    final ColorSetting died = this.add(new ColorSetting("Died", new Color(-4980736, true)).injectBoolean(true));
    final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(-3618816, true)).injectBoolean(true));
    final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(1677721600, true)).injectBoolean(true));

    public final BooleanSetting potionGroup = this.add(new BooleanSetting("PotionSettings", true).setParent());
    public final SliderSetting effectRadius = this.add(new SliderSetting("EffectRadius", 1.5, 0.5, 4.0, 0.1, potionGroup::isOpen).setSuffix("m"));
    public final BooleanSetting showPotionLevel = this.add(new BooleanSetting("ShowPotionLevel", true, potionGroup::isOpen));

    final DecimalFormat df = new DecimalFormat("0.0");

    public NameTags() {
        super("NameTags", Module.Category.Render);
        this.setChinese("名字标签");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        PotionTracker.clear();
    }

    private String toRoman(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "";
        }
    }

    private String getEffectDisplayName(RegistryEntry<StatusEffect> effect) {
        if (effect.matches(StatusEffects.STRENGTH)) return "Strength";
        else if (effect.matches(StatusEffects.RESISTANCE)) return "Resistance";
        return "Unknown";
    }

    private int getEffectColor(RegistryEntry<StatusEffect> effect) {
        if (effect.matches(StatusEffects.STRENGTH)) return 0xFFFFC700;
        else if (effect.matches(StatusEffects.RESISTANCE)) return 0xFF9146F0;
        return 0xFFFFFFFF;
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!potionGroup.getValue()) return;
        if (!(event.getPacket() instanceof WorldEventS2CPacket packet)) return;
        if (packet.getEventId() != 2002) return;

        Vec3d splashPos = Vec3d.ofCenter(packet.getPos());
        PotionEntity potion = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof PotionEntity pe) {
                double d = e.getPos().squaredDistanceTo(splashPos);
                if (d < 16.0 && d < minDist) {
                    minDist = d;
                    potion = pe;
                }
            }
        }
        if (potion == null) return;

        ItemStack stack = potion.getStack();
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return;

        RegistryEntry<StatusEffect> effectToTrack = null;
        int baseDuration = 0;
        int level = 0;

        if (contents.matches(Potions.STRENGTH)) {
            effectToTrack = StatusEffects.STRENGTH;
            baseDuration = 3600;
            level = 1;
        } else if (contents.matches(Potions.LONG_STRENGTH)) {
            effectToTrack = StatusEffects.STRENGTH;
            baseDuration = 9600;
            level = 1;
        } else if (contents.matches(Potions.STRONG_STRENGTH)) {
            effectToTrack = StatusEffects.STRENGTH;
            baseDuration = 1800;
            level = 2;
        } else if (contents.matches(Potions.TURTLE_MASTER)) {
            effectToTrack = StatusEffects.RESISTANCE;
            baseDuration = 400;
            level = 3;
        } else if (contents.matches(Potions.LONG_TURTLE_MASTER)) {
            effectToTrack = StatusEffects.RESISTANCE;
            baseDuration = 800;
            level = 3;
        } else if (contents.matches(Potions.STRONG_TURTLE_MASTER)) {
            effectToTrack = StatusEffects.RESISTANCE;
            baseDuration = 400;
            level = 4;
        }

        if (effectToTrack == null || baseDuration <= 0) return;

        final RegistryEntry<StatusEffect> trackedEffect = effectToTrack;
        float radius = effectRadius.getValueFloat();

        for (PlayerEntity player : mc.world.getPlayers()) {
            Vec3d closestPoint = MathUtil.getClosestPointToBox(splashPos, player.getBoundingBox());
            double dist = splashPos.distanceTo(closestPoint);
            if (dist > radius) continue;
            double impact = 1.0 - dist / radius;
            if (impact < 0) impact = 0;
            int duration = (int) (impact * baseDuration);
            if (duration > 20) {
                long durationMs = duration * 50L;
                PotionTracker.addEffect(player.getId(), trackedEffect, level, durationMs);
            }
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (NameTags.mc.gameRenderer == null || mc.getCameraEntity() == null) return;
        PotionTracker.tick();

        Camera camera = NameTags.mc.gameRenderer.getCamera();
        RenderSystem.enableBlend();
        GL11.glDepthFunc(519);
        MatrixStack matrixStack = new MatrixStack();

        Vec3d cameraPos = camera.getPos();
        int max = (int) this.maxPlayers.getValue();
        List<PlayerEntity> playersToRender = Astra.THREAD.getPlayers().stream()
                .filter(player -> {
                    if (player == NameTags.mc.player && NameTags.mc.options.getPerspective().isFirstPerson()) return false;
                    if (!this.died.booleanValue && !player.isAlive()) return false;
                    if (!this.invisibleConfig.booleanValue && player.isInvisible()) return false;
                    return true;
                })
                .sorted(Comparator.comparingDouble(player -> {
                    Vec3d pos = player.getPos();
                    return cameraPos.squaredDistanceTo(pos.x, pos.y + player.getHeight() / 2.0, pos.z);
                }))
                .limit(max)
                .collect(Collectors.toList());

        for (PlayerEntity playerEntity : playersToRender) {
            String info = this.getNametagInfo(playerEntity);
            Vec3d renderPosition = MathUtil.getRenderPosition(playerEntity, event.tickDelta);
            double x = renderPosition.getX();
            double y = renderPosition.getY();
            double z = renderPosition.getZ();
            int width = NameTags.mc.textRenderer.getWidth(info);
            float hwidth = (float) width / 2.0f;
            this.renderInfo(info, hwidth, playerEntity, x, y, z, camera, matrixStack);
        }

        GL11.glDepthFunc(515);
        RenderSystem.disableBlend();
    }

    private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
        Vec3d pos = camera.getPos();
        double eyeY = y + entity.getHeight() + (entity.isSneaking() ? 0.4f : 0.43f) + this.heightConfig.getValueFloat();
        float scale = (float) (-0.025f * this.scaleConfig.getValueFloat() + (this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat() ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025f * this.scalingConfig.getValueFloat() : 0.0));
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - pos.getX(), eyeY - pos.getY() + ((scale / -0.025f - 1.0f) / 4.0f), z - pos.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(scale, scale, -1.0f);

        List<EffectSegment> effectSegments = new ArrayList<>();
        if (potionGroup.getValue()) {
            long now = System.currentTimeMillis();
            // 获取力量效果
            int strengthLevel = PotionTracker.getStrengthLevel(entity);
            long strengthSeconds = PotionTracker.getStrengthRemainingSeconds(entity);
            if (strengthLevel > 0 && strengthSeconds > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Strength ");
                if (showPotionLevel.getValue()) sb.append(toRoman(strengthLevel)).append(" ");
                sb.append(strengthSeconds).append("s");
                effectSegments.add(new EffectSegment(sb.toString().trim(), 0xFFFFC700));
            }
            // 获取抗性效果
            int resistanceLevel = PotionTracker.getResistanceLevel(entity);
            long resistanceSeconds = PotionTracker.getResistanceRemainingSeconds(entity);
            if (resistanceLevel > 0 && resistanceSeconds > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Resistance ");
                if (showPotionLevel.getValue()) sb.append(toRoman(resistanceLevel)).append(" ");
                sb.append(resistanceSeconds).append("s");
                effectSegments.add(new EffectSegment(sb.toString().trim(), 0xFF9146F0));
            }
        }

        float nameWidth = mc.textRenderer.getWidth(info);
        if (this.rectConfig.booleanValue) {
            float rectX = -nameWidth / 2 - 2;
            float rectY = -2;
            Render2DUtil.drawRect(matrices, rectX, rectY, nameWidth + 4, mc.textRenderer.fontHeight + 4, this.rectConfig.getValue());
        }
        this.drawWithShadow(matrices, info, -nameWidth / 2, 0, this.getNametagColor(entity));

        if (this.armorConfig.getValue()) {
            this.renderItems(matrices, entity, effectSegments);
        } else if (!effectSegments.isEmpty()) {
            float yOffset = mc.textRenderer.fontHeight + 2;
            float totalWidth = 0;
            for (EffectSegment seg : effectSegments) totalWidth += mc.textRenderer.getWidth(seg.text) + 4;
            if (!effectSegments.isEmpty()) totalWidth -= 4;
            float startX = -totalWidth / 2;
            for (EffectSegment seg : effectSegments) {
                this.drawWithShadow(matrices, seg.text, startX, yOffset, seg.color);
                startX += mc.textRenderer.getWidth(seg.text) + 4;
            }
        }

        matrices.pop();
    }

    private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        NameTags.mc.textRenderer.draw(info, x, y, color, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        immediate.draw();
    }

    private void renderItems(MatrixStack matrixStack, PlayerEntity player, List<EffectSegment> effectSegments) {
        List<ItemStack> displayItems = new ArrayList<>();
        if (!player.getOffHandStack().isEmpty()) displayItems.add(player.getOffHandStack());
        player.getInventory().armor.forEach(armorStack -> { if (!armorStack.isEmpty()) displayItems.add(armorStack); });
        if (!player.getMainHandStack().isEmpty()) displayItems.add(player.getMainHandStack());
        Collections.reverse(displayItems);
        float x = 0.0f;
        int n11 = 0;
        for (ItemStack itemStack : displayItems) {
            x -= 8.0f;
            if (itemStack.getEnchantments().getSize() > n11) n11 = itemStack.getEnchantments().getSize();
        }
        float y = this.offsetConfig.getValueFloat();
        for (ItemStack stack : displayItems) {
            GL11.glDepthFunc(519);
            if (this.drawItemConfig.getValue()) this.renderItemStack(matrixStack, stack, x, y + 1.0f);
            this.renderItemOverlay(matrixStack, stack, x, y + 2.5f);
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            if (this.durabilityConfig.getValue()) this.renderDurability(matrixStack, stack, x + 2.0f, y + 11.5f);
            if (this.enchantmentsConfig.getValue()) this.renderEnchants(matrixStack, stack, x + 2.0f, y + 7.0f);
            matrixStack.scale(2.0f, 2.0f, 2.0f);
            x += 16.0f;
            GL11.glDepthFunc(515);
        }

        if (effectSegments != null && !effectSegments.isEmpty()) {
            float nameY = y - 4.5f + this.enchantOffset(n11);
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            float totalWidth = 0;
            for (EffectSegment seg : effectSegments) totalWidth += mc.textRenderer.getWidth(seg.text) + 4;
            if (!effectSegments.isEmpty()) totalWidth -= 4;
            float drawX = -totalWidth / 2;
            for (EffectSegment seg : effectSegments) {
                float segWidth = mc.textRenderer.getWidth(seg.text);
                this.drawWithShadow(matrixStack, seg.text, drawX, nameY * 2.0f, seg.color);
                drawX += segWidth + 4;
            }
            matrixStack.scale(2.0f, 2.0f, 2.0f);
        }
    }

    private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        matrixStack.push();
        matrixStack.translate(x, y, 0.0f);
        matrixStack.translate(8.0f, 8.0f, 0.0f);
        matrixStack.scale(16.0f, 16.0f, 1.0E-8f);
        matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        DiffuseLighting.disableGuiDepthLighting();
        BakedModel model = mc.getItemRenderer().getModel(stack, NameTags.mc.world, null, 0);
        VertexConsumerProvider.Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
        mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 0xFF0000, OverlayTexture.DEFAULT_UV, model);
        i.draw();
        DiffuseLighting.enableGuiDepthLighting();
        matrixStack.pop();
    }

    private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        matrixStack.push();
        if (stack.getCount() != 1) {
            String string = String.valueOf(stack.getCount());
            this.drawWithShadow(matrixStack, string, x + 17.0f - mc.textRenderer.getWidth(string), y + 9.0f, -1);
        }
        if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
            int i = stack.getItemBarStep();
            int j = stack.getItemBarColor();
            float k = x + 2.0f;
            float l = y + 13.0f;
            Render2DUtil.drawRect(matrixStack, k, l, 13.0f, 2.0f, -16777216);
            Render2DUtil.drawRect(matrixStack, k, l, (float) i, 1.0f, j | 0xFF000000);
        }
        matrixStack.pop();
    }

    private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
        if (!itemStack.isDamageable()) return;
        int n = itemStack.getMaxDamage();
        int n2 = itemStack.getDamage();
        int durability = (int) ((float) (n - n2) / (float) n * 100.0f);
        this.drawWithShadow(matrixStack, durability + "%", x * 2.0f, y * 2.0f, ColorUtil.hslToColor((float) (n - n2) / (float) n * 120.0f, 100.0f, 50.0f, 1.0f).getRGB());
    }

    private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
        if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            this.drawWithShadow(matrixStack, "God", x * 2.0f, y * 2.0f, -3977663);
            return;
        }
        if (!itemStack.hasEnchantments()) return;
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
        float n2 = 0.0f;
        for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = enchantment.value().toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
                enchantString.append("\u00a7cVan");
            } else if (translatedName.contains("Bind")) {
                enchantString.append("\u00a7cBind");
            } else {
                int maxLen = lvl > 1 ? 2 : 3;
                if (translatedName.length() > maxLen) translatedName = translatedName.substring(0, maxLen);
                enchantString.append(translatedName);
                enchantString.append(lvl);
            }
            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0f, (y + n2) * 2.0f, -1);
            n2 -= 4.5f;
        }
    }

    private float enchantOffset(int n) {
        if (!this.enchantmentsConfig.getValue() || n <= 2) return 0.0f;
        return -2.0f - (n - 3) * 4.5f;
    }

    private String getNametagInfo(PlayerEntity player) {
        int totems;
        StringBuilder info = new StringBuilder();
        if (this.gamemodeConfig.getValue()) {
            if (player.isCreative()) info.append(Formatting.GOLD).append("[C] ");
            else if (player.isSpectator()) info.append(Formatting.GRAY).append("[I] ");
            else info.append(Formatting.BOLD).append("[S] ");
        }
        if (this.pingConfig.getValue()) {
            info.append(this.getEntityPing(player)).append("ms ").append(Formatting.RESET);
        }
        info.append(player.getName().getString()).append(" ");
        if (this.entityIdConfig.getValue()) {
            info.append("ID: ").append(player.getId()).append(" ");
        }
        if (this.healthConfig.getValue()) {
            double health = player.getHealth() + player.getAbsorptionAmount();
            Formatting hcolor = health > 18.0 ? Formatting.GREEN : (health > 16.0 ? Formatting.DARK_GREEN : (health > 12.0 ? Formatting.YELLOW : (health > 8.0 ? Formatting.GOLD : (health > 4.0 ? Formatting.RED : Formatting.DARK_RED))));
            info.append(hcolor).append(this.df.format(health)).append(" ");
        }
        if (this.totemsConfig.getValue() && (totems = Astra.POP.getPop(player)) > 0) {
            info.append(TextRadar.getPopColor(totems)).append(-totems).append(" ");
        }
        return info.toString().trim();
    }

    private String getEntityPing(PlayerEntity entity) {
        if (mc.getNetworkHandler() == null) return "\u00a77-1";
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerListEntry == null) return "\u00a77-1";
        int ping = playerListEntry.getLatency();
        Formatting color = ping >= 200 ? Formatting.RED : (ping >= 100 ? Formatting.YELLOW : Formatting.GREEN);
        return color.toString() + ping;
    }

    private int getNametagColor(PlayerEntity player) {
        if (this.friendConfig.booleanValue && player.getDisplayName() != null && Astra.FRIEND.isFriend(player))
            return this.friendConfig.getValue().getRGB();
        if (this.invisibleConfig.booleanValue && player.isInvisible())
            return this.invisibleConfig.getValue().getRGB();
        if (this.sneakingConfig.booleanValue && player.isSneaking())
            return this.sneakingConfig.getValue().getRGB();
        if (!player.isAlive()) return this.died.getValue().getRGB();
        return this.colorConfig.getValue().getRGB();
    }

    private static class EffectSegment {
        String text;
        int color;
        EffectSegment(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}