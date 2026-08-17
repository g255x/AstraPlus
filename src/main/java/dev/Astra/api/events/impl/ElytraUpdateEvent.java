package dev.Astra.api.events.impl;

import net.minecraft.entity.LivingEntity;

/**
 * 模仿 LeavesHack 的 ElytraUpdateEvent
 * 当实体尝试更新鞘翅状态时触发
 */
public class ElytraUpdateEvent {
    private final LivingEntity entity;
    private boolean cancelled;

    public ElytraUpdateEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}