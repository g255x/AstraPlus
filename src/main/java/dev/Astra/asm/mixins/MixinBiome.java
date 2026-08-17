/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.biome.Biome
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.Ambience;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(value={Biome.class})
public class MixinBiome {
    @Inject(method={"getFogColor"}, at={@At(value="HEAD")}, cancellable=true)
    private void hookGetFogColor(CallbackInfoReturnable<Integer> cir) {
        if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.dimensionColor.booleanValue) {
            Color color = Ambience.INSTANCE.dimensionColor.getValue();
            cir.setReturnValue(color.getRGB());
        }
    }
}

