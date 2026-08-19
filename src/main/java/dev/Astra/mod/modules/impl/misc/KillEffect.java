package dev.Astra.mod.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.DeathEvent;
import dev.Astra.api.events.impl.Render3DEvent;
import dev.Astra.api.utils.math.MathUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.registry.Registries;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public class KillEffect extends Module {
    private static final Identifier KILL1_ID = Identifier.of("alien", "killeffect.kill1");
    private static final Identifier KILL2_ID = Identifier.of("alien", "killeffect.kill2");
    private static final Identifier KILL3_ID = Identifier.of("alien", "killeffect.kill3");
    private static final Identifier KILL4_ID = Identifier.of("alien", "killeffect.kill4");
    private static final Identifier KILL5_ID = Identifier.of("alien", "killeffect.kill5");
    public static final SoundEvent KILL1_SOUND = SoundEvent.of(KILL1_ID);
    public static final SoundEvent KILL2_SOUND = SoundEvent.of(KILL2_ID);
    public static final SoundEvent KILL3_SOUND = SoundEvent.of(KILL3_ID);
    public static final SoundEvent KILL4_SOUND = SoundEvent.of(KILL4_ID);
    public static final SoundEvent KILL5_SOUND = SoundEvent.of(KILL5_ID);
    private static final SoundEvent[] STREAK_SOUNDS = new SoundEvent[]{KILL1_SOUND, KILL2_SOUND, KILL3_SOUND, KILL4_SOUND, KILL5_SOUND};

    private final EnumSetting<KillEffectMode> mode;
    private final BooleanSetting lightning;
    private final BooleanSetting levelUp;
    private final SliderSetting lMaxPitch;
    private final SliderSetting lMinPitch;
    private final BooleanSetting trident;
    private final SliderSetting tMaxPitch;
    private final SliderSetting tMinPitch;
    private final SliderSetting factor;
    private final SliderSetting duration;
    private final SliderSetting scale;
    private final BooleanSetting flash;
    private final SliderSetting lightPillarDuration;
    private final SliderSetting lightPillarWidth;
    private final SliderSetting lightPillarHeight;
    private final SliderSetting lightPillarAlpha;
    private final BooleanSetting streakSound;
    private final SliderSetting streakResetTime;
    private final BooleanSetting neglectOneself;
    private int killStreak = 0;
    private long lastKillTime = 0L;
    private final List<DeathEffect> deathEffects = new ArrayList<>();
    private final List<LightPillarEffect> lightPillarEffects = new ArrayList<>();

    public static SoundEvent getStreakSound(int streak) {
        int index = Math.min(streak - 1, STREAK_SOUNDS.length - 1);
        return index < 0 ? STREAK_SOUNDS[0] : STREAK_SOUNDS[index];
    }

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, KILL1_ID, KILL1_SOUND);
        Registry.register(Registries.SOUND_EVENT, KILL2_ID, KILL2_SOUND);
        Registry.register(Registries.SOUND_EVENT, KILL3_ID, KILL3_SOUND);
        Registry.register(Registries.SOUND_EVENT, KILL4_ID, KILL4_SOUND);
        Registry.register(Registries.SOUND_EVENT, KILL5_ID, KILL5_SOUND);
    }

    public KillEffect() {
        super("KillEffect", Module.Category.Misc);
        this.mode = this.add(new EnumSetting<>("Mode", KillEffectMode.DEFAULT));
        this.lightning = this.add(new BooleanSetting("Lightning", true, () -> this.mode.getValue() == KillEffectMode.DEFAULT));
        this.levelUp = this.add(new BooleanSetting("LevelUp", true, () -> this.mode.getValue() == KillEffectMode.DEFAULT).setParent());
        this.lMaxPitch = this.add(new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffectMode.DEFAULT && this.levelUp.isOpen()));
        this.lMinPitch = this.add(new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffectMode.DEFAULT && this.levelUp.isOpen()));
        this.trident = this.add(new BooleanSetting("Trident", false, () -> this.mode.getValue() == KillEffectMode.DEFAULT).setParent());
        this.tMaxPitch = this.add(new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffectMode.DEFAULT && this.trident.isOpen()));
        this.tMinPitch = this.add(new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffectMode.DEFAULT && this.trident.isOpen()));
        this.factor = this.add(new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0, () -> this.mode.getValue() == KillEffectMode.DEFAULT));
        this.duration = this.add(new SliderSetting("Duration", 3.0, 1.0, 10.0, 0.1, () -> this.mode.getValue() == KillEffectMode.HereHasALowiqDie));
        this.scale = this.add(new SliderSetting("Scale", 1.0, 0.1, 3.0, 0.1, () -> this.mode.getValue() == KillEffectMode.HereHasALowiqDie));
        this.flash = this.add(new BooleanSetting("Flash", true, () -> this.mode.getValue() == KillEffectMode.HereHasALowiqDie));
        this.lightPillarDuration = this.add(new SliderSetting("PillarDuration", 0.4, 0.5, 5.0, 0.1, () -> this.mode.getValue() == KillEffectMode.LightPillar));
        this.lightPillarWidth = this.add(new SliderSetting("PillarWidth", 0.4, 0.3, 1.5, 0.05, () -> this.mode.getValue() == KillEffectMode.LightPillar));
        this.lightPillarHeight = this.add(new SliderSetting("PillarHeight", 3.0, 1.0, 5.0, 0.1, () -> this.mode.getValue() == KillEffectMode.LightPillar));
        this.lightPillarAlpha = this.add(new SliderSetting("PillarAlpha", 180.0, 50.0, 255.0, 5.0, () -> this.mode.getValue() == KillEffectMode.LightPillar));
        this.streakSound = this.add(new BooleanSetting("StreakSound", true));
        this.streakResetTime = this.add(new SliderSetting("StreakReset", 5.0, 1.0, 30.0, 1.0, () -> this.streakSound.getValue()));
        this.neglectOneself = this.add(new BooleanSetting("NeglectOneself", true));
        this.setChinese("击杀效果");
    }

    @Override
    public String getInfo() {
        return this.streakSound.getValue() && this.killStreak > 0 ? "Streak: " + this.killStreak : this.mode.getValue().name();
    }

    @EventListener
    public void onPlayerDeath(DeathEvent event) {
        PlayerEntity player;
        if (!nullCheck() && (player = event.getPlayer()) != null) {
            if (this.neglectOneself.getValue() && player == mc.player) {
                return;
            }
            if (this.streakSound.getValue()) {
                long currentTime = System.currentTimeMillis();
                if ((double)(currentTime - this.lastKillTime) > this.streakResetTime.getValue() * 1000.0) {
                    this.killStreak = 0;
                }
                ++this.killStreak;
                this.lastKillTime = currentTime;
            }

            if (this.mode.getValue() == KillEffectMode.DEFAULT) {
                if (this.streakSound.getValue() && this.killStreak >= 1) {
                    mc.world.playSound(mc.player, player.getX(), player.getY(), player.getZ(), getStreakSound(this.killStreak), SoundCategory.PLAYERS, 100.0F, 1.0F);
                }
                for (int i = 0; (double)i < this.factor.getValue(); ++i) {
                    this.doEffect(player);
                }
                return;
            }

            if (this.mode.getValue() == KillEffectMode.HereHasALowiqDie) {
                boolean exists = false;
                for (DeathEffect effect : this.deathEffects) {
                    if (Math.abs(effect.x - player.getX()) < 0.5 && Math.abs(effect.y - player.getY()) < 0.5 && Math.abs(effect.z - player.getZ()) < 0.5) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    this.deathEffects.add(new DeathEffect(player.getX(), player.getY() + 1.0, player.getZ(), System.currentTimeMillis(), this.killStreak));
                }
            }

            if (this.mode.getValue() == KillEffectMode.LightPillar) {
                this.lightPillarEffects.add(new LightPillarEffect(player.getX(), player.getY(), player.getZ(), System.currentTimeMillis(), this.killStreak));
            }
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (mc.world != null) {
            if (this.mode.getValue() == KillEffectMode.HereHasALowiqDie) {
                this.deathEffects.removeIf(effect -> (double)(System.currentTimeMillis() - effect.timestamp) > this.duration.getValue() * 1000.0);
            }
            if (this.mode.getValue() == KillEffectMode.LightPillar) {
                this.lightPillarEffects.removeIf(effect -> (double)(System.currentTimeMillis() - effect.timestamp) > this.lightPillarDuration.getValue() * 1000.0);
            }
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.world != null) {
            if (this.mode.getValue() == KillEffectMode.HereHasALowiqDie) {
                for (DeathEffect effect : this.deathEffects) {
                    if (this.streakSound.getValue() && !effect.soundPlayed && effect.streak >= 1) {
                        mc.world.playSound(mc.player, effect.x, effect.y - 1.0, effect.z, getStreakSound(effect.streak), SoundCategory.PLAYERS, 100.0F, 1.0F);
                        effect.soundPlayed = true;
                    }
                    if (!this.flash.getValue()) {
                        this.drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1.0F);
                    } else if (System.currentTimeMillis() / 200L % 2L == 0L) {
                        this.drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1.0F);
                    }
                }
            }

            if (this.mode.getValue() == KillEffectMode.LightPillar) {
                for (LightPillarEffect effect : this.lightPillarEffects) {
                    if (this.streakSound.getValue() && !effect.soundPlayed && effect.streak >= 1) {
                        mc.world.playSound(mc.player, effect.x, effect.y, effect.z, getStreakSound(effect.streak), SoundCategory.PLAYERS, 100.0F, 1.0F);
                        effect.soundPlayed = true;
                    }
                    this.drawLightPillar(event.matrixStack, effect);
                }
            }
        }
    }

    private void doEffect(PlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (this.lightning.getValue()) {
            LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
            lightningEntity.setPosition(x, y, z);
            lightningEntity.refreshPositionAfterTeleport(x, y, z);
            mc.world.spawnEntity(lightningEntity);
        }
        if (this.levelUp.getValue()) {
            mc.world.playSound(mc.player, x, y, z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 100.0F, MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat()));
        }
        if (this.trident.getValue()) {
            mc.world.playSound(mc.player, x, y, z, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 999.0F, MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat()));
        }
    }

    private void drawEffect(MatrixStack matrixStack, double x, double y, double z, float alpha) {
        matrixStack.push();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;
        matrixStack.translate(x - cameraX, y - cameraY, z - cameraZ);
        matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());
        double scaleValue = 0.0245 * this.scale.getValue();
        matrixStack.scale((float)scaleValue, (float)scaleValue, (float)scaleValue);
        Identifier yuanquanTexture = Identifier.of("alien", "killeffect/yuanquan.png");
        RenderSystem.setShaderTexture(0, yuanquanTexture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        this.drawTexture(matrixStack, -32.0F, -32.0F, 64.0F, 64.0F);
        Identifier jiantouTexture = Identifier.of("alien", "killeffect/jiantou.png");
        RenderSystem.setShaderTexture(0, jiantouTexture);
        this.drawTexture(matrixStack, -5.0F, -5.0F, 64.0F, 64.0F);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        matrixStack.pop();
    }

    private void drawTexture(MatrixStack matrices, float x, float y, float width, float height) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 0.0F);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 0.0F);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 1.0F);
        bufferBuilder.vertex(matrix, x, y, 0.0F).texture(0.0F, 1.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void drawLightPillar(MatrixStack matrixStack, LightPillarEffect effect) {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - effect.timestamp;
        double durationMs = this.lightPillarDuration.getValue() * 1000.0;
        double progress = (double)elapsed / durationMs;
        if (progress > 1.0) {
            progress = 1.0;
        }
        double easedProgress = 1.0 - Math.pow(1.0 - progress, 3.0);
        double shrinkFactor = 1.0 - easedProgress;
        double currentWidth = this.lightPillarWidth.getValue() * shrinkFactor;
        if (!(currentWidth < 0.01)) {
            double height = this.lightPillarHeight.getValue();
            int baseAlpha = (int)this.lightPillarAlpha.getValue();
            float fadeAlpha = (float)(1.0 - progress);
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            double x = effect.x - cameraPos.x;
            double y = effect.y - cameraPos.y;
            double z = effect.z - cameraPos.z;
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();
            double halfWidth = currentWidth / 2.0;
            double minX = x - halfWidth;
            double maxX = x + halfWidth;
            double minZ = z - halfWidth;
            double maxZ = z + halfWidth;
            int bottomAlpha = (int)((float)baseAlpha * fadeAlpha);
            int topAlpha = (int)((double)((float)baseAlpha * fadeAlpha) * 0.3);
            bufferBuilder.vertex(matrix, (float)minX, (float)y, (float)minZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)y, (float)minZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)(y + height), (float)minZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)(y + height), (float)minZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)y, (float)minZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)y, (float)maxZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)(y + height), (float)maxZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)(y + height), (float)minZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)y, (float)maxZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)y, (float)maxZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)(y + height), (float)maxZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)maxX, (float)(y + height), (float)maxZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)y, (float)maxZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)y, (float)minZ).color(255, 255, 255, bottomAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)(y + height), (float)minZ).color(30, 30, 30, topAlpha);
            bufferBuilder.vertex(matrix, (float)minX, (float)(y + height), (float)maxZ).color(30, 30, 30, topAlpha);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    public enum KillEffectMode {
        DEFAULT,
        HereHasALowiqDie,
        LightPillar
    }

    private static class DeathEffect {
        public final double x;
        public final double y;
        public final double z;
        public final long timestamp;
        public final int streak;
        public boolean soundPlayed = false;

        public DeathEffect(double x, double y, double z, long timestamp, int streak) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
            this.streak = streak;
        }
    }

    private static class LightPillarEffect {
        public final double x;
        public final double y;
        public final double z;
        public final long timestamp;
        public final int streak;
        public boolean soundPlayed = false;

        public LightPillarEffect(double x, double y, double z, long timestamp, int streak) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
            this.streak = streak;
        }
    }
}