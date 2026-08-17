/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.biome.BiomeEffects
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.Ambience;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(value={BiomeEffects.class})
public class MixinBiomeEffects {
    @Inject(method={"getSkyColor"}, at={@At(value="HEAD")}, cancellable=true)
    private void hookGetSkyColor(CallbackInfoReturnable<Integer> cir) {
        if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.sky.booleanValue) {
            Color sky = Ambience.INSTANCE.sky.getValue();
            cir.setReturnValue(sky.getRGB());
        }
    }
}

