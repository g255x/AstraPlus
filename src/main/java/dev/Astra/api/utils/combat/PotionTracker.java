package dev.Astra.api.utils.combat;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PotionTracker {
    private static final Map<Integer, List<PotionEffectInfo>> PLAYER_EFFECTS = new ConcurrentHashMap<>();

    private static class PotionEffectInfo {
        public RegistryEntry<StatusEffect> effect;
        public int level;
        public long expireTime;

        public PotionEffectInfo(RegistryEntry<StatusEffect> effect, int level, long expireTime) {
            this.effect = effect;
            this.level = level;
            this.expireTime = expireTime;
        }
    }

    public static void addEffect(int playerId, RegistryEntry<StatusEffect> effect, int level, long durationMs) {
        if (durationMs <= 0) return;
        long expireTime = System.currentTimeMillis() + durationMs;
        List<PotionEffectInfo> list = PLAYER_EFFECTS.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>());
        list.removeIf(info -> info.effect.equals(effect));
        list.add(new PotionEffectInfo(effect, level, expireTime));
    }

    public static int getStrengthLevel(PlayerEntity player) {
        if (player == null) return 0;
        List<PotionEffectInfo> effects = PLAYER_EFFECTS.get(player.getId());
        if (effects == null || effects.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        for (PotionEffectInfo info : effects) {
            if (info.expireTime < now) continue;
            if (info.effect.matches(StatusEffects.STRENGTH)) {
                return info.level;
            }
        }
        return 0;
    }

    public static int getResistanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        List<PotionEffectInfo> effects = PLAYER_EFFECTS.get(player.getId());
        if (effects == null || effects.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        for (PotionEffectInfo info : effects) {
            if (info.expireTime < now) continue;
            if (info.effect.matches(StatusEffects.RESISTANCE)) {
                return info.level;
            }
        }
        return 0;
    }

    public static long getStrengthRemainingSeconds(PlayerEntity player) {
        if (player == null) return 0;
        List<PotionEffectInfo> effects = PLAYER_EFFECTS.get(player.getId());
        if (effects == null || effects.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        for (PotionEffectInfo info : effects) {
            if (info.expireTime < now) continue;
            if (info.effect.matches(StatusEffects.STRENGTH)) {
                return (info.expireTime - now) / 1000;
            }
        }
        return 0;
    }

    public static long getResistanceRemainingSeconds(PlayerEntity player) {
        if (player == null) return 0;
        List<PotionEffectInfo> effects = PLAYER_EFFECTS.get(player.getId());
        if (effects == null || effects.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        for (PotionEffectInfo info : effects) {
            if (info.expireTime < now) continue;
            if (info.effect.matches(StatusEffects.RESISTANCE)) {
                return (info.expireTime - now) / 1000;
            }
        }
        return 0;
    }

    public static boolean hasStrength(PlayerEntity player) {
        return getStrengthLevel(player) > 0;
    }

    public static boolean hasResistance(PlayerEntity player) {
        return getResistanceLevel(player) > 0;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        PLAYER_EFFECTS.values().forEach(list -> {
            list.removeIf(info -> info.expireTime < now);
        });
        PLAYER_EFFECTS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void clear() {
        PLAYER_EFFECTS.clear();
    }

    public static void clearPlayer(int playerId) {
        PLAYER_EFFECTS.remove(playerId);
    }

    public static Map<Integer, List<PotionEffectInfo>> getEffects() {
        return PLAYER_EFFECTS;
    }
}