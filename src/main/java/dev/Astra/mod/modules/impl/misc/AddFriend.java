/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.util.hit.HitResult
 */
package dev.Astra.mod.modules.impl.misc;

import dev.Astra.Astra;
import dev.Astra.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class AddFriend
extends Module {
    public static AddFriend INSTANCE;

    public AddFriend() {
        super("AddFriend", Module.Category.Misc);
        this.setChinese("加好友");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        EntityHitResult entityHitResult;
        Entity entity;
        if (AddFriend.nullCheck()) {
            this.disable();
            return;
        }
        HitResult target = AddFriend.mc.crosshairTarget;
        if (target instanceof EntityHitResult && (entity = (entityHitResult = (EntityHitResult)target).getEntity()) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            Astra.FRIEND.friend(player);
        }
        this.disable();
    }
}

